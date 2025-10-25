package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** v0.6 提交要求：将数据重置为仅 3 种语言 + 仅 5 个学生 */
public final class DefaultThreeLangAndFiveStudents {
    private static final String APP_FOLDER = ".studentknowledgedb";
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), APP_FOLDER);
    private static final Path LANG_TXT  = DATA_DIR.resolve("languages.txt");
    private static final Path PROFILES  = DATA_DIR.resolve("profiles.csv");

    private DefaultThreeLangAndFiveStudents() {}

    public static void run() {
        try { Files.createDirectories(DATA_DIR); }
        catch (IOException e) { throw new RuntimeException("Cannot create data dir: " + DATA_DIR, e); }

        resetLanguagesToExactly3();
        resetProfilesToExactly5();
    }

    private static void resetLanguagesToExactly3() {
        try {
            Files.write(LANG_TXT,
                    List.of("Java", "Python", "C++"),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Seed languages failed", e);
        }
    }

    private static void resetProfilesToExactly5() {
        try {
            Files.deleteIfExists(PROFILES);
            ProfileStore store = new ProfileStore();

            StudentProfile p;

            // A - Freshman
            p = new StudentProfile();
            p.setFullName("Student A");
            p.setAcademicStatus("Freshman");
            p.setEmployed(false);
            p.setJobDetails("");
            p.setLanguages(List.of("Java"));
            p.setDatabases(List.of("SQLite"));
            p.setPreferredRole("Front-End");
            p.setComments(List.of("Comment 1"));
            p.setWhitelist(true); p.setBlacklist(false);
            store.save(p);

            // B - Sophomore
            p = new StudentProfile();
            p.setFullName("Student B");
            p.setAcademicStatus("Sophomore");
            p.setEmployed(false);
            p.setJobDetails("");
            p.setLanguages(List.of("Python"));
            p.setDatabases(List.of("SQLite"));
            p.setPreferredRole("Back-End");
            p.setComments(List.of("Comment 2"));
            p.setWhitelist(false); p.setBlacklist(false);
            store.save(p);

            // C - Junior
            p = new StudentProfile();
            p.setFullName("Student C");
            p.setAcademicStatus("Junior");
            p.setEmployed(true);
            p.setJobDetails("Intern at TechCorp");
            p.setLanguages(List.of("C++", "Java"));
            p.setDatabases(List.of("SQLite"));
            p.setPreferredRole("Full-Stack");
            p.setComments(List.of("Comment 3"));
            p.setWhitelist(true); p.setBlacklist(false);
            store.save(p);

            // D - Senior
            p = new StudentProfile();
            p.setFullName("Student D");
            p.setAcademicStatus("Senior");
            p.setEmployed(true);
            p.setJobDetails("Part-time researcher");
            p.setLanguages(List.of("Python", "C++"));
            p.setDatabases(List.of("SQLite"));
            p.setPreferredRole("Data");
            p.setComments(List.of("Comment 4"));
            p.setWhitelist(false); p.setBlacklist(false);
            store.save(p);

            // E - Graduate
            p = new StudentProfile();
            p.setFullName("Student E");
            p.setAcademicStatus("Graduate");
            p.setEmployed(false);
            p.setJobDetails("");
            p.setLanguages(List.of("Java", "Python"));
            p.setDatabases(List.of("SQLite"));
            p.setPreferredRole("Other");
            p.setComments(List.of("Comment 5"));
            p.setWhitelist(true); p.setBlacklist(false);
            store.save(p);

        } catch (IOException e) {
            throw new RuntimeException("Seed profiles failed", e);
        }
    }
}
