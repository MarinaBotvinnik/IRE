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
import java.util.concurrent.*;

public class Indexer {
    private String path;
    private volatile int iteration;
    private double time;
    int maxTerm;
    int maxDoc;
    int writes;
    int docDirectoryNum;
    boolean isStem;
    HashMap<String, Document> documentsPosting;
    HashMap<String, String> documentsDictionary;
    volatile ConcurrentHashMap<String, String> dictionary;
    volatile ConcurrentHashMap<String, Term> posting;
    HashMap<String, Term> entities;

    public Indexer(boolean stem) {
        path="C:/";
        iteration=0;
        writes=0;
        time=System.currentTimeMillis();
        documentsPosting = new HashMap<>();
        documentsDictionary = new HashMap<>();
        dictionary = new ConcurrentHashMap<>();
        posting = new ConcurrentHashMap<>();
        entities = new HashMap<>();
        maxDoc = 10000;
        maxTerm=100000;
        docDirectoryNum =1;
        isStem = false;
    }

    public void setStem(boolean stem) {
        isStem = stem;
    }

    public int getNumOfDocs() {
        return documentsDictionary.size();
    }

    public int getNumOfTerm() {
        return dictionary.size();
    }

    public void reset() {
        documentsPosting.clear();
        documentsDictionary.clear();
        dictionary.clear();
        posting.clear();
        entities.clear();
    }

    public void setPath(String path) {
        if (isStem) {
            this.path = path + "/Stemming";
        } else this.path = path + "/noStemming";
    }

