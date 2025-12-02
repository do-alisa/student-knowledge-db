package cs151.application;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller for ProfileDetail.fxml
 * Edit a single StudentProfile with multi-select languages/DBs and a tagged faculty note.
 */
public class ProfileDetailController {

    // ---------- UI controls ----------
    @FXML private TextField txtName, txtJob;
    @FXML private ComboBox<String> cmbStatus, cmbRole;
    @FXML private CheckBox chkEmployed, chkWhitelist, chkBlacklist;
    @FXML private ListView<String> lstLanguages, lstDatabases;
    @FXML private TextArea txtFacultyEval;

    // ---------- Model & services ----------
    private StudentProfile original;              // From search table
    private StudentProfile working;               // Local working copy
    private ProfileStore store;                   // Persistence
    private Consumer<StudentProfile> onSaved;     // Callback to refresh table

    // ---------- Constants ----------
    private static final String FACULTY_TAG = "FACULTY_NOTE";
    private static final List<String> STATUSES = List.of("Freshman","Sophomore","Junior","Senior","Graduate");
    private static final List<String> ROLES    = List.of("Front-End","Back-End","Full-Stack","Data","Other");
    private static final List<String> DBS      = List.of("MySQL","PostgreSQL","SQLite","MongoDB","Oracle");
    private static final Path LANG_FILE        = Path.of(System.getProperty("user.home"), ".studentknowledgedb2", "languages.txt");

    // ---------- Unsaved-changes helpers ----------
    private boolean dirty = false;              // not strictly required, kept for listeners
    private void markDirty(){ dirty = true; }

    // Window close guard (attach on show, detach when leaving)
    private EventHandler<WindowEvent> closeGuard;

    // ---------- Lifecycle & wiring ----------
    void initData(StudentProfile profile, ProfileStore store, Consumer<StudentProfile> onSaved) {
        this.original = profile;
        this.working  = copyOf(profile);
        this.store    = store;
        this.onSaved  = onSaved;

        // Choice lists
        cmbStatus.getItems().setAll(STATUSES);
        cmbRole.getItems().setAll(ROLES);

        // Multi-select lists
        lstLanguages.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lstDatabases.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Options
        List<String> langs = new ArrayList<>(loadLangs());
        lstLanguages.getItems().setAll(langs);
        lstDatabases.getItems().setAll(DBS);

        // Populate fields
        txtName.setText(working.getFullName());
        cmbStatus.getSelectionModel().select(working.getAcademicStatus());
        chkEmployed.setSelected(working.isEmployed());
        txtJob.setText(working.getJobDetails());
        cmbRole.getSelectionModel().select(working.getPreferredRole());
        chkWhitelist.setSelected(working.isWhitelist());
        chkBlacklist.setSelected(working.isBlacklist());
        txtFacultyEval.setText(extractFacultyNote(working.getComments()));

        preselect(lstLanguages, working.getLanguages());
        preselect(lstDatabases, working.getDatabases());

        // Mutually exclusive WL/BL
        chkWhitelist.selectedProperty().addListener((o,ov,nv) -> { if (nv) chkBlacklist.setSelected(false); });
        chkBlacklist.selectedProperty().addListener((o,ov,nv) -> { if (nv) chkWhitelist.setSelected(false); });

        // Keep Job field in sync with employment (and clear it when unemployed)
        updateEmploymentUI(chkEmployed.isSelected());
        chkEmployed.selectedProperty().addListener((o, was, isNow) -> updateEmploymentUI(isNow));

        // Dirty listeners (extra belt—actual guard relies on structural comparison)
        txtName.textProperty().addListener((o,ov,nv)->markDirty());
        txtJob.textProperty().addListener((o,ov,nv)->markDirty());
        cmbStatus.valueProperty().addListener((o,ov,nv)->markDirty());
        cmbRole.valueProperty().addListener((o,ov,nv)->markDirty());
        chkEmployed.selectedProperty().addListener((o,ov,nv)->markDirty());
        chkWhitelist.selectedProperty().addListener((o,ov,nv)->markDirty());
        chkBlacklist.selectedProperty().addListener((o,ov,nv)->markDirty());
        txtFacultyEval.textProperty().addListener((o,ov,nv)->markDirty());
        lstLanguages.getSelectionModel().getSelectedItems().addListener((ListChangeListener<String>) c -> markDirty());
        lstDatabases.getSelectionModel().getSelectedItems().addListener((ListChangeListener<String>) c -> markDirty());

        // Attach window-close guard after scene is ready
        Platform.runLater(this::attachCloseGuard);
    }

