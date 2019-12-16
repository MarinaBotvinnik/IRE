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
import java.util.concurrent.ConcurrentHashMap;

public class Indexer {
    private int maxTerm;
    private int maxDoc;
    private int docDirectoryNum;
    private boolean isStem;
    private String path;
    private HashMap<String, Document> documentsPosting;
    private HashMap<String, String> documentsDictionary;
    private ConcurrentHashMap<String, String> dictionary;
    private HashMap<String, Term> posting;
    private HashMap<String, Term> entities;
    private int iteration;
    private int writes;
    private double time;
    private volatile ConcurrentHashMap<String, Object> filesLockers;

    public Indexer(boolean stem) {
        iteration = 0;
        writes = 0;
        time = System.currentTimeMillis();
        filesLockers = new ConcurrentHashMap<>();
        documentsPosting = new HashMap<>();
        documentsDictionary = new HashMap<>();
        dictionary = new ConcurrentHashMap<>();
        posting = new HashMap<>();
        entities = new HashMap<>();
        maxDoc = 10000;
        maxTerm = 50;
        maxDoc = 20;
        maxTerm = 50000;
        docDirectoryNum = 1;
        isStem = stem;
        path = "C:/";
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
            stemmer.add(Name.toCharArray(), Name.length());
            stemmer.stem();
            termName = stemmer.toString();
            if (isUP)
                termName = termName.toUpperCase();
        }
        //if it exists in the dictionary
        char first = termName.charAt(0);
        // the word isn't in the dictionary yet
        if (!posting.containsKey(termName.toUpperCase()) || !posting.containsKey(termName.toLowerCase())) {
            if (Character.isUpperCase(first)) {
                posting.put(termName.toUpperCase(), new Term(termName.toUpperCase(), docNo, position));
            } else posting.put(termName.toLowerCase(), new Term(termName.toLowerCase(), docNo, position));
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
        if (posting.size() >= maxTerm) {
            double start = System.currentTimeMillis();
            System.out.println(((start - time) / 60000) + " parsing time");
            writes = 0;
            writeToTempPosting();
            double end = System.currentTimeMillis();
            System.out.println(((end - start) / 60000) + " writing time");
            System.out.println(writes);
            posting.clear();
            iteration++;
            time = end;
        }
    }

