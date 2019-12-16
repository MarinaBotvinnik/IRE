package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.HashMap;
import java.util.TreeMap;


public class ReadFile {
    private HashMap<String,String> docMap;
    private Parse parser;

    public ReadFile(boolean isStem) {
        docMap = new HashMap<>();
        parser = new Parse(isStem);
    }

    public void readFile(String path){
        //listFilesForFolder(path);
        parser.closeParser();
        //parser.upload(); //temp function
    }


    private void listFilesForFolder(String filePath){
        final File folder = new File(filePath);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry.getPath());
            } else {//I GOT TO THE DOCUMENT
                System.out.println(fileEntry.getPath());
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
                if(doc.select("TEXT").first()!=null) {
                    parser.parse(doc.select("TEXT").text(), docNo);
                }
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void setIndexerStem(boolean isStem){
        parser.setIndexerStem(isStem);
    }

    public TreeMap<String,String > upload(){
        return parser.upload();
    }

    public void setIndexerPath(String path){
        parser.setIndexerPath(path);
    }

    public int getNumOfDocs(){
        return parser.getNumOfDocs();
    }
    public int getNumOfTerm(){
        return parser.getNumOfTerm();
    }

    public void reset(){
        parser.reset();
    }
}