    // ---------- Actions ----------

    @FXML private void onSave(){
        // Validation
        if (txtName.getText().isBlank()) { warn("Name is required."); return; }
        if (cmbStatus.getValue() == null || cmbStatus.getValue().isBlank()) { warn("Academic Status is required."); return; }
        if (cmbRole.getValue() == null || cmbRole.getValue().isBlank()) { warn("Professional Role is required."); return; }
        if (chkEmployed.isSelected() && txtJob.getText().isBlank()) { warn("Job details are required when employed."); return; }
        if (lstLanguages.getSelectionModel().getSelectedItems().isEmpty()) { warn("Select at least one programming language."); return; }
        if (lstDatabases.getSelectionModel().getSelectedItems().isEmpty()) { warn("Select at least one database."); return; }

        // Write UI -> working copy
        working.setFullName(txtName.getText().trim());
        working.setAcademicStatus(cmbStatus.getValue());
        working.setEmployed(chkEmployed.isSelected());
        working.setJobDetails(chkEmployed.isSelected() ? txtJob.getText().trim() : ""); // clear when unemployed
        working.setPreferredRole(cmbRole.getValue());
        working.setLanguages(new ArrayList<>(lstLanguages.getSelectionModel().getSelectedItems()));
        working.setDatabases(new ArrayList<>(lstDatabases.getSelectionModel().getSelectedItems()));
        working.setWhitelist(chkWhitelist.isSelected());
        working.setBlacklist(chkBlacklist.isSelected());
        upsertFacultyNote(working.getComments(), txtFacultyEval.getText());

        try {
            // Persist
            store.update(original.getFullName(), working);
            copyInto(original, working);
            if (onSaved != null) onSaved.accept(original);

            // leave page
            dirty = false;
            detachCloseGuard();
            goBack();
        } catch (Exception ex) {
            error("Failed to save: " + ex.getMessage());
        }
    }

    @FXML private void onCancel() {
        // Only prompt if form differs from last saved/loaded state
        if (hasFormChanges() && !confirmDiscard()) return;
        detachCloseGuard();
        goBack();
    }

    private void goBack() {
        try {
            detachCloseGuard();
            Stage stage = (Stage) txtName.getScene().getWindow();
            Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/cs151/application/SearchProfiles.fxml")), 1000, 650);
            stage.setScene(scene);
            stage.setTitle("StudentKnowledgeDB | Search Students Profiles");
            stage.centerOnScreen();
        } catch (Exception e) {
            error("Failed to go back: " + e.getMessage());
        }
    }

    // ---------- Helper methods ----------

    /** Enable/disable Job field based on employment; clear when unemployed. */
    private void updateEmploymentUI(boolean employed) {
        txtJob.setDisable(!employed);
        if (!employed) {
            txtJob.clear();  // no markDirty strictly needed; hasFormChanges() will detect this
        }
    }

    private static StudentProfile copyOf(StudentProfile p){
        StudentProfile c = new StudentProfile();
        c.setFullName(p.getFullName());
        c.setAcademicStatus(p.getAcademicStatus());
        c.setEmployed(p.isEmployed());
        c.setJobDetails(p.getJobDetails());
        c.setLanguages(new ArrayList<>(p.getLanguages()));
        c.setDatabases(new ArrayList<>(p.getDatabases()));
        c.setPreferredRole(p.getPreferredRole());
        c.setWhitelist(p.isWhitelist());
        c.setBlacklist(p.isBlacklist());
        c.setComments(new ArrayList<>(p.getComments()));
        return c;
    }

