package Model;

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
    private ExecutorService executor;
    private String path;
    private volatile int iteration;
    private int maxDoc;
    private int docDirectoryNum;
    private boolean isStem;
    private ConcurrentHashMap<String, Document> documentsPosting;
    private ConcurrentHashMap<String, String> documentsDictionary;
    private volatile ConcurrentHashMap<String, String> dictionary;
    private volatile ConcurrentHashMap<String, Term> posting;
    private HashMap<String, Term> entities;
    private int numOfTerms;
    private int numOfDocs;

    /**
     * Constructor of the class, sets if the words need to be stemmed
     *
     * @param stem - true if words should be stemmed, false otherwise
     */
    public Indexer(boolean stem) {
        path = "";
        iteration = 0;
        documentsPosting = new ConcurrentHashMap<>();
        documentsDictionary = new ConcurrentHashMap<>();
        dictionary = new ConcurrentHashMap<>();
        posting = new ConcurrentHashMap<>();
        entities = new HashMap<>();
        maxDoc = 1000;
        docDirectoryNum = 1;
        isStem = stem;
        executor = Executors.newFixedThreadPool(8);
        numOfDocs = 0;
        numOfTerms = 0;
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
     * @return
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

    /**
     * Method saves the terms in the sorted tree map to a temporary posting file which will be merged later on
     * to the final posting file with the other iterations
     *
     * @param iteration
     * @param sortedPosting
     */
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
                HashMap<String, Integer> docs = currTerm.getDocs();
                HashMap<String, String> positionsList = currTerm.getPositions();
                toWrite += currTerm.getTermName() + "[" + docs.size() + "]";
                for (Map.Entry<String, Integer> entry : docs.entrySet()) {
                    toWrite += "[" + entry.getKey() + "," + entry.getValue() + ",";
                    if (!dictionary.containsKey(currTerm.getTermName())) {
                        dictionary.put(currTerm.getTermName(), this.path + "\\Posting\\" + charAt0 + "\\" + charAt1 + ".txt" + "," + entry.getValue());
                    } else {
                        String vals[] = ("" + dictionary.get(currTerm.getTermName())).split(",");
                        int tf = Integer.parseInt(vals[1]) + Integer.parseInt("" + entry.getValue());
                        dictionary.replace(currTerm.getTermName(), this.path + "\\Posting\\" + charAt0 + "\\" + charAt1 + ".txt" + "," + tf);
                    }
                    String positions = positionsList.get(entry.getKey());
                    toWrite += positions + "]";
                }
                toWrite += "\n";
                if (!it.hasNext()) {
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
            String toWrite = "";
            for (Map.Entry<String, Document> stringIntegerEntry : documentsPosting.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                Document document = (Document) pair.getValue();
                if (document == null || document.getMax_Term_name() == null)
                    System.out.printf("ohhh nooo document is null");
                toWrite += document.getDocName() + "," + document.getMax_tf() + "," + document.getMax_Term_name() + "," + document.getUniqueTermsNum() + "\n";
            }
            writer.write(toWrite);
            writer.close();
            documentsPosting.clear();
        } catch (IOException e) {
            e.printStackTrace();
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
        //its an entity that showed up only once and now its its second time
        if (entities.containsKey(Name) && !entities.get(Name).getDocs().containsKey(docNo)) {
            Term ent = entities.remove(Name);
            String oldDoc = ent.getDocs().keySet().iterator().next();
            ent.addDocPosition(docNo, position);
            posting.put(Name.toLowerCase(), ent);
            //the dictionary hasn't been written yet
            if (!documentsDictionary.containsKey(oldDoc)) {
                Document document = documentsPosting.get(oldDoc);
                document.addTermWithTF(Name, ent.getDocs().get(oldDoc));
                document.closeDoc();
            }
            return true;
        }
        //its entity that appeared more than once but we did'nt write it yet
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

    /**
     * Method accepts a document and adds it to the documents dictionary and the document posting held in the ram in this iteration.
     * if we pass the documents threshold we clear the documents and terms position file to the disk and start a new iteration
     *
     * @param doc
     */
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

    /**
     * Method waits for all the iterations to finish saving their terms to the temporary posting file,
     * checks if there are anymore documents or terms to save to the disk, and saves the if there are
     * saves the terms and documents dictionaries to the disk
     * and calls for the merging function of the temporary posting files
     */
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
            executor.awaitTermination(1, TimeUnit.HOURS);
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

    public HashMap<String, String> getTermDic(boolean stem, String path) {
        try {
            setStem(stem);
            setPath(path);
            Map<String, String> dic;
            FileInputStream fis = new FileInputStream(new File(this.path + "/TermDictionary/TermDictionary.ser"));
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            dic = (Map) inputStream.readObject();
            HashMap<String, String> termDicBeforeRemove = new HashMap<>(dic);
            HashMap<String, String> termDicFinal = new HashMap<>();
            for (Map.Entry<String, String> stringIntegerEntry : termDicBeforeRemove.entrySet()) {
                HashMap.Entry pair = stringIntegerEntry;
                String termValue = (String) pair.getValue();
                String[] vals = termValue.split(" ");
                String termPath = vals[0];
                termDicFinal.put((String) pair.getKey(), termPath);
            }
            fis.close();
            inputStream.close();
            return termDicFinal;

        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
    }

    public HashMap<String, String> getDocDic(boolean stem, String path) {
        try {
            setStem(stem);
            setPath(path);
            Map<String, String> dic;
            FileInputStream fis = new FileInputStream(new File(this.path + "/DocumentsDictionary/DocumentsDictionary.ser"));
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            dic = (Map) inputStream.readObject();
            HashMap<String, String> docDic = new HashMap<>(dic);
            fis.close();
            inputStream.close();
            return docDic;
        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
    }
}