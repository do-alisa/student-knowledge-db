package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple CSV store for student profiles at ~/.studentknowledgedb/profiles.csv
 * Field order:
 * 0 fullName, 1 academicStatus, 2 employed, 3 jobDetails,
 * 4 languages (pipe-joined), 5 databases (pipe-joined), 6 preferredRole,
 * 7 comments (pipe-joined), 8 whitelist, 9 blacklist
 *
 * IMPORTANT: All list fields are joined with the pipe character '|' and the entire field
 *            is CSV-escaped using {@link #escape(String)} so commas/quotes are safe.
 */
public class ProfileStore {
    private static final String APP_FOLDER = ".studentknowledgedb";
    private static final String FILE_NAME  = "profiles.csv";

    final Path dataDir  = Paths.get(System.getProperty("user.home"), APP_FOLDER);
    final Path dataFile = dataDir.resolve(FILE_NAME);

    public ProfileStore() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data dir: " + dataDir, e);
        }
    }

    /** Append a profile using the unified serializer {@link #toCsvLine(StudentProfile)}. */
    public synchronized void save(StudentProfile p) {
        final String line = toCsvLine(p) + System.lineSeparator();
        try {
            Files.writeString(
                    dataFile,
                    line,
                    StandardCharsets.UTF_8,
                    Files.exists(dataFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save profile", e);
        }
    }

    /** Load all profiles from CSV, tolerant to empty/malformed lines. */
    public List<StudentProfile> loadAll() {
        if (!Files.exists(dataFile)) return List.of();
        try {
            return Files.readAllLines(dataFile, StandardCharsets.UTF_8)
                    .stream()
                    .map(this::parse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("READ profiles FAILED: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Overwrite file with the given list (used for delete/bulk update).
     * Uses {@link #toCsvLine(StudentProfile)} for every record.
     */
    public synchronized void overwriteAll(List<StudentProfile> profiles) {
        try {
            Files.createDirectories(dataDir);
            final StringBuilder sb = new StringBuilder();
            for (StudentProfile p : profiles) {
                sb.append(toCsvLine(p)).append(System.lineSeparator());
            }
            Files.writeString(
                    dataFile,
                    sb.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to overwrite profiles", e);
        }
    }

    /**
     * Delete a target profile by matching its serialized CSV line.
     * This is robust against duplicate names because it matches the entire record.
     */
    public synchronized boolean delete(StudentProfile target) {
        List<StudentProfile> all = new ArrayList<>(loadAll());
        final String targetLine = toCsvLine(target);
        boolean removed = false;
        for (Iterator<StudentProfile> it = all.iterator(); it.hasNext();) {
            if (toCsvLine(it.next()).equals(targetLine)) {
                it.remove();
                removed = true;
                break;
            }
        }
        if (removed) overwriteAll(all);
        return removed;
    }

    // -------------------- UPDATE (consistent with toCsvLine) --------------------

    /**
     * Update a profile by old full name (case-insensitive). If not found, append as new.
     * Uses the same CSV format as save()/overwriteAll() (i.e., toCsvLine).
     */
    public synchronized boolean update(String oldName, StudentProfile newData) {
        List<StudentProfile> all = new ArrayList<>(loadAll());

        // Find index by old name (case-insensitive).
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            String fn = all.get(i).getFullName();
            if (fn != null && oldName != null && fn.equalsIgnoreCase(oldName)) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            all.set(idx, newData);   // Replace existing record
        } else {
            all.add(newData);        // Not found -> append new record
        }

        overwriteAll(all);           // Persist using toCsvLine(...)
        return true;
    }

    /** Overload for cases where the name hasn't changed. */
    public synchronized boolean update(StudentProfile newData) {
        return update(newData.getFullName(), newData);
    }

    // -------------------- Serialization & Parsing helpers --------------------

    /**
     * Serialize a profile into one CSV line. List fields are joined by '|'.
     * The entire field is CSV-escaped so commas/quotes/newlines are safe.
     */
    private String toCsvLine(StudentProfile p) {
        return String.join(",",
                escape(p.getFullName()),
                escape(p.getAcademicStatus()),
                escape(Boolean.toString(p.isEmployed())),
                escape(opt(p.getJobDetails())),
                escape(String.join("|", p.getLanguages())),
                escape(String.join("|", p.getDatabases())),
                escape(opt(p.getPreferredRole())),
                escape(String.join("|", p.getComments())),
                escape(Boolean.toString(p.isWhitelist())),
                escape(Boolean.toString(p.isBlacklist()))
        );
    }

    /**
     * Parse a CSV line into a StudentProfile.
     * Supports the legacy 3-column format [fullName, studentId, languages] for backward compatibility.
     */
    private StudentProfile parse(String line) {
        List<String> cols = parseCsv(line);
        if (cols.isEmpty()) return null;

        // Legacy 3-col format
        if (cols.size() == 3) {
            StudentProfile p = new StudentProfile();
            p.setFullName(cols.get(0));
            p.setAcademicStatus("");
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

    /** Null-safe string. */
    private static String opt(String s) { return (s == null) ? "" : s; }

    /** Split a pipe-joined list, allowing empty as an empty list. */
    private static List<String> splitPipes(String s) {
        return (s == null || s.isBlank()) ? new ArrayList<>() : Arrays.asList(s.split("\\|"));
    }

    /** Safe index getter for parsed columns. */
    private String get(List<String> cols, int i) { return (i < cols.size()) ? cols.get(i) : ""; }

    /**
     * CSV escaping for a single field (RFC4180-style):
     * - Wrap in quotes if it contains a comma, a quote, or a newline.
     * - Quotes inside the value are doubled.
     */
    String escape(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String out = s.replace("\"", "\"\"");
        return needQuote ? "\"" + out + "\"" : out;
    }

    /**
     * Minimal CSV parser for one line:
     * - Supports quoted fields.
     * - Doubled quotes inside a quoted field are un-escaped.
     */
    private List<String> parseCsv(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

        boolean inQ = false;
        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); // escaped quote
                        i++;
                    } else {
                        inQ = false;     // end quote
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"') {
                    inQ = true;
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }
}
