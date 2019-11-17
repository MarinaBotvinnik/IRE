package Model;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.beans.Expression;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
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
