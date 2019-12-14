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
    int docDirectoryNum;
    boolean isStem;
    HashMap<String, Document> documentsPosting;
    HashMap<String, String> documentsDictionary;
    HashMap<String, String> dictionary;
    HashMap<String, Term> posting;
    HashMap<String, Term> entities;
    Stemmer stemmer;

    public Indexer() {
        documentsPosting = new HashMap<>();
        documentsDictionary = new HashMap<>();
        dictionary = new HashMap<>();
        posting = new HashMap<>();
        entities = new HashMap<>();
        maxDoc = 20;
        docDirectoryNum =1;
        isStem = false;
    }

    public void setStem(boolean stem) {
        isStem = stem;
    }

    public void addTermToDic(String Name, String docNo, int position) {
        String termName=Name;
        if(isStem){
            stemmer = new Stemmer();
            stemmer.add(Name.toCharArray(),Name.length());
            stemmer.stem();
            termName = stemmer.toString();
        }
        if (posting.size() < maxTerm) {
            //if it exists in the dictionary
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
                Iterator iter = posting.entrySet().iterator();
                while (iter.hasNext()) {
                    Term term = (Term) iter.next();
                    if (!dictionary.containsKey(term.getTermName().toLowerCase()) || !dictionary.containsKey(term.getTermName().toUpperCase())) {
                        if (!Files.isDirectory(Paths.get("/Posting/" + term.getTermName().charAt(0)))) {
                            File termPostingFolder = new File("/Posting/" + term.getTermName().toLowerCase().charAt(0));
                            termPostingFolder.mkdir();
                        }
                        Path path = Paths.get("/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt");
                        String str = null;
                        str = "/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt";
                        File termPostingFile = new File(str);
                        if (!Files.exists(path)) {
                            termPostingFile.createNewFile();
                        }
                        FileInputStream fis = new FileInputStream(termPostingFile);
                        org.jsoup.nodes.Document postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                        Element termNode= postingFileEditer.createElement(term.getTermName());
                        HashMap<String,Integer> docs=term.getDocs();
                        HashMap<String, List<Integer>> positionsList= term.getPositions();
                        termNode.appendElement("idf").appendText(""+docs.size());
                        Element docsNode=termNode.appendElement("docs");
                        for(Map.Entry<String,Integer> entry : docs.entrySet()){
                            Element docNode=docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
                            docNode.appendElement("TF").appendText(""+entry.getValue());
                            StringBuilder positions= new StringBuilder();
                            for(Integer pos: positionsList.get(entry.getKey())){
                                positions.append(pos).append(",");
                            }
                            docNode.appendElement("Positions").appendText(positions.toString());
                        }
                        dictionary.put(term.getTermName(),"/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt");
                        fis.close();
                    }
                    else{
                        String tagName;
                        FileInputStream fis;
                        Element termNode;
                        org.jsoup.nodes.Document postingFileEditer;
                        if (Character.isLowerCase(term.getTermName().charAt(0)) && dictionary.containsKey(term.getTermName().toUpperCase())) {
                            dictionary.put(term.getTermName(),dictionary.get(term.getTermName().toUpperCase()));
                            dictionary.remove(term.getTermName().toUpperCase());
                            fis = new FileInputStream(new File(dictionary.get(term.getTermName())));
                            postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            termNode=postingFileEditer.select(term.getTermName().toUpperCase()).first();
                            termNode.tagName(term.getTermName());
                            tagName=term.getTermName();
                        }
                        else if(Character.isUpperCase(term.getTermName().charAt(0)) && dictionary.containsKey(term.getTermName().toLowerCase())){
                            fis = new FileInputStream(new File(dictionary.get(term.getTermName().toLowerCase())));
                            postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            tagName=term.getTermName().toLowerCase();
                            termNode=postingFileEditer.select(tagName).first();
                        }
                        else{
                            fis = new FileInputStream(new File(dictionary.get(term.getTermName())));
                            postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            tagName=term.getTermName();
                            termNode=postingFileEditer.select(tagName).first();
                        }
                        HashMap<String,Integer> docs=term.getDocs();
                        HashMap<String, List<Integer>> positionsList= term.getPositions();
                        int idf=Integer.parseInt(termNode.select("idf").first().text());
                        Element docsNode=termNode.appendElement("docs");
                        for(Map.Entry<String,Integer> entry : docs.entrySet()){
                            String positions="";
                            for(Integer pos: positionsList.get(entry.getKey())){
                                positions+=pos+",";
                            }
                            if(docsNode.select("doc[DOCNAME='"+entry.getKey()+"']")==null){
                                idf++;
                                Element docNode=docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
                                docNode.appendElement("TF").appendText(""+entry.getValue());
                                docNode.appendElement("Positions").appendText(positions);
                            }
                            else{
                                Element docNode=docsNode.select("doc[DOCNAME='"+entry.getKey()+"']").first();
                                int tf=Integer.parseInt(docNode.select("TF").first().text())+entry.getValue();
                                docNode.select("TF").first().text(""+tf);
                                String str=docNode.select("Positions").first().text();
                                str+=positions;
                                docNode.select("Positions").first().text(str);
                            }
                        }
                        termNode.select("idf").first().text(""+idf);
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
                File termPostingFolder = new File("/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1));
                termPostingFolder.mkdir();
                String str = "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) +"/"+ docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) +".txt";
                File termPostingFile = new File(str);
                termPostingFile.createNewFile();
                FileInputStream fis = new FileInputStream(termPostingFile);
                org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                docDirectoryNum +=maxDoc;
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
                    documentsDictionary.put(document.getDocName(), str);
                }
                writer.close();
                fis.close();
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

        private boolean changeTerm (String termName) {
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

        public void closeIndexer (){
            //writeDictionary("TermDictionary",dictionary);
            writeDictionary("DocumentsDictionary",documentsDictionary);
        }

        private void writeDictionary(String dicName, HashMap<String,String> dictionary){
            try{
                Path path = Paths.get("/"+dicName);
                if (!Files.isDirectory(path)) {
                    File postingFolder = new File("/"+dicName);
                    postingFolder.mkdir();
                }
                String str ="/"+dicName+"/"+dicName +".txt";
                File termPostingFile = new File(str);
                termPostingFile.createNewFile();
                FileInputStream fis = new FileInputStream(termPostingFile);
                org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                for (Map.Entry<String,String> stringIntegerEntry : dictionary.entrySet()) {
                    HashMap.Entry pair = stringIntegerEntry;
                    String termName = (String) pair.getKey();
                    String termPath =(String) pair.getValue();
                    Element docNode = postingFileEditor.createElement("newElement"); // this tag name is not seen anywhere
                    Element docAtt = postingFileEditor.createElement(termName);
                    docAtt.appendElement("PATH").appendText(termPath);
                    docNode.appendChild(docAtt);
                    writer.write(docNode.html());
                    writer.newLine();
                }
                writer.close();
                fis.close();

            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public TreeMap<String,String> uploadDictionary() {
            HashMap<String, String> dic = new HashMap<>();
            try {
                FileInputStream fis = new FileInputStream(new File("/DocumentsDictionary/DocumentsDictionary.txt"));
                org.jsoup.nodes.Document file = Jsoup.parse(fis, null, "", Parser.xmlParser());
                Elements terms = file.children();
                for (Element term : terms) {
                    String termName = term.nodeName();
                    String termPath = term.select("PATH").text();
                    dic.put(termName, termPath);
                }
                fis.close();
                TreeMap<String, String> alphabeticDic = new TreeMap<>(dic);
                return alphabeticDic;

            } catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
            }
        }
}
