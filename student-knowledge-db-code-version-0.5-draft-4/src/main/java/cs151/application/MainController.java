package cs151.application;


import javafx.collections.FXCollections;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class MainController {
    // Window + resource base
    private static final double APP_W = 1000;
    private static final double APP_H = 650;
    private static final String R = "/cs151/application/";

    // Defaults to show on first run (optional)
    private static final List<String> DEFAULT_LANGS = List.of("Java", "Python", "C++");

    // Define page controls (match your FXML fx:ids)
    @FXML private TextField nameInput;
    @FXML private TableView<Language> langTable;
    @FXML private TableColumn<Language, String> nameCol;

    // Storage
    private final LanguageRepository repo = new LanguageRepository();

    // Lifecycle: runs each time an FXML using this controller loads
    @FXML
    private void initialize() {
        if (langTable != null) {
            langTable.setPlaceholder(new Label("No languages yet"));

            // Bind column to model's name property
            nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
            nameCol.setComparator(String::compareToIgnoreCase); // A→Z, case-insensitive

            // Ensure storage, seed once, then load and sort
            repo.ensureDataDir();
            repo.seedDefaultsIfEmpty(DEFAULT_LANGS);
            refreshTable();
        }
    }

    // ---------- Navigation ----------
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

    @FXML
    private void onGoToDefinePage(ActionEvent e) throws IOException {
        setScene((Stage)((Node)e.getSource()).getScene().getWindow(),
                R + "DefineProgrammingLanguages.fxml",
                "StudentKnowledgeDB | Define Programming Languages");
    }

    @FXML
    private void onBackToHome(ActionEvent e) throws IOException {
        setScene((Stage)((Node)e.getSource()).getScene().getWindow(),
                R + "hello-view.fxml",
                "StudentKnowledgeDB | Home Page");
    }

    @FXML
    private void onGoToCreateProfile(ActionEvent e) throws IOException {
        setScene((Stage) ((Node) e.getSource()).getScene().getWindow(),
                R + "CreateStudentProfile.fxml",
                "StudentKnowledgeDB | Create Student Profile");
    }

    // ---------- Form actions ----------
    @FXML
    private void onSubmitLanguage(ActionEvent e) {
        String raw = nameInput == null ? "" : nameInput.getText();
        String name = raw == null ? "" : raw.trim();

        if (name.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Error", "Please enter a language name.");
            return;
        }
        try {
            repo.saveName(name);  // write to file (de-duped)
            refreshTable();       // reload + sort
            alert(Alert.AlertType.INFORMATION, "Submitted", "Successfully submitted: " + name);
            nameInput.clear();
            nameInput.requestFocus();
        } catch (IllegalArgumentException iae) {
            alert(Alert.AlertType.WARNING, "Warning", iae.getMessage());
        } catch (IOException ioe) {
            alert(Alert.AlertType.ERROR, "Error", "Save failed: " + ioe.getMessage());
        }
    }

    @FXML
    private void onCancelForm(ActionEvent e) {
        if (nameInput != null) {
            nameInput.clear();
            nameInput.requestFocus();
        }
    }

    // ---------- Helpers ----------
    private void refreshTable() {
        if (langTable == null) return;
        var all = new ArrayList<>(repo.findAll());
        all.sort(Comparator.comparing(
                l -> l.getName() == null ? "" : l.getName().toLowerCase()
        )); // A→Z, case-insensitive
        langTable.setItems(FXCollections.observableArrayList(all));
        if (!all.isEmpty()) langTable.getSelectionModel().selectFirst();
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
}