    private void writeToTempPosting() {
        try {
            if (!Files.isDirectory(Paths.get("/Posting"))) {
                File postingFolder = new File("/Posting");
                postingFolder.mkdir();
            }
            TreeMap<String, Term> sortedPosting = new TreeMap<>(this.posting);
            Set set = sortedPosting.entrySet();
            Iterator it = set.iterator();
            Term currTerm;
            String charAt0 = null;
            String charAt1 = null;
            String str = null;
            Element root = null;
            FileInputStream fis = null;
            org.jsoup.nodes.Document postingFileEditer;
            while (it.hasNext()) {
                currTerm = (Term) ((Map.Entry) it.next()).getValue();
                if (!((currTerm.getTermName().charAt(0) + "").toLowerCase()).equals(charAt0) || (currTerm.getTermName().length() > 1 && !((currTerm.getTermName().charAt(1) + "").toLowerCase()).equals(charAt1))) {
                    if (root != null) {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                        writer.write(root.outerHtml());
                        writer.close();
                        fis.close();
                        fis.close();
                        writes++;
                    }
                    charAt0 = ("" + currTerm.getTermName().charAt(0)).toLowerCase();
                    if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) != ' ' && currTerm.getTermName().charAt(1) != '.')
                        charAt1 = ("" + currTerm.getTermName().charAt(1)).toLowerCase();
                    else if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) == ' ')
                        charAt1 = "_";
                    else if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) == '.')
                        charAt1 = "dot";
                    else if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) == '/')
                        charAt1 = "slash";
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
                if (!dictionary.containsKey(currTerm.getTermName())) {
                    dictionary.put(currTerm.getTermName(), "/Posting/" + charAt0 + "/" + charAt1 + ".txt");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeDocsToPosting() {
        try {
            Path path = Paths.get(this.path + "/DocumentsPosting");
            if (!Files.isDirectory(path)) {
                File postingFolder = new File(this.path + "/DocumentsPosting");
                postingFolder.mkdir();
            }
            File termPostingFolder = new File(this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1));
            termPostingFolder.mkdir();
            String str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
            File termPostingFile = new File(str);
            termPostingFile.createNewFile();
            FileInputStream fis = new FileInputStream(termPostingFile);
            org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
            Element root = postingFileEditor.createElement("root");
            docDirectoryNum += maxDoc;
            BufferedWriter writer = new BufferedWriter(new FileWriter(str));
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
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

    public void addEntToDic(String Name, String docNo, int position) {
        //its an entity that showed up only once and now its its second time
        if (entities.containsKey(Name)) {
            Term ent = entities.remove(Name);
            ent.addDocPosition(docNo, position);
            ent.addTf(docNo);
            posting.put(Name, ent);
            //the dictionary hasn't been written yet
            if (!documentsDictionary.containsKey(docNo)) {
                Document document = documentsPosting.get(docNo);
                document.addTerm(Name);
                document.closeDoc();
            } else {
                String path = documentsDictionary.get(docNo);

            }
        }
        //its entity that appeared more than once but we didnt write it yet
        else if (posting.containsKey(Name)) {
            Term ent = posting.remove(Name);
            ent.addTf(docNo);
            ent.addDocPosition(docNo, position);
            posting.put(Name, ent);

        }
        //its an entity that has been written before so we need to update it
        else if (dictionary.containsKey(Name)) {

        }
        //its completely new entity!
        else {
            Term term = new Term(Name, docNo, position);
            entities.put(Name, term);
        }
    }

    public void addDocToDic(Document doc) {
        if (documentsPosting.size() < maxDoc) {
            documentsPosting.put(doc.getDocName(), doc);
        } else {
            //writeDocsToPosting();
        }

    }

    public void closeIndexer() {
        //writeDictionary("TermDictionary",dictionary);
        //writeDictionary("DocumentsDictionary",documentsDictionary);
        mergePostingToOne(this.path + "/Posting");
    }

    // all the files of the first letter
    private void mergePostingToOne(String filePath) {
        File file = new File(filePath);
        File[] firstLetters = file.listFiles();
        for (File secondLetter : firstLetters) {
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
            for (File iter : iters) {
                FileInputStream fis2 = new FileInputStream(iter);
                org.jsoup.nodes.Document tempEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                Elements terms = tempEditor.select("term");
                for (Element term : terms) {
                    String name = term.attr("TERMNAME");
                    Element upper = root.selectFirst("term[TERMNAME='" + name.toUpperCase() + "']");
                    Element lower = root.selectFirst("term[TERMNAME='" + name.toLowerCase() + "']");
                    Element currTerm;
                    //it's not on the final merge yet
                    if (upper == null && lower == null) {
                        root.appendChild(term);
                        continue;
                    }
                    if (upper != null && Character.isLowerCase(name.charAt(0))) {
                        upper.attr("TERMNAME", name);
                        currTerm = upper;
                    } else {
                        currTerm = lower;
                    }
                    //update idf
                    int idfCurr = Integer.parseInt(currTerm.select("idf").first().text());
                    int idfMerge = Integer.parseInt(term.select("idf").first().text());
                    currTerm.select("idf").first().replaceWith(new TextNode("" + idfCurr + idfMerge));
                    //update the tf
                    Elements docsCurr = currTerm.select("doc");
                    Elements docsMerge = term.select("doc");
                    for (Element doc : docsMerge) {
                        if (docsCurr.select("doc[DOCNAME=" + doc.attr("DOCNAME") + "]") == null) {
                            docsCurr.append(doc.html());
                        } else {
                            int tfCurr = Integer.parseInt(docsCurr.select("doc[DOCNAME=" + doc.attr("DOCNAME") + "]").select("tf").first().text());
                            int tfMerge = Integer.parseInt(doc.select("tf").first().text());
                            docsCurr.select("doc[DOCNAME=" + doc.attr("DOCNAME") + "]").select("tf").first().replaceWith(new TextNode("" + tfCurr + tfMerge));
                            String positions = doc.select("positions").first().text();
                            String currPositions = docsCurr.select("doc[DOCNAME=" + doc.attr("DOCNAME") + "]").select("positions").first().text();
                            currPositions = currPositions + positions;
                            docsCurr.select("doc[DOCNAME=" + doc.attr("DOCNAME") + "]").select("positions").first().replaceWith(new TextNode(currPositions));
                        }
                    }

                }
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
