package cs151.application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommentsController {

    @FXML private ListView<String> lstComments;
    @FXML private TextArea txtNewComment;
    @FXML private Label lblStudent;

    private StudentProfile original;
    private StudentProfile working;
    private ProfileStore store;
    private Consumer<StudentProfile> onSaved;

    private static final String FACULTY_TAG = "FACULTY_NOTE";
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ISO_DATE; // yyyy-MM-dd

    /** Called by whoever opens this page. */
    void initData(StudentProfile profile, ProfileStore store, Consumer<StudentProfile> onSaved) {
        this.original = profile;
        this.working  = copyOf(profile);
        this.store    = store;
        this.onSaved  = onSaved;

        lblStudent.setText(profile.getFullName());
        refreshList();
    }

    @FXML private void onAddComment() {
        String text = (txtNewComment.getText() == null) ? "" : txtNewComment.getText().trim();
        if (text.isBlank()) {
            warn("Please enter a comment.");
            return;
        }

        // Date-only stamp (required by spec)
        String stamped = LocalDate.now().format(DATE_ONLY) + " — " + text;

        // Append to comments list
        List<String> comments = new ArrayList<>(working.getComments());
        comments.add(stamped);
        working.setComments(comments);

        // Persist and reflect back to source object
        try {
            store.update(original.getFullName(), working);
            copyInto(original, working);
            if (onSaved != null) onSaved.accept(original);

            txtNewComment.clear();
            refreshList();
            info("Comment saved.");
        } catch (Exception ex) {
            error("Failed to save comment: " + ex.getMessage());
        }
    }

    @FXML private void onBack() {
        try {
            Stage stage = (Stage) lstComments.getScene().getWindow();
            Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/cs151/application/SearchProfiles.fxml")), 1000, 650);
            stage.setScene(scene);
            stage.setTitle("StudentKnowledgeDB | Search Students Profiles");
            stage.centerOnScreen();
        } catch (Exception e) {
            error("Failed to go back: " + e.getMessage());
        }
    }

    // ----- Helpers -----

    /** Show all non-faculty-note comments, newest first. */
    private void refreshList() {
        List<String> shown = working.getComments().stream()
                .filter(c -> c != null && !c.startsWith(FACULTY_TAG + "|"))
                .sorted(Comparator.reverseOrder()) // newest first if strings start with yyyy-MM-dd
                .collect(Collectors.toList());
        lstComments.getItems().setAll(shown);
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

    //Comment popup window
    @FXML
    private void initialize() {
        lstComments.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setOnMouseClicked(e -> {
                if (!cell.isEmpty()) {
                    showCommentPopup(cell.getItem());
                }
            });
            return cell;
        });
    }

    private void showCommentPopup(String comment) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Comment");
        alert.setHeaderText(null);

        TextArea area = new TextArea(comment);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(600, 400);

        alert.getDialogPane().setContent(area);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private static void warn(String m){ new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
}
