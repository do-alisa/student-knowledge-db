package cs151.application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {
    // App window size
    private static final double APP_W = 1000;
    private static final double APP_H = 650;
    private static final String R = "/cs151/application/";


    // Input on the Define page
    @FXML private TextField nameInput;
    @FXML private TableView<String> langTable;
    @FXML private TableColumn<String, String> nameCol;
    private static final String APP_FOLDER = ".studentknowledgedb2";
    private static final String FILE_NAME  = "languages.txt";
    private final Path dataDir  = Paths.get(System.getProperty("user.home"), APP_FOLDER);
    private final Path dataFile = dataDir.resolve(FILE_NAME);
    private static final List<String> DEFAULT_LANGS = List.of("Java", "Python", "C++");


    private void seedDefaultLangsIfEmpty() {
        List<String> all = readAllLanguages();
        if (all.isEmpty()) {
            try {
                for (String s : DEFAULT_LANGS) {
                    saveLanguage(s);
                }
            } catch (IOException e) {
                System.err.println("SEED FAILED: " + e.getMessage());
            }
        }
    }


    @FXML
    private void initialize() {
        if (langTable != null) {
            langTable.setPlaceholder(new Label("No languages yet"));
            nameCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
            nameCol.setComparator(String::compareToIgnoreCase); // A→Z
            ensureDataDir();

            // first time show 3 language
            seedDefaultLangsIfEmpty();
            refreshTable(); // load from disk and sort
        }
    }

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
        setScene((Stage)((Node)e.getSource()).getScene().getWindow(),
                R + "DefineProgrammingLanguages.fxml",
                "StudentKnowledgeDB | Define Programming Languages");
    }

    // Define -> Home
    @FXML
    private void onBackToHome(ActionEvent e) throws IOException {
        setScene((Stage)((Node)e.getSource()).getScene().getWindow(),
                R + "hello-view.fxml",
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
        try {
            ensureDataDir();
            saveLanguage(name);   // write to file
            refreshTable();       // reload and sort
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
    private void ensureDataDir() {
        try { Files.createDirectories(dataDir); }
        catch (IOException e) { throw new RuntimeException("Cannot create data dir: " + dataDir, e); }
    }

    private List<String> readAllLanguages() {
        try {
            if (!Files.exists(dataFile)) return List.of();
            return Files.readAllLines(dataFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("READ FAILED: " + e);
            return List.of();
        }
    }

    private void saveLanguage(String raw) throws IOException {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Language name is required.");

        // prevent duplicates (case-insensitive)
        boolean exists = readAllLanguages().stream().anyMatch(s -> s.equalsIgnoreCase(name));
        if (exists) return;

        Files.writeString(
                dataFile,
                name + System.lineSeparator(),
                StandardCharsets.UTF_8,
                Files.exists(dataFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
        );
    }

    private void refreshTable() {
        if (langTable == null) return;
        List<String> all = new ArrayList<>(readAllLanguages());
        all.sort(String::compareToIgnoreCase); // A→Z
        langTable.setItems(FXCollections.observableArrayList(all));
        if (!all.isEmpty()) langTable.getSelectionModel().selectFirst();
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }


    @FXML
    private void onGoToCreateProfile(ActionEvent e) throws IOException {
        setScene((Stage) ((Node) e.getSource()).getScene().getWindow(),
                R + "CreateStudentProfile.fxml",
                "StudentKnowledgeDB | Create Student Profile");
    }


    @FXML
    private void onGoToSearchPage(javafx.event.ActionEvent e) throws IOException {
        setScene((Stage)((javafx.scene.Node)e.getSource()).getScene().getWindow(),
                "/cs151/application/SearchProfiles.fxml",
                "StudentKnowledgeDB | Search Students Profiles");
    }

    @FXML
    private void onGoToReportsPage(javafx.event.ActionEvent e) throws IOException {
        setScene((Stage)((javafx.scene.Node)e.getSource()).getScene().getWindow(),
                "/cs151/application/ReportsView.fxml",
                "StudentKnowledgeDB | Reports");
    }




}