    private static void copyInto(StudentProfile dst, StudentProfile src){
        dst.setFullName(src.getFullName());
        dst.setAcademicStatus(src.getAcademicStatus());
        dst.setEmployed(src.isEmployed());
        dst.setJobDetails(src.getJobDetails());
        dst.setLanguages(new ArrayList<>(src.getLanguages()));
        dst.setDatabases(new ArrayList<>(src.getDatabases()));
        dst.setPreferredRole(src.getPreferredRole());
        dst.setWhitelist(src.isWhitelist());
        dst.setBlacklist(src.isBlacklist());
        dst.setComments(new ArrayList<>(src.getComments()));
    }

    private static void preselect(ListView<String> listView, List<String> values) {
        if (values == null) return;
        for (String v : values) {
            int idx = listView.getItems().indexOf(v);
            if (idx >= 0) listView.getSelectionModel().select(idx);
        }
    }

    private List<String> loadLangs() {
        try {
            if (!Files.exists(LANG_FILE)) return List.of();
            List<String> list = Files.readAllLines(LANG_FILE, StandardCharsets.UTF_8).stream()
                    .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
            list.sort(String::compareToIgnoreCase);
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractFacultyNote(List<String> comments) {
        if (comments == null) return "";
        for (String c : comments) {
            if (c != null && c.startsWith(FACULTY_TAG + "|")) {
                return c.substring((FACULTY_TAG + "|").length());
            }
        }
        return "";
    }

    private void upsertFacultyNote(List<String> comments, String note) {
        if (comments == null) return;
        comments.removeIf(c -> c != null && c.startsWith(FACULTY_TAG + "|"));
        String clean = (note == null) ? "" : note.trim();
        if (!clean.isBlank()) {
            comments.add(FACULTY_TAG + "|" + clean);
        }
    }

    /** Structural comparison of form vs. last saved/loaded state (working). */
    private boolean hasFormChanges() {
        if (!Objects.equals(txtName.getText().trim(), working.getFullName())) return true;
        if (!Objects.equals(cmbStatus.getValue(), working.getAcademicStatus())) return true;
        if (chkEmployed.isSelected() != working.isEmployed()) return true;
        if (!Objects.equals(txtJob.getText().trim(), working.getJobDetails())) return true;
        if (!Objects.equals(cmbRole.getValue(), working.getPreferredRole())) return true;

        Set<String> uiLangs = new HashSet<>(lstLanguages.getSelectionModel().getSelectedItems());
        Set<String> wkLangs = new HashSet<>(working.getLanguages());
        if (!uiLangs.equals(wkLangs)) return true;

        Set<String> uiDbs = new HashSet<>(lstDatabases.getSelectionModel().getSelectedItems());
        Set<String> wkDbs = new HashSet<>(working.getDatabases());
        if (!uiDbs.equals(wkDbs)) return true;

        if (chkWhitelist.isSelected() != working.isWhitelist()) return true;
        if (chkBlacklist.isSelected() != working.isBlacklist()) return true;

        String wkNote = extractFacultyNote(working.getComments());
        String uiNote = (txtFacultyEval.getText() == null) ? "" : txtFacultyEval.getText().trim();
        return !Objects.equals(uiNote, wkNote);
    }

    private boolean confirmDiscard() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Discard unsaved changes?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void attachCloseGuard() {
        if (txtName.getScene() == null || txtName.getScene().getWindow() == null) return;
        Stage stage = (Stage) txtName.getScene().getWindow();
        if (closeGuard != null) return; // already attached
        closeGuard = e -> { if (hasFormChanges() && !confirmDiscard()) e.consume(); };
        stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, closeGuard);
    }

    private void detachCloseGuard() {
        if (txtName.getScene() == null || txtName.getScene().getWindow() == null) { closeGuard = null; return; }
        Stage stage = (Stage) txtName.getScene().getWindow();
        if (closeGuard != null) {
            stage.removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, closeGuard);
            closeGuard = null;
        }
    }

    private static void warn(String m){ new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
}
