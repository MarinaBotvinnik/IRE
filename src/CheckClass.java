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
import Model.ReadFile;

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
        //Show the Main Window
        primaryStage.show();
    }


    public static void main(String[] args) {
        //launch(args);
        ReadFile readFile = new ReadFile("Resource/toCheck");
        readFile.readFile();
    }
}
