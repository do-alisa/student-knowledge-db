package cs151.application;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Controller for SearchProfiles.fxml */
public class SearchProfilesController {

    // ==== Search filters ====
    @FXML private TextField nameLike;
    @FXML private ChoiceBox<String> statusChoice, employmentChoice, roleChoice;
    @FXML private ChoiceBox<String> languagesChoice, databasesChoice; // ChoiceBox instead of ListView
    @FXML private CheckBox whitelistOnly, blacklistOnly;

    // ==== TableView ====
    @FXML private TableView<StudentProfile> table;
    @FXML private TableColumn<StudentProfile, String>
            nameCol, statusCol, empCol, jobCol, langsCol, dbsCol, roleCol, wlCol, blCol, delCol;

    private final ProfileStore store = new ProfileStore();
    private final ObservableList<StudentProfile> all = FXCollections.observableArrayList();
    private final ObservableList<StudentProfile> filtered = FXCollections.observableArrayList();

    private static final Path LANG_FILE = Paths.get(System.getProperty("user.home"), ".studentknowledgedb", "languages.txt");
    private static final List<String> STATUSES = List.of("Freshman","Sophomore","Junior","Senior","Graduate");
    private static final List<String> ROLES    = List.of("Front-End","Back-End","Full-Stack","Data","Other");
    private static final List<String> DBS      = List.of("MySQL","Postgres","MongoDB","SQLite","Oracle","SQL Server","N/A");

    @FXML
    private void initialize() {
        // --- setup filters ---
        statusChoice.setItems(FXCollections.observableArrayList("Any"));
        statusChoice.getItems().addAll(STATUSES);
        statusChoice.getSelectionModel().selectFirst();

        employmentChoice.setItems(FXCollections.observableArrayList("Any","Employed","Not Employed"));
        employmentChoice.getSelectionModel().selectFirst();

        roleChoice.setItems(FXCollections.observableArrayList("Any"));
        roleChoice.getItems().addAll(ROLES);
        roleChoice.getSelectionModel().selectFirst();

        // Languages -> ChoiceBox
        java.util.List<String> langs = new ArrayList<>();
        langs.add("Any");
        langs.addAll(loadLangs());
        languagesChoice.setItems(FXCollections.observableArrayList(langs));
        languagesChoice.getSelectionModel().selectFirst();

        // Databases -> ChoiceBox
        java.util.List<String> dbs = new ArrayList<>();
        dbs.add("Any");
        dbs.addAll(DBS);
        databasesChoice.setItems(FXCollections.observableArrayList(dbs));
        databasesChoice.getSelectionModel().selectFirst();

        // --- setup table columns ---
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAcademicStatus()));
        empCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isEmployed() ? "Employed" : "Not Employed"));
        jobCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getJobDetails()));
        langsCol.setCellValueFactory(c -> new SimpleStringProperty(String.join(", ", c.getValue().getLanguages())));
        dbsCol.setCellValueFactory(c -> new SimpleStringProperty(String.join(", ", c.getValue().getDatabases())));
        roleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPreferredRole()));
        wlCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isWhitelist() ? "Yes" : "No"));
        blCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isBlacklist() ? "Yes" : "No"));

        // ---- Delete button for each row ----
        delCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setOnAction(e -> {
                    StudentProfile p = getTableView().getItems().get(getIndex());

                    // Optional: confirmation dialog
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete \"" + p.getFullName() + "\"?",
                            ButtonType.OK, ButtonType.CANCEL);
                    confirm.setHeaderText(null);
                    confirm.setTitle("Confirm Delete");
                    Optional<ButtonType> choice = confirm.showAndWait();

                    if (choice.isPresent() && choice.get() == ButtonType.OK) {
                        boolean ok = store.delete(p);   // Permanently remove from CSV file
                        if (ok) {
                            all.remove(p);              // Remove from in-memory list
                            applyFilters();             // Refresh the TableView
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Delete failed.").showAndWait();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn); // Show button only for non-empty rows
            }
        });

        // --- load all data from CSV ---
        all.setAll(store.loadAll());
        applyFilters();
        table.setItems(filtered);
        table.setPlaceholder(new Label("No matching profiles"));

        // WL/BL mutual exclusion
        whitelistOnly.selectedProperty().addListener((o,ov,nv)->{ if (nv) blacklistOnly.setSelected(false); });
        blacklistOnly.selectedProperty().addListener((o,ov,nv)->{ if (nv) whitelistOnly.setSelected(false); });




    }

    // ==== Buttons ====
    @FXML private void onSearch() { applyFilters(); }

    @FXML private void onClear() {
        nameLike.clear();
        statusChoice.getSelectionModel().selectFirst();
        employmentChoice.getSelectionModel().selectFirst();
        roleChoice.getSelectionModel().selectFirst();
        languagesChoice.getSelectionModel().selectFirst();
        databasesChoice.getSelectionModel().selectFirst();
        whitelistOnly.setSelected(false);
        blacklistOnly.setSelected(false);
        applyFilters();
    }

    @FXML
    private void onBack(ActionEvent e) throws IOException {
        Stage stage = (Stage) table.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cs151/application/hello-view.fxml"));
        stage.setScene(new Scene(loader.load(), 1000, 650));
        stage.setTitle("StudentKnowledgeDB | Home Page");
        stage.centerOnScreen();
        stage.show();
    }

    // ==== Filtering Logic ====
    private void applyFilters() {
        String name = opt(nameLike.getText()).toLowerCase();
        String status = statusChoice.getValue();
        String emp = employmentChoice.getValue();
        String role = roleChoice.getValue();

        String langSel = languagesChoice.getValue();
        String dbSel   = databasesChoice.getValue();
        boolean wlOnly = whitelistOnly.isSelected();
        boolean blOnly = blacklistOnly.isSelected();

        filtered.setAll(all.stream().filter(p -> {
                    if (!name.isEmpty() && (p.getFullName()==null || !p.getFullName().toLowerCase().contains(name))) return false;
                    if (!"Any".equals(status) && !Objects.equals(p.getAcademicStatus(), status)) return false;
                    if ("Employed".equals(emp) && !p.isEmployed()) return false;
                    if ("Not Employed".equals(emp) && p.isEmployed()) return false;
                    if (!"Any".equals(role) && !Objects.equals(p.getPreferredRole(), role)) return false;
                    if (wlOnly && !p.isWhitelist()) return false;
                    if (blOnly && !p.isBlacklist()) return false;

                    if (langSel != null && !"Any".equals(langSel)
                            && p.getLanguages().stream().noneMatch(l -> l.equalsIgnoreCase(langSel))) return false;

                    if (dbSel != null && !"Any".equals(dbSel)
                            && p.getDatabases().stream().noneMatch(d -> d.equalsIgnoreCase(dbSel))) return false;

                    return true;
                }).sorted(Comparator.comparing((StudentProfile sp) -> sp.getLastName().toLowerCase())
                        .thenComparing(sp -> sp.getFullName().toLowerCase()))
                .collect(Collectors.toList()));
    }

    private List<String> loadLangs() {
        try {
            if (!Files.exists(LANG_FILE)) return List.of();
            List<String> list = Files.readAllLines(LANG_FILE, StandardCharsets.UTF_8).stream()
                    .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
            list.sort(String::compareToIgnoreCase);
            return list;
        } catch (IOException e) { return List.of(); }
    }

    private static String opt(String s){ return (s == null) ? "" : s.trim(); }
}
