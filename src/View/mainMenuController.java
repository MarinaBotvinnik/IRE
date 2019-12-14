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
        if (corpusPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't enter a corpus path, Please enter a path before continue");
            alert.show();
        } else if (postingPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't enter a posting path, Please enter a path before continue");
            alert.show();
        }
        //the two text boxes are filled
        else {
            p_first.setVisible(false);
            p_first.setDisable(true);
            p_second.setVisible(true);
            p_second.setDisable(false);
            boolean isStem = cb_stem.isSelected();
            viewModel.setStem(isStem);
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
        viewModel.uploadDictionary();
        isUploaded = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "The dictionary uploaded correctly!");
        alert.show();
    }

    public void showDictionary(){
        if(isUploaded) {
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


}
