package Model;

import java.util.*;

public class Ranker {
    BM25 bmFunction;

    public Ranker(){
        bmFunction = new BM25();
    }

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
