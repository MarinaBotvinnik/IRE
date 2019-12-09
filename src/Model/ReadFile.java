package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class ReadFile {
    private String path;
    private HashMap<String,String> docMap;
    private Parse parser;
    private Dictionary dictionary;

    public ReadFile(String path) {
        this.path = path;
        docMap = new HashMap<>();
        parser = new Parse();
        ConcurrentHashMap <String,String> map = new ConcurrentHashMap<>();
        dictionary = new Dictionary();
    }

    public void readFile(){
        listFilesForFolder(path);
    }


    private void listFilesForFolder(String filePath){
        final File folder = new File(filePath);
        for (final File fileEntry : folder.listFiles()) {
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
     * @param docPath
     */
    private void readDoc(String docPath){
        try {
            FileInputStream fis = new FileInputStream(new File(docPath));
            Document file = Jsoup.parse(fis, null, "", Parser.xmlParser());
            Elements Documents=file.select("DOC");
            for(Element doc : Documents){
                String docNo = doc.select("DOCNO").text();
                docMap.put(docNo,docPath);
                if(!doc.select("TEXT").isEmpty()) {
                    parser.parse(dictionary, doc.select("TEXT").text(), docNo);
                }
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
