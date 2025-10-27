package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Simple CSV store for student profiles at ~/.studentknowledgedb/profiles.csv */
public class ProfileStore {
    private static final String APP_FOLDER = ".studentknowledgedb";
    private static final String FILE_NAME  = "profiles.csv";

    final Path dataDir  = Paths.get(System.getProperty("user.home"), APP_FOLDER);
    final Path dataFile = dataDir.resolve(FILE_NAME);

    public ProfileStore() {
        try { Files.createDirectories(dataDir); }
        catch (IOException e) { throw new RuntimeException("Cannot create data dir: " + dataDir, e); }
    }

    // CSV columns (new schema):
    // 0 fullName, 1 academicStatus, 2 employed, 3 jobDetails,
    // 4 languages (pipe-joined), 5 databases (pipe-joined), 6 preferredRole,
    // 7 comments (pipe-joined), 8 whitelist, 9 blacklist

    /** Append a profile (uses toCsvLine for stable format). */
    public synchronized void save(StudentProfile p) {
        String line = toCsvLine(p) + System.lineSeparator();
        try {
            Files.writeString(
                    dataFile, line, StandardCharsets.UTF_8,
                    Files.exists(dataFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (IOException e) { throw new RuntimeException("Failed to save profile", e); }
    }

    /** Load all profiles. */
    public List<StudentProfile> loadAll() {
        if (!Files.exists(dataFile)) return List.of();
        try {
            return Files.readAllLines(dataFile, StandardCharsets.UTF_8).stream()
                    .map(this::parse).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("READ profiles FAILED: " + e.getMessage());
            return List.of();
        }
    }

    /** Overwrite file with given list (used for delete/bulk update). */
    public synchronized void overwriteAll(List<StudentProfile> profiles) {
        try {
            Files.createDirectories(dataDir);
            StringBuilder sb = new StringBuilder();
            for (StudentProfile p : profiles) sb.append(toCsvLine(p)).append(System.lineSeparator());
            Files.writeString(
                    dataFile, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) { throw new RuntimeException("Failed to overwrite profiles", e); }
    }

    /** Delete target profile (match by its serialized CSV line). */
    public synchronized boolean delete(StudentProfile target) {
        List<StudentProfile> all = new ArrayList<>(loadAll());
        String targetLine = toCsvLine(target);
        boolean removed = false;
        for (Iterator<StudentProfile> it = all.iterator(); it.hasNext();) {
            if (toCsvLine(it.next()).equals(targetLine)) { it.remove(); removed = true; break; }
        }
        if (removed) overwriteAll(all);
        return removed;
    }

    // ---- serialization & parsing helpers ----

    private String toCsvLine(StudentProfile p) {
        return String.join(",",
                escape(p.getFullName()),
                escape(p.getAcademicStatus()),
                escape(Boolean.toString(p.isEmployed())),
                escape(opt(p.getJobDetails())),
                escape(String.join("|", p.getLanguages())),
                escape(String.join("|", p.getDatabases())),
                escape(opt(p.getPreferredRole())),
                escape(Boolean.toString(p.isWhitelist())),
                escape(Boolean.toString(p.isBlacklist()))
        );
    }

    private StudentProfile parse(String line) {
        List<String> cols = parseCsv(line);
        if (cols.isEmpty()) return null;

        // Backward compat: old 3-col format [fullName, studentId, languages]
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
        p.setWhitelist(Boolean.parseBoolean(get(cols, 7)));
        p.setBlacklist(Boolean.parseBoolean(get(cols, 8)));
        return p;
    }

    private static String opt(String s){ return s==null? "" : s; }
    private static List<String> splitPipes(String s){ return (s==null||s.isBlank())? new ArrayList<>() : Arrays.asList(s.split("\\|")); }
    private String get(List<String> cols, int i){ return (i<cols.size())? cols.get(i) : ""; }

    String escape(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String out = s.replace("\"", "\"\"");
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
}
