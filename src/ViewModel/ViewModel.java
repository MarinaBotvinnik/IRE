package ViewModel;
import Model.Model;
import java.util.TreeMap;

public class ViewModel {
    private Model model;
    private TreeMap<String ,String> dictionary;

    public ViewModel()
    {
       dictionary = new TreeMap<>();
       model=null;
    }

    public void uploadDictionary(boolean stem,String path){
        if(model==null) {
            model = new Model(stem);
        }
        dictionary = model.uploadDictionary(stem, path);
    }

    public TreeMap<String,String> getDictionary(){
        return dictionary;
    }

    public void setStem(boolean isStem) {
        model.setStem(isStem);
    }

    public void start(String corpusPath,String PostingPath, boolean isStem){
        this.model = new Model(isStem);
        setStem(isStem);
        model.startPosting(corpusPath,PostingPath);
    }

    public int getNumOfDocs(){
        return model.getNumOfDocs();
    }

    public int getNumOfTerm(){
        return model.getNumOfTerm();
    }

    public void reset(){
        model.reset();
    }
}
