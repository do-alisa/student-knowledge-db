package cs151.application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller for ProfileDetail.fxml
 * Allows editing of a single StudentProfile record.
 * Supports multiple selection for languages and databases,
 * and a free-text Faculty Evaluation stored inside comments.
 */
public class ProfileDetailController {
    @FXML private TextField txtName, txtJob;
    @FXML private ComboBox<String> cmbStatus, cmbRole;
    @FXML private CheckBox chkEmployed, chkWhitelist, chkBlacklist;
    @FXML private ListView<String> lstLanguages, lstDatabases;
    @FXML private TextArea txtFacultyEval; // Faculty evaluation textbox

    private StudentProfile original;
    private StudentProfile working;
    private ProfileStore store;
    private Consumer<StudentProfile> onSaved;

    private static final List<String> STATUSES =
            List.of("Freshman","Sophomore","Junior","Senior","Graduate");
    private static final List<String> ROLES =
            List.of("Front-End","Back-End","Full-Stack","Data","Other");
    private static final List<String> DBS =
            List.of("MySQL","Postgres","MongoDB","SQLite","Oracle","SQL Server","N/A");

    // Path to the list of available programming languages
    private static final Path LANG_FILE =
            Paths.get(System.getProperty("user.home"), ".studentknowledgedb", "languages.txt");

    // Tag used to store/retrieve the faculty evaluation in the comments list
    private static final String FACULTY_TAG = "FACULTY_NOTE";

    /** Initialize the page with data from the selected StudentProfile. */
    public void initData(StudentProfile profile, ProfileStore store, Consumer<StudentProfile> onSaved) {
        this.original = profile;
        this.working  = copyOf(profile);
        this.store    = store;
        this.onSaved  = onSaved;

        // Initialize dropdowns
        cmbStatus.getItems().setAll(STATUSES);
        cmbRole.getItems().setAll(ROLES);

        // Enable multiple selection for Languages and Databases
        lstLanguages.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lstDatabases.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Load language options from languages.txt; use default DB options
        List<String> langs = new ArrayList<>(loadLangs());
        lstLanguages.getItems().setAll(langs);
        lstDatabases.getItems().setAll(DBS);

        // Populate UI fields from the working copy
        txtName.setText(working.getFullName());
        cmbStatus.getSelectionModel().select(working.getAcademicStatus());
        chkEmployed.setSelected(working.isEmployed());
        txtJob.setText(working.getJobDetails());
        cmbRole.getSelectionModel().select(working.getPreferredRole());
        chkWhitelist.setSelected(working.isWhitelist());
        chkBlacklist.setSelected(working.isBlacklist());

        // Preselect existing languages and databases
        preselect(lstLanguages, working.getLanguages());
        preselect(lstDatabases, working.getDatabases());

        // Load existing faculty evaluation (if any) into the text area
        txtFacultyEval.setText(extractFacultyNote(working.getComments()));

        // Enforce mutual exclusion between whitelist and blacklist
        chkWhitelist.selectedProperty().addListener((o,ov,nv)->{ if (nv) chkBlacklist.setSelected(false); });
        chkBlacklist.selectedProperty().addListener((o,ov,nv)->{ if (nv) chkWhitelist.setSelected(false); });

        // Disable and clear job field when not employed
        txtJob.setDisable(!chkEmployed.isSelected());
        chkEmployed.selectedProperty().addListener((o,ov,nv)-> {
            txtJob.setDisable(!nv);
            if (!nv) txtJob.clear();
        });
    }

    /** Triggered when user clicks the Save button. Performs validation and persists data. */
    @FXML
    private void onSave() {
        // Basic validation
        if (txtName.getText().isBlank()) { warn("Name is required."); return; }
        if (cmbStatus.getValue() == null || cmbStatus.getValue().isBlank()) { warn("Academic Status is required."); return; }
        if (cmbRole.getValue() == null || cmbRole.getValue().isBlank()) { warn("Professional Role is required."); return; }
        if (chkEmployed.isSelected() && txtJob.getText().isBlank()) { warn("Job details required when employed."); return; }
        if (lstLanguages.getSelectionModel().getSelectedItems().isEmpty()) { warn("Select at least one programming language."); return; }

        // Write form values back into the working copy
        working.setFullName(txtName.getText().trim());
        working.setAcademicStatus(cmbStatus.getValue());
        working.setEmployed(chkEmployed.isSelected());
        working.setJobDetails(txtJob.getText().trim());
        working.setPreferredRole(cmbRole.getValue());
        working.setLanguages(new ArrayList<>(lstLanguages.getSelectionModel().getSelectedItems()));
        working.setDatabases(new ArrayList<>(lstDatabases.getSelectionModel().getSelectedItems()));
        working.setWhitelist(chkWhitelist.isSelected());
        working.setBlacklist(chkBlacklist.isSelected());

        // Upsert the faculty evaluation entry inside comments (tagged line)
        upsertFacultyNote(working.getComments(), txtFacultyEval.getText());

        // Persist changes
        try {
            store.update(original.getFullName(), working);
            copyInto(original, working);
            if (onSaved != null) onSaved.accept(original);
            goBack(); // return to search page
        } catch (Exception ex) {
            error("Failed to save: " + ex.getMessage());
        }
    }

    /** Triggered when user clicks Cancel or Back button. */
    @FXML private void onCancel() { goBack(); }

    /** Return to the SearchProfiles page. */
    private void goBack() {
        try {
            Stage stage = (Stage) txtName.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(
                    javafx.fxml.FXMLLoader.load(getClass().getResource("/cs151/application/SearchProfiles.fxml")),
                    1000, 650
            ));
            stage.setTitle("StudentKnowledgeDB | Search Students Profiles");
            stage.centerOnScreen();
        } catch (Exception e) {
            error("Failed to go back: " + e.getMessage());
        }
    }

    // ---------- Helper methods ----------

    /** Creates a deep copy of a StudentProfile for editing. */
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

    /** Copies values from one profile to another (used after saving). */
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

    /** Preselects given values inside a ListView. */
    private static void preselect(ListView<String> listView, List<String> values) {
        if (values == null) return;
        for (String v : values) {
            int idx = listView.getItems().indexOf(v);
            if (idx >= 0) listView.getSelectionModel().select(idx);
        }
    }

    /** Loads available programming languages from languages.txt. */
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

    /** Extract the faculty evaluation text stored as "FACULTY_NOTE|<text>" from comments. */
    private String extractFacultyNote(List<String> comments) {
        if (comments == null) return "";
        for (String c : comments) {
            if (c != null && c.startsWith(FACULTY_TAG + "|")) {
                return c.substring((FACULTY_TAG + "|").length());
            }
        }
        return "";
    }

    /** Replace or insert a single "FACULTY_NOTE|<text>" entry in the comments list. */
    private void upsertFacultyNote(List<String> comments, String note) {
        if (comments == null) return;
        // remove any previous faculty note
        comments.removeIf(c -> c != null && c.startsWith(FACULTY_TAG + "|"));
        // add new note only when non-blank
        String clean = (note == null) ? "" : note.trim();
        if (!clean.isBlank()) {
            comments.add(FACULTY_TAG + "|" + clean);
        }
    }

    /** Show a warning alert. */
    private static void warn(String m){ new Alert(Alert.AlertType.WARNING, m).showAndWait(); }

    /** Show an error alert. */
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
}
