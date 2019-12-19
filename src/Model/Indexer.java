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
import java.util.concurrent.*;

public class Indexer {
    private ExecutorService executor;
    private String path;
    private volatile int iteration;
    private double time;
    private int maxTerm;
    private int maxDoc;
    private int writes;
    private int docDirectoryNum;
    private boolean isStem;
    private ConcurrentHashMap<String, Document> documentsPosting;
    private ConcurrentHashMap<String, String> documentsDictionary;
    private volatile ConcurrentHashMap<String, String> dictionary;
    private volatile ConcurrentHashMap<String, Term> posting;
    private HashMap<String, Term> entities;
    private int numOfTerms;
    private int numOfDocs;

    public Indexer(boolean stem) {
        path = "";
        iteration = 0;
        writes = 0;
        time = System.currentTimeMillis();
        documentsPosting = new ConcurrentHashMap<>();
        documentsDictionary = new ConcurrentHashMap<>();
        dictionary = new ConcurrentHashMap<>();
        posting = new ConcurrentHashMap<>();
        entities = new HashMap<>();
        maxDoc = 1000;
        maxTerm = 100000;
        docDirectoryNum = 1;
        isStem = stem;
        executor = Executors.newFixedThreadPool(8);
        numOfDocs = 0;
        numOfTerms = 0;
    }

    public void setStem(boolean stem) {
        isStem = stem;
    }

    public int getNumOfDocs() {
        return numOfDocs;
    }

    public int getNumOfTerm() {
        return numOfTerms;
    }

    public void reset() {
        documentsPosting.clear();
        documentsDictionary.clear();
        dictionary.clear();
        posting.clear();
        entities.clear();
        deleteAll(this.path);
        File file = new File(this.path);
        file.delete();
    }

