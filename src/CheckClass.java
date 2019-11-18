import Model.ReadFile;

import java.io.File;

public class CheckClass {

    public static void main (String[] args) {
        String path = "Resource/corpus";
        ReadFile readFile = new ReadFile(path);
        readFile.readFile(path);
        //readFile.readDoc("Resource/corpus/FB396001/FB396001");
    }
}
