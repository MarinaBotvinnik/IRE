package Model;

import java.util.TreeMap;

public class Model {
    private ReadFile readFile;

    public Model(boolean isStem) {
        readFile = new ReadFile(isStem);
    }

    public TreeMap<String,String> uploadDictionary(boolean stem,String path){
        return readFile.upload(stem,path);
    }

    public void setStem(boolean isStem) {
        readFile.setIndexerStem(isStem);
    }

    public void startPosting(String corpusPath, String postingPath){
        readFile.setIndexerPath(postingPath);
        readFile.readFile(corpusPath);
    }
    public int getNumOfDocs(){
        return readFile.getNumOfDocs();
    }
    public int getNumOfTerm(){
        return readFile.getNumOfTerm();
    }

    public void reset(){
        readFile.reset();
    }
}
