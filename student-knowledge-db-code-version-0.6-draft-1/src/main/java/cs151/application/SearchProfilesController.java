
package cs151.application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SearchProfilesController {

    // --- Filter controls ---
    @FXML private TextField searchName;
    @FXML private ChoiceBox<String> searchStatus;
    @FXML private RadioButton empAnyRadio;
    @FXML private RadioButton empYesRadio;
    @FXML private RadioButton empNoRadio;
    @FXML private ListView<String> searchLanguages;
    @FXML private ListView<String> searchDatabases;
    @FXML private ChoiceBox<String> searchRole;
    @FXML private CheckBox searchWhitelist;
    @FXML private CheckBox searchBlacklist;

    // --- Table ---
    @FXML private TableView<StudentProfile> resultsTable;
    @FXML private TableColumn<StudentProfile, String> nameCol;
    @FXML private TableColumn<StudentProfile, String> statusCol;
    @FXML private TableColumn<StudentProfile, String> employedCol;
    @FXML private TableColumn<StudentProfile, String> langsCol;
    @FXML private TableColumn<StudentProfile, String> dbsCol;
    @FXML private TableColumn<StudentProfile, String> roleCol;
    @FXML private TableColumn<StudentProfile, String> jobCol;
    @FXML private TableColumn<StudentProfile, String> wlCol;
    @FXML private TableColumn<StudentProfile, String> blCol;
    @FXML private TableColumn<StudentProfile, String> commentsCol;
    @FXML private TableColumn<StudentProfile, Void> actionCol;

    @FXML private Button btnSearch;
    @FXML private Button btnShowAll;
    @FXML private Button btnBack;

    private final ProfileStore store = new ProfileStore();

    private static final List<String> ACADEMIC_STATUSES =
            List.of("Freshman", "Sophomore", "Junior", "Senior", "Graduate");

    private static final List<String> ROLES = List.of("Front-End", "Back-End", "Full-Stack", "Data", "Other");

    private static final List<String> DATABASES = List.of(
            "MySQL", "PostgreSQL", "SQLite", "MongoDB", "Oracle"
    );

    private static final Path LANG_FILE = Paths.get(
            System.getProperty("user.home"), ".studentknowledgedb", "languages.txt");

    @FXML
    private void initialize() {
        // Filters
        searchStatus.setItems(FXCollections.observableArrayList(ACADEMIC_STATUSES));
        searchRole.setItems(FXCollections.observableArrayList(ROLES));

        ToggleGroup tg = new ToggleGroup();
        empAnyRadio.setToggleGroup(tg);
        empYesRadio.setToggleGroup(tg);
        empNoRadio.setToggleGroup(tg);
        empAnyRadio.setSelected(true);

        searchLanguages.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        searchDatabases.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        searchLanguages.setItems(FXCollections.observableArrayList(loadLanguagesSorted()));
        searchDatabases.setItems(FXCollections.observableArrayList(DATABASES));

        // Table
        resultsTable.setPlaceholder(new Label("No results yet"));
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFullName()));
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAcademicStatus()));
        employedCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().isEmployed() ? "Employed" : "Not Employed"));
        langsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLanguagesAsString()));
        dbsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDatabasesAsString()));
        roleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPreferredRole()));
        jobCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getJobDetails()));
        wlCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().isWhitelist() ? "Yes" : "No"));
        blCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().isBlacklist() ? "Yes" : "No"));
        commentsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCommentsAsString()));

        addDeleteButton();

        // Buttons
        btnSearch.setOnAction(e -> applyFilters());
        btnShowAll.setOnAction(e -> {
            clearFilters();
            resultsTable.setItems(FXCollections.observableArrayList(store.loadAll()));
        });
        btnBack.setOnAction(this::onBackToHome);
    }

    private void addDeleteButton() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setOnAction(e -> {
                    StudentProfile sp = getTableView().getItems().get(getIndex());
                    if (sp == null) return;
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete profile for \"" + sp.getFullName() + "\"?\nThis cannot be undone.",
                            ButtonType.OK, ButtonType.CANCEL);
                    confirm.setHeaderText("Confirm Delete");
                    Optional<ButtonType> res = confirm.showAndWait();
                    if (res.isPresent() && res.get() == ButtonType.OK) {
                        boolean removed = store.deleteByFullName(sp.getFullName());
                        if (removed) {
                            getTableView().getItems().remove(sp);
                            info("Deleted: " + sp.getFullName());
                        } else {
                            warn("Profile not removed. Please try again.");
                        }
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void clearFilters() {
        searchName.clear();
        searchStatus.getSelectionModel().clearSelection();
        empAnyRadio.setSelected(true);
        searchLanguages.getSelectionModel().clearSelection();
        searchDatabases.getSelectionModel().clearSelection();
        searchRole.getSelectionModel().clearSelection();
        searchWhitelist.setSelected(false);
        searchBlacklist.setSelected(false);
    }

    private void applyFilters() {
        String nameQ = (searchName.getText() == null ? "" : searchName.getText().trim()).toLowerCase();
        String status = searchStatus.getValue();
        String role = searchRole.getValue();

        boolean wantYes = empYesRadio.isSelected();
        boolean wantNo  = empNoRadio.isSelected();
        boolean filterEmployment = wantYes || wantNo; // if both false, "Any"

        List<String> langs = new ArrayList<>(searchLanguages.getSelectionModel().getSelectedItems());
        List<String> dbs   = new ArrayList<>(searchDatabases.getSelectionModel().getSelectedItems());
        boolean wantWL = searchWhitelist.isSelected();
        boolean wantBL = searchBlacklist.isSelected();

        List<StudentProfile> data = store.loadAll();
        List<StudentProfile> filtered = new ArrayList<>();
        for (StudentProfile p : data) {
            if (!nameQ.isEmpty()) {
                String n = p.getFullName() == null ? "" : p.getFullName().toLowerCase();
                if (!n.contains(nameQ)) continue;
            }
            if (status != null && !status.isBlank()) {
                if (!status.equals(p.getAcademicStatus())) continue;
            }
            if (filterEmployment) {
                if (wantYes && !p.isEmployed()) continue;
                if (wantNo  &&  p.isEmployed()) continue;
            }
            if (!langs.isEmpty()) {
                if (!p.getLanguages().containsAll(langs)) continue; // must include all selected
            }
            if (!dbs.isEmpty()) {
                if (!p.getDatabases().containsAll(dbs)) continue;
            }
            if (role != null && !role.isBlank()) {
                if (!role.equals(p.getPreferredRole())) continue;
            }
            if (wantWL && !p.isWhitelist()) continue;
            if (wantBL && !p.isBlacklist()) continue;

            filtered.add(p);
        }
        // Sort A→Z by last name, then full name
        filtered.sort(Comparator.comparing((StudentProfile sp) -> sp.getLastName().toLowerCase())
                .thenComparing(sp -> sp.getFullName().toLowerCase()));
        resultsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // Load languages sorted A→Z from user file
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

    @FXML
    private void onBackToHome(ActionEvent e) {
        try {
            setScene((Stage)((Node)e.getSource()).getScene().getWindow(),
                    "/cs151/application/hello-view.fxml",
                    "StudentKnowledgeDB | Home Page");
        } catch (IOException ioException) {
            warn("Failed to navigate: " + ioException.getMessage());
        }
    }

    private void setScene(Stage stage, String resource, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Scene s = new Scene(loader.load(), 1000, 650);
        stage.setTitle(title);
        stage.setScene(s);
        stage.centerOnScreen();
        stage.show();
    }

    private void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private void warn(String m){ new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
}
