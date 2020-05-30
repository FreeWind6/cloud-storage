import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StartClient extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        primaryStage.setTitle("Cloud storage FreeWind");
        primaryStage.setScene(new Scene(root, 1280, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
