import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Model.Model;
import ViewModel.ViewModel;
import View.mainMenuController;
import Model.ReadFile;

public class CheckClass extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //ViewModel -> Model
        Model model = new Model();
        ViewModel viewModel = new ViewModel();

        //Loading Main Windows
        primaryStage.setTitle("The Engine Search");
        primaryStage.setWidth(600);
        primaryStage.setHeight(400);
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getClassLoader().getResource("mainMenu.fxml").openStream());
        Scene scene = new Scene(root, 600, 400);
        //scene.getStylesheets().add(getClass().getResource("View/mainMenu.fxml").toExternalForm());
        primaryStage.minWidthProperty().bind(scene.heightProperty());
        primaryStage.minHeightProperty().bind(scene.widthProperty().divide(2));
        primaryStage.setScene(scene);

        //View -> ViewModel
        mainMenuController view = fxmlLoader.getController();
        view.initialize(viewModel,primaryStage,scene);
        //Show the Main Window
        primaryStage.show();

        /*FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getClassLoader().getResource("mainMenu.fxml").openStream());
        primaryStage.setTitle("The Search Engine");
        primaryStage.setScene(new Scene(root, 800, 700));
        primaryStage.show();*/
    }


    public static void main(String[] args) {

        ReadFile readFile = new ReadFile();
        long startTime = System.currentTimeMillis();
        long start = System.nanoTime();
        readFile.readFile("Resource/toCheck");
        long end = System.nanoTime();
        long endTime = System.currentTimeMillis();
        System.out.println("------ALL DONE MATE!-----");
        System.out.println((end-start)/1000000.0 + "milisec");
        System.out.println((endTime-startTime)/60000 + "minutes");
        //launch(args);
         System.exit(0);
    }
}