    public void addTermToDic(String Name, String docNo, int position) {
        String termName = Name;
        if (isStem) {
            boolean isUP = Character.isUpperCase(termName.charAt(0));
            Stemmer stemmer = new Stemmer();
            stemmer.add(Name.toCharArray(),Name.length());
            stemmer.stem();
            termName = stemmer.toString();
            if(isUP)
                termName = termName.toUpperCase();
        }
            //if it exists in the dictionary
            char first = termName.charAt(0);
            // the word isn't in the dictionary yet
            if (!posting.containsKey(termName.toLowerCase())) {
                if (Character.isUpperCase(first)) {
                    posting.put(termName.toLowerCase(), new Term(termName.toUpperCase(), docNo, position));
                } else posting.put(termName.toLowerCase(), new Term(termName.toLowerCase(), docNo, position));
            } else {
                if (Character.isLowerCase(first) && Character.isUpperCase(posting.get(termName.toLowerCase()).getTermName().charAt(0))) {
                    posting.get(termName.toLowerCase()).changeName(termName.toLowerCase());
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                } else {
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position);
                }
            }
       // if(documentsPosting.size() >= maxDoc){
//            double start = System.currentTimeMillis();
//            System.out.println(((start-time)/60000)+" parsing time");
//            writes=0;
//            iteration++;
//            TreeMap<String, Term> sortedPosting = new TreeMap<>(this.posting);
//            this.posting.clear();
//            Thread thread=new Thread(()->writeToTempPosting(this.iteration,sortedPosting));
//            thread.start();
//            double end =  System.currentTimeMillis();
//            System.out.println(((end-start)/60000 )+" writing time");
//            System.out.println(writes);
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
//            time=end;
        //}
    }

    private void writeToTempPosting(int iteration, TreeMap<String, Term> sortedPosting){
        try {
            if (!Files.isDirectory(Paths.get("/Posting"))) {
                File postingFolder = new File("/Posting");
                postingFolder.mkdir();
            }
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
                    if(currTerm.getTermName().charAt(0)!='/')
                        charAt0 = ("" + currTerm.getTermName().charAt(0)).toLowerCase();
                    else
                        charAt0="slash";
                    if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1)!=' ' && currTerm.getTermName().charAt(1)!='.' && currTerm.getTermName().charAt(1)!='/')
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
                //writeDocsToPosting();
                iteration++;
                TreeMap<String, Term> sortedPosting = new TreeMap<>(this.posting);
                this.posting.clear();
                this.documentsPosting.clear();
                Thread thread=new Thread(()->writeToTempPosting(this.iteration,sortedPosting));
                thread.start();
            }

        }

    public void closeIndexer() {
        //writeDictionary("TermDictionary",dictionary);
        this.dictionary.clear();
        //writeDictionary("DocumentsDictionary",documentsDictionary);
        this.documentsDictionary.clear();
        mergePostingToOne(this.path + "/Posting");
    }

        // all the files of the first letter
        private void mergePostingToOne(String filePath) {
            Thread[] threads=new Thread[50];
            int j=0;
            File file = new File(filePath);
            File[] firstLetters = file.listFiles();
//            ExecutorService executor= Executors.newFixedThreadPool(2);
            for (File firstLetter : firstLetters) {
                File[] secondLetters = firstLetter.listFiles();
//                Runnable runnable = () -> {
                    for (File secLetter : secondLetters) {
                        if(secLetter.isDirectory()) {
                            mergeToOne(secLetter);
                        }
                    }
                    for (int i = 0; i < secondLetters.length; i++) {
                        if(secondLetters[i].isDirectory())
                            secondLetters[i].delete();
                    }
//                };
//                executor.execute(runnable);
            }
//            executor.shutdown();
//            try {
//                executor.awaitTermination(1,TimeUnit.HOURS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        // merge all the files
        private void mergeToOne(File secondLetters) {
            try {
                File[] iters = secondLetters.listFiles();
                if(iters.length!=0) {
                    File termPostingFile = new File(secondLetters.getAbsolutePath() + ".txt");
                    if(secondLetters.getAbsolutePath().contains("1\\0"))
                        System.out.println("check");
                    termPostingFile.createNewFile();
                    FileInputStream fis = new FileInputStream(termPostingFile);
                    org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                    fis.close();
                    Element root = postingFileEditor.createElement("root");
                    for (int i = 0; i < iters.length; i++) {
                        FileInputStream fis2 = new FileInputStream(iters[i]);
                        org.jsoup.nodes.Document tempEditor = Jsoup.parse(fis2, null, "", Parser.xmlParser());
                        fis2.close();
                        iters[i].delete();
                        Elements terms = tempEditor.select("term");
                        Elements currTerms = root.select("term");
                        Iterator elementIter=currTerms.iterator();
                        for (int j=0; j<terms.size(); j++) {
                            Element term=terms.first();
                            String name = term.attr("TERMNAME");
                            Element currTerm=null;
                            boolean isNew=true;
                            boolean appended=false;
                            while(elementIter.hasNext()){
                                currTerm=(Element)elementIter.next();
                                if(currTerm.attr("TERMNAME").toLowerCase().equals(name.toLowerCase())) {
                                    isNew = false;
                                    break;
                                }
                                else if(currTerm.attr("TERMNAME").toLowerCase().compareTo(name.toLowerCase())>0){
                                    currTerm.before(term.outerHtml());
                                    terms.remove(term);
                                    appended=true;
                                    break;
                                }
                            }
                            if(isNew && !appended){
                                root.appendChild(term);
                                terms.remove(term);
                                continue;
                            }
                            else if(isNew && appended) {
                                terms.remove(term);
                                continue;
                            }
                            else{
                                if(Character.isUpperCase(currTerm.attr("TERMNAME").charAt(0)) && Character.isLowerCase(name.charAt(0))){
                                    currTerm.attr("TERMNAME",name);
                                }
                                //update df
                                int dfCurr = Integer.parseInt(currTerm.select("df").first().text());
                                int dfMerge = Integer.parseInt(term.select("df").first().text());
                                currTerm.select("df").first().text(""+(dfCurr+dfMerge));
                                //update the tf
                                Elements docsCurr = currTerm.select("doc");
                                Elements docsMerge = term.select("doc");
                                currTerm.selectFirst("docs").append(docsMerge.outerHtml());
                                terms.remove(term);
                            }
                        }
                    }
                    Elements all = root.select("term");
                    String toSave="";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(secondLetters.getAbsolutePath() + ".txt"));
                    while(!all.isEmpty()) {
                        for(int i=0; i<100; i++) {
                            if(!all.isEmpty()) {
                                toSave += all.first().outerHtml();
                            }
                            else{
                                break;
                            }
                            all.remove(all.first());
                        }
                        writer.write(toSave);
                        writer.newLine();
                        writer.flush();
                        toSave="";
                    }
                    root.empty();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }

    private void writeDictionary(String dicName, HashMap<String, String> dictionary) {
        try {
            Path path = Paths.get(this.path + "/" + dicName);
            if (!Files.isDirectory(path)) {
                File postingFolder = new File("/" + dicName);
                postingFolder.mkdir();
            }
            String str = "/" + dicName + "/" + dicName + ".txt";
            File termPostingFile = new File(str);
            termPostingFile.createNewFile();
            FileInputStream fis = new FileInputStream(termPostingFile);
            org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
            BufferedWriter writer = new BufferedWriter(new FileWriter(str));
            Element root = postingFileEditor.createElement("root");
            for (Map.Entry<String, String> stringIntegerEntry : dictionary.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                String termName = (String) pair.getKey();
                String termPath = (String) pair.getValue();
                Element docAtt = postingFileEditor.createElement(termName);
                docAtt.appendElement("PATH").appendText(termPath);
                root.appendChild(docAtt);
            }
            writer.write(root.html());
            writer.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TreeMap<String, String> uploadDictionary() {
        HashMap<String, String> dic = new HashMap<>();
        try {
            //NEED TO CHANGE THE PATH TO THE DICTIONARY PATH
            FileInputStream fis = new FileInputStream(new File(this.path + "/DocumentsDictionary/DocumentsDictionary.txt"));
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