package Model;

import java.util.*;

/**
 * This class is in charge of rating the documents according to the users query
 * The class contains an instance of BM25
 */
public class Ranker {
    BM25 bmFunction;

    /**
     * Constructor initializes BM25 instance
     */
    public Ranker(){
        bmFunction = new BM25();
    }

    /**
     * Method accepts a list of documents and a list of terms which appear in the query and returns a list
     * of 50 highest ranked documents
     * @param terms array of terms which appears in the query
     * @param allDocs the number of all the documents we have in the corpus
     * @param idfs the amount of documents each terms appears in
     * @param tf the amount of time the terms appear in each document it appeared in
     * @param docsForQuery the potential documents relevant for the user's query
     * @param docLengths the length of each document
     * @param avgLength the average document length
     * @return the top 50 documents according to their rating
     */
    public HashMap<String,Double> rank(String[] terms,int allDocs,HashMap<String, Integer> idfs, HashMap<String, HashMap<String, Integer>> tf, HashSet<String> docsForQuery, HashMap<String, Integer> docLengths, double avgLength) {
        HashMap<String,Double> final50 = new HashMap<>(); //this is the 50 most relevant docs
        HashMap<String,Double> allRankings = new HashMap<>();
        for (String doc: docsForQuery) {
            int docSize = docLengths.get(doc);
            int[] idf = new int[terms.length];
            int[] tfs = new int[terms.length];
            for(int i=0; i<terms.length;i++){
                if(idfs.containsKey(terms[i])) {
                    idf[i] = idfs.get(terms[i]);
                }
                if(tf.containsKey(terms[i]) && tf.get(terms[i]).containsKey(doc)) {
                    tfs[i] = tf.get(terms[i]).get(doc);
                }
            }
            double ranking = bmFunction.rank(allDocs,docSize,avgLength,terms,tfs,idf);
            allRankings.put(doc,ranking);
        }
        final50 = getTop50(allRankings);
        return final50;
    }

    /**
     * Method accepts a list of documents and their ratings and returns the top 50 documents according to their rating
     * @param allRankings a database that holds all the potential documents and their ratings
     * @return a database that holds the top 50 documents and their ratings.
     */
    private HashMap<String, Double> getTop50(HashMap<String, Double> allRankings) {
        // Create a list from elements of HashMap
        List<HashMap.Entry<String, Double> > list = new LinkedList<>(allRankings.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2)
            {
                return -(o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap (the first 50)
        HashMap<String, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Double> doc : list) {
            if(temp.size()<50) {
                temp.put(doc.getKey(), doc.getValue());
            }
        }

        return temp;
    }
}
