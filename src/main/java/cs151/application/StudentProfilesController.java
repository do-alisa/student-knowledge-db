package cs151.application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Controller for Create Student Profile page per spec (save & load CSV). */
public class StudentProfilesController {

    // --- Form controls (fx:id must match FXML) ---
    // 2.1 Basic
    @FXML private TextField fullNameInput;
    @FXML private ChoiceBox<String> academicStatusChoice;
    @FXML private RadioButton employedYesRadio;
    @FXML private RadioButton employedNoRadio;
    @FXML private TextField jobDetailsInput;

    // 2.2 Skills/Interests
    @FXML private ListView<String> languagesList;  // from languages.txt (multi-select)
    @FXML private ListView<String> databasesList;  // hard-coded (multi-select)
    @FXML private ChoiceBox<String> preferredRoleChoice;

    // 2.3 Comments
    @FXML private TextArea commentInput;
    @FXML private Button addCommentBtn;

    // 2.4 Flags
    @FXML private CheckBox whitelistCheck;
    @FXML private CheckBox blacklistCheck;

    // --- TableView + columns ---
    @FXML private TableView<StudentProfile> profilesTable;
    @FXML private TableColumn<StudentProfile, String> nameCol;
    @FXML private TableColumn<StudentProfile, String> statusCol;
    @FXML private TableColumn<StudentProfile, String> employedCol;
    @FXML private TableColumn<StudentProfile, String> langsCol;
    @FXML private TableColumn<StudentProfile, String> dbsCol;
    @FXML private TableColumn<StudentProfile, String> roleCol;

    @FXML private VBox profilesSection;
    @FXML private Button toggleProfilesBtn;

    private final ProfileStore store = new ProfileStore();
    private final ObservableList<StudentProfile> profiles = FXCollections.observableArrayList();

    // languages.txt location
    private static final Path LANG_FILE = Paths.get(
            System.getProperty("user.home"), ".studentknowledgedb", "languages.txt");

    private static final List<String> ACADEMIC_STATUSES =
            List.of("Freshman", "Sophomore", "Junior", "Senior", "Graduate");

    private static final List<String> ROLES =
            List.of("Front-End", "Back-End", "Full-Stack", "Data", "Other");

    private static final List<String> DATABASES =
            List.of("MySQL", "Postgres", "MongoDB", "SQLite", "Oracle", "SQL Server");

