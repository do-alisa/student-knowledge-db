package cs151.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private static final double APP_W = 1000;
    private static final double APP_H = 650;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/cs151/application/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), APP_W, APP_H);
        stage.setTitle("StudentKnowledgeDB | Home Page");
        stage.setScene(scene);

        stage.setMinWidth(APP_W);
        stage.setMinHeight(APP_H);

        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }
}