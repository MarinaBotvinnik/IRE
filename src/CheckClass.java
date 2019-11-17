import Model.ReadFile;

public class CheckClass {

    public static void main (String[] args){
        String path = "C:\\Users\\Marina\\Desktop\\TryDocs";
        ReadFile readFile = new ReadFile(path);
        readFile.readFile(path);
    }
}
