import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.Dictionary;


public class ReadFile {
    private String path;
    private Dictionary<String,Tagenizer> allPapers;

    public ReadFile(String path) {
        this.path = path;
    }
    public void readFile(String curr){
        listFilesForFolder(curr);
    }
    private void listFilesForFolder(String filePath){
        final File folder = new File(filePath);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry.getPath());
            } else {//I GOT TO THE DOCUMENT
                File newName = new File(fileEntry.getPath()+".xml");
                boolean b = fileEntry.renameTo(newName);
                if(!b)
                    System.out.println("OH NOOOOOO!");
                //System.out.println(fileEntry.getPath());
                readDoc(fileEntry.getPath());
            }
        }
    }

    /**
     * This function is in charge of spliting a document by 3 main tags:
     * <DOC> </DOC>
     * <DOCNO> </DOCNO>
     * <TEXT> </TEXT>
     * @param docPath
     */
    private void readDoc(String docPath){
        try {
            File file = new File(docPath);
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            String st;
            while ((st = buffer.readLine()) != null){
              if(st.equals("<DOC>")){ //this is a new document
                  while(!st.equals("</DOCNO>")){
                      st = buffer.readLine();
                  }
              }
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
