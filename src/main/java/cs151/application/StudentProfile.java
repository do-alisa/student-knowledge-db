package cs151.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Data model for a student profile per spec. */
public class StudentProfile {
    // 2.1 Basic Info
    private String fullName;                 // required, used for A→Z sorting by last name
    private String academicStatus;           // [Freshman, Sophomore, Junior, Senior, Graduate], required
    private boolean employed;                // Employed / Not Employed (radio)
    private String jobDetails;               // required iff employed

    // 2.2 Skills and Interests
    private List<String> languages;          // multi-select, from languages.txt, required
    private List<String> databases;          // multi-select, hard-coded list, required
    private String preferredRole;            // [Front-End, Back-End, Full-Stack, Data, Other], required

    // 2.3 Faculty Evaluation
    private List<String> comments;           // multiple entries accumulated over time

    // 2.4 Future Services Flags (mutually exclusive)
    private boolean whitelist;
    private boolean blacklist;

    public StudentProfile() {
        this.languages = new ArrayList<>();
        this.databases = new ArrayList<>();
        this.comments  = new ArrayList<>();
    }

    public StudentProfile(String fullName) { this(); this.fullName = fullName; }

    // --- Getters / Setters ---
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLastName() {
        if (fullName == null || fullName.isBlank()) return "";
        int idx = fullName.trim().lastIndexOf(' ');
        return (idx >= 0) ? fullName.trim().substring(idx + 1) : fullName.trim();
    }

    public String getAcademicStatus() { return academicStatus; }
    public void setAcademicStatus(String academicStatus) { this.academicStatus = academicStatus; }

    public boolean isEmployed() { return employed; }
    public void setEmployed(boolean employed) { this.employed = employed; }

    public String getJobDetails() { return jobDetails; }
    public void setJobDetails(String jobDetails) { this.jobDetails = jobDetails; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) {
        this.languages = (languages == null) ? new ArrayList<>() : new ArrayList<>(languages);
    }

    public List<String> getDatabases() { return databases; }
    public void setDatabases(List<String> databases) {
        this.databases = (databases == null) ? new ArrayList<>() : new ArrayList<>(databases);
    }

    public String getPreferredRole() { return preferredRole; }
    public void setPreferredRole(String preferredRole) { this.preferredRole = preferredRole; }

    public List<String> getComments() { return comments; }
    public void setComments(List<String> comments) {
        this.comments = (comments == null) ? new ArrayList<>() : new ArrayList<>(comments);
    }
    public void addComment(String comment) {
        if (comment != null && !comment.isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            comments.add(ts + " — " + comment.trim());
        }
    }

    public boolean isWhitelist() { return whitelist; }
    public void setWhitelist(boolean whitelist) { this.whitelist = whitelist; }
    public boolean isBlacklist() { return blacklist; }
    public void setBlacklist(boolean blacklist) { this.blacklist = blacklist; }

    // Convenience for table display
    public String getLanguagesAsString() { return String.join(", ", languages); }
    public String getDatabasesAsString() { return String.join(", ", databases); }
    public String getCommentsAsString() { return String.join(" | ", comments); }

    @Override
    public String toString() {
        return "StudentProfile{" +
                "fullName='" + fullName + '\'' +
                ", academicStatus='" + academicStatus + '\'' +
                ", employed=" + employed +
                ", jobDetails='" + jobDetails + '\'' +
                ", languages=" + languages +
                ", databases=" + databases +
                ", preferredRole='" + preferredRole + '\'' +
                ", comments=" + comments +
                ", whitelist=" + whitelist +
                ", blacklist=" + blacklist +
                '}';
    }
}
