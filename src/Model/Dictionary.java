package Model;

import java.util.HashMap;


public class Dictionary {
    private HashMap<String,String> allTermsWithoutStem;
    //private ConcurrentHashMap<String,String> allTermsWithStem;
    //private boolean stemming;

    public Dictionary (){
        allTermsWithoutStem = new HashMap<>();
    }

    public void addTerm(String key, String value)
    {
         allTermsWithoutStem.put(key,value);

    }

    public void saveCorrectly(String key, String value) {
        if (key.length() > 0) {
            char first = key.charAt(0);
            // the word isn't in the dictionary yet
            if (!allTermsWithoutStem.containsKey(key.toUpperCase()) || !allTermsWithoutStem.containsKey(key.toLowerCase())) {
                if (Character.isUpperCase(first)) {
                    allTermsWithoutStem.put(key.toUpperCase(), value);
                } else allTermsWithoutStem.put(key.toLowerCase(), value);
            }
            // the word is already in the dictionary
            else {
                if (Character.isLowerCase(first)) {
                    if (allTermsWithoutStem.containsKey(key.toUpperCase())) {
                        allTermsWithoutStem.remove(key.toUpperCase());
                        allTermsWithoutStem.put(key.toLowerCase(), value);
                    }
                }
            }
        }
    }
}
