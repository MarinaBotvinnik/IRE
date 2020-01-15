
package Model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * Class that in charge of taking the requests from the ViewModel layer and implements them in the proper classes in the Model layer. 
 */
public class Model {
    private ReadFile readFile;
    private Searcher searcher;

    /**
     * Constructor of the class that initializes the readFile class with the stem value
     * @param isStem - - true if stem needed, false otherwise
     */
    public Model(boolean isStem, String path) {
        readFile = new ReadFile(isStem,path);
    }

    /**
     * Method that sends a request to the readFile to uploads the correct dictionary from the parameters
     * @param stem - Stemming/noStemming dictionary
     * @param path - the path to the posting files
     * @return - the dictionary of the terms.
     */
    public LinkedHashMap<String,String> uploadDictionary(boolean stem, String path){
        return readFile.upload(stem,path);
    }

    /**
     * Method that sets the Stem value in the ReadFile
     * @param isStem - true if stem needed, false otherwise
     */
    public void setStem(boolean isStem) {
        readFile.setIndexerStem(isStem);
    }

    /**
     /**
     * Method that sends a request to the ReadFile to start the posting process
     * and sets the path to the posting files in the indexer
     * @param corpusPath - the path where the corpus is
     * @param postingPath - the path where to put the posting
     */
    public void startPosting(String corpusPath, String postingPath){
        readFile.setIndexerPath(postingPath);
        readFile.readFile(corpusPath);
    }

    /**
     * Method recieves the path to the posting file and sets the indexer path (where it saves
     * its indexed value)accordingly
     * @param postingPath
     */
    public void setIndexerPath(String postingPath){
        readFile.setIndexerPath(postingPath);
    }

    /**
     * returns number of documents in the corpus
     * @return
     */
    public int getNumOfDocs(){
        return readFile.getNumOfDocs();
    }
    /**
     * returns number of terms in the corpus
     * @return
     */
    public int getNumOfTerm(){
        return readFile.getNumOfTerm();
    }

    /**
     * Method that sends a request to the ReadFile to reset the System.
     */
    public void reset(){
        readFile.reset();
    }

    /**
     * Method recieves a list of queries, a stem indicator, a semantic indicator and the path to the
     * posting files, it then creates an new searcher and activates its searching function
     * @param queries list of queries
     * @param isStem indicates if the stemmer was activated
     * @param isSemantic indicates whether to use semantics
     * @param postingPath the path to the posting file
     */
    public void search(LinkedHashMap<String,String> queries, boolean isStem, boolean isSemantic, String postingPath) {
        searcher = new Searcher(isStem,postingPath,isSemantic,readFile.getParser());
        searcher.search(queries);
    }

    /**
     * Method returns the document that were returned from the sent queries
     * @return documents
     */
    public HashMap<String, HashMap<String, LinkedHashMap<String, Double>>> getAnswers() {
        return searcher.getDocsAndEntitiesForQuery();
    }

    /**
     * Method writes the queries results to the given path
     * @param path
     */
    public void writeAns(String path){
        searcher.writeQueriesResults(path);
    }
}