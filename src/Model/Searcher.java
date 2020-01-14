package Model;

import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Double.parseDouble;

public class Searcher {

    private Ranker ranker;
    private Parse parser;
    private String postingPath;
    private HashMap<String,String> d_terms;
    private HashMap<String,String> d_docs;
    private HashSet<String> d_entities;
    private HashMap<String,HashMap<String,LinkedHashMap<String,Double>>> d_docsAndEntitiesForQuery;
    private boolean isSemantic;
    private double avgLength;
    ConcurrentHashMap<String,Stack<Pair<String, Integer>>> tempmap;

    public Searcher(boolean stem, String path, boolean isSemantic,Parse parser){
        ranker = new Ranker();
        this.parser = parser;
        if(stem) {
            postingPath = path+"//Stemming";
        }
        else{
            postingPath = path+"//noStemming";
        }
        d_terms = parser.getTermDicWithoutUpload();
        d_docs = parser.getDocDic(stem,path);
        d_entities = findEntities();
        this.isSemantic = isSemantic;
        setAvgLength();
        d_docsAndEntitiesForQuery = new HashMap<>();
        try {
            FileInputStream fis = new FileInputStream(new File(this.postingPath + "\\docsents\\docsents.ser"));
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            tempmap=(ConcurrentHashMap)inputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private HashSet<String> findEntities() {
        HashSet<String> entities = new HashSet<>();
        for (Map.Entry<String, String > terms : d_terms.entrySet()) {
            String term = terms.getKey();
            String[] words = term.split(" ");
            if(Character.isUpperCase(term.charAt(0)) && words.length>1){
                entities.add(term);
            }
        }
        return entities;
    }

    public void search(LinkedHashMap<String,String> queries) {
        //for every query that we get DO
        for (Map.Entry<String,String> query: queries.entrySet()) {
            //get the terms of the query
            String t = parser.parseQuery(query.getValue()).substring(1);
            if(isSemantic){
                t = semantic(t);
            }
            String[] terms = t.split(" ");
            HashMap<String,Integer> idf = new HashMap<>(); //***First thing I need for the ranker Term-->idf ****
            HashMap<String,HashMap<String,Integer>> tf= new HashMap<>(); //***Second thing I need for the ranker Term-->DocNO->Tf***
            HashMap<String,HashSet<String>> docs = new HashMap<>(); //***Third thing I need for the ranker Query-->allDocs***
            HashMap<String,Integer> docLengths; //***Fourth thing I need for the ranker Document-->maxTF***
            HashSet<String> docsForQuery = new HashSet<>();
            //for every term in the query
            // all terms of query are complete
            for (int i = 0; i < terms.length; i++) {
                String s = terms[i];
                if (!d_terms.containsKey(s)) {
                    continue;
                }
                String path = d_terms.get(s).split(",")[0];
                try {
                    File file = new File(path);
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String term;
                    while ((term = br.readLine()) != null) {
                        String[] termInfo = term.split("[\\[\\]]");
                        //we got the right term from the posting file
                        if (termInfo[0].equalsIgnoreCase(s)) {
                            int df=0;
                            //idf.put(s, Integer.parseInt(termInfo[1]));
                            HashMap<String, Integer> allTfs = new HashMap<>();
                            for (int j = 2; j < termInfo.length; j++) {
                                String[] doc = termInfo[j].split(",");
                                String docNO = doc[0];
                                if(!docNO.isEmpty()) {
                                    int docTF = Integer.parseInt(doc[1]);
                                    allTfs.put(docNO, docTF);
                                    docsForQuery.add(docNO);
                                    df++;
                                }
                            }
                            idf.put(s, df);
                            tf.put(s, allTfs);
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            docLengths = getLengthofDocs(docsForQuery);
            HashMap<String,Double> rankedDocs = ranker.rank(terms,d_docs.size(),idf,tf,docsForQuery,docLengths,avgLength);
            d_docsAndEntitiesForQuery.put(query.getKey(),getEntities(rankedDocs));
        }
    }

    public HashMap<String, HashMap<String, LinkedHashMap<String, Double>>> getDocsAndEntitiesForQuery() {
        return d_docsAndEntitiesForQuery;
    }

    public void writeQueriesResults() {
        try{
            File termPostingFile = new File(this.postingPath+"\\results.txt");
            termPostingFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(termPostingFile));
            for (Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>> info: d_docsAndEntitiesForQuery.entrySet()) {
                String queryNum = info.getKey();
                HashSet<String> docs = new HashSet<>(info.getValue().keySet());
                for (String doc:docs) {
                    writer.write(queryNum+" 0 "+doc+" 0 42.38 mt\n");
                }
            }
            writer.close();

        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    private HashMap<String, LinkedHashMap<String, Double>> getEntities(HashMap<String, Double> rankedDocs) {
        HashMap<String,LinkedHashMap<String,Double>> docEntities = new HashMap<>();
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
                        //String corpusFile = docInfo[4];
                        //String docPath = corpusPath+ "\\" + corpusFile;
                        //LinkedHashMap<String,Integer>ents = readFile.readDoc(docPath,docNo,d_entities);
                        int maxTf=Integer.parseInt(docInfo[1]);
                        Stack entStack=tempmap.get(docInfo[0]);
                        LinkedHashMap<String,Double>ents = new LinkedHashMap<>();
                        if(entStack!=null) {
                            while (!entStack.isEmpty()) {
                                Pair<String, Integer> tempEnt = (Pair<String, Integer>) entStack.pop();
                                ents.put(tempEnt.getKey(), (((double) tempEnt.getValue()) / maxTf));
                            }
                        }
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

    public String semantic(String query){
        try {
            Word2VecModel model = Word2VecModel.fromTextFile(new File("Resource/word2vec.c.output.model.txt"));
            com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
            int numOfResultInList = 2;
            String[] terms = query.split(" ");
            StringBuilder queryBuilder = new StringBuilder(query);
            for (String term: terms) {
                List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(term, numOfResultInList);
                for (com.medallia.word2vec.Searcher.Match match : matches) {
                    if(match.distance()>0.97){
                        queryBuilder.append(" ").append(match.match());
                    }
                }
            }
            query = queryBuilder.toString();
            return query;

        } catch (IOException | com.medallia.word2vec.Searcher.UnknownWordException e){
            e.printStackTrace();
            return null;
        }

    }

    private void setAvgLength() {
        try {
            File file = new File(this.postingPath + "\\avg\\avg.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String avgfile;
            double avg=0;
            while ((avgfile = br.readLine()) != null) {
                avg = parseDouble(avgfile);
            }
            this.avgLength = avg;
        } catch (IOException e) {
            e.printStackTrace();
        }
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
