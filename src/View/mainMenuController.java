package View;

import ViewModel.ViewModel;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class mainMenuController {

    public TextField tf_corpusPath;
    public TextField tf_postingPath;
    public CheckBox cb_stem;
    public Pane p_first;
    public Pane p_second;
    public Pane p_dictionary;
    public TextArea c_Posting;


    private ViewModel viewModel;
    private Stage stage;
    private Scene scene;
    private boolean isUploaded;
    private String postPath;

    public void initialize(ViewModel model, Stage primaryS, Scene scene){
        this.viewModel = model;
        this.stage = primaryS;
        this.scene = scene;
        isUploaded = false;
    }

    //when GO! pressed
    public void setPane2() {
        String corpusPath = tf_corpusPath.getText();
        String postingPath = tf_postingPath.getText();
        postPath = postingPath;
        if (corpusPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't enter a corpus path, Please enter a path before continue");
            alert.show();
        } else if (postingPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't enter a posting path, Please enter a path before continue");
            alert.show();
        }
        //the two text boxes are filled
        else {
            boolean isStem = cb_stem.isSelected();
            File withStem=new File(postingPath +"/Stemming");
            File withoutStem=new File(postingPath+"/noStemming");
            if(withStem.exists() && isStem){
                Alert alert = new Alert(Alert.AlertType.WARNING, "You already built a posting file with stemming for this corpus");
                alert.show();
                p_first.setVisible(false);
                p_first.setDisable(true);
                p_second.setVisible(true);
                p_second.setDisable(false);
                return;
            }
            else if(withoutStem.exists() && !isStem){
                Alert alert = new Alert(Alert.AlertType.WARNING, "You already built a posting file without stemming for this corpus");
                alert.show();
                p_first.setVisible(false);
                p_first.setDisable(true);
                p_second.setVisible(true);
                p_second.setDisable(false);
                return;
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("The engine is running");
            alert.setContentText("Please wait while the posting files are created");
            alert.show();
            alert.getDialogPane().setDisable(true);
            long startTime 	= System.nanoTime();
            viewModel.start(corpusPath,postingPath,isStem);
            long endTime = System.nanoTime();
            long div = 1000000;
            long totalTime =(endTime-startTime)/div;
            p_first.setVisible(false);
            p_first.setDisable(true);
            p_second.setVisible(true);
            p_second.setDisable(false);
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
            alert1.setTitle("The posting is DONE!");
            alert1.setContentText("Number of documents:"+viewModel.getNumOfDocs() + "\n"+
                    "Number of terms:" + viewModel.getNumOfTerm()+ "\n"+
                    "Total time of running(in seconds): "+totalTime/1000 + "\n"+
                    "Total time of running(in minutes): "+totalTime/60000);
            alert1.show();
            alert.getDialogPane().setDisable(false);
        }
    }

    public void setPane3(){
        p_second.setVisible(false);
        p_second.setDisable(true);
        p_dictionary.setVisible(true);
        p_dictionary.setDisable(false);
    }

    public void setBackPane2(){
        p_dictionary.setVisible(false);
        p_dictionary.setDisable(true);
        p_second.setVisible(true);
        p_second.setDisable(false);
    }

    public void browseCorpus(){
        getPath(tf_corpusPath);
    }

    public void browsePost(){
        getPath(tf_postingPath);
    }

    private void getPath(TextField tf) {
        tf.clear();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.showOpenDialog(null);
        File file = fileChooser.getSelectedFile();
        if(file!=null) {
            String p = file.getAbsolutePath();
            tf.appendText(p);
        }
    }

    public void uploadDictionary(){
        viewModel.uploadDictionary(cb_stem.isSelected(),postPath);
        isUploaded = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "The dictionary uploaded correctly!");
        alert.show();
    }

    public void showDictionary(){
        try {
            if (isUploaded) {
                LinkedHashMap<String, String> dic = viewModel.getDictionary();
                List<String> freq = new ArrayList<>();
                StringBuilder str1 = new StringBuilder();
//            for(Map.Entry<String,String> entry : dic.entrySet()) {
//                String term = entry.getKey();
//                String posting = entry.getValue();
//                String[] show = posting.split(",");
//                str1.append(term).append("       --->       ").append(show[1]).append("\n");
//                freq.add(show[1]);
                BufferedWriter writer = new BufferedWriter(new FileWriter("D:\\204096242\\frequencies.txt"));
                Collection<String> f = dic.values();
                Collections.sort(freq, String.CASE_INSENSITIVE_ORDER);
                String v="";
                for (String fr : f) {
                    String[] a = fr.split(",");
                    //freq.add(a[1]);
                    v = v + a[1]+",";
                }
                writer.write(v);
                writer.close();
                for (int i = 0; i < 10; i++) {
                    System.out.println("Most frequens: " + freq.get(i));
                    System.out.println("Less frequens: " + freq.get(freq.size() - 1 - i));
                }
                c_Posting.textProperty().set(str1.toString());
                setPane3();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't uploaded your dictionary yet.");
                alert.show();
            }
        }catch (IOException e){
            e.getMessage();
        }
    }

    public void reset(){
        viewModel.reset();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "you reset the system , all posting files were deleted!");
        alert.show();
        setPane1();
    }

    private void setPane1(){
        p_second.setDisable(true);
        p_second.setVisible(false);
        p_first.setVisible(true);
        p_first.setDisable(false);
    }

}
