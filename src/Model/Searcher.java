package Model;

import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Double.parseDouble;

/**
 * Class is in charge of searching for the top 50 relevant documents according
 * to the user's queries and the given index.
 * The class contains a Ranker instance, a Parser instance, the path to the posting files,
 * d_terms- terms dictionary, d_docsAndEntitiesForQuery- a database that holds the top 50 documents according to their rate
 * and their top 5 entities according to appearence inside them, d_docLength- a database that holds all the documents lengths,
 * d_docmaxTF- holds the number of times the most frequent term appeared in each document, isSemantic-
 * hold whether or not we should use semantics when ranking the documents, avgLength- average document length,
 * d_docEntities-a database that holds the top 5 frequent entities that appears in
 * all the documents in the index documents posting, isStem- whether or not we should stem the query
 */
public class Searcher {

    private Ranker ranker;
    private Parse parser;
    private String postingPath;
    private HashMap<String,String> d_terms;
    private HashMap<String,HashMap<String,LinkedHashMap<String,Double>>> d_docsAndEntitiesForQuery;
    private HashMap<String,Integer>d_docLength;
    private HashMap<String,Integer> d_docmaxTF;
    private boolean isSemantic;
    private double avgLength;
    private HashMap<String,HashMap<String,Integer>> d_docEntities;
    private boolean isStem;

    /**
     * Constructor initializes some of the databases
     * @param stem indicates whether we should stem or not
     * @param path the path to the posting files
     * @param isSemantic indicates whether we should use semantics
     * @param parser the parser we will use
     */
    public Searcher(boolean stem, String path, boolean isSemantic,Parse parser){
        ranker = new Ranker();
        isStem = stem;
        this.parser = parser;
        if(stem) {
            postingPath = path+"//Stemming";
        }
        else{
            postingPath = path+"//noStemming";
        }
        d_terms = parser.getTermDicWithoutUpload();
        fillDictionaries();
        fillEntities();
        this.isSemantic = isSemantic;
        setAvgLength();
        d_docsAndEntitiesForQuery = new HashMap<>();

    }

