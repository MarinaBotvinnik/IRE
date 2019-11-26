import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import Model.Model;
import ViewModel.ViewModel;
import View.mainMenuController;

import java.util.Optional;

public class CheckClass extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //ViewModel -> Model
        Model model = new Model();
        ViewModel viewModel = new ViewModel(model);

        //Loading Main Windows
        primaryStage.setTitle("The Engine Search");
        primaryStage.setWidth(600);
        primaryStage.setHeight(400);
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("View/mainMenu.fxml").openStream());
        Scene scene = new Scene(root, 800, 700);
        scene.getStylesheets().add(getClass().getResource("View/mainMenu.fxml").toExternalForm());
        primaryStage.minWidthProperty().bind(scene.heightProperty());
        primaryStage.minHeightProperty().bind(scene.widthProperty().divide(2));
        primaryStage.setScene(scene);

        //View -> ViewModel
        mainMenuController view = fxmlLoader.getController();
        view.initialize(viewModel,primaryStage,scene);
        //--------------
        //setStageCloseEvent(primaryStage,model);
        //setStageCloseEvent(primaryStage, model);
        //
        //Show the Main Window
        primaryStage.show();
    }

    /*private void setStageCloseEvent(Stage primaryStage, MyModel model) {
        primaryStage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure you want to exit?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK
                // Close the program properly
                model.close();
            } else {
                // ... user chose CANCEL or closed the dialog
                event.consume();
            }
        });
    }*/

    public static void main(String[] args) {
        launch(args);
    }
}
