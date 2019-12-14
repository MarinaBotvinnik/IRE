package ViewModel;
import Model.Model;

import java.util.HashMap;
import java.util.TreeMap;

public class ViewModel {
    private Model model;
    private TreeMap<String ,String> dictionary;
    public ViewModel()
    {
        this.model = new Model();
    }

    public void uploadDictionary(){
        dictionary= model.uploadDictionary();
    }

    public TreeMap<String,String> getDictionary(){
        return dictionary;
    }
    public void setStem(boolean isStem) {
        model.setStem(isStem);
    }
}
