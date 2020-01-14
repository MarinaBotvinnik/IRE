
package View;

import ViewModel.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * Class in charge of controlling the gui
 */
public class mainMenuController {

    public TextField tf_corpusPath;
    public TextField tf_postingPath;
    public TextField tf_OptionA;
    public TextField tf_OptionB;
    public CheckBox cb_stem;
    public CheckBox cb_semantic;
    public Pane p_first;
    public Pane p_second;
    public Pane p_dictionary;
    public Pane p_Query;
    public Pane p_Options;
    public Pane p_Answers;
    public TextArea c_Posting;
    public TextArea c_docsAndEnt;
    public ChoiceBox<String> ch_queryOp;
    public ChoiceBox<String> ch_queries;


    private ViewModel viewModel;
    private boolean isUploaded;
    private String postPath;
    private boolean isStem;
    private String corpusPath;

    /**
     * Method that initializing the view model to the view
     */
    public void initialize(ViewModel model, Stage primaryS, Scene scene){
        this.viewModel = model;
        isUploaded = false;
    }

    /**
     * Method that begins when the "GO" button is pressed.
     * The method takes the texts of the paths from the text fields and starts the posting proccess if it didnt make it before
     * in the same path.
     * After the posting completed , it will show a message with the neede information.
     * then , it will show the next pane.
     */
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
            this.isStem = isStem;
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
            long startTime     = System.nanoTime();
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

    /**
     * Method that replace the second pane with the third one
     */
    public void setPane3(){
        p_second.setVisible(false);
        p_second.setDisable(true);
        p_dictionary.setVisible(true);
        p_dictionary.setDisable(false);
    }

    /**
     * Method that replace pane 3 with pane 2
     */
    public void setBackPane2(){
        p_dictionary.setVisible(false);
        p_dictionary.setDisable(true);
        p_second.setVisible(true);
        p_second.setDisable(false);
    }

    /**
     * Method that replace pane 2 with pane 4
     */
    public void setQueryPane(){
//        if(!isUploaded) {
//            Alert alert = new Alert(Alert.AlertType.WARNING, "Please upload the dictionary first");
//            alert.show();
//        }
//        else {
            p_second.setVisible(false);
            p_second.setDisable(true);
            p_Query.setVisible(true);
            p_Query.setDisable(false);
        //}
    }

    public void setQueryPaneBack(){
        p_Options.setDisable(true);
        p_Options.setVisible(false);
        p_Query.setVisible(true);
        p_Query.setDisable(false);
    }

    public void setOptionsPane(){
        p_Query.setVisible(false);
        p_Query.setDisable(true);
        p_Options.setDisable(false);
        p_Options.setVisible(true);
    }

    public void setOptionsPaneBack(){
        p_Answers.setVisible(false);
        p_Answers.setDisable(true);
        p_Options.setDisable(false);
        p_Options.setVisible(true);
    }

    public void setP_Answers(){
        viewModel.writeAns();
        ch_queries = new ChoiceBox<>();
        LinkedHashSet<String> queries = viewModel.getQueriesWithNoDesc();
        ObservableList<String> add = FXCollections.observableArrayList();
        String defaultQ="";
        for (String query: queries) {
            if(defaultQ.isEmpty()){
                defaultQ = query;
            }
            add.add(query);
        }
        ch_queries = new ChoiceBox<>(add);
        ch_queries.setValue(defaultQ);
        p_Answers.getChildren().add(ch_queries);
        p_Options.setDisable(true);
        p_Options.setVisible(false);
        p_Answers.setVisible(true);
        p_Answers.setDisable(false);

    }

    public void startSearch(){
        String choose = ch_queryOp.getValue();
        boolean isSemantic = cb_semantic.isSelected();
        // if there is a document of queries
        if(choose.equals("OPTION A")){
            String optionAText = tf_OptionA.getText();
            if (optionAText.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You need to enter a path, or choose another option");
                alert.show();
            }
            else{
                viewModel.searchQuery(optionAText,true,isStem,isSemantic,postPath);
            }
        }
        // the user entered his own query
        else{
            String optionBText = tf_OptionB.getText();
            if (optionBText.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You need to enter a query, or choose another option");
                alert.show();
            }
            else{
                viewModel.searchQuery(optionBText,false,isStem, isSemantic, postPath);
            }
        }
        setOptionsPane();
    }

    public void showAnswers(){
        c_docsAndEnt.clear();
        HashMap<String,HashMap<String,LinkedHashMap<String,Double>>> d_docsAndEntitiesForQuery = viewModel.getAnswers();
        String query = ch_queries.getValue();
        if(query.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please choose a query!");
            alert.show();
        }
        else{
            HashMap<String,LinkedHashMap<String,Double>> docs = d_docsAndEntitiesForQuery.get(query);
            StringBuilder str1 = new StringBuilder();
            for (Map.Entry<String, LinkedHashMap<String,Double>> entry : docs.entrySet()) {
                String docNo = entry.getKey();
                LinkedHashMap<String,Double> entities = entry.getValue();
                str1.append(docNo).append("               ------->              ");
                int count =0;
                for (Map.Entry<String, Double> entry1: entities.entrySet()){
                    String entity = entry1.getKey();
                    str1.append(entity).append(" ").append(entry1.getValue()).append(",");
                    count++;
                }
                if(count ==0){
                    str1.append("no entities in this document");
                }
                str1.append("\n");
            }
            c_docsAndEnt.textProperty().set(str1.toString());
        }
    }
    /**
     * Method that begins when "browse" of corpus is pressed
     * it will put text of the chosen path to the text field
     */
    public void browseCorpus(){
        getPath(tf_corpusPath);
    }

    /**
     * Method that begins when "browse" of posting is pressed
     * it will put text of the chosen path to the text field
     */
    public void browsePost(){
        getPath(tf_postingPath);
    }

    public void browseQuery(){
        tf_OptionA.clear();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.showOpenDialog(null);
        File file = fileChooser.getSelectedFile();
        if(file!=null) {
            String p = file.getAbsolutePath();
            tf_OptionA.appendText(p);
        }
    }

    /**
     * Method that accepts a text filed and fills it with the path from the file chooser
     */
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

    /**
     * This method uploads the dictionary to the ViewModel and shows a proper message
     */
    public void uploadDictionary(){
        viewModel.uploadDictionary(cb_stem.isSelected(),postPath);
        isUploaded = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "The dictionary uploaded correctly!");
        alert.show();
    }

    /**
     * This method shows in the third pane the dictionary that was uploaded
     */
    public void showDictionary() {
        if (isUploaded) {
            LinkedHashMap<String, String> dic = viewModel.getDictionary();
            StringBuilder str1 = new StringBuilder();
            for (Map.Entry<String, String> entry : dic.entrySet()) {
                String term = entry.getKey();
                String posting = entry.getValue();
                String[] show = posting.split(",");
                str1.append(term).append("       --->       ").append(show[1]).append("\n");
            }
            c_Posting.textProperty().set(str1.toString());
            setPane3();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You didn't uploaded your dictionary yet.");
            alert.show();
        }
    }

    /**
     * Method that sends a request to the ViewModel to reset the system.
     * after the function is complete it will show the proper message
     */
    public void reset(){
        viewModel.reset();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "you reset the system , all posting files were deleted!");
        alert.show();
        setPane1();
    }

    /**
     * Method that sets back pane one
     */
    public void setPane1(){
        isUploaded = false;
        tf_corpusPath.clear();
        tf_postingPath.clear();
        p_second.setDisable(true);
        p_second.setVisible(false);
        p_first.setVisible(true);
        p_first.setDisable(false);
    }


}