package Model;

import javafx.util.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class is in charge of indexing the documents and terms that had been parsed
 * and creating a posting file and a dictionary for both the terms and documents
 */
public class Indexer {
    private volatile ConcurrentHashMap<String,Stack<Pair<String, Integer>>> docEnts;
    private String path;
    private volatile int iteration;
    private volatile int maxDoc;
    private volatile int docDirectoryNum;
    private volatile boolean isStem;
    private volatile ConcurrentHashMap<String, Document> documentsPosting;
    private volatile ConcurrentHashMap<String, String> documentsDictionary;
    private volatile ConcurrentHashMap<String, String> dictionary;
    private volatile ConcurrentHashMap<String, Term> posting;
    private volatile ConcurrentHashMap<String, Term> entities;
    private volatile ConcurrentHashMap<String, String> openedDocs;
    private volatile int numOfTerms;
    private volatile int numOfDocs;
    private ArrayList<Integer> docLengths;

    /**
     * Constructor of the class, sets if the words need to be stemmed
     *
     * @param stem - true if words should be stemmed, false otherwise
     */
    public Indexer(boolean stem) {
        path = "";
        iteration = 0;
        docEnts=new ConcurrentHashMap<>();
        documentsPosting = new ConcurrentHashMap<>();
        documentsDictionary = new ConcurrentHashMap<>();
        dictionary = new ConcurrentHashMap<>();
        posting = new ConcurrentHashMap<>();
        entities = new ConcurrentHashMap<>();
        openedDocs=new ConcurrentHashMap<>();
        maxDoc = 10000;
        docDirectoryNum = 1;
        isStem = stem;
        numOfDocs = 0;
        numOfTerms = 0;
        docLengths = new ArrayList<>();
    }

    /**
     * Method to set the stemmer on so the terms will be stemmed
     *
     * @param stem - true if words should be stemmed, false otherwise
     */
    public void setStem(boolean stem) {
        isStem = stem;
    }

    /**
     * Method returns the number of documents parsed so far
     *
     * @return number of docs parsed
     */
    public int getNumOfDocs() {
        return numOfDocs;
    }

    /**
     * Method returns the number of documents parsed so far
     *
     * @return numOfTerms
     */
    public int getNumOfTerm() {
        return numOfTerms;
    }

    /**
     * Method resets the indexer by clearing all the data base and deleting all
     * the files from the posting file
     */
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

    /**
     * Method deletes all the files from the given path
     *
     * @param filepath
     */
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

    /**
     * Method sets the posting file path, where to save the posting files
     *
     * @param path
     */
    public void setPath(String path) {
        if (isStem) {
            this.path = path + "\\Stemming";
        } else this.path = path + "\\noStemming";
        File file = new File(this.path);
        if (!file.isDirectory())
            file.mkdir();
    }

