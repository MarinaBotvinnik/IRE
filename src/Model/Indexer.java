package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class Indexer {
    int iteration;
    double time;
    Object lock1;
    Object lock2;
    Object lock3;
    int maxTerm;
    int maxDoc;
    int writes;
    int docDirectoryNum;
    boolean isStem;
    HashMap<String, Document> documentsPosting;
    HashMap<String, String> documentsDictionary;
    volatile ConcurrentHashMap<String, String> dictionary;
    HashMap<String, Term> posting;
    HashMap<String, Term> entities;
    volatile ConcurrentHashMap<String,Object> filesLockers;
    Stemmer stemmer;

    public Indexer() {
        iteration=0;
        writes=0;
        time=System.currentTimeMillis();
        filesLockers=new ConcurrentHashMap<>();
        lock1=new Object();
        lock2=new Object();
        lock3=new Object();
        documentsPosting = new HashMap<>();
        documentsDictionary = new HashMap<>();
        dictionary = new ConcurrentHashMap<>();
        posting = new HashMap<>();
        entities = new HashMap<>();
        maxDoc = 20;
        maxTerm=200000;
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
            //if it exists in the dictionary
            char first = termName.charAt(0);
            // the word isn't in the dictionary yet
            if (!posting.containsKey(termName.toLowerCase())) {
                if (Character.isUpperCase(first)) {
                    posting.put(termName.toLowerCase(), new Term(termName.toUpperCase(), docNo, position));
                }
                else
                    posting.put(termName.toLowerCase(), new Term(termName.toLowerCase(), docNo, position));
            } else {
                if (Character.isLowerCase(first) && Character.isUpperCase(posting.get(termName.toLowerCase()).getTermName().charAt(0))) {
                    posting.get(termName.toLowerCase()).changeName(termName.toLowerCase());
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                } else if (posting.containsKey(termName.toUpperCase()) && Character.isUpperCase(first)) {
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                } else {
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                }
            }
        if(posting.size() >= maxTerm){
            double start = System.currentTimeMillis();
            System.out.println(((start-time)/60000)+" parsing time");
            writes=0;
            writeToTempPosting();
            double end =  System.currentTimeMillis();
            System.out.println(((end-start)/60000 )+" writing time");
            System.out.println(writes);
//            ExecutorService executor= Executors.newFixedThreadPool(100);
//            for (Map.Entry<String, Term> stringTermEntry : posting.entrySet()){
//                executor.execute(()->writeTermsToPosting(stringTermEntry));
//            }
//            executor.shutdown();
//            try {
//                executor.awaitTermination(1, TimeUnit.HOURS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            posting.clear();
            iteration++;
            time=end;
        }
    }

    private void writeToTempPosting(){
        try {
            if (!Files.isDirectory(Paths.get("/Posting"))) {
                File postingFolder = new File("/Posting");
                postingFolder.mkdir();
            }
            TreeMap<String, Term> sortedPosting = new TreeMap<>(this.posting);
            Set set = sortedPosting.entrySet();
            Iterator it = set.iterator();
            Term currTerm;
            String charAt0=null;
            String charAt1=null;
            String str=null;
            Element root=null;
            FileInputStream fis=null;
            org.jsoup.nodes.Document postingFileEditer;
            while (it.hasNext()) {
                currTerm = (Term) ((Map.Entry) it.next()).getValue();
                if(!((currTerm.getTermName().charAt(0)+"").toLowerCase()).equals(charAt0) || (currTerm.getTermName().length()>1 && !((currTerm.getTermName().charAt(1)+"").toLowerCase()).equals(charAt1))) {
                    if(root!=null){
                        BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                        writer.write(root.outerHtml());
                        writer.close();
                        fis.close();
                        fis.close();
                        writes++;
                    }
                    charAt0 = ("" + currTerm.getTermName().charAt(0)).toLowerCase();
                    if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1)!=' ' && currTerm.getTermName().charAt(1)!='.')
                        charAt1 = ("" + currTerm.getTermName().charAt(1)).toLowerCase();
                    else if(currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1)==' ')
                        charAt1="_";
                    else if(currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1)=='.')
                        charAt1="dot";
                    else if(currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1)=='/')
                        charAt1="slash";
                    else
                        charAt1 = "single";
                    if (!Files.isDirectory(Paths.get("/Posting/" + charAt0))) {
                        File termPostingFolder = new File("/Posting/" + charAt0);
                        termPostingFolder.mkdir();
                    }
                    str = "/Posting/" + charAt0 + "/" + charAt1;
                    if (!Files.isDirectory(Paths.get(str))) {
                        File termPostingFolder = new File(str);
                        termPostingFolder.mkdir();
                    }
                    str = str + "/" + iteration + ".txt";
                    File termPostingFile = new File(str);
                    termPostingFile.createNewFile();
                    fis = new FileInputStream(termPostingFile);
                    postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                    root = postingFileEditer.createElement("root");
                }
                Element termNode = root.appendElement("term").attr("TERMNAME", currTerm.getTermName());
                HashMap<String, Integer> docs = currTerm.getDocs();
                HashMap<String, List<Integer>> positionsList = currTerm.getPositions();
                termNode.appendElement("df").appendText("" + docs.size());
                Element docsNode = termNode.appendElement("docs");
                for (Map.Entry<String, Integer> entry : docs.entrySet()) {
                    Element docNode = docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
                    docNode.appendElement("TF").appendText("" + entry.getValue());
                    String positions = "";
                    for (Integer pos : positionsList.get(entry.getKey())) {
                        positions += pos + ",";
                    }
                    docNode.appendElement("Positions").appendText(positions);
                }
                if(!dictionary.containsKey(currTerm.getTermName())) {
                    dictionary.put(currTerm.getTermName(), "/Posting/" + charAt0 + "/" + charAt1 + ".txt");
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

        private void writeTermsToPosting(Map.Entry<String, Term> stringTermEntry) {
            try {
                if (!Files.isDirectory(Paths.get("/Posting"))) {
                    File postingFolder = new File("/Posting");
                    postingFolder.mkdir();
                }
                    Term term = stringTermEntry.getValue();
                    if (!dictionary.containsKey(term.getTermName().toLowerCase()) && !dictionary.containsKey(term.getTermName().toUpperCase())) {
                        if (!Files.isDirectory(Paths.get("/Posting/" + term.getTermName().charAt(0)))) {
                            File termPostingFolder = new File("/Posting/" + term.getTermName().toLowerCase().charAt(0));
                            termPostingFolder.mkdir();
                        }
                        String str;
                        if(term.getTermName().length()>1)
                            str = "/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + term.getTermName().toLowerCase().charAt(1) + ".txt";
                        else
                            str = "/Posting/" + term.getTermName().toLowerCase().charAt(0) + "/" + "singlechar.txt";
                        if(!filesLockers.containsKey(str)){
                            filesLockers.put(str,new Object());
                        }
                        synchronized (filesLockers.get(str)) {
                            Path path = Paths.get(str);
                            File termPostingFile = new File(str);
                            boolean boolTemp = false;
                            if (!Files.exists(path)) {
                                termPostingFile.createNewFile();
                                boolTemp = true;
                            }
                            FileInputStream fis = new FileInputStream(termPostingFile);
                            org.jsoup.nodes.Document postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                            Element root;
                            if (boolTemp == true)
                                root = postingFileEditer.createElement("root");
                            else
                                root = postingFileEditer.selectFirst("root");
                            Element termNode = root.appendElement("term").attr("TERMNAME", term.getTermName());
                            HashMap<String, Integer> docs = term.getDocs();
                            HashMap<String, List<Integer>> positionsList = term.getPositions();
                            termNode.appendElement("df").appendText("" + docs.size());
                            Element docsNode = termNode.appendElement("docs");
                            for (Map.Entry<String, Integer> entry : docs.entrySet()) {
                                Element docNode = docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
                                docNode.appendElement("TF").appendText("" + entry.getValue());
                                String positions = "";
                                for (Integer pos : positionsList.get(entry.getKey())) {
                                    positions += pos + ",";
                                }
                                docNode.appendElement("Positions").appendText(positions);
                            }
                                dictionary.put(term.getTermName(), str);
                                BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                                writer.write(root.outerHtml());
                                writer.close();
                                fis.close();
                        }
                    }
                    else{
                        String src;
                        String tagName;
                        FileInputStream fis;
                        Element termNode;
                        BufferedWriter writer;
                        org.jsoup.nodes.Document postingFileEditer;
                        Element root;
                        if (Character.isLowerCase(term.getTermName().charAt(0)) && dictionary.containsKey(term.getTermName().toUpperCase())) {
                            synchronized (filesLockers.get(dictionary.get(term.getTermName().toUpperCase()))) {
                                dictionary.put(term.getTermName(), dictionary.get(term.getTermName().toUpperCase()));
                                dictionary.remove(term.getTermName().toUpperCase());
                                fis = new FileInputStream(new File(dictionary.get(term.getTermName())));
                                postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                                root = postingFileEditer.selectFirst("root");
                                termNode = root.selectFirst("term[TERMNAME='" + term.getTermName().toUpperCase() + "']");
                                termNode.attr("TERMNAME", term.getTermName());
                                tagName = term.getTermName();
                                src = dictionary.get(term.getTermName());
                                updateDocsinTerms(term,tagName,postingFileEditer,src);
                                fis.close();
                            }
                        }
                        else if(Character.isUpperCase(term.getTermName().charAt(0)) && dictionary.containsKey(term.getTermName().toLowerCase())){
                            synchronized (filesLockers.get(dictionary.get(term.getTermName().toLowerCase()))) {
                                fis = new FileInputStream(new File(dictionary.get(term.getTermName().toLowerCase())));
                                postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                                tagName = term.getTermName().toLowerCase();
                                root = postingFileEditer.selectFirst("root");
                                termNode = root.selectFirst("term[TERMNAME='" + tagName + "']");
                                src = dictionary.get(term.getTermName().toLowerCase());
                                updateDocsinTerms(term,tagName,postingFileEditer,src);
                                fis.close();
                            }
                        }
                        else{
                            synchronized (filesLockers.get(dictionary.get(term.getTermName()))) {
                                fis = new FileInputStream(new File(dictionary.get(term.getTermName())));
                                postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                                tagName = term.getTermName();
                                root = postingFileEditer.selectFirst("root");
                                termNode = root.selectFirst("term[TERMNAME='" + term.getTermName() + "']");
                                src = dictionary.get(term.getTermName());
                                updateDocsinTerms(term,tagName,postingFileEditer,src);
                                fis.close();
                            }
                        }
//                        HashMap<String,Integer> docs=term.getDocs();
//                        HashMap<String, List<Integer>> positionsList= term.getPositions();
//                        int df=Integer.parseInt(termNode.select("df").first().text());
//                        Element docsNode=termNode.select("docs").first();
//                        for(Map.Entry<String,Integer> entry : docs.entrySet()){
//                            String positions="";
//                            for(Integer pos: positionsList.get(entry.getKey())){
//                                positions+=pos+",";
//                            }
//                            if(docsNode.select("doc[DOCNAME='"+entry.getKey()+"']").first()==null){
//                                df++;
//                                Element docNode=docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());
//                                docNode.appendElement("TF").appendText(""+entry.getValue());
//                                docNode.appendElement("Positions").appendText(positions);
//                            }
//                            else{
//                                Element docNode=docsNode.select("doc[DOCNAME="+entry.getKey()+"]").first();
//                                int tf=Integer.parseInt(docNode.select("TF").first().text());
//                                tf+=entry.getValue();
//                                docNode.select("TF").first().text(""+tf);
//                                String str=docNode.select("Positions").first().text();
//                                str+=positions;
//                                docNode.select("Positions").first().text(str);
//                            }
//                        }
//                        termNode.select("df").first().text(""+df);
//                        writer= new BufferedWriter(new FileWriter(src));
//                        writer.write(postingFileEditer.selectFirst("root").outerHtml());
//                        writer.close();
//                        fis.close();
                    }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateDocsinTerms(Term term, String tagName, org.jsoup.nodes.Document postingFileEditer, String src){
            Element root=postingFileEditer.selectFirst("root");
            Element termNode=root.selectFirst("term[TERMNAME='"+tagName+"']");
            HashMap<String,Integer> docs=term.getDocs();
            HashMap<String, List<Integer>> positionsList= term.getPositions();
            int check=0;
            if(!termNode.select("df").hasText())
                check=5;
            int df=Integer.parseInt(termNode.select("df").first().text());
            Element docsNode=termNode.select("docs").first();
            for(Map.Entry<String,Integer> entry : docs.entrySet()){
                String positions="";
                for(Integer pos: positionsList.get(entry.getKey())){
                    positions+=pos+",";
                }
                if(docsNode.select("doc[DOCNAME='"+entry.getKey()+"']").first()==null){
                    df++;
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
            termNode.select("df").first().text(""+df);
            BufferedWriter writer= null;
            try {
                writer = new BufferedWriter(new FileWriter(src));
                writer.write(postingFileEditer.selectFirst("root").outerHtml());
                writer.close();
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
                //writeDocsToPosting();
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
