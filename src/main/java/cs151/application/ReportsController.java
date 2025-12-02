package cs151.application;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportsController {

    @FXML private TableView<StudentProfile> table;
    @FXML private TableColumn<StudentProfile, String> colName;
    @FXML private TableColumn<StudentProfile, String> colStatus;
    @FXML private TableColumn<StudentProfile, String> colEmployed;
    @FXML private TableColumn<StudentProfile, String> colJob;
    @FXML private TableColumn<StudentProfile, String> colLangs;
    @FXML private TableColumn<StudentProfile, String> colDBs;
    @FXML private TableColumn<StudentProfile, String> colRole;
    @FXML private TableColumn<StudentProfile, String> colFlag;

    @FXML private RadioButton rbWhitelist;
    @FXML private RadioButton rbBlacklist;

    private final ProfileStore store = new ProfileStore();

    // NEW: the context depends on the parent type (interface)
    private ProfileFilter currentFilter;

    @FXML
    private void initialize() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create concrete strategy objects (children implementing ProfileFilter)
        ProfileFilter whitelistFilter = new WhitelistProfileFilter();
        ProfileFilter blacklistFilter = new BlacklistProfileFilter();

        // ToggleGroup wires UI to the current filter (polymorphism)
        ToggleGroup group = new ToggleGroup();
        rbWhitelist.setToggleGroup(group);
        rbBlacklist.setToggleGroup(group);

        // Default selection: whitelist
        rbWhitelist.setSelected(true);
        currentFilter = whitelistFilter;

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == rbWhitelist) {
                currentFilter = whitelistFilter;
            } else if (newToggle == rbBlacklist) {
                currentFilter = blacklistFilter;
            }
            refresh();
        });

        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        colStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().getAcademicStatus())));
        colEmployed.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isEmployed() ? "Yes" : "No"));
        colJob.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().getJobDetails())));
        colLangs.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getLanguagesAsString()));
        colDBs.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDatabasesAsString()));
        colRole.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().getPreferredRole())));
        colFlag.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().isWhitelist() ? "Whitelist" :
                        (c.getValue().isBlacklist() ? "Blacklist" : "")));

        // Double-click to view details (unchanged)
        table.setRowFactory(tv -> {
            TableRow<StudentProfile> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    StudentProfile p = row.getItem();
                    Stage owner = (Stage) table.getScene().getWindow();
                    StudentDetailsController.showDialog(owner, p);
                }
            });
            return row;
        });

        refresh();
    }

    @FXML private void onToggle() { refresh(); }
    @FXML private void onRefresh() { refresh(); }

    // Now uses the polymorphic filter instead of an if/else
    private void refresh() {
        List<StudentProfile> all = store.loadAll();

        List<StudentProfile> result = all.stream()
                .filter(p -> currentFilter == null || currentFilter.matches(p))
                .sorted(Comparator.comparing(StudentProfile::getLastName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        table.setItems(FXCollections.observableArrayList(result));
        if (!result.isEmpty()) {
            table.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onBackToHome(javafx.event.ActionEvent e) throws IOException {
        javafx.fxml.FXMLLoader loader =
                new javafx.fxml.FXMLLoader(getClass().getResource("/cs151/application/hello-view.fxml"));
        javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 1000, 650);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setTitle("StudentKnowledgeDB | Home Page");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.centerOnScreen();
        stage.show();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