    private void deleteAll(String filepath) {
        final File folder = new File(filepath);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                deleteAll(fileEntry.getPath());
                fileEntry.delete();
            } else {//I GOT TO THE DOCUMENT
                fileEntry.delete();
            }
        }

    }

    public void setPath(String path) {
        if (isStem) {
            this.path = path + "\\Stemming";
        } else this.path = path + "\\noStemming";
        File file = new File(this.path);
        if (!file.isDirectory())
            file.mkdir();
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
    }

    private void writeToTempPosting(int iteration, TreeMap<String, Term> sortedPosting) {
        try {
            if (!Files.isDirectory(Paths.get(this.path + "/Posting"))) {
                File postingFolder = new File(this.path + "/Posting");
                postingFolder.mkdir();
            }
            Set set = sortedPosting.entrySet();
            Iterator it = set.iterator();
            Term currTerm;
            boolean needWrite = false;
            String charAt0 = null;
            String charAt1 = null;
            String str = null;
            String toWrite = "";
            while (it.hasNext()) {
                currTerm = (Term) ((Map.Entry) it.next()).getValue();
                if (!((currTerm.getTermName().charAt(0) + "").toLowerCase()).equals(charAt0) || (currTerm.getTermName().length() > 1 && !((currTerm.getTermName().charAt(1) + "").toLowerCase()).equals(charAt1))) {
                    if (needWrite) {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                        //writer.write(root.outerHtml());//------------------------------
                        writer.write(toWrite);//*************************************
                        writer.close();
                        //fis.close();
                        writes++;
                        toWrite = "";
                    }
                    if (currTerm.getTermName().charAt(0) != '/')
                        charAt0 = ("" + currTerm.getTermName().charAt(0)).toLowerCase();
                    else
                        charAt0 = "slash";
                    if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) != ' ' && currTerm.getTermName().charAt(1) != '.' && currTerm.getTermName().charAt(1) != '/')
                        charAt1 = ("" + currTerm.getTermName().charAt(1)).toLowerCase();
                    else if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) == ' ')
                        charAt1 = "_";
                    else if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) == '.')
                        charAt1 = "dot";
                    else if (currTerm.getTermName().length() > 1 && currTerm.getTermName().charAt(1) == '/')
                        charAt1 = "slash";
                    else
                        charAt1 = "single";
                    if (!Files.isDirectory(Paths.get(this.path + "/Posting/" + charAt0))) {
                        File termPostingFolder = new File(this.path + "/Posting/" + charAt0);
                        termPostingFolder.mkdir();
                    }
                    str = this.path + "/Posting/" + charAt0 + "/" + charAt1;
                    if (!Files.isDirectory(Paths.get(str))) {
                        File termPostingFolder = new File(str);
                        termPostingFolder.mkdir();
                    }
                    str = str + "/" + iteration + ".txt";
                    File termPostingFile = new File(str);
                    termPostingFile.createNewFile();
                    //fis = new FileInputStream(termPostingFile);
                    //postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());//--------------------------------
                    //root = postingFileEditer.createElement("root");//------------------------------------------------
                    needWrite = true;
                }
                //Element termNode = root.appendElement("term").attr("TERMNAME", currTerm.getTermName());//----------------------------
                HashMap<String, Integer> docs = currTerm.getDocs();
                HashMap<String, List<Integer>> positionsList = currTerm.getPositions();
                toWrite += currTerm.getTermName() + "[" + docs.size() + "]";//******************************************************************
                //termNode.appendElement("df").appendText("" + docs.size());//----------------------------------------
                //Element docsNode = termNode.appendElement("docs");//---------------------------------------------------------
                for (Map.Entry<String, Integer> entry : docs.entrySet()) {
                    //Element docNode = docsNode.appendElement("doc").attr("DOCNAME", entry.getKey());//-----------------------------------------------
                    toWrite += "[" + entry.getKey() + "," + entry.getValue() + ",";//****************************************
                    //docNode.appendElement("TF").appendText("" + entry.getValue());//---------------------------------------------------------
                    if (!dictionary.containsKey(currTerm.getTermName())) {
                        dictionary.put(currTerm.getTermName(), this.path + "\\Posting\\" + charAt0 + "\\" + charAt1 + ".txt" + "," + entry.getValue());
                    } else {
                        String vals[] = ("" + dictionary.get(currTerm.getTermName())).split(",");
                        int tf = Integer.parseInt(vals[1]) + Integer.parseInt("" + entry.getValue());
                        dictionary.replace(currTerm.getTermName(), this.path + "\\Posting\\" + charAt0 + "\\" + charAt1 + ".txt" + "," + tf);
                    }
                    String positions = "";
                    for (Integer pos : positionsList.get(entry.getKey())) {
                        positions += pos + ",";
                    }
                    //docNode.appendElement("Positions").appendText(positions);//-------------------------------------------
                    toWrite += positions + "]";//***************************************************************
                }
                toWrite += "\n";
                if (!it.hasNext()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                    //writer.write(root.outerHtml());//------------------------------
                    writer.write(toWrite);//*************************************
                    writer.close();
                    //fis.close();
                    writes++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDocsToPosting(ConcurrentHashMap<String, Document> documentsPosting) {
        try {
            Path path = Paths.get(this.path + "/DocumentsPosting");
            if (!Files.isDirectory(path)) {
                File postingFolder = new File(this.path + "/DocumentsPosting");
                postingFolder.mkdir();
            }
            String str;
            FileInputStream fis = null;
            synchronized (this) {
                File termPostingFolder = new File(this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1));
                termPostingFolder.mkdir();
                str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
                File termPostingFile = new File(termPostingFolder.getAbsolutePath(), (docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1)) + ".txt");
                termPostingFile.createNewFile();
                fis = new FileInputStream(termPostingFile);//---------------------
                docDirectoryNum += maxDoc;
            }
            //org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());//------------------
            //Element root = postingFileEditor.createElement("root");//-------------------------
            BufferedWriter writer = new BufferedWriter(new FileWriter(str));
            String toWrite = "";
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
                //Element docAtt = postingFileEditor.createElement(document.getDocName());//-----------------
                if (document == null || document.getMax_Term_name() == null)
                    System.out.printf("ohhh nooo document is null");
                //docAtt.appendElement("maxTf").appendText("" + document.getMax_tf());//------------------------
                //docAtt.appendElement("maxTfName").appendText(""+document.getMax_Term_name());//--------------------------
                //docAtt.appendElement("uniqueTerms").appendText("" + document.getUniqueTermsNum());//-------------------------
                //root.appendChild(docAtt);//-----------------------------
                toWrite += document.getDocName() + "," + document.getMax_tf() + "," + document.getMax_Term_name() + "," + document.getUniqueTermsNum() + "\n";
            }
            //writer.write(root.html());//------------------------------
            writer.write(toWrite);
            writer.close();
            //fis.close();
            documentsPosting.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addEntToDic(String Name, String docNo, int position) {
        //its an entity that showed up only once and now its its second time
        if (entities.containsKey(Name) && !entities.get(Name).getDocs().containsKey(docNo)) {
            Term ent = entities.remove(Name);
            String oldDoc = ent.getDocs().keySet().iterator().next();
            ent.addDocPosition(docNo, position);
            posting.put(Name.toLowerCase(), ent);
            //the dictionary hasn't been written yet
            if (!documentsDictionary.containsKey(oldDoc)) {
                Document document = documentsPosting.get(oldDoc);
                if (ent.getDocs().get(oldDoc) == null || document == null)
                    System.out.println("sd");
                document.addTermWithTF(Name, ent.getDocs().get(oldDoc));
                document.closeDoc();
            } else {
                String path = documentsDictionary.get(oldDoc);
                File termPostingFile = new File(path);
                FileInputStream fis;
//                    try {
//                        fis = new FileInputStream(termPostingFile);
//                        org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
//                        fis.close();
//                        Element doc=postingFileEditor.selectFirst(oldDoc);
//                        int maxTf=Integer.parseInt(doc.selectFirst("maxtf").text());
//                        if(ent.getDocs().get(oldDoc)>maxTf){
//                            doc.selectFirst("maxtf").text(""+ent.getDocs().get(oldDoc));
//                            doc.selectFirst("maxtfname").text(ent.getTermName());
//                            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
//                            writer.write(postingFileEditor.html());
//                            writer.close();
//                        }
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
            }
            return true;
        }
        //its entity that appeared more than once but we didnt write it yet
        else if (posting.containsKey(Name.toLowerCase())) {
            Term ent = posting.get(Name.toLowerCase());
            ent.addDocPosition(docNo, position);
            return true;
        }
        //its an entity that has been written before so we need to update it
        else if (dictionary.containsKey(Name.toUpperCase())) {
            posting.put(Name.toLowerCase(), new Term(Name.toUpperCase(), docNo, position));
            return true;
        }
        //its completely new entity!
        else {
            Term term = new Term(Name, docNo, position);
            entities.put(Name, term);
            return false;
        }
    }

    public void addDocToDic(Document doc) {
        documentsPosting.put(doc.getDocName(), doc);
        if (documentsPosting.size() >= maxDoc) {
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
                String str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
                documentsDictionary.put(document.getDocName(), str);
            }
            ConcurrentHashMap<String, Document> copy = new ConcurrentHashMap<>(documentsPosting);
            this.documentsPosting.clear();
            Runnable runnable1 = () -> {
                writeDocsToPosting(copy);
            };
            this.executor.execute(runnable1);
            iteration++;
            ConcurrentHashMap<String, Term> copy2 = new ConcurrentHashMap<>(this.posting);
            this.posting.clear();
            Runnable runnable2 = () -> {
                TreeMap<String, Term> sortedPosting = new TreeMap<>(copy2);
                writeToTempPosting(this.iteration, sortedPosting);
            };
            this.executor.execute(runnable2);
        }
    }

    public void closeIndexer() {
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!posting.isEmpty() || !documentsPosting.isEmpty()) {
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
                String str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
                documentsDictionary.put(document.getDocName(), str);
            }
            ConcurrentHashMap<String, Document> copy = documentsPosting;
            Thread thread1 = new Thread(() -> writeDocsToPosting(copy));
            thread1.start();
            iteration++;
            TreeMap<String, Term> sortedPosting = new TreeMap<>(this.posting);
            this.posting.clear();
            this.documentsPosting.clear();
            writeToTempPosting(this.iteration, sortedPosting);
        }
        writeDictionary("TermDictionary", dictionary);
        numOfTerms = dictionary.size();
        this.dictionary.clear();
        writeDictionary("DocumentsDictionary", documentsDictionary);
        numOfDocs = documentsDictionary.size();
        this.documentsDictionary.clear();
        mergePostingToOne(this.path + "\\Posting");
    }

    // all the files of the first letter
    private void mergePostingToOne(String filePath) {
        File file = new File(filePath);
        File[] firstLetters = file.listFiles();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (File firstLetter : firstLetters) {
            File[] secondLetters = firstLetter.listFiles();
            Runnable runnable = () -> {
                for (File secLetter : secondLetters) {
                    if (secLetter.isDirectory()) {
                        mergeToOne2(secLetter);
                    }
                }
                for (int i = 0; i < secondLetters.length; i++) {
                    if (secondLetters[i].isDirectory())
                        secondLetters[i].delete();
                }
            };
            executor.execute(runnable);
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // merge all the files
    private void mergeToOne2(File secondLetters) {
        try {
            File[] iters = secondLetters.listFiles();
            if (iters.length != 0) {
                File termPostingFile = new File(secondLetters.getAbsolutePath() + ".txt");
                termPostingFile.createNewFile();
                LinkedList<String> toWrite = new LinkedList<>();
                for (int i = 0; i < iters.length; i++) {
                    LinkedList<String> temp = new LinkedList<>(toWrite);
                    toWrite.clear();
                    Iterator elementIter = temp.iterator();
                    BufferedReader br = new BufferedReader(new FileReader(iters[i]));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.equals("]") || line.equals(""))
                            continue;
                        String[] seperated1 = line.split("[\\[\\]]");
                        boolean isNew = true;
                        boolean appended = false;
                        while (elementIter.hasNext()) {
                            String term = (String) elementIter.next();
                            String[] seperated2 = term.split("[\\[\\]]");
                            if (seperated1.length == 0 || seperated2.length == 0)
                                System.out.println("sdf");
                            if (seperated2[0].toLowerCase().equals(seperated1[0].toLowerCase())) {
                                String termName;
                                if (Character.isUpperCase(seperated2[0].charAt(0)) && Character.isLowerCase(seperated1[0].charAt(0))) {
                                    termName = seperated1[0];
                                } else
                                    termName = seperated2[0];
                                int df = Integer.parseInt(seperated1[1]) + Integer.parseInt(seperated2[1]);
                                String docs = term.substring(term.indexOf("][") + 1) + line.substring(line.indexOf("][") + 1);
                                toWrite.add(termName + "[" + df + "]" + docs);
                                isNew = false;
                                appended = true;
                                break;
                            } else if (seperated2[0].toLowerCase().compareTo(seperated1[0].toLowerCase()) > 0) {
                                toWrite.add(line);
                                toWrite.add(term);
                                appended = true;
                                break;
                            } else {
                                toWrite.add(term);
                            }
                        }
                        if (isNew && !appended) {
                            toWrite.add(line);
                        }
                    }
                    br.close();
                    while (elementIter.hasNext()) {
                        toWrite.add((String) elementIter.next());
                    }
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(secondLetters.getAbsolutePath() + ".txt"));
                String finalize = "";
                for (String i : toWrite) {
                    finalize += i + "\n";
                }
                writer.write(finalize);
                writer.close();
                for (int i = 0; i < iters.length; i++) {
                    iters[i].delete();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }


    private void mergeToOne(File secondLetters) {
        try {
            File[] iters = secondLetters.listFiles();
            if (iters.length != 0) {
                File termPostingFile = new File(secondLetters.getAbsolutePath() + ".txt");
                termPostingFile.createNewFile();
                FileInputStream fis = new FileInputStream(termPostingFile);
                org.jsoup.nodes.Document postingFileEditor = Jsoup.parse(fis, null, "", Parser.xmlParser());
                fis.close();
                Element root = postingFileEditor.createElement("root");
                //go over all the 1-n.txt files
                for (int i = 0; i < iters.length; i++) {
                    FileInputStream fis2 = new FileInputStream(iters[i]);
                    org.jsoup.nodes.Document tempEditor = Jsoup.parse(fis2, null, "", Parser.xmlParser());
                    fis2.close();
                    //
                    Elements terms = tempEditor.select("term");
                    Elements currTerms = root.select("term");
                    Iterator elementIter = currTerms.iterator();
                    int k = terms.size();
                    for (int j = 0; j < k; j++) {
                        Element term = terms.first();
                        String name = term.attr("TERMNAME");
                        Element currTerm = null;
                        boolean isNew = true;
                        boolean appended = false;
                        while (elementIter.hasNext()) {
                            currTerm = (Element) elementIter.next();
                            if (currTerm.attr("TERMNAME").toLowerCase().equals(name.toLowerCase())) {
                                isNew = false;
                                break;
                            } else if (currTerm.attr("TERMNAME").toLowerCase().compareTo(name.toLowerCase()) > 0) {
                                currTerm.before(term.outerHtml());
                                terms.remove(term);
                                appended = true;
                                break;
                            }
                        }
                        if (isNew && !appended) {
                            root.appendChild(term);
                            terms.remove(term);
                            continue;
                        } else if (isNew && appended) {
                            terms.remove(term);
                            continue;
                        } else {
                            if (Character.isUpperCase(currTerm.attr("TERMNAME").charAt(0)) && Character.isLowerCase(name.charAt(0))) {
                                currTerm.attr("TERMNAME", name);
                            }
                            //update df
                            int dfCurr = Integer.parseInt(currTerm.select("df").first().text());
                            int dfMerge = Integer.parseInt(term.select("df").first().text());
                            currTerm.select("df").first().text("" + (dfCurr + dfMerge));
                            //update the tf
                            Elements docsCurr = currTerm.select("doc");
                            Elements docsMerge = term.select("doc");
                            currTerm.selectFirst("docs").insertChildren(-1, docsMerge);
                            terms.remove(term);
                        }
                    }
                }
                for (int i = 0; i < iters.length; i++) {
                    iters[i].delete();
                }
                Elements all = root.select("term");
                String toSave = "";
                BufferedWriter writer = new BufferedWriter(new FileWriter(secondLetters.getAbsolutePath() + ".txt"));
                while (!all.isEmpty()) {
                    for (int i = 0; i < 100; i++) {
                        if (!all.isEmpty()) {
                            toSave += all.first().outerHtml();
                        } else {
                            break;
                        }
                        all.remove(all.first());
                    }
                    writer.write(toSave);
                    writer.newLine();
                    writer.flush();
                    toSave = "";
                }
                root.empty();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private void writeDictionary(String dicName, ConcurrentHashMap<String, String> dictionary) {
        Path path = Paths.get(this.path + "/" + dicName);
        if (!Files.isDirectory(path)) {
            File postingFolder = new File(this.path + "/" + dicName);
            postingFolder.mkdir();
        }
        try {
            String str = this.path + "/" + dicName + "/" + dicName + ".ser";
            File termPostingFile = new File(str);
            termPostingFile.createNewFile();
            FileOutputStream file = new FileOutputStream(str);
            ObjectOutputStream outputStream = new ObjectOutputStream(file);
            outputStream.writeObject(dictionary);
            outputStream.close();
            file.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public LinkedHashMap<String, String> uploadDictionary(boolean stem, String path) {
        setStem(stem);
        setPath(path);
        Map<String, String> dic;
        try {
            FileInputStream fis = new FileInputStream(new File(this.path + "/TermDictionary/TermDictionary.ser"));
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            dic = (Map) inputStream.readObject();
            ConcurrentHashMap<String, String> append = new ConcurrentHashMap<>(dic);
            dictionary = append;
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, String> stringIntegerEntry : dictionary.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                String termName = (String) pair.getKey();
                names.add(termName);
            }
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
            for (String name : names) {
                ordered.put(name, dictionary.get(name));
            }
            return ordered;
        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
    }
}