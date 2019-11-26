package Model;

import java.util.concurrent.ConcurrentHashMap;

public class Dictionary {
    ConcurrentHashMap<String,String> allTerms;
    ConcurrentHashMap<String,String> allTermsWithStem;

    public Dictionary (ConcurrentHashMap<String,String> map){
        allTerms = new ConcurrentHashMap<>();
        this.allTerms = map;
    }

    public void addTerm(String key, String value){
        allTerms.put(key,value);
    }

    public void saveCorrectly(String key){
        char first = key.charAt(0);
        // the word isn't in the dictionary yet
        if(!allTerms.containsKey(key.toUpperCase()) || !allTerms.containsKey(key.toLowerCase())){
            if(Character.isUpperCase(first)){
                allTerms.put(key.toUpperCase(),"");
            }
            else allTerms.put(key.toLowerCase(),"");
        }
        // the word is already in the dictionary
        else{
            if(Character.isLowerCase(first)){
                if(allTerms.containsKey(key.toUpperCase())){
                    allTerms.remove(key.toUpperCase());
                    allTerms.put(key.toLowerCase(),"");
                }
            }
        }
    }
}
