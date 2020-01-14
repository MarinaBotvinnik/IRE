package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

/**
 * This class in charge of reading all the documents in the corpus.
 * the class seperates the TEXT tag of each document.
 */
public class ReadFile {
    //private HashMap<String,String> docMap;
    private Parse parser;
    private int docsSent;
    private int max;
    //private ReentrantLock lock;
    private ExecutorService executor;

    /**
     * Constructor of the class, sets if the words need to be stemmed
     * @param isStem - true if words should be stemmed, false otherwise
     */
    public ReadFile(boolean isStem) {
        //docMap = new HashMap<>();
        parser = new Parse(isStem);
        docsSent=0;
        max=10000;
        //lock=new ReentrantLock();
        executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Method that starts the proceeding of the corpus with the path it gets.
     * @param path - String that represent the path of the corpus
     */
    public void readFile(String path){
        listFilesForFolder(path);
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parser.closeParser();
    }

    /**
     * Method that gets to every text file in the corpus recursively
     * @param filePath - String of the corpus
     */
    private void listFilesForFolder(String filePath){
        final File folder = new File(filePath);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry.getPath());
            } else {//I GOT TO THE DOCUMENT
                readDoc(fileEntry.getPath());
            }
        }
    }

    /**
     * This function is in charge of spliting a document by 3 main tags:
     * <DOC> </DOC>
     * <DOCNO> </DOCNO>
     * <TEXT> </TEXT>
     * @param docPath - String that represents the path of the document.txt
     */
    private void readDoc(String docPath){
        try {
            FileInputStream fis = new FileInputStream(new File(docPath));
            Document file = Jsoup.parse(fis, null, "", Parser.xmlParser());
            Elements Documents=file.select("DOC");
            for(Element doc : Documents){
                if(docsSent==max){
                    this.executor.shutdown();
                    try {
                        this.executor.awaitTermination(1, TimeUnit.HOURS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    executor = Executors.newFixedThreadPool(4);
                    parser.write();
                    docsSent=0;
                }
                String docNo = doc.select("DOCNO").text();
                //docMap.put(docNo,docPath);
                if(doc.select("TEXT").first()!=null) {
                    Runnable runnable1 = () -> parser.parse(doc.select("TEXT").text(), docNo);
                    executor.execute(runnable1);
                    this.docsSent++;
                }
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    /**
     *  Method set the stem value by sending it to th parser
     */
    public void setIndexerStem(boolean isStem){
        parser.setIndexerStem(isStem);
    }

    /**
     * Method that gets the correct dictionary from the parser by the stem value and the path.
     * @param stem - true - if stem, false - otherwise
     * @return dictionary of all the terms.
     */
    public LinkedHashMap<String,String > upload(boolean stem, String path){
        return parser.upload(stem,path);
    }

    /**
     * set the path to write the posting by sending the value to the Parser.
     * @param path - where the posting files will be saved
     */
    public void setIndexerPath(String path){
        parser.setIndexerPath(path);
    }

    /**
     * gets the number of the documents in the corpus
     * @return number of the documents in the corpus
     */
    public int getNumOfDocs(){
        return parser.getNumOfDocs();
    }

    /**
     * gets the number of the terms in the corpus
     * @return number of the terms in the corpus
     */
    public int getNumOfTerm(){
        return parser.getNumOfTerm();
    }

    /**
     * sends a command to erase all the necessary data to the Parser
     */
    public void reset(){
        parser.reset();
    }

    public Parse getParser() {
        return parser;
    }
}