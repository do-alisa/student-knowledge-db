package cs151.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


public class LanguageRepository {
    public static final String DEFAULT_APP_FOLDER = ".studentknowledgedb";
    public static final String DEFAULT_FILE_NAME  = "languages.txt";

    private final Path dataDir;
    private final Path dataFile;

    public LanguageRepository() {
        this(Paths.get(System.getProperty("user.home"), DEFAULT_APP_FOLDER), DEFAULT_FILE_NAME);
    }

    public LanguageRepository(Path dir, String fileName) {
        this.dataDir  = dir;
        this.dataFile = dir.resolve(fileName);
    }

    //Ensure the folder exists.
    public void ensureDataDir() {
        try { Files.createDirectories(dataDir); }
        catch (IOException e) { throw new RuntimeException("Cannot create data dir: " + dataDir, e); }
    }

    // Read all language names from disk (plain strings).
    public List<String> findAllNames() {
        try {
            if (!Files.exists(dataFile)) return List.of();
            return Files.readAllLines(dataFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("READ FAILED: " + e);
            return List.of();
        }
    }

    //Read all as Language objects (handy for TableView<Language>).
    public List<Language> findAll() {
        return findAllNames().stream().map(Language::new).collect(Collectors.toList());
    }

    // Save one language name (case-insensitive de-dup).
    public void saveName(String raw) throws IOException {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Language name is required.");

        Set<String> existingLower = findAllNames().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (existingLower.contains(name.toLowerCase())) return; // already exists

        ensureDataDir();
        Files.writeString(
                dataFile,
                name + System.lineSeparator(),
                StandardCharsets.UTF_8,
                Files.exists(dataFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
        );
    }

    public void save(Language language) throws IOException {
        if (language == null) throw new IllegalArgumentException("Language is null.");
        saveName(language.getName());
    }

    // Seed defaults only if the file is currently empty.
    public void seedDefaultsIfEmpty(Collection<String> defaults) {
        List<String> current = findAllNames();
        if (current.isEmpty() && defaults != null && !defaults.isEmpty()) {
            try {
                for (String s : defaults) saveName(s);
            } catch (IOException e) {
                System.err.println("SEED FAILED: " + e.getMessage());
            }
        }
    }


}
