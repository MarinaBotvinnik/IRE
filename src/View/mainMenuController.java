package View;

import ViewModel.ViewModel;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

public class mainMenuController {

    public TextField tf_corpusPath;
    public TextField tf_postingPath;
    public CheckBox cb_stem;
    public Pane p_first;
    public Pane p_second;

    private ViewModel viewModel;
    private Stage stage;
    private Scene scene;

    public void initialize(ViewModel model, Stage primaryS, Scene scene){
        this.viewModel = model;
        this.stage = primaryS;
        this.scene = scene;
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
            viewModel.getStem(isStem);
        }
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


}
