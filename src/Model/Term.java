package Model;

import java.util.HashMap;
import java.util.HashSet;

public class Term {

    private String termName;
    private int df; // in how many documents this term exist
    private HashMap<String,Integer> docs; //all the documents this term appears in

    public Term (String name, String doc){
        termName = name;
        df = 1;
        docs = new HashMap<>();
        docs.put(doc,1);
    }

    public String getTermName() {
        return termName;
    }

    public int getDf() {
        return df;
    }

    public HashMap<String,Integer> getDocs() {
        return docs;
    }

    public void setTermName(String termName) {
        this.termName = termName;
    }

    public void setDf(int df) {
        this.df = df;
    }

    // increase value of certain document
    public void increaeDocNum (String doc){
        Integer num = docs.get(doc);
        num = num++;
        docs.replace(doc,num);
    }
    //add document to the list
    public void addDoc (String doc){
        docs.put(doc,1);
    }
    //increase the df of the term
    public void increasDf(){
        df++;
    }

    public boolean isEqual(String name){
        if(name.equals(this.termName))
            return true;
        return false;
    }
}
