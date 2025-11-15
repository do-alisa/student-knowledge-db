package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV store for student profiles at ~/.studentknowledgedb/profiles.csv
 * Columns:
 * 0 fullName, 1 academicStatus, 2 employed, 3 jobDetails,
 * 4 languages (pipe-joined), 5 databases (pipe-joined), 6 preferredRole,
 * 7 comments (pipe-joined), 8 whitelist, 9 blacklist
 *
 * Fixes:
 * - Canonicalize full name on all lookups (trim + collapse spaces + lowercase).
 * - update(...) NEVER appends when not found (throws instead).
 * - save(...) rejects duplicate names.
 * - loadAll() deduplicates by name (keeps last) and rewrites file if duplicates existed.
 * - Atomic writes to avoid partial files.
 */
public class ProfileStore {
    private static final Path ROOT = Paths.get(System.getProperty("user.home"), ".studentknowledgedb");
    private static final Path CSV  = ROOT.resolve("profiles.csv");
    private static final String LIST_SEP = "|";

    // ---- Public API ----
    public synchronized List<StudentProfile> loadAll() {
        ensureRoot();
        if (!Files.exists(CSV)) return List.of();
        try {
            List<String> lines = Files.readAllLines(CSV, StandardCharsets.UTF_8);
            List<StudentProfile> parsed = new ArrayList<>();
            for (String line : lines) {
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;
                parsed.add(decode(line));
            }
            // Deduplicate by canonical full name; keep the LAST occurrence
            Map<String, StudentProfile> byName = new LinkedHashMap<>();
            for (StudentProfile p : parsed) {
                byName.put(canon(p.getFullName()), p);
            }
            List<StudentProfile> deduped = new ArrayList<>(byName.values());
            if (deduped.size() != parsed.size()) {
                saveAll(deduped); // rewrite to clean the file
            }
            return deduped;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Create new — rejects duplicate (normalized) names. */
    public synchronized void save(StudentProfile data) throws IOException {
        ensureRoot();
        List<StudentProfile> all = new ArrayList<>(loadAll());
        String key = canon(data.getFullName());
        for (StudentProfile p : all) {
            if (canon(p.getFullName()).equals(key)) {
                throw new IllegalStateException("A student with this name already exists.");
            }
        }
        if (data.getFullName() != null) data.setFullName(data.getFullName().trim());
        all.add(copyOf(data));
        saveAll(all);
    }

    /** Update by ORIGINAL full name; NEVER appends when not found. */
    public synchronized void update(String originalFullName, StudentProfile newData) throws IOException {
        ensureRoot();
        List<StudentProfile> all = new ArrayList<>(loadAll());
        String key = canon(originalFullName);
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (canon(all.get(i).getFullName()).equals(key)) { idx = i; break; }
        }
        if (idx == -1) {
            throw new IllegalStateException("Update target not found: '" + originalFullName + "'");
        }
        StudentProfile copy = copyOf(newData);
        if (copy.getFullName() != null) copy.setFullName(copy.getFullName().trim());
        all.set(idx, copy);
        saveAll(all);
    }

    public synchronized boolean delete(StudentProfile data) {
        ensureRoot();
        try {
            List<StudentProfile> all = new ArrayList<>(loadAll());
            String key = canon(data.getFullName());
            int idx = -1;
            for (int i = 0; i < all.size(); i++) {
                if (canon(all.get(i).getFullName()).equals(key)) { idx = i; break; }
            }
            if (idx < 0) return false;
            all.remove(idx);
            saveAll(all);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- Internal helpers ----
    private void ensureRoot() {
        try { Files.createDirectories(ROOT); } catch (IOException ignored) {}
    }

    private void saveAll(List<StudentProfile> list) throws IOException {
        ensureRoot();
        List<String> lines = list.stream().map(this::encode).collect(Collectors.toList());
        Path tmp = CSV.resolveSibling("profiles.csv.tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, CSV, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, CSV, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Normalize names for matching (trim + collapse spaces + lowercase). */
    private static String canon(String s) {
        if (s == null) return "";
        String t = s.trim().replaceAll("\\s+", " ");
        return t.toLowerCase(java.util.Locale.ROOT);
    }

    private String encode(StudentProfile s) {
        String name = nz(s.getFullName());
        String status = nz(s.getAcademicStatus());
        String employed = Boolean.toString(s.isEmployed());
        String job = nz(s.getJobDetails());
        String langs = String.join(LIST_SEP, s.getLanguages() == null ? List.of() : s.getLanguages());
        String dbs   = String.join(LIST_SEP, s.getDatabases() == null ? List.of() : s.getDatabases());
        String role  = nz(s.getPreferredRole());
        String comments = String.join(LIST_SEP, s.getComments() == null ? List.of() : s.getComments());
        String wl = Boolean.toString(s.isWhitelist());
        String bl = Boolean.toString(s.isBlacklist());
        return String.join(",", Arrays.asList(name, status, employed, job, langs, dbs, role, comments, wl, bl));
    }

    private StudentProfile decode(String line) {
        String[] a = line.split(",", -1); // keep empties
        if (a.length < 10) {
            String[] b = new String[10];
            System.arraycopy(a, 0, b, 0, a.length);
            for (int i = a.length; i < 10; i++) b[i] = "";
            a = b;
        }
        StudentProfile s = new StudentProfile();
        s.setFullName(nz(a[0]));
        s.setAcademicStatus(nz(a[1]));
        s.setEmployed(Boolean.parseBoolean(nz(a[2])));
        s.setJobDetails(nz(a[3]));
        s.setLanguages(splitList(a[4]));
        s.setDatabases(splitList(a[5]));
        s.setPreferredRole(nz(a[6]));
        s.setComments(splitList(a[7]));
        s.setWhitelist(Boolean.parseBoolean(nz(a[8])));
        s.setBlacklist(Boolean.parseBoolean(nz(a[9])));
        return s;
    }

    private static String nz(String v) { return v == null ? "" : v; }

    private static List<String> splitList(String s) {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(s.split("\\|", -1)));
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
}
