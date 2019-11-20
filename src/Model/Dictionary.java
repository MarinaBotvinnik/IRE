package Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Dictionary {
    ConcurrentHashMap<String,String> allTerms;

    public Dictionary (ConcurrentHashMap<String,String> map){
        allTerms = new ConcurrentHashMap<>();
        this.allTerms = map;
    }

    public void addTerm(String key, String value){
        allTerms.put(key,value);
    }
}