    /**
     * Method fills from posting files the 5 most frequent entities that appear
     * in each document
     */
    private void fillEntities() {
        try {
            File file = new File(this.postingPath + "/docsents/docsents.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            d_docEntities = new HashMap<>();
            String term;
            while ((term = br.readLine()) != null) {
                String[] info = term.split(",");
                HashMap<String,Integer> entities = new HashMap<>();
                for(int i=1;i<info.length;i=i+2){
                    String entity = info[i];
                    int rank = Integer.parseInt(info[i+1]);
                    entities.put(entity,rank);
                }
                d_docEntities.put(info[0],entities);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Method fills from posting files the length of each document and the number of times the
     * most frequent term appeared in each document
     */
    private void fillDictionaries() {
        d_docLength = new HashMap<>();
        d_docmaxTF = new HashMap<>();
        File file = new File(this.postingPath+"\\DocumentsPosting");
        File[] files = file.listFiles();
        for (File curr : files) {
            File[] docfiles = curr.listFiles();
            for(File docfile : docfiles){
                try {
                    BufferedReader br = new BufferedReader(new FileReader(docfile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] seperated=line.split(",");
                        d_docLength.put(seperated[0],Integer.parseInt(seperated[3]));
                        d_docmaxTF.put(seperated[0],Integer.parseInt(seperated[1]));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method recieves a database filled with the user's queries and fills the database
     * that holds the top 50 documents relevant to each query
     * @param queries
     */
    public void search(LinkedHashMap<String,String> queries) {
        //for every query that we get DO
        for (Map.Entry<String,String> query: queries.entrySet()) {
            //get the terms of the query
            String t = parser.parseQuery(query.getValue(),isStem).substring(1);
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
                String path;
                if (d_terms.containsKey(s.toLowerCase())) {
                    path = d_terms.get(s.toLowerCase()).split(",")[0];
                }
                else if(d_terms.containsKey(s.toUpperCase())){
                    path = d_terms.get(s.toUpperCase()).split(",")[0];
                }
                else continue;
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
                    System.out.println(path);
                    System.out.println(terms[i]);
                    e.printStackTrace();
                }
            }
            docLengths = getLengthofDocs(docsForQuery);
            HashMap<String,Double> rankedDocs = ranker.rank(terms,d_docLength.size(),idf,tf,docsForQuery,docLengths,avgLength);
            d_docsAndEntitiesForQuery.put(query.getKey(),getEntities(rankedDocs));
        }
    }

    /**
     * Method retuns the database that holds the top 50 documents for each query
     * @return top 50 documents for each query
     */
    public HashMap<String, HashMap<String, LinkedHashMap<String, Double>>> getDocsAndEntitiesForQuery() {
        return d_docsAndEntitiesForQuery;
    }

    /**
     * Method writes the queries results to the given path
     * @param path path to where we want to save our results
     */
    public void writeQueriesResults(String path) {
        try{
            // Create a list from elements of HashMap
            List<Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>>> list = new LinkedList<>(d_docsAndEntitiesForQuery.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>>>() {
                public int compare(Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>> o1,
                                   Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>> o2)
                {
                    if(Integer.parseInt(o1.getKey())<Integer.parseInt(o2.getKey())){
                        return -1;
                    }
                    if(Integer.parseInt(o1.getKey())==Integer.parseInt(o2.getKey())){
                        return 0;
                    }
                    else return 1;
                }
            });

            // put data from sorted list to hashmap (the first 50)
            HashMap<String, HashMap<String, LinkedHashMap<String, Double>>> temp = new LinkedHashMap<>();
            for (Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>> doc : list) {
                    temp.put(doc.getKey(), doc.getValue());
            }
            File termPostingFile = new File(path+"\\results.txt");
            termPostingFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(termPostingFile));
            for (Map.Entry<String, HashMap<String, LinkedHashMap<String, Double>>> info: temp.entrySet()) {
                String queryNum = info.getKey();
                HashSet<String> docs = new HashSet<>(info.getValue().keySet());
                for (String doc:docs) {
                    writer.write(queryNum+" 0 "+doc+" 0 42.38 mt\n");
                }
            }
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method gets a list of file and returns the top 5 most frequent entities that appeared in each document
     * @param rankedDocs
     * @return top 5 most frequent entities that appeared in each document
     */
    private HashMap<String, LinkedHashMap<String, Double>> getEntities(HashMap<String, Double> rankedDocs) {
        HashMap<String, LinkedHashMap<String, Double>> docEntities = new HashMap<>();
        for (Map.Entry<String, Double> docs : rankedDocs.entrySet()) {
            String docNo = docs.getKey();
            int maxTf = d_docmaxTF.get(docNo);
            HashMap<String,Integer> entStack = d_docEntities.get(docNo);
            LinkedHashMap<String,Double> ents = new LinkedHashMap<>();
            if(entStack!=null && !entStack.isEmpty()) {
                for (Map.Entry<String, Integer> entry : entStack.entrySet()) {
                    String name = entry.getKey();
                    double rank = (double) entry.getValue() / maxTf;
                    ents.put(name, rank);
                }
            }
            docEntities.put(docNo, ents);
        }
        return docEntities;
    }

    /**
     * Method accepts a query and adds words to it that resembles to the query words semantically
     * @param query
     * @return modified query
     */
    public String semantic(String query){
        try {
            Word2VecModel model = Word2VecModel.fromTextFile(new File("Resource/word2vec.c.output.model.txt"));
            com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();
            int numOfResultInList = 2;
            String[] terms = query.split(" ");
            StringBuilder queryBuilder = new StringBuilder(query);
            for (String term: terms) {
                if (d_terms.containsKey(term) || d_terms.containsKey(term.toUpperCase()) || d_terms.containsKey(term.toLowerCase())) {
                    try {
                        List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(term, numOfResultInList);
                    int count =0;
                    for (com.medallia.word2vec.Searcher.Match match : matches) {
                        if (match.distance() > 0.9) {
                            count++;
                            if(count>1) {
                                queryBuilder.append(" ").append(match.match());
                            }
                        }
                    }
                    }catch (com.medallia.word2vec.Searcher.UnknownWordException e){
                        continue;
                    }
                }
            }
            query = queryBuilder.toString();
            return query;

        } catch (IOException e){
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Method accesses the posting files and sets the average document length
     */
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

    /**
     * Method documents and returns each document length
     * @param docsForQuery
     * @return each document length
     */
    private HashMap<String, Integer> getLengthofDocs(HashSet<String> docsForQuery) {
            HashMap<String,Integer> docLength = new HashMap<>();
            for (String docNo: docsForQuery) {
                docLength.put(docNo,d_docLength.get(docNo));
            }
            return docLength;
    }
}
