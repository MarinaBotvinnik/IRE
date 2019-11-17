package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.beans.Expression;
import java.io.*;
import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReadFile {
    private String path;
    private Dictionary<String, Tagenizer> allPapers;

    public ReadFile(String path) {
        this.path = path;
    }
    public void readFile(String curr){
        new File(System.getProperty("java.io.tmpdir")+"/SeperatedFiles").mkdir();
        listFilesForFolder(curr);
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
                File file2 = new File(System.getProperty("java.io.tmpdir")+"/SeperatedFiles/"+doc.select("DOCNO").text());
                BufferedWriter bf=new BufferedWriter(new FileWriter(file2));
                bf.write(doc.html());
                //System.out.println(doc.html());
                bf.close();
            }
            /*DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            File f = new File(docPath);
            Document document = builder.parse(f);
            NodeList nodeList = document.getElementsByTagName("DOC");
            for(int i=0; i<nodeList.getLength(); i++){
                Node node = nodeList.item(i);
                System.out.println(node);
            }*/
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
