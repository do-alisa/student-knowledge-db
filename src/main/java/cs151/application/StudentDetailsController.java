package cs151.application;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Shows read-only profile details + comments table with excerpt. */
public class StudentDetailsController {

    // Header
    @FXML private Label titleLabel;

    // Form fields (read-only)
    @FXML private TextField fullName;
    @FXML private TextField status;
    @FXML private TextField employed;
    @FXML private TextField job;
    @FXML private TextArea  langs;
    @FXML private TextArea  dbs;
    @FXML private TextField role;
    @FXML private TextField flag;

    // Comments
    @FXML private TableView<CommentRow> commentsTable;
    @FXML private TableColumn<CommentRow, String> dateCol;
    @FXML private TableColumn<CommentRow, String> excerptCol;

    private StudentProfile profile;

    /** Utility model for the comments table. */
    public static class CommentRow {
        private final String date;
        private final String excerpt;
        private final String full;

        public CommentRow(String date, String excerpt, String full) {
            this.date = date;
            this.excerpt = excerpt;
            this.full = full;
        }
        public String getDate()    { return date; }
        public String getExcerpt() { return excerpt; }
        public String getFull()    { return full; }
    }

    @FXML
    private void initialize() {
        // setup columns
        dateCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDate()));
        excerptCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExcerpt()));
        commentsTable.setRowFactory(tv -> {
            TableRow<CommentRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    CommentRow cr = row.getItem();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Comment");
                    alert.setHeaderText(null);
                    TextArea area = new TextArea(cr.getFull());
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.setPrefSize(600, 400);
                    alert.getDialogPane().setContent(area);
                    alert.setResizable(true);
                    alert.showAndWait();
                }
            });
            return row;
        });
    }

    /** Call after FXML load to inject the profile. */
    public void setProfile(StudentProfile p) {
        this.profile = p;
        populate();
    }

    private void populate() {
        if (profile == null) return;
        titleLabel.setText("Student Details — " + nz(profile.getFullName()));
        fullName.setText(nz(profile.getFullName()));
        status.setText(nz(profile.getAcademicStatus()));
        employed.setText(profile.isEmployed() ? "Yes" : "No");
        job.setText(nz(profile.getJobDetails()));
        langs.setText(profile.getLanguagesAsString());
        dbs.setText(profile.getDatabasesAsString());
        role.setText(nz(profile.getPreferredRole()));
        flag.setText(profile.isWhitelist() ? "Whitelist" : (profile.isBlacklist() ? "Blacklist" : ""));

        // Build comment rows with date and an excerpt
        List<CommentRow> rows = new ArrayList<>();
        for (String raw : profile.getComments()) {
            // Expected format: "yyyy-MM-dd HH:mm — comment"
            String date = "";
            String text = nz(raw);
            int sep = text.indexOf(" — ");
            if (sep >= 0) {
                date = text.substring(0, sep).trim();
                text = text.substring(sep + 3).trim();
            }
            rows.add(new CommentRow(date, makeExcerpt(text, 80), text));
        }
        commentsTable.setItems(FXCollections.observableArrayList(rows));
    }

    private static String makeExcerpt(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return (t.length() <= max) ? t : t.substring(0, max - 1) + "…";
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @FXML
    private void onClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }

    /** Convenience: open modal dialog from anywhere. */
    public static void showDialog(Stage owner, StudentProfile p) {
        try {
            FXMLLoader l = new FXMLLoader(StudentDetailsController.class.getResource("/cs151/application/StudentDetailsView.fxml"));
            Scene scene = new Scene(l.load(), 900, 600);
            StudentDetailsController ctrl = l.getController();
            ctrl.setProfile(p);

            Stage dialog = new Stage();
            dialog.setTitle("Student Details");
            dialog.initModality(Modality.WINDOW_MODAL);
            if (owner != null) dialog.initOwner(owner);
            dialog.setScene(scene);
            dialog.setMinWidth(720);
            dialog.setMinHeight(480);
            dialog.centerOnScreen();
            dialog.show();
        } catch (IOException ex) {
            // If something goes wrong, show a simple alert
            Alert a = new Alert(Alert.AlertType.ERROR, "Unable to open details: " + ex.getMessage());
            a.setHeaderText(null);
            a.setTitle("Error");
            a.showAndWait();
        }
    }
}

