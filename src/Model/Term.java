package Model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class Term {

    private String termName;
    private HashMap<String,Integer> docs; //all the documents this term appears in and how many times it appeared in each of them
    private HashMap<String, List<Integer>> positions; //term position per document;
    boolean isEntity;

    public Term (String name, String doc, int position, boolean isEntity){
        termName = name;
        docs = new HashMap<>();
        docs.put(doc,1);
        positions=new HashMap<>();
        this.isEntity=isEntity;
        positions.put(doc,new LinkedList<>());
        positions.get(doc).add(position);
    }

    public String getTermName() {
        return termName;
    }

    public HashMap<String,Integer> getDocs() {
        return docs;
    }

    public HashMap<String, List<Integer>> getPositions(){
        return positions;
    }

    public boolean isEntity(){
        return isEntity;
    }

    //add document to the list
    public void addDocPosition (String doc, int position){
        if(docs.containsKey(doc)){
            docs.replace(doc,docs.get(doc)+1);
            positions.get(doc).add(position);
        }
        else{
            docs.put(doc,1);
            positions.put(doc,new LinkedList<>());
            positions.get(doc).add(position);
        }
    }

    public boolean isEqual(String name){
        if(name.equals(this.termName))
            return true;
        return false;
    }

    public void setEntity(boolean isEntity){
        this.isEntity=isEntity;
    }
}
