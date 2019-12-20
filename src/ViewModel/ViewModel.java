
package ViewModel;
import Model.Model;

import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * Class that controls between the view and the model lairs
 */
public class ViewModel {
    private Model model;
    private LinkedHashMap<String ,String> dictionary;

    /**
     * Constructor of the class
     */
    public ViewModel()
    {
        dictionary = new LinkedHashMap<>();
        model=null;
    }

    /**
     * Method that sends a request to the Model to uploads the correct dictionary from the parameters
     * @param stem - Stemming/noStemming dictionary
     * @param path - the path to the posting files
     */
    public void uploadDictionary(boolean stem,String path){
        if(model==null) {
            model = new Model(stem);
        }
        dictionary = model.uploadDictionary(stem, path);
    }

    /**
     * Method returns the dictionary
     * @return
     */
    public LinkedHashMap<String,String> getDictionary(){
        return dictionary;
    }

    /**
     * Method that sets the Stem value in the Model lair
     * @param isStem - true if stem needed, false otherwise
     */
    public void setStem(boolean isStem) {
        model.setStem(isStem);
    }

    /**
     * Method that sends a request to the Model lair to start the posting process
     * @param corpusPath - the path where the corpus is
     * @param PostingPath - the path where to put the posting 
     * @param isStem - true if stem needed, false otherwise
     */
    public void start(String corpusPath,String PostingPath, boolean isStem){
        this.model = new Model(isStem);
        setStem(isStem);
        model.startPosting(corpusPath,PostingPath);
    }

    /**
     * returns number of documents in the corpus
     * @return
     */
    public int getNumOfDocs(){
        return model.getNumOfDocs();
    }

    /**
     * returns number of terms in the corpus
     * @return
     */
    public int getNumOfTerm(){
        return model.getNumOfTerm();
    }

    /**
     * Method that sends a request to the model to reset the System.
     */
    public void reset(){
        model.reset();
    }
}