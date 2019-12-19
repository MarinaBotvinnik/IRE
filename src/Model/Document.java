package Model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private HashMap<String,Integer> term_frq;
    private int uniqueTermsNum;
    private String doc_name;

    /**
     * Constructor of the docu
     * @param doc_name
     */
    public Document(String doc_name) {
        max_tf =0;
        max_Term_name = null;
        term_frq = new HashMap<>();
        uniqueTermsNum =0;
        this.doc_name = doc_name;
    }

    public String getDocName(){
        return doc_name;
    }

    public int getUniqueTermsNum(){
        return uniqueTermsNum;
    }

    public int getMax_tf() {
        return max_tf;
    }

    public String getMax_Term_name() {
        return max_Term_name;
    }

    public void addTerm(String term){
        String lowerTerm = term.toLowerCase();
        if(term_frq.containsKey(lowerTerm)){
            term_frq.replace(lowerTerm,term_frq.get(lowerTerm)+1);
        }
        else term_frq.put(lowerTerm,1);
        if(term_frq.get(lowerTerm)>this.max_tf){
            this.max_tf=term_frq.get(lowerTerm);
            max_Term_name=lowerTerm;
        }
    }

    public void addTermWithTF(String term, int tf){
        String lowerTerm = term.toLowerCase();
        if(term_frq.containsKey(lowerTerm)){
            term_frq.replace(lowerTerm,tf);
        }
        else term_frq.put(lowerTerm,tf);
        if(term_frq.get(lowerTerm)>this.max_tf){
            this.max_tf=term_frq.get(lowerTerm);
            max_Term_name=lowerTerm;
        }
    }

    public void closeDoc(){
        uniqueTermsNum = term_frq.size();
    }
}