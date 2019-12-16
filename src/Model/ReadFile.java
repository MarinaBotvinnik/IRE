package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.HashMap;


public class ReadFile {
    private String path;
    private HashMap<String,String> docMap;
    private Parse parser;
    private Dictionary dictionary;

    public ReadFile() {
        docMap = new HashMap<>();
        parser = new Parse();
        HashMap <String,String> map = new HashMap<>();
        dictionary = new Dictionary();
        path = "";
    }

    public void readFile(String path){
        this.path = path;
        listFilesForFolder(path);
        parser.closeParser();
        parser.upload(); //temp function
    }

//    public void tryFunc(){
//        try {
//            FileInputStream fis = new FileInputStream(new File(path));
//            Document file = Jsoup.parse(fis, null, "", Parser.xmlParser());
//            Elements Documents=file.select("Term");
//            for(Element doc : Documents){
//                String name = doc.select("Name").text();
//                System.out.println(name);
//            }
//
//        }
//        catch (Exception e){
//            System.out.println("SHIT");
//        }
//    }


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
                int check;
                if(docNo.equals("LA011089-0070"))
                    check=0;
                if(doc.select("TEXT").hasText()) {
                    parser.parse(dictionary, doc.select("TEXT").text(), docNo);
                }
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
