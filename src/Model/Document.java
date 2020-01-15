package Model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that represents a document with the following definitions:
 * name of the document
 * the word with most appearences
 * the num value of most appearences
 * all words in this document and their frequences
 */
public class Document {

    private int max_tf;
    private String max_Term_name;
    private ConcurrentHashMap<String,Integer> term_frq;
    private int uniqueTermsNum;
    private int length;
    private String doc_name;

    /**
     * Constructor of the document
     * @param doc_name - the doc Id
     */
    public Document(String doc_name) {
        max_tf =0;
        max_Term_name = null;
        term_frq = new ConcurrentHashMap<>();
        uniqueTermsNum =0;
        length = 0;
        this.doc_name = doc_name;
    }


    /**
     * getter to the doc ID
     * @return
     */
    public String getDocName(){
        synchronized (this) {
            return doc_name;
        }
    }

    /**
     * getter to the doc number of the unique words
     * @return
     */
    public int getUniqueTermsNum(){
        synchronized (this) {
            return uniqueTermsNum;
        }
    }

    /**
     * getter to the max tf of the doc
     * @return
     */
    public int getMax_tf() {
        synchronized (this) {
            return max_tf;
        }
    }
    /**
     * getter to the name with the most frequency
     * @return
     */
    public String getMax_Term_name() {
        synchronized (this) {
            return max_Term_name;
        }
    }

    /**
     * Method adds a word to the dictionary and updates all the parameters according to it
     * @param term - the new word
     */
    public void addTerm(String term){
        synchronized (this) {
            length++;
            String lowerTerm = term.toLowerCase();
            if (term_frq.containsKey(lowerTerm)) {
                term_frq.replace(lowerTerm, term_frq.get(lowerTerm) + 1);
            } else term_frq.put(lowerTerm, 1);
            if (term_frq.get(lowerTerm) > this.max_tf) {
                this.max_tf = term_frq.get(lowerTerm);
                max_Term_name = lowerTerm;
            }
        }
    }

    /**
     * Method adds a word with its Tf to this document to the dictionary and updates all the parameters according to it
     * @param term - the new word
     * @param tf - thw words tf
     */
    public void addTermWithTF(String term, int tf){
        synchronized (this) {
            String lowerTerm = term.toLowerCase();
            if (term_frq.containsKey(lowerTerm)) {
                term_frq.replace(lowerTerm, tf);
            } else term_frq.put(lowerTerm, tf);
            if (term_frq.get(lowerTerm) > this.max_tf) {
                this.max_tf = term_frq.get(lowerTerm);
                max_Term_name = lowerTerm;
            }
        }
    }

    /**
     * Getter got the length of the document
     * @return length of the doc
     */
    public int getLength() {
        return length;
    }

    /**
     * Method called when the parser is done with the whole document, it's updates the number of the unique words.
     */
    public void closeDoc(){
        synchronized (this) {
            uniqueTermsNum = term_frq.size();
        }
    }
}