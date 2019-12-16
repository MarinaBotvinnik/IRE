package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Indexer {
    private int maxTerm;
    private int maxDoc;
    private int docDirectoryNum;
    private boolean isStem;
    private String path;
    private HashMap<String, Document> documentsPosting;
    private HashMap<String, String> documentsDictionary;
    private HashMap<String, String> dictionary;
    private HashMap<String, Term> posting;
    private HashMap<String, Term> entities;
    private Stemmer stemmer;

    public Indexer(boolean stem) {
        documentsPosting = new HashMap<>();
        documentsDictionary = new HashMap<>();
        dictionary = new HashMap<>();
        posting = new HashMap<>();
        entities = new HashMap<>();
        maxDoc = 10000;
        maxTerm=50;
        docDirectoryNum =1;
        isStem = stem;
    }

    public void setStem(boolean stem) {
        isStem = stem;
    }

    public int getNumOfDocs(){
        return documentsDictionary.size();
    }
    public int getNumOfTerm(){
        return dictionary.size();
    }

    public void reset(){
        documentsPosting.clear();
        documentsDictionary.clear();
        dictionary.clear();
        posting.clear();
        entities.clear();
    }

    public void setPath(String path){
        if(isStem){
            this.path = path + "/Stemming";
        }
        else this.path = path + "/noStemming";
    }

    public void addTermToDic(String Name, String docNo, int position) {
        String termName=Name;
        if(isStem){
            boolean isUP = Character.isUpperCase(termName.charAt(0));
            stemmer = new Stemmer();
            stemmer.add(Name.toCharArray(),Name.length());
            stemmer.stem();
            termName = stemmer.toString();
            if(isUP)
                termName = termName.toUpperCase();
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
                if (!Files.isDirectory(Paths.get(path + "/Posting"))) {
                    File postingFolder = new File(path +"/Posting");
                    postingFolder.mkdir();
                }
                for (Map.Entry<String, Term> stringTermEntry : posting.entrySet()) {
                    Term term = stringTermEntry.getValue();
                    if (!dictionary.containsKey(term.getTermName().toLowerCase()) && !dictionary.containsKey(term.getTermName().toUpperCase()) && !term.getTermName().equals("--")) {
                        if (!Files.isDirectory(Paths.get(path +"/Posting/" + term.getTermName().charAt(0)))) {
                            File termPostingFolder = new File(path +"/Posting/" + term.getTermName().toLowerCase().charAt(0));
                            termPostingFolder.mkdir();
                        }
                        Path path = Paths.get(this.path +"/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt");
                        String str = this.path +"/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt";
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
                        dictionary.put(term.getTermName(),this.path +"/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt");
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
                Path path = Paths.get(this.path +"/DocumentsPosting");
                if (!Files.isDirectory(path)) {
                    File postingFolder = new File(this.path +"/DocumentsPosting");
                    postingFolder.mkdir();
                }
                File termPostingFolder = new File(this.path +"/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1));
                termPostingFolder.mkdir();
                String str = this.path +"/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) +"/"+ docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) +".txt";
                File termPostingFile = new File(str);
                termPostingFile.createNewFile();
                FileInputStream fis = new FileInputStream(termPostingFile);
                org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                Element root = postingFileEditor.createElement("root");
                docDirectoryNum +=maxDoc;
                BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                for (Map.Entry<String,Document> stringIntegerEntry : documentsPosting.entrySet()) {
                    HashMap.Entry pair = stringIntegerEntry;
                    Document document =(Document)pair.getValue();
                    Element docAtt = postingFileEditor.createElement(document.getDocName());
                    docAtt.appendElement("maxTf").appendText("" + document.getMax_tf());
                    docAtt.appendElement("maxTfName").appendText(document.getMax_Term_name());
                    docAtt.appendElement("uniqueTerms").appendText("" + document.getUniqueTermsNum());
                    root.appendChild(docAtt);
                    documentsDictionary.put(document.getDocName(), str);
                }
                writer.write(root.html());
                writer.close();
                fis.close();
                documentsPosting.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void addEntToDic (String Name, String docNo,int position){
        //its an entity that showed up only once and now its its second time
            if(entities.containsKey(Name)){
                Term ent = entities.remove(Name);
                ent.addDocPosition(docNo,position);
                ent.addTf(docNo);
                posting.put(Name,ent);
                //the dictionary hasn't been written yet
                if(!documentsDictionary.containsKey(docNo)){
                    Document document = documentsPosting.get(docNo);
                    document.addTerm(Name);
                    document.closeDoc();
                }else{
                    String path = documentsDictionary.get(docNo);

                }
            }
            //its entity that appeared more than once but we didnt write it yet
            else if(posting.containsKey(Name)){
                Term ent = posting.remove(Name);
                ent.addTf(docNo);
                ent.addDocPosition(docNo,position);
                posting.put(Name,ent);

            }
            //its an entity that has been written before so we need to update it
            else if(dictionary.containsKey(Name)){

            }
            //its completely new entity!
            else{
                Term term = new Term(Name,docNo,position);
                entities.put(Name,term);
            }
        }

        public void addDocToDic (Document doc) {
            if (documentsPosting.size() < maxDoc) {
                documentsPosting.put(doc.getDocName(), doc);
            }
            else{
                writeDocsToPosting();
            }

        }

        public void closeIndexer (){
            writeDictionary("TermDictionary",dictionary);
            writeDictionary("DocumentsDictionary",documentsDictionary);
            mergePostingToOne(this.path+"/Posting");
        }

        // all the files of the first letter
        private void mergePostingToOne(String filePath) {
            File file = new File(filePath);
            File[] firstLetters = file.listFiles();
            for (File secondLetter: firstLetters) {
               mergeToOne(secondLetter);
            }
        }

        // merge all the files
        private void mergeToOne(File secondLetters) {
            try {
                File[] iters = secondLetters.listFiles();
                File termPostingFile = new File(secondLetters.getAbsolutePath());
                termPostingFile.createNewFile();
                FileInputStream fis = new FileInputStream(termPostingFile);
                org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                Element root = postingFileEditor.createElement("root");
                for (File iter: iters) {
                    FileInputStream fis2 = new FileInputStream(iter);
                    org.jsoup.nodes.Document tempEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                    Elements terms = tempEditor.select("term");
                    for (Element term: terms) {
                        String name = term.attr("TERMNAME");
                        Element upper = root.selectFirst("term[TERMNAME='"+name.toUpperCase()+"']");
                        Element lower = root.selectFirst("term[TERMNAME='"+name.toLowerCase()+"']");
                        Element currTerm;
                        //it's not on the final merge yet
                        if(upper==null && lower==null ){
                            root.appendChild(term);
                            continue;
                        }
                        if(upper!=null && Character.isLowerCase(name.charAt(0))){
                            upper.attr("TERMNAME",name);
                            currTerm = upper;
                        }
                        else{
                            currTerm = lower;
                        }
                        //update idf
                        int idfCurr=Integer.parseInt(currTerm.select("idf").first().text());
                        int idfMerge = Integer.parseInt(term.select("idf").first().text());
                        currTerm.select("idf").first().replaceWith(new TextNode(""+idfCurr+idfMerge));
                        //update the tf
                        Elements docsCurr = currTerm.select("doc");
                        Elements docsMerge = term.select("doc");
                        for (Element doc: docsMerge) {
                            if(docsCurr.select("doc[DOCNAME="+doc.attr("DOCNAME")+"]")==null){
                                docsCurr.append(doc.html());
                            }
                            else{
                                int tfCurr=Integer.parseInt(docsCurr.select("doc[DOCNAME="+doc.attr("DOCNAME")+"]").select("tf").first().text());
                                int tfMerge = Integer.parseInt(doc.select("tf").first().text());
                                docsCurr.select("doc[DOCNAME="+doc.attr("DOCNAME")+"]").select("tf").first().replaceWith(new TextNode(""+tfCurr+tfMerge));
                                String positions = doc.select("positions").first().text();
                                String currPositions = docsCurr.select("doc[DOCNAME="+doc.attr("DOCNAME")+"]").select("positions").first().text();
                                currPositions = currPositions+positions;
                                docsCurr.select("doc[DOCNAME="+doc.attr("DOCNAME")+"]").select("positions").first().replaceWith(new TextNode(currPositions));
                            }
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        private Elements readDoc(String path){
            try {
                //NEED TO CHANGE THE PATH TO THE DICTIONARY PATH
                FileInputStream fis = new FileInputStream(new File(path));
                org.jsoup.nodes.Document file = Jsoup.parse(fis, null, "", Parser.xmlParser());
                Elements terms = file.select("term");
                return terms;
            }catch (IOException e){
                e.printStackTrace();
                return null;
            }
        }

        private void writeDictionary(String dicName, HashMap<String,String> dictionary){
            try{
                Path path = Paths.get(this.path +"/"+dicName);
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
                Element root = postingFileEditor.createElement("root");
                for (Map.Entry<String,String> stringIntegerEntry : dictionary.entrySet()) {
                    HashMap.Entry pair = stringIntegerEntry;
                    String termName = (String) pair.getKey();
                    String termPath =(String) pair.getValue();
                    Element docAtt = postingFileEditor.createElement(termName);
                    docAtt.appendElement("PATH").appendText(termPath);
                    root.appendChild(docAtt);
                }
                writer.write(root.html());
                writer.close();
                fis.close();

            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public TreeMap<String,String> uploadDictionary() {
            HashMap<String, String> dic = new HashMap<>();
            try {
                //NEED TO CHANGE THE PATH TO THE DICTIONARY PATH
                FileInputStream fis = new FileInputStream(new File(this.path +"/DocumentsDictionary/DocumentsDictionary.txt"));
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
