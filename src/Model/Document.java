package Model;
import java.util.HashMap;
import java.util.Iterator;

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


    public void addFrequency(String term){
        if(term_frq.containsKey(term)){
            term_frq.replace(term,term_frq.get(term)+1);
        }
        else term_frq.put(term,1);
    }

    public void closeDoc(){
        Iterator iterator = term_frq.entrySet().iterator();
        while (iterator.hasNext()){
            HashMap.Entry pair = (HashMap.Entry)iterator.next();
            if(((int)pair.getValue())>max_tf){
                max_tf= ((int)pair.getValue());
                max_Term_name = (String)pair.getKey();
            }
        }
        uniqueTermsNum = term_frq.size();

    }
}
