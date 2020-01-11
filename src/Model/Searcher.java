package Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Searcher {

    private Ranker ranker;
    private Parse parser;
    private ReadFile readFile;
    private String postingPath;
    private String corpusPath;
    private HashMap<String,String> d_terms;
    private HashMap<String,String> d_docs;
    private HashSet<String> d_entities;
    private HashMap<String,HashMap<String,LinkedHashMap<String,Integer>>> d_docsAndEntitiesForQuery;
    private boolean isStem;
    private boolean isSemantic;
    private double avgLength;

    public Searcher(boolean stem, String path, boolean isSemantic, String corpusPath){
        ranker = new Ranker();
        parser = new Parse(stem);
        postingPath = path;
        isStem = stem;
        readFile = new ReadFile(stem);
        this.corpusPath = corpusPath;
        d_terms = parser.getTermDic(stem,path);
        d_docs = parser.getDocDic(stem,path);
        d_entities = findEntities();
        this.isSemantic = isSemantic;
        setAvgLength();
    }

    private HashSet<String> findEntities() {
        HashSet<String> entities = new HashSet<>();
        for (Map.Entry<String, String > terms : d_terms.entrySet()) {
            String term = terms.getKey();
            if(Character.isUpperCase(term.indexOf(0))){
                entities.add(term);
            }
        }
        return entities;
    }

    public void search(LinkedHashSet<String> queries) {
        //for every query that we get DO
        for (String query: queries) {
            //get the terms of the query
            String t = parser.parseQuery(query);
            String[] terms = t.split(" ");
            HashMap<String,Integer> idf = new HashMap<>(); //***First thing I need for the ranker Term-->idf ****
            HashMap<String,HashMap<String,Integer>> tf= new HashMap<>(); //***Second thing I need for the ranker Term-->DocNO->Tf***
            HashMap<String,HashSet<String>> docs = new HashMap<>(); //***Third thing I need for the ranker Query-->allDocs***
            HashMap<String,Integer> docLengths = new HashMap<>(); //***Fourth thing I need for the ranker Document-->maxTF***
            HashSet<String> docsForQuery = new HashSet<>();
            //for every term in the query
            // all terms of query are complete
            for (int i = 0; i < terms.length; i++) {
                String s = terms[i];
                if (!d_terms.containsKey(s)) {
                    continue;
                }
                String path = d_terms.get(s);
                try {
                    File file = new File(path);
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String term;
                    while ((term = br.readLine()) != null) {
                        String[] termInfo = term.split("[\\[\\]]");
                        //we got the right term from the posting file
                        if (termInfo[0].equals(s)) {
                            idf.put(term, Integer.parseInt(termInfo[1]));
                            HashMap<String, Integer> allTfs = new HashMap<>();
                            for (int j = 2; j < termInfo.length; j++) {
                                String[] doc = termInfo[j].split(",");
                                String docNO = doc[0];
                                int docTF = Integer.parseInt(doc[1]);
                                allTfs.put(docNO, docTF);
                                docsForQuery.add(docNO);
                            }
                            tf.put(term, allTfs);
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
            docLengths = getLengthofDocs(docsForQuery);
            HashMap<String,Double> rankedDocs = ranker.rank(terms,d_docs.size(),idf,tf,docsForQuery,docLengths,avgLength);
            d_docsAndEntitiesForQuery.put(query,getEntities(rankedDocs));
        }
    }

    public HashMap<String, HashMap<String, LinkedHashMap<String, Integer>>> getDocsAndEntitiesForQuery() {
        return d_docsAndEntitiesForQuery;
    }

    private HashMap<String,LinkedHashMap<String,Integer>> getEntities(HashMap<String, Double> rankedDocs) {
        HashMap<String,LinkedHashMap<String,Integer>> docEntities = new HashMap<>();
        for (Map.Entry<String, Double > docs : rankedDocs.entrySet()) {
            String docNo = docs.getKey();
            String path = d_docs.get(docNo);
            try {
                File file = new File(path);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String doc;
                while ((doc = br.readLine()) != null){
                    String[] docInfo = doc.split(",");
                    //we got the right doc from the posting file
                    if(docInfo[0].equals(docNo)){
                        //NEED TO ADD WHERE THE DOCUMENT IN THE CORPUS
                        String corpusFile = docInfo[4];
                        String docPath = corpusPath+ "\\" + corpusFile;
                        LinkedHashMap<String,Integer>ents = readFile.readDoc(docPath,docNo,d_entities);
                        docEntities.put(docNo,ents);
                    }
                }
            }
            catch (IOException e){
                e.getStackTrace();
            }
        }
        return docEntities;
    }


    ///////CHANGEEEEE///////
    private void setAvgLength(){
        this.avgLength=10;
    }

    private HashMap<String, Integer> getLengthofDocs(HashSet<String> docsForQuery) {
        try{
            HashMap<String,Integer> docLength = new HashMap<>();
            for (String docNo: docsForQuery) {
                String path = d_docs.get(docNo);
                File file = new File(path);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String doc;
                while ((doc = br.readLine()) != null){
                    String[] docInfo = doc.split(",");
                    if(docInfo[0].equals(docNo)){
                        //NEED TO CHANGE THAT TO THE REAL LENGTH
                        int length = Integer.parseInt(docInfo[3]);
                        docLength.put(docNo,length);
                        break;
                    }
                }
            }
            return docLength;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
}
