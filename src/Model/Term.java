package Model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that represents a word and all the information about the word:
 * name
 * list of documents and all the appearances of the word in every document - TF
 * list of positions the term in every document
 */
public class Term {

    private String termName;
    private HashMap<String,Integer> docs; //all the documents this term appears in and how many times it appeared in each of them
    private HashMap<String, String> positions; //term position per document;
    int idf;

    /**
     * Constructor of the term that gets the name, the document it first appear in, and the position int this document
     * @param name - the word itself
     * @param doc - DOCNO of the document
     * @param position - word # in the document
     */
    public Term (String name, String doc, int position){
        termName = name;
        docs = new HashMap<>();
        docs.put(doc,1);
        positions=new HashMap<>();
        //positions.put(doc,new LinkedList<>());
        positions.put(doc,""+position);
        idf=1;
    }

    /**
     * sets the terms name
     * @param name - the new name
     */
    public void changeName(String name){
        this.termName=name;
    }

    /**
     * getter of the terms name
     * @return
     */
    public String getTermName() {
        return termName;
    }

    /**
     * getter of the TF
     * @return
     */
    public HashMap<String,Integer> getDocs() {
        return docs;
    }

    /**
     * Getter of the positions
     * @return
     */
    public HashMap<String, String> getPositions(){
        return positions;
    }

    /**
     * method that adds a new appearance of the term if a document to the TF list and the position list
     * @param doc - DOCNO of the document
     * @param position - the word # in the doc
     */
    public void addDocPosition (String doc, int position){
        if(docs.containsKey(doc)){
            docs.replace(doc,docs.get(doc)+1);
            positions.replace(doc,positions.get(doc)+","+position);
        }
        else{
            docs.put(doc,1);
            positions.put(doc,""+position);
        }
    }
}