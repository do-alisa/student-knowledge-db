package cs151.application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    // App window size
    private static final double APP_W = 1000;
    private static final double APP_H = 650;

    // Input on the Define page
    @FXML private TextField nameInput;

    // switch scenes and a fixed window size
    private void setScene(Stage stage, String fxml, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Scene scene = new Scene(loader.load(), APP_W, APP_H);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMinWidth(APP_W);
        stage.setMinHeight(APP_H);
        stage.centerOnScreen();
        stage.show();
    }

    // Home -> Define
    @FXML
    private void onGoToDefinePage(ActionEvent e) throws IOException {
        setScene((Stage) ((Node) e.getSource()).getScene().getWindow(),
                "/cs151/application/DefineProgrammingLanguages.fxml",
                "StudentKnowledgeDB | Define Programming Languages");
    }

    // Define -> Home
    @FXML
    private void onBackToHome(ActionEvent e) throws IOException {
        setScene((Stage) ((Node) e.getSource()).getScene().getWindow(),
                "/cs151/application/hello-view.fxml",
                "StudentKnowledgeDB | Home Page");
    }

    // Submit: simple required check
    @FXML
    private void onSubmitLanguage(ActionEvent e) {
        String name = nameInput == null ? "" : nameInput.getText().trim();
        if (name.isEmpty()) {
            Alert empty = new Alert(Alert.AlertType.WARNING, "Please enter a language name.");
            empty.setHeaderText(null);
            empty.setTitle("Error");
            empty.showAndWait();
            return;
        }
        Alert submitted = new Alert(Alert.AlertType.INFORMATION, "Successfully submitted: " + name);
        submitted.setHeaderText(null);
        submitted.setTitle("Submitted");
        submitted.showAndWait();
        nameInput.clear();
    }

    @FXML
    private void onCancelForm(ActionEvent e) {
        if (nameInput != null) {
            nameInput.clear();
            nameInput.requestFocus();
        }
    }

}