    @FXML
    private void initialize() {
        // ChoiceBoxes
        academicStatusChoice.setItems(FXCollections.observableArrayList(ACADEMIC_STATUSES));
        preferredRoleChoice.setItems(FXCollections.observableArrayList(ROLES));

        // Radios (toggle group)
        ToggleGroup empGroup = new ToggleGroup();
        employedYesRadio.setToggleGroup(empGroup);
        employedNoRadio.setToggleGroup(empGroup);
        employedNoRadio.setSelected(true); // default not employed
        jobDetailsInput.setDisable(true);

        // job details enable/disable
        employedYesRadio.selectedProperty().addListener((obs, old, isSel) -> jobDetailsInput.setDisable(!isSel));
        employedNoRadio.selectedProperty().addListener((obs, old, isSel) -> jobDetailsInput.setDisable(isSel));

        // checkboxes mutual exclusivity
        whitelistCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) blacklistCheck.setSelected(false);
        });
        blacklistCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) whitelistCheck.setSelected(false);
        });

        // languages list (multi-select)
        languagesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        languagesList.setItems(FXCollections.observableArrayList(loadLanguagesSorted()));

        // databases list (multi-select)
        databasesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        databasesList.setItems(FXCollections.observableArrayList(new ArrayList<>(DATABASES)));
        databasesList.getItems().sort(String::compareToIgnoreCase);

        // comments
        addCommentBtn.setOnAction(e -> onAddComment());

        // table columns bindings
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFullName()));
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAcademicStatus()));
        employedCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().isEmployed() ? "Employed" : "Not Employed"));
        langsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLanguagesAsString()));
        dbsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDatabasesAsString()));
        roleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPreferredRole()));

        // load existing profiles
        profiles.setAll(store.loadAll());
        sortByNameAZ();
        profilesTable.setItems(profiles);

        // select first row if exists
        if (!profiles.isEmpty()) profilesTable.getSelectionModel().selectFirst();

        profilesSection.setVisible(false);
        profilesSection.setManaged(false); // removes it from layout when hidden
        toggleProfilesBtn.setText("View All Profiles");
    }

    // Actions
    @FXML
    private void onSubmitProfile() {
        String name = trimOrEmpty(fullNameInput.getText());
        if (name.isEmpty()) { warn("Full Name is required."); return; }
        if (existsByName(name)) { warn("Duplicate entry: a profile with this full name already exists."); return; }

        String status = academicStatusChoice.getValue();
        if (status == null || status.isBlank()) { warn("Academic Status is required."); return; }

        boolean employed = employedYesRadio.isSelected();
        String jobDetails = trimOrEmpty(jobDetailsInput.getText());
        if (employed && jobDetails.isEmpty()) { warn("Job Details are required for employed students."); return; }

        List<String> langs = new ArrayList<>(languagesList.getSelectionModel().getSelectedItems());
        if (langs.isEmpty()) { warn("Please select at least one Programming Language."); return; }

        List<String> dbs = new ArrayList<>(databasesList.getSelectionModel().getSelectedItems());
        if (dbs.isEmpty()) { warn("Please select at least one Database."); return; }

        String role = preferredRoleChoice.getValue();
        if (role == null || role.isBlank()) { warn("Preferred Professional Role is required."); return; }

        boolean whitelist = whitelistCheck.isSelected();
        boolean blacklist = blacklistCheck.isSelected();
        if (whitelist && blacklist) { warn("Whitelist and Blacklist are mutually exclusive."); return; }

        // Build profile
        StudentProfile p = new StudentProfile();
        p.setFullName(name);
        p.setAcademicStatus(status);
        p.setEmployed(employed);
        p.setJobDetails(jobDetails);
        p.setLanguages(langs);
        p.setDatabases(dbs);
        p.setPreferredRole(role);
        p.setWhitelist(whitelist);
        p.setBlacklist(blacklist);

        // Add initial comment if any (optional)
        String firstComment = trimOrEmpty(commentInput.getText());
        if (!firstComment.isEmpty()) p.addComment(firstComment);

        // persist + update table
        store.save(p);
        profiles.add(p);
        sortByNameAZ();
        clearForm();
        info("Saved profile for: " + name);
    }

    @FXML
    private void onCancelForm() { clearForm(); }

    @FXML
    private void onBackToHome(ActionEvent e) throws IOException {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cs151/application/hello-view.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 650);
        stage.setTitle("StudentKnowledgeDB | Home Page");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private void onAddComment() {
        String txt = trimOrEmpty(commentInput.getText());
        if (txt.isEmpty()) { warn("Type a comment first."); return; }
        StudentProfile selected = profilesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { warn("Select a student in the table to add a comment."); return; }
        selected.addComment(txt);
        // Re-save profile by appending a new row OR (better) rebuild the file.
        // For simplicity, append a new full row for now (spec allows multiple comments over time).
        store.save(selected);
        commentInput.clear();
        info("Comment added.");
    }

    @FXML
    private void onToggleProfiles() {
        boolean nowVisible = !profilesSection.isVisible();
        if (nowVisible) {
            sortByNameAZ(); // ensure A→Z before showing
        }
        profilesSection.setVisible(nowVisible);
        profilesSection.setManaged(nowVisible);
        toggleProfilesBtn.setText(nowVisible ? "Hide Profiles" : "View All Profiles");
    }


    // Helper functions
    private void clearForm() {
        fullNameInput.clear();
        academicStatusChoice.getSelectionModel().clearSelection();
        employedNoRadio.setSelected(true);
        jobDetailsInput.clear();
        jobDetailsInput.setDisable(true);

        languagesList.getSelectionModel().clearSelection();
        databasesList.getSelectionModel().clearSelection();
        preferredRoleChoice.getSelectionModel().clearSelection();

        whitelistCheck.setSelected(false);
        blacklistCheck.setSelected(false);

        commentInput.clear();
        fullNameInput.requestFocus();
    }

    private void sortByNameAZ() {
        // Sort by last name, then full name for consistent order
        FXCollections.sort(profiles, Comparator.comparing((StudentProfile sp) -> sp.getLastName().toLowerCase())
                .thenComparing(sp -> sp.getFullName().toLowerCase()));
    }

    private List<String> loadLanguagesSorted() {
        try {
            if (!Files.exists(LANG_FILE)) return List.of();
            List<String> list = Files.readAllLines(LANG_FILE, StandardCharsets.UTF_8).stream()
                    .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
            list.sort(String::compareToIgnoreCase);
            return list;
        } catch (IOException e) {
            System.err.println("READ LANGUAGES FAILED: " + e.getMessage());
            return List.of();
        }
    }

    private boolean existsByName(String nameTrimmed) {
        String key = nameTrimmed.trim().toLowerCase();
        return profiles.stream().anyMatch(p -> p.getFullName() != null && p.getFullName().trim().toLowerCase().equals(key));
    }

    private static String trimOrEmpty(String s){ return (s == null) ? "" : s.trim(); }

    private void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private void warn(String m){ new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
}
