package Model;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;

public class Indexer {
    int Max;
    HashMap<String,String> documents;
    HashMap<String,Integer> dictionary;
    HashMap<String,Term> posting;


    public Indexer(){
        documents = new HashMap<>();

    }

    public void addTermToDic(Term term){
        String termName = term.getTermName();
        if(posting.size()<Max){
            //if it exists in the dictionary
            boolean existsDic = isTermExistInDic (termName);
            //it means it exist in the posting and the dictionary
            if(existsDic){
                //the term changed to lowerCase
                if(changeTerm(termName)){
                    Term chaged = posting.get(termName);
                }
                //term stays the same
                else{
                    Term existTerm = posting.get(termName);

                }
            }
            //it exist in the dictionary but not in the posting
            else{
                posting.put(termName,term);
            }

        }
        //we reached the max size of the posting, time to write to the disc
        else{

        }

    }

    public void addDocToDic(Document doc){

    }

    private boolean isTermExistInDic(String termName){
        char first = termName.charAt(0);
        // the word isn't in the dictionary yet
        if (!dictionary.containsKey(termName.toUpperCase()) || !dictionary.containsKey(termName.toLowerCase())) {
            if (Character.isUpperCase(first)) {
               dictionary.put(termName.toUpperCase(),1);
            } else dictionary.put(termName.toLowerCase(),1);
            return false;
        }
        return true;
    }

    private boolean changeTerm(String termName){
        char first = termName.charAt(0);
        if (Character.isLowerCase(first)) {
            if (dictionary.containsKey(termName.toUpperCase())) {
                int value = dictionary.get(termName.toUpperCase());
                dictionary.remove(termName.toUpperCase());
                dictionary.put(termName.toLowerCase(), value+1);
                return true;
            }
        }
        else dictionary.replace(termName,dictionary.get(termName)+1);
        return false;
    }
}
