package Model;

import java.util.TreeMap;

public class Model {
    private ReadFile readFile;
    private Indexer indexer;

    public Model() {
        readFile = new ReadFile();
        indexer = new Indexer();
    }

    public TreeMap<String,String> uploadDictionary(){
        return indexer.uploadDictionary();
    }

    public void setStem(boolean isStem) {
        indexer.setStem(isStem);
    }
}
