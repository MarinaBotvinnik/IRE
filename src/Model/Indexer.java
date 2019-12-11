package Model;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Indexer {
    int maxTerm;
    int maxDoc;
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
        maxDoc = 100;
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
                Iterator iter = posting.entrySet().iterator();
                while (iter.hasNext()) {
                    Term term = (Term) iter.next();
                    if (!dictionary.containsKey(term.getTermName().toLowerCase()) || !dictionary.containsKey(term.getTermName().toUpperCase())) {
                        if (!Files.isDirectory(Paths.get("/Posting/" + term.getTermName().charAt(0)))) {
                            File termPostingFolder = new File("/Posting/" + term.getTermName().charAt(0));
                            termPostingFolder.mkdir();
                        }
                        Path path = Paths.get("/Posting/" + term.getTermName().charAt(0) + "/" + term.getTermName().charAt(1) + ".txt");
                        String str = null;
                        str = "/Posting/" + term.getTermName().charAt(0) + "/" + term.getTermName().charAt(1) + ".txt";
                        File termPostingFile = new File(str);
                        if (!Files.exists(path)) {
                            termPostingFile.createNewFile();
                        }
                        FileInputStream fis = new FileInputStream(termPostingFile);
                        org.jsoup.nodes.Document postingFileEditer = Jsoup.parse(fis, null, "", Parser.xmlParser());
                        
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeDocsToPosting() {
            try {
                if (!Files.isDirectory(Paths.get("/docPosting"))) {
                }
            }
            catch (IOException e) {
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