    /**
     * Method adds parsed terms to the dictionary and stems the term if the stem is set to true
     *
     * @param Name
     * @param docNo
     * @param position
     */
    public void addTermToDic(String Name, String docNo, int position) {
        if(!this.openedDocs.containsKey(docNo))
            this.openedDocs.put(docNo,docNo);
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
        synchronized (this) {
            if (!posting.containsKey(termName.toLowerCase())) {
                if (Character.isUpperCase(first)) {
                    posting.put(termName.toLowerCase(), new Term(termName.toUpperCase(), docNo, position));
                } else posting.put(termName.toLowerCase(), new Term(termName.toLowerCase(), docNo, position));
            } else {
                if (Character.isLowerCase(first) && Character.isUpperCase(posting.get(termName.toLowerCase()).getTermName().charAt(0))) {
                    posting.get(termName.toLowerCase()).changeName(termName.toLowerCase());
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position, this.openedDocs);
                } else {
                    posting.get(termName.toLowerCase()).addDocPosition(docNo, position, this.openedDocs);
                }
            }
        }
    }

    /**
     * Method saves the terms in the sorted tree map to a temporary posting file which will be merged later on
     * to the final posting file with the other iterations
     *
     * @param iteration
     * @param queue
     */
    private void writeToTempPosting(int iteration, Queue<Term> queue) {
        try {
            if (!Files.isDirectory(Paths.get(this.path + "/Posting"))) {
                File postingFolder = new File(this.path + "/Posting");
                postingFolder.mkdir();
            }
            Term currTerm;
            boolean needWrite = false;
            String charAt0 = null;
            String charAt1 = null;
            String str = null;
            String toWrite = "";
            while (!queue.isEmpty()) {
                currTerm = queue.remove();
                if (!((currTerm.getTermName().charAt(0) + "").toLowerCase()).equals(charAt0) || (currTerm.getTermName().length() > 1 && !((currTerm.getTermName().charAt(1) + "").toLowerCase()).equals(charAt1))) {
                    if (needWrite) {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                        writer.write(toWrite);
                        writer.close();
                        toWrite = "";
                    }
                    if (currTerm.getTermName().charAt(0) != '/' || currTerm.getTermName().charAt(0) != '\\')
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
                    needWrite = true;
                }
                if (!dictionary.containsKey(currTerm.getTermName())) {
                    dictionary.put(currTerm.getTermName(), this.path + "\\Posting\\" + charAt0 + "\\" + charAt1 + ".txt" + "," + currTerm.getTf());
                } else{
                    String vals[] = ("" + dictionary.get(currTerm.getTermName())).split(",");
                        int tf = Integer.parseInt(vals[1]) + Integer.parseInt("" + currTerm.getTf());
                        dictionary.replace(currTerm.getTermName(), this.path + "\\Posting\\" + charAt0 + "\\" + charAt1 + ".txt" + "," + tf);
                }
                toWrite +=currTerm.toString()+"\n";
                if (queue.isEmpty()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(str));
                    writer.write(toWrite);
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method saves the documents of this iterations to the documents posting file
     *
     * @param documentsPosting
     */
    private void writeDocsToPosting(ConcurrentHashMap<String, Document> documentsPosting) {
        try {
            Path path = Paths.get(this.path + "/DocumentsPosting");
            if (!Files.isDirectory(path)) {
                File postingFolder = new File(this.path + "/DocumentsPosting");
                postingFolder.mkdir();
            }
            String str;
            synchronized (this) {
                File termPostingFolder = new File(this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1));
                termPostingFolder.mkdir();
                str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
                File termPostingFile = new File(termPostingFolder.getAbsolutePath(), (docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1)) + ".txt");
                termPostingFile.createNewFile();
                docDirectoryNum += maxDoc;
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(str));
            StringBuilder toWrite = new StringBuilder();
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
                toWrite.append(document.getDocName()).append(",").append(document.getMax_tf()).append(",").append(document.getMax_Term_name()).append(",").append(document.getLength()).append("\n");
                docLengths.add(document.getLength());
            }
            writer.write(toWrite.toString());
            writer.close();
            documentsPosting.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method checks if the entity is one of the most frequent entities in the
     * document. If it is, the method puts the entity in the data base.
     * @param ent
     * @param doc
     */
    private void addEntToEntDocPosting(Term ent, String doc){
        if(!this.docEnts.containsKey(doc)){
            this.docEnts.put(doc,new Stack<>());
            this.docEnts.get(doc).add(new Pair(ent.getTermName(),ent.getDocs().get(doc)));
        }
        else{
            Stack<Pair<String,Integer>> stack=this.docEnts.get(doc);
            Stack<Pair<String,Integer>> temp=new Stack<>();
            Pair<String,Integer> entPair=new Pair<>(ent.getTermName(),ent.getDocs().get(doc));
            while(!stack.empty()){
                Pair<String,Integer> curr=stack.pop();
                if(curr.getValue()<=entPair.getValue()){
                    if(!curr.getKey().equals(entPair.getKey()))
                        temp.add(curr);
                    if(stack.isEmpty()) {
                        stack.add(entPair);
                        break;
                    }
                }
                else{
                    stack.add(curr);
                    temp.add(entPair);
                    break;
                }
            }
            while(stack.size()<5){
                if(temp.isEmpty())
                    break;
                else
                    stack.add(temp.pop());
            }
        }
    }

    /**
     * Method accepts a potential entity and accepts it as a term if it had been seen in at least
     * one other documents, or saves it in the entity database if it seen it only in one document
     * but dosent accept it as a term
     *
     * @param Name
     * @param docNo
     * @param position
     * @return true if the entity is accepted as a term or false otherwise
     */
    public boolean addEntToDic(String Name, String docNo, int position) {
        synchronized (this) {
            //its an entity that showed up only once and now its its second time
            if (!this.openedDocs.containsKey(docNo))
                this.openedDocs.put(docNo, docNo);
            if (entities.containsKey(Name) && !entities.get(Name).getDocs().containsKey(docNo)) {
                Term ent = entities.remove(Name);
                String oldDoc = ent.getDocs().keySet().iterator().next();
                ent.addDocPosition(docNo, position, this.openedDocs);
                posting.put(Name.toLowerCase(), ent);
                //the dictionary hasn't been written yet
                if (!documentsDictionary.containsKey(oldDoc)) {
                    Document document = documentsPosting.get(oldDoc);
                    if(document!=null) {
                        document.addTermWithTF(Name, ent.getDocs().get(oldDoc));
                        document.closeDoc();
                    }
                }
                entities.remove(Name);
                addEntToEntDocPosting(ent,oldDoc);
                addEntToEntDocPosting(ent,docNo);
                return true;
            }
            //its entity that appeared more than once but we did'nt write it yet
            else if (posting.containsKey(Name.toLowerCase())) {
                Term ent = posting.get(Name.toLowerCase());
                ent.addDocPosition(docNo, position, this.openedDocs);
                addEntToEntDocPosting(ent,docNo);
                return true;
            }
            //its an entity that has been written before so we need to update it
            else if (dictionary.containsKey(Name.toUpperCase())) {
                Term ent=new Term(Name.toUpperCase(), docNo, position);
                posting.put(Name.toLowerCase(), ent);
                addEntToEntDocPosting(ent,docNo);
                return true;
            }
            //its completely new entity!
            else {
                if (entities.containsKey(Name)) {
                    entities.get(Name).addDocPosition(docNo, position, this.openedDocs);
                } else {
                    Term term = new Term(Name.toUpperCase(), docNo, position);
                    entities.put(Name, term);
                }
                return false;
            }
        }
    }

    /**
     * Method adds a document to the documents posting database, and removes
     * the document from the open documnets database (it is now considred close)
     *
     * @param doc
     */
    public void addDocToDic(Document doc) {
        documentsPosting.put(doc.getDocName(), doc);
        this.openedDocs.remove(doc.getDocName());
    }

    /**
     * Method writes all the documents in the current database to the documents dictionary and sends to a function that writes them to the documents posting.
     * Method writes sends all the terms currently held in the database to a function that wrties them to the temporary term posting and writes them in the dictionary.
     */
    public void write() {
        for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
            HashMap.Entry pair = stringIntegerEntry;
            Document document = (Document) pair.getValue();
            String str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
            documentsDictionary.put(document.getDocName(), str);
        }
        ConcurrentHashMap<String, Document> copy = new ConcurrentHashMap<>(documentsPosting);
        ConcurrentHashMap<String, Term> copy2 = new ConcurrentHashMap<>(this.posting);
        this.posting.clear();
        this.documentsPosting.clear();
        writeDocsToPosting(copy);
        iteration++;
        TreeMap<String, Term> sortedPosting = new TreeMap<>(copy2);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Term currTerm;
        boolean needWrite = false;
        String charAt0 = null;
        Queue<Term> queue = new LinkedList<Term>();
        while (!sortedPosting.isEmpty()) {
            currTerm = sortedPosting.remove(sortedPosting.firstKey());
            if (!((currTerm.getTermName().charAt(0) + "").toLowerCase()).equals(charAt0)) {
                if (needWrite) {
                    Queue<Term> temp=new LinkedList<Term>(queue);
                    Runnable runnable = () -> {
                        writeToTempPosting(this.iteration, temp);
                    };
                    executor.execute(runnable);
                    queue.clear();
                }
                charAt0 = ("" + currTerm.getTermName().charAt(0)).toLowerCase();
                needWrite = true;
            }
            queue.add(currTerm);
            if (sortedPosting.isEmpty()) {
                Queue<Term> temp=new LinkedList<Term>(queue);
                Runnable runnable = () -> {
                    writeToTempPosting(this.iteration, temp);
                };
                executor.execute(runnable);
                queue.clear();
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method checks if there are anymore documents or terms to save to the disk, and saves the terms and documents dictionaries to the disk.
     * Method calls for the merging function of the temporary posting files
     */
    public void closeIndexer() {
        if (!posting.isEmpty() || !documentsPosting.isEmpty()) {
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
                String str = this.path + "/DocumentsPosting/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + "/" + docDirectoryNum + "-" + (docDirectoryNum + maxDoc - 1) + ".txt";
                documentsDictionary.put(document.getDocName(), str);
            }
            ConcurrentHashMap<String, Document> copy = documentsPosting;
            writeDocsToPosting(copy);
            iteration++;
            TreeMap<String, Term> sortedPosting = new TreeMap<>(this.posting);
            this.posting.clear();
            this.documentsPosting.clear();
            ExecutorService executor = Executors.newFixedThreadPool(3);
            Term currTerm;
            boolean needWrite = false;
            String charAt0 = null;
            Queue<Term> queue = new LinkedList<Term>();
            while (!sortedPosting.isEmpty()) {
                currTerm = sortedPosting.remove(sortedPosting.firstKey());
                if (!((currTerm.getTermName().charAt(0) + "").toLowerCase()).equals(charAt0)) {
                    if (needWrite) {
                        Queue<Term> temp=new LinkedList<Term>(queue);
                        Runnable runnable = () -> {
                            writeToTempPosting(this.iteration, temp);
                        };
                        executor.execute(runnable);
                        queue.clear();
                    }
                    charAt0 = ("" + currTerm.getTermName().charAt(0)).toLowerCase();
                    needWrite = true;
                }
                queue.add(currTerm);
                if (sortedPosting.isEmpty()) {
                    Queue<Term> temp=new LinkedList<Term>(queue);
                    Runnable runnable = () -> {
                        writeToTempPosting(this.iteration, temp);
                    };
                    executor.execute(runnable);
                    queue.clear();
                }
            }
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        writeDictionary("TermDictionary", dictionary);
        numOfTerms = dictionary.size();
        this.dictionary.clear();
        writeDictionary("DocumentsDictionary", documentsDictionary);
        numOfDocs = documentsDictionary.size();
        this.documentsDictionary.clear();
        writeDocsEnts();
        mergePostingToOne(this.path + "\\Posting");
        writeAverage(this.path + "\\avg");
    }

    /**
     * Method writes the data base that holds the 5 most frequent entities in each document to the disk
     */
    private void writeDocsEnts(){
        try {
            File avgFolder = new File(this.path + "\\docsents");
            avgFolder.mkdir();
            String str = this.path + "\\docsents\\docsents.txt";
            File docsEntsFile = new File(str);
            docsEntsFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(str));
            for (Map.Entry<String, Stack<Pair<String,Integer>>> entry : this.docEnts.entrySet()) {
                writer.write(entry.getKey());
                Stack temp=entry.getValue();
                while(!temp.isEmpty()){
                    Pair<String,Integer> tempPair=(Pair)temp.pop();
                    writer.write(","+tempPair.getKey()+","+tempPair.getValue());
                }
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Method writes the average document size to the disk
     * @param p Path of where we want to save the average
     */
    private void writeAverage(String p){
        File avgFolder = new File(p);
        avgFolder.mkdir();
        double sum=0;
        for (Integer length: docLengths) {
            sum += length;
        }
        double avg = sum/docLengths.size();
        try {
            String str = p + "\\avg.txt";
            File termPostingFile = new File(str);
            termPostingFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(termPostingFile));
            writer.write(""+avg);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method accepts the path to the temporary posting and sends all the files according
     * to their first and second letter that need to be merged
     *
     * @param filePath
     */
    private void mergePostingToOne(String filePath) {
        File file = new File(filePath);
        File[] firstLetters = file.listFiles();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (File firstLetter : firstLetters) {
            File[] secondLetters = firstLetter.listFiles();
            Runnable runnable = () -> {
                for (File secLetter : secondLetters) {
                    if (secLetter.isDirectory()) {
                        mergeToOne(secLetter);
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
            executor.awaitTermination(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method accepts all the files that needs to be merged
     *
     * @param secondLetters
     */
    private void mergeToOne(File secondLetters) {
        try {
            File[] iters = secondLetters.listFiles();
            if (iters.length != 0) {
                File termPostingFile = new File(secondLetters.getAbsolutePath() + ".txt");
                termPostingFile.createNewFile();
                LinkedList<String> toWrite = new LinkedList<>();
                for (int i = 0; i < iters.length; i++) {
                    LinkedList<String> temp = toWrite;
                    toWrite=new LinkedList<>();
                    BufferedReader br = new BufferedReader(new FileReader(iters[i]));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.equals("]") || line.equals(""))
                            continue;
                        String[] seperated1 = line.split("[\\[\\]]");
                        boolean isNew = true;
                        boolean appended = false;
                        while (!temp.isEmpty()) {
                            String term = temp.remove();
                            String[] seperated2 = term.split("[\\[\\]]");
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
                                temp.addFirst(term);
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
                    while (!temp.isEmpty()) {
                        toWrite.add(temp.remove());
                    }
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(secondLetters.getAbsolutePath() + ".txt"));
                for (String i : toWrite) {
                    writer.write(i);
                    writer.newLine();
                }
                writer.close();
                for (int i = 0; i < iters.length; i++) {
                    iters[i].delete();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * Method accepts the dictionary name and a hash map data base, and
     * saves the to the disk as a serializable file
     *
     * @param dicName
     * @param dictionary
     */
    private void writeDictionary(String dicName, ConcurrentHashMap<String, String> dictionary) {
        Path path = Paths.get(this.path + "/" + dicName);
        if (!Files.isDirectory(path)) {
            File postingFolder = new File(this.path + "/" + dicName);
            postingFolder.mkdir();
        }
        try {
            String str = this.path + "/" + dicName + "/" + dicName + ".txt";
            File termPostingFile = new File(str);
            termPostingFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(str));
            for (Map.Entry<String, String> entry : dictionary.entrySet()) {
                writer.write(entry.getKey()+","+entry.getValue());
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method accepts a path to the terms dictionary that needs to be uploaded and
     * which one needs to be uploaded, the stemmed dictionary or the unstemmed one
     * (true for stemmed and false for unstemmed)
     *
     * @param stem
     * @param path
     * @return
     */
    public LinkedHashMap<String, String> uploadDictionary(boolean stem, String path) {
        setStem(stem);
        setPath(path);
        try {
            File file = new File(this.path + "/TermDictionary/TermDictionary.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            ConcurrentHashMap<String, String> termDicBeforeRemove = new ConcurrentHashMap<>();
            String term;
            while ((term = br.readLine()) != null) {
                String[] info = term.split(",");
                termDicBeforeRemove.put(info[0],info[1]+","+info[2]);
            }
            dictionary = termDicBeforeRemove;
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
            e.printStackTrace();
            return null;
        }
    }

    /**
     * getter fot the dictionary of the terms
     * @return a hash map of all the terms and their path to the disc
     */
    public HashMap<String,String> getTermDicWithoutUpload(){
        HashMap<String,String> dic = new HashMap<>(dictionary);
        return dic;
    }
}