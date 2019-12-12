package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Indexer {
    int maxTerm;
    int maxDoc;
    int docDirecotryNum;
    HashMap<String, Document> documentsPosting;
    HashMap<String, String> documentsDictionary;
    HashMap<String, String> dictionary;
    HashMap<String, Term> posting;
    HashMap<String, Term> entities;

    public Indexer() {
        documentsPosting = new HashMap<>();
        documentsDictionary = new HashMap<>();
        dictionary = new HashMap<>();
        posting = new HashMap<>();
        entities = new HashMap<>();
        maxDoc = 20;
        docDirecotryNum=1;
        maxTerm=50;
    }

    public void addTermToDic(String Name, String docNo, int position) {
        String termName = Name;
        if (posting.size() < maxTerm) {
            //if it exists in the dictionary
            //boolean existsDic = isTermExistInDic (termName);
            char first = termName.charAt(0);
            // the word isn't in the dictionary yet
            if (!posting.containsKey(termName.toUpperCase()) || !posting.containsKey(termName.toLowerCase())) {
                if (Character.isUpperCase(first)) {
                    posting.put(termName.toUpperCase(), new Term(termName.toUpperCase(), docNo, position));
                } else posting.put(termName.toLowerCase(), new Term(termName.toLowerCase(), docNo, position));
            } else {
                if (Character.isLowerCase(first) && posting.containsKey(termName.toUpperCase())) {
                    posting.get(termName.toUpperCase()).changeName(termName.toLowerCase());
                    posting.put(termName.toLowerCase(), posting.get(termName.toUpperCase()));
                    posting.remove(termName.toUpperCase());
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                } else if (posting.containsKey(termName.toUpperCase()) && Character.isUpperCase(first)) {
                    posting.get(termName.toUpperCase()).addDocPosition(docNo, position);
                } else {
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                }
            }
            //it means it exist in the posting and the dictionary

//            if(existsDic){
//                //the term changed to lowerCase
//                if(changeTerm(termName)){
//                    Term chaged = posting.get(termName);
//                }
//                //term stays the same
//                else{
//                    Term existTerm = posting.get(termName);
//
//                }
//            }
//            //it exist in the dictionary but not in the posting
//            else{
//                posting.put(termName,term);
//            }
//
//        }
            //we reached the max size of the posting, time to write to the disc
        }
        else{
                this.writeTermsToPosting();
            }
    }

        private void writeTermsToPosting() {
            try {
                if (!Files.isDirectory(Paths.get("/Posting"))) {
                    File postingFolder = new File("/Posting");
                    postingFolder.mkdir();
                }
                for (Map.Entry<String, Term> stringTermEntry : posting.entrySet()) {
                    Term term = stringTermEntry.getValue();
                    if (!dictionary.containsKey(term.getTermName().toLowerCase()) && !dictionary.containsKey(term.getTermName().toUpperCase()) && !term.getTermName().equals("--")) {
                        if (!Files.isDirectory(Paths.get("/Posting/" + term.getTermName().charAt(0)))) {
                            File termPostingFolder = new File("/Posting/" + term.getTermName().toLowerCase().charAt(0));
                            termPostingFolder.mkdir();
                        }
                        Path path = Paths.get("/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt");
                        String str = "/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt";
                        File termPostingFile = new File(str);
                        boolean boolTemp=false;
                        if (!Files.exists(path)) {
                            termPostingFile.createNewFile();
                            boolTemp=true;
                        }
                        FileInputStream fis = new FileInputStream(termPostingFile);
                        org.jsoup.nodes.Document postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                        Element root;
                        if(boolTemp==true)
                            root=postingFileEditer.createElement("root");
                        else
                            root=postingFileEditer.selectFirst("root");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                        Element termNode= root.appendElement("term").attr("TERMNAME",term.getTermName());
                        HashMap<String,Integer> docs=term.getDocs();
                        HashMap<String, List<Integer>> positionsList= term.getPositions();
                        termNode.appendElement("idf").appendText(""+docs.size());
                        Element docsNode=termNode.appendElement("docs");
                        for(Map.Entry<String,Integer> entry : docs.entrySet()){
                            Element docNode=docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
                            docNode.appendElement("TF").appendText(""+entry.getValue());
                            String positions="";
                            for(Integer pos: positionsList.get(entry.getKey())){
                                positions+=pos+",";
                            }
                            docNode.appendElement("Positions").appendText(positions);
                        }
                        dictionary.put(term.getTermName(),"/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt");
                        writer.write(root.outerHtml());
                        writer.close();
                        fis.close();
                    }
                    else if(!term.getTermName().equals("--")){
                        String tagName;
                        FileInputStream fis;
                        Element termNode;
                        BufferedWriter writer;
                        org.jsoup.nodes.Document postingFileEditer;
                        Element root;
                        if (Character.isLowerCase(term.getTermName().charAt(0)) && dictionary.containsKey(term.getTermName().toUpperCase())) {
                            dictionary.put(term.getTermName(),dictionary.get(term.getTermName().toUpperCase()));
                            dictionary.remove(term.getTermName().toUpperCase());
                            fis = new FileInputStream(new File(dictionary.get(term.getTermName())));
                            postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            root=postingFileEditer.selectFirst("root");
                            termNode=root.selectFirst("term[TERMNAME='"+term.getTermName().toUpperCase()+"']");
                            termNode.attr("TERMNAME",term.getTermName());
                            tagName=term.getTermName();
                            writer= new BufferedWriter(new FileWriter(dictionary.get(term.getTermName())));
                        }
                        else if(Character.isUpperCase(term.getTermName().charAt(0)) && dictionary.containsKey(term.getTermName().toLowerCase())){
                            fis = new FileInputStream(new File(dictionary.get(term.getTermName().toLowerCase())));
                            postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            tagName=term.getTermName().toLowerCase();
                            root=postingFileEditer.selectFirst("root");
                            termNode=root.selectFirst("term[TERMNAME='"+tagName+"']");
                            writer= new BufferedWriter(new FileWriter(dictionary.get(term.getTermName().toLowerCase())));
                        }
                        else{
                            fis = new FileInputStream(new File(dictionary.get(term.getTermName())));
                            postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            tagName=term.getTermName();
                            root=postingFileEditer.selectFirst("root");
                            termNode=root.selectFirst("term[TERMNAME='"+term.getTermName()+"']");
                            writer= new BufferedWriter(new FileWriter(dictionary.get(term.getTermName())));
                        }
                        HashMap<String,Integer> docs=term.getDocs();
                        HashMap<String, List<Integer>> positionsList= term.getPositions();
                        int idf=Integer.parseInt(termNode.select("idf").first().text());
                        Element docsNode=termNode.select("docs").first();
                        for(Map.Entry<String,Integer> entry : docs.entrySet()){
                            String positions="";
                            for(Integer pos: positionsList.get(entry.getKey())){
                                positions+=pos+",";
                            }
                            if(docsNode.select("doc[DOCNAME='"+entry.getKey()+"']").first()==null){
                                idf++;
                                Element docNode=docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
                                docNode.appendElement("TF").appendText(""+entry.getValue());
                                docNode.appendElement("Positions").appendText(positions);
                            }
                            else{
                                Element docNode=docsNode.select("doc[DOCNAME="+entry.getKey()+"]").first();
                                int tf=Integer.parseInt(docNode.select("TF").first().text());
                                tf+=entry.getValue();
                                docNode.select("TF").first().text(""+tf);
                                String str=docNode.select("Positions").first().text();
                                str+=positions;
                                docNode.select("Positions").first().text(str);
                            }
                        }
                        termNode.select("idf").first().text(""+idf);
                        writer.write(postingFileEditer.selectFirst("root").outerHtml());
                        writer.close();
                        fis.close();
                    }
                }
                posting.clear();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeDocsToPosting() {
            try {
                Path path = Paths.get("/DocumentsPosting");
                if (!Files.isDirectory(path)) {
                    File postingFolder = new File("/DocumentsPosting");
                    postingFolder.mkdir();
                }
                File termPostingFolder = new File("/DocumentsPosting/" + docDirecotryNum + "-" + (docDirecotryNum + maxDoc - 1));
                termPostingFolder.mkdir();
                String str;
                str = "/DocumentsPosting/" + docDirecotryNum + "-" + (docDirecotryNum + maxDoc - 1) +"/"+ docDirecotryNum + "-" + (docDirecotryNum + maxDoc - 1) +".txt";
                File termPostingFile = new File(str);
                termPostingFile.createNewFile();
                FileInputStream fis = new FileInputStream(termPostingFile);
                org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                docDirecotryNum+=maxDoc;
                BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                for (Map.Entry<String,Document> stringIntegerEntry : documentsPosting.entrySet()) {
                    HashMap.Entry pair = stringIntegerEntry;
                    Document document =(Document)pair.getValue();
                    Element docNode = postingFileEditor.createElement("newElement"); // this tag name is not seen anywhere
                    Element docAtt = postingFileEditor.createElement(document.getDocName());
                    docAtt.appendElement("maxTf").appendText("" + document.getMax_tf());
                    docAtt.appendElement("maxTfName").appendText(document.getMax_Term_name());
                    docAtt.appendElement("uniqueTerms").appendText("" + document.getUniqueTermsNum());
                    docNode.appendChild(docAtt);
                    writer.write(docNode.html());
                    writer.newLine();
                    dictionary.put(document.getDocName(), str);
                }
                writer.write("DONE");
                writer.close();
                documentsPosting.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void addEntToDic (String Name, String docNo,int position){

        }

        public void addDocToDic (Document doc) {
            if (documentsPosting.size() < maxDoc) {
                documentsPosting.put(doc.getDocName(), doc);
            }
            else{
                writeDocsToPosting();
            }

        }

        private boolean isTermExistInDic (String termName){
            char first = termName.charAt(0);
            // the word isn't in the dictionary yet
            if (!posting.containsKey(termName.toUpperCase()) || !posting.containsKey(termName.toLowerCase())) {
                if (Character.isUpperCase(first)) {
                    posting.put(termName.toUpperCase(), null);
                } else posting.put(termName.toLowerCase(), null);
                return false;
            } else {
            }
            return true;
        }

        private boolean changeTerm (String termName){
            char first = termName.charAt(0);
            if (Character.isLowerCase(first)) {
                if (dictionary.containsKey(termName.toUpperCase())) {
                    String value = dictionary.get(termName.toUpperCase());
                    dictionary.remove(termName.toUpperCase());
                    dictionary.put(termName.toLowerCase(), value);
                    return true;
                }
            } else dictionary.replace(termName, dictionary.get(termName) + 1);
            return false;
    }
}
