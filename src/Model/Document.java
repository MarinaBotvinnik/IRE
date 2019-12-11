package Model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Document {
    
    private int max_tf;
    private String max_Term_name;
    private HashMap<String,Integer> term_frq;
    private int uniqueTermsNum;
    private String doc_name;

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
    }

    public void closeDoc(){
        for (Map.Entry<String, Integer> stringIntegerEntry : term_frq.entrySet()) {
            HashMap.Entry pair = stringIntegerEntry;
            if (!pair.getKey().equals("") && !pair.getKey().equals("and")&&((int) pair.getValue()) > max_tf) {
                max_tf = ((int) pair.getValue());
                max_Term_name = (String) pair.getKey();
            }
        }
        uniqueTermsNum = term_frq.size();
    }
}
