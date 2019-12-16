package View;

import ViewModel.ViewModel;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.File;
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
    private String postingPath;
    private boolean reset;

    public void initialize(ViewModel model, Stage primaryS, Scene scene){
        this.viewModel = model;
        this.stage = primaryS;
        this.scene = scene;
        isUploaded = false;
        reset = false;
    }

    //when GO! pressed
    public void setPane2() {
        String corpusPath = tf_corpusPath.getText();
        String postingPath = tf_postingPath.getText();
        this.postingPath = postingPath;
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
                return;
            }
            else if(withoutStem.exists() && !isStem){
                Alert alert = new Alert(Alert.AlertType.WARNING, "You already built a posting file without stemming for this corpus");
                alert.show();
                return;
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("The engine is running");
            alert.setContentText("Please wait while the posting files are created");
            alert.getDialogPane().setDisable(true);
            long startTime 	= System.nanoTime();
            viewModel.start(corpusPath,postingPath,isStem);
            long endTime = System.nanoTime();
            long div = 1000000;
            long totalTime =(endTime-startTime)/div;
            alert.getDialogPane().setDisable(false);
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.showOpenDialog(null);
        File file = fileChooser.getSelectedFile();
        String p = file.getAbsolutePath();
        tf.appendText(p);
    }

    public void uploadDictionary(){
        if(reset){
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You reset the system so there is no dictionary.");
            alert.show();
            return;
        }
        viewModel.uploadDictionary();
        isUploaded = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "The dictionary uploaded correctly!");
        alert.show();
    }

    public void showDictionary(){
        if(isUploaded && !reset) {
            TreeMap<String, String> dic = viewModel.getDictionary();
            StringBuilder str1 = new StringBuilder();
            for(Map.Entry<String,String> entry : dic.entrySet()) {
                String term = entry.getKey();
                String posting = entry.getValue();
                str1.append(term).append("    ->    ").append(posting).append("\n");
            }
            c_Posting.textProperty().set(str1.toString());
            setPane3();
        }
        else{
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't uploaded your dictionary yet.");
            alert.show();
        }
    }

    public void reset(){
        viewModel.reset();
        delete(new File(postingPath+"/Stemming"));
        delete(new File(postingPath+"/noStemming"));
        reset = true;
    }
    private void delete(File file) {
        if (file.isDirectory()) {
            for (File deleteMe : file.listFiles()) {
                // recursive delete
                delete(deleteMe);
            }
        }
    }


}
