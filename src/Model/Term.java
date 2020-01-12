package Model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class that represents a word and all the information about the word:
 * name
 * list of documents and all the appearances of the word in every document - TF
 * list of positions the term in every document
 */
public class Term {

    private String termName;
    private String documents;
    private String currDocs;
    private ConcurrentHashMap<String,Integer> docs; //all the documents this term appears in and how many times it appeared in each of them
    private ConcurrentHashMap<String, String> positions; //term position per document;
    private ConcurrentLinkedQueue<String> openDocs;
    private int tf;

    /**
     * Constructor of the term that gets the name, the document it first appear in, and the position int this document
     * @param name - the word itself
     * @param doc - DOCNO of the document
     * @param position - word # in the document
     */
    public Term (String name, String doc, int position){
        openDocs=new ConcurrentLinkedQueue<String>();
        openDocs.add(doc);
        termName = name;
        docs = new ConcurrentHashMap<>();
        docs.put(doc,1);
        positions=new ConcurrentHashMap<>();
        //positions.put(doc,new LinkedList<>());
        positions.put(doc,""+position);
        currDocs=doc;
        documents="";
        tf=1;
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
    public ConcurrentHashMap<String,Integer> getDocs() {
        return docs;
    }

    /**
     * Getter of the positions
     * @return
     */
    public ConcurrentHashMap<String, String> getPositions(){
        return positions;
    }

    /**
     * method that adds a new appearance of the term if a document to the TF list and the position list
     * @param doc - DOCNO of the document
     * @param position - the word # in the doc
     */
    public void addDocPosition (String doc, int position, ConcurrentHashMap<String, String> opdocs){
        synchronized (this) {
            tf++;
            String temp;
            for (int i = 0; i < this.openDocs.size(); i++) {
                temp = this.openDocs.remove();
                if (opdocs.containsKey(temp))
                    this.openDocs.add(temp);
                else {
                    documents += "[" + temp + "," + this.docs.get(temp) + "," + this.positions.get(temp) + "]";
                }
            }
            if (docs.containsKey(doc)) {
                docs.replace(doc, docs.get(doc) + 1);
                positions.replace(doc, positions.get(doc) + "," + position);
            } else {
//            documents+="["+this.currDocs+","+this.docs.get(this.currDocs)+","+this.positions.get(this.currDocs)+"]";
                docs.put(doc, 1);
                positions.put(doc, "" + position);
//            this.currDocs=doc;
                this.openDocs.add(doc);
            }
        }
    }

    public int getTf(){
        return this.tf;
    }

    public String toString() {
        synchronized (this) {
            String temp;
            for (int i = 0; i < this.openDocs.size(); i++) {
                temp = this.openDocs.remove();
                documents += "[" + temp + "," + this.docs.get(temp) + "," + this.positions.get(temp) + "]";
            }
            return this.termName + "[" + docs.size() + "]" + this.documents;
        }
    }
}