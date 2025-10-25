package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ProfileStore {
    private static final String APP_FOLDER = ".studentknowledgedb";
    private static final String FILE_NAME  = "profiles.csv";

    private final Path dataDir  = Paths.get(System.getProperty("user.home"), APP_FOLDER);
    private final Path dataFile = dataDir.resolve(FILE_NAME);

    public ProfileStore() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data dir: " + dataDir, e);
        }
    }

    // CSV columns (new schema):
    // 0 fullName
    // 1 academicStatus
    // 2 employed (true/false)
    // 3 jobDetails
    // 4 languages pipe-joined
    // 5 databases pipe-joined
    // 6 preferredRole
    // 7 comments pipe-joined
    // 8 whitelist (true/false)
    // 9 blacklist (true/false)

    public synchronized void save(StudentProfile p) {
        String line = String.join(",",
                escape(p.getFullName()),
                escape(p.getAcademicStatus()),
                escape(Boolean.toString(p.isEmployed())),
                escape(nullToEmpty(p.getJobDetails())),
                escape(String.join("|", p.getLanguages())),
                escape(String.join("|", p.getDatabases())),
                escape(p.getPreferredRole()),
                escape(String.join("|", p.getComments())),
                escape(Boolean.toString(p.isWhitelist())),
                escape(Boolean.toString(p.isBlacklist()))
        ) + System.lineSeparator();

        try {
            ensureDataDir();
            Files.writeString(
                    dataFile, line, StandardCharsets.UTF_8,
                    Files.exists(dataFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save profile", e);
        }
    }

    public List<StudentProfile> loadAll() {
        if (!Files.exists(dataFile)) return List.of();
        try {
            return Files.readAllLines(dataFile, StandardCharsets.UTF_8).stream()
                    .map(this::parse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("READ profiles FAILED: " + e.getMessage());
            return List.of();
        }
    }

    private StudentProfile parse(String line) {
        List<String> cols = parseCsv(line);
        if (cols.isEmpty()) return null;

        // Backward compatibility: if old 3-column file is found, try a best-effort parse
        // OLD: [fullName, studentId (ignored), languages]
        if (cols.size() == 3) {
            StudentProfile p = new StudentProfile();
            p.setFullName(cols.get(0));
            p.setAcademicStatus(""); // unknown
            p.setEmployed(false);
            p.setJobDetails("");
            String rawLangs = cols.get(2);
            p.setLanguages(rawLangs.isBlank() ? List.of() : Arrays.asList(rawLangs.split("\\|")));
            p.setDatabases(List.of());
            p.setPreferredRole("");
            p.setComments(List.of());
            p.setWhitelist(false);
            p.setBlacklist(false);
            return p;
        }

        // New schema
        StudentProfile p = new StudentProfile();
        p.setFullName(get(cols, 0));
        p.setAcademicStatus(get(cols, 1));
        p.setEmployed(Boolean.parseBoolean(get(cols, 2)));
        p.setJobDetails(get(cols, 3));
        p.setLanguages(splitPipes(get(cols, 4)));
        p.setDatabases(splitPipes(get(cols, 5)));
        p.setPreferredRole(get(cols, 6));
        p.setComments(splitPipes(get(cols, 7)));
        p.setWhitelist(Boolean.parseBoolean(get(cols, 8)));
        p.setBlacklist(Boolean.parseBoolean(get(cols, 9)));
        return p;
    }

    // --- helpers ---
    private static String nullToEmpty(String s){ return (s == null) ? "" : s; }

    private static List<String> splitPipes(String s) {
        return (s == null || s.isBlank()) ? new ArrayList<>() : Arrays.asList(s.split("\\|"));
    }

    private String get(List<String> cols, int idx) { return (idx < cols.size()) ? cols.get(idx) : ""; }

    private String escape(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String out = s.replace("\"", "\"\""); // escape quotes
        return needQuote ? "\"" + out + "\"" : out;
    }

    private List<String> parseCsv(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;
        boolean inQ = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQ = false;
                } else cur.append(c);
            } else {
                if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else if (c == '"') { inQ = true; }
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** Ensure app data directory exists. */
    public void ensureDataDir() {
        try {
            if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory: " + dataDir, e);
        }
    }

    /** Delete one profile by full name (case-insensitive). Returns true if something was removed. */
    public synchronized boolean deleteByFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return false;
        if (!Files.exists(dataFile)) return false;
        try {
            List<String> lines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
            List<String> kept  = new ArrayList<>();
            boolean removed = false;
            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                List<String> cols = parseCsv(line);
                String name = cols.isEmpty() ? "" : cols.get(0);
                if (name != null && name.trim().equalsIgnoreCase(fullName.trim())) {
                    removed = true;
                } else {
                    kept.add(line);
                }
            }
            if (removed) {
                ensureDataDir();
                Files.write(dataFile, kept, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return removed;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete profile", e);
        }
    }

    /** Seed exactly 5 demo profiles if store is empty (idempotent). */
    public void seedDefaultsIfEmpty() {
        List<StudentProfile> current = loadAll();
        if (current != null && !current.isEmpty()) return;

        // Build 5 deterministic demo profiles
        String[] statuses = {"Freshman", "Sophomore", "Junior", "Senior", "Graduate"};
        String[][] langs = {
                {"Java", "Python"},
                {"Java"},
                {"Python"},
                {"Java", "C++"},
                {"C++"}
        };
        String[][] dbs = {
                {"MySQL"},
                {"PostgreSQL"},
                {"MongoDB"},
                {"SQLite"},
                {"Oracle"}
        };
        String[] roles = {"Front-End", "Back-End", "Full-Stack", "Data", "Other"};
        String[] names = {
                "Alice Johnson", "Brian Lee", "Carla Nguyen", "David Rodríguez", "Esha Patel"
        };
        boolean[] employed = {true, false, false, true, false};
        String[] jobs = {"TA, SJSU", "", "", "Intern, Acme Corp", ""};

        for (int i = 0; i < 5; i++) {
            StudentProfile p = new StudentProfile();
            p.setFullName(names[i]);
            p.setAcademicStatus(statuses[i]);
            p.setEmployed(employed[i]);
            p.setJobDetails(jobs[i]);
            p.setLanguages(java.util.Arrays.asList(langs[i]));
            p.setDatabases(java.util.Arrays.asList(dbs[i]));
            p.setPreferredRole(roles[i]);
            if (i % 2 == 0) p.setWhitelist(true);
            p.addComment("Demo profile " + (i + 1));
            save(p);
        }
    }

}
