package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * v0.6 submission requirement:
 * Only generate 3 languages and 5 student profiles on the very first run.
 * Even if data files are later deleted, they will not be regenerated automatically.
 */
public final class DefaultThreeLangAndFiveStudents {
    private static final String APP_FOLDER = ".studentknowledgedb2";
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), APP_FOLDER);
    private static final Path LANG_TXT  = DATA_DIR.resolve("languages.txt");
    private static final Path PROFILES  = DATA_DIR.resolve("profiles.csv");
    // Sentinel file to mark that initialization has been completed once
    private static final Path FIRST_RUN_MARK = DATA_DIR.resolve(".initialized");

    private DefaultThreeLangAndFiveStudents() {}

    public static void run() throws IOException {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data directory: " + DATA_DIR, e);
        }

        // Only perform initialization if the sentinel file does not exist
        if (!Files.exists(FIRST_RUN_MARK)) {
            seedExactlyOnce();
            createFirstRunMark(); // mark successful first-time initialization
        }
        // If the sentinel already exists: do nothing, even if files were deleted
    }

    private static void seedExactlyOnce() throws IOException {
        resetLanguagesToExactly3();
        resetProfilesToExactly5();
    }

    private static void createFirstRunMark() {
        try {
            Files.write(FIRST_RUN_MARK,
                    List.of("initialized at " + java.time.ZonedDateTime.now()),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignore) {
            // Ignore if created concurrently by another process
        } catch (IOException e) {
            throw new RuntimeException("Failed to create sentinel file: " + FIRST_RUN_MARK, e);
        }
    }

    private static void resetLanguagesToExactly3() {
        try {
            Files.write(LANG_TXT,
                    List.of("Java", "Python", "C++"),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to seed languages", e);
        }
    }

    private static void resetProfilesToExactly5() throws IOException {
        ProfileStore store = new ProfileStore();
        StudentProfile p;

        // Student A
        p = new StudentProfile();
        p.setFullName("Student A");
        p.setAcademicStatus("Freshman");
        p.setEmployed(false);
        p.setJobDetails("");
        p.setLanguages(List.of("Java"));
        p.setDatabases(List.of("SQLite"));
        p.setPreferredRole("Front-End");
        p.setComments(List.of("Enthusiastic about learning front-end development. They are studying at SJSU and trying to find internships.", " Currently learning JavaScript and improving their CSS and HTML skills."));
        p.setWhitelist(true);
        p.setBlacklist(false);
        store.save(p);

        // Student B
        p = new StudentProfile();
        p.setFullName("Student B");
        p.setAcademicStatus("Sophomore");
        p.setEmployed(false);
        p.setJobDetails("");
        p.setLanguages(List.of("Python"));
        p.setDatabases(List.of("SQLite"));
        p.setPreferredRole("Back-End");
        p.setComments(List.of("Enjoys working on backend APIs and logic. Studying at a local community college in hopes they can transfer to a larger university to continue their studies.", "Has learned a lot of Python in their time studying but is currently trying to learn how to improve their Java and C++ skills."));
        p.setWhitelist(false);
        p.setBlacklist(true);
        store.save(p);

        // Student C
        p = new StudentProfile();
        p.setFullName("Student C");
        p.setAcademicStatus("Junior");
        p.setEmployed(true);
        p.setJobDetails("Intern at TechCorp");
        p.setLanguages(List.of("C++", "Java"));
        p.setDatabases(List.of("SQLite"));
        p.setPreferredRole("Full-Stack");
        p.setComments(List.of("Interested in building scalable full-stack systems. Currently interning at TechCorp where they have showed off their impressive skills as a full-stack developer.", "The student has mastered Java and C++ but is currently putting time in to try to learn more Python."));
        p.setWhitelist(true);
        p.setBlacklist(false);
        store.save(p);

        // Student D
        p = new StudentProfile();
        p.setFullName("Student D");
        p.setAcademicStatus("Senior");
        p.setEmployed(true);
        p.setJobDetails("Part-time researcher");
        p.setLanguages(List.of("Python", "C++"));
        p.setDatabases(List.of("SQLite"));
        p.setPreferredRole("Data");
        p.setComments(List.of("Passionate about data analysis and machine learning. Currently working as a part-time researcher and hoping to move into a full time position after graduation.", "They have mastered Python and C++ as well as the SQLite database, but hopes to expand their knowledge to more languages and databases in the future."));
        p.setWhitelist(false);
        p.setBlacklist(true);
        store.save(p);

        // Student E
        p = new StudentProfile();
        p.setFullName("Student E");
        p.setAcademicStatus("Graduate");
        p.setEmployed(false);
        p.setJobDetails("");
        p.setLanguages(List.of("Java", "Python"));
        p.setDatabases(List.of("SQLite"));
        p.setPreferredRole("Other");
        p.setComments(List.of("Exploring software architecture and project design. Graduated from university with a Bachelors Degree last year but is still in search of a job", "Has impressive Java and Python skills but has yet to come across a position at a company that found their skills useful enough to hire them."));
        p.setWhitelist(true);
        p.setBlacklist(false);
        store.save(p);

        // Student F
        p = new StudentProfile();
        p.setFullName("Student F");
        p.setAcademicStatus("Freshman");
        p.setEmployed(false);
        p.setJobDetails("");
        p.setLanguages(List.of("Java"));
        p.setDatabases(List.of("SQLite"));
        p.setPreferredRole("Other");
        p.setComments(List.of("Learning Java as a freshman at San Jose State University. Has not developed many skills beyond basic programming knowledge but is hopeful to learn more in the coming years.", "Currently learning first database in SQLite, though needs to put more time into learning programming fundamentals."));
        p.setWhitelist(false);
        p.setBlacklist(true);
        store.save(p);
    }
}
