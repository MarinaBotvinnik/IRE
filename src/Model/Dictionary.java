package Model;

import java.util.concurrent.ConcurrentHashMap;

public class Dictionary {
    private ConcurrentHashMap<String,String> allTermsWithoutStem;
    private ConcurrentHashMap<String,String> allTermsWithStem;
    private boolean stemming;

    public Dictionary (ConcurrentHashMap<String,String> map, boolean stem){
        allTermsWithoutStem = new ConcurrentHashMap<>();
        allTermsWithStem = new ConcurrentHashMap<>();
        if(stem)
            this.allTermsWithStem = map;
        else this.allTermsWithoutStem = map;
        this.stemming = stem;
    }

    public void addTerm(String key, String value)
    {
        if(stemming)
            allTermsWithStem.put(key,value);
        else allTermsWithStem.put(key,value);

    }

    public void saveCorrectly(String key, String value){
        char first = key.charAt(0);
        // the word isn't in the dictionary yet
        if(!allTermsWithoutStem.containsKey(key.toUpperCase()) || !allTermsWithoutStem.containsKey(key.toLowerCase())){
            if(Character.isUpperCase(first)){
                allTermsWithoutStem.put(key.toUpperCase(),value);
            }
            else allTermsWithoutStem.put(key.toLowerCase(),value);
        }
        // the word is already in the dictionary
        else{
            if(Character.isLowerCase(first)){
                if(allTermsWithoutStem.containsKey(key.toUpperCase())){
                    allTermsWithoutStem.remove(key.toUpperCase());
                    allTermsWithoutStem.put(key.toLowerCase(),value);
                }
            }
        }
    }
}
