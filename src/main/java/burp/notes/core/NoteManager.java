package burp.notes.core;

import burp.api.montoya.logging.Logging;
import burp.notes.model.Note;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NoteManager {
    private final Path notesDirectory;
    private final Logging logging;

    public NoteManager(Logging logging) {
        this.logging = logging;
        // Consider making the notes directory configurable or use a Burp-specific
        // persistent storage API if available
        String userHome = System.getProperty("user.home");
        this.notesDirectory = Paths.get(userHome, ".BurpSuite", "NotesPlusExtension");

        try {
            if (!Files.exists(notesDirectory)) {
                Files.createDirectories(notesDirectory);
                logging.logToOutput("Created notes directory: " + notesDirectory.toString());
            }
        } catch (IOException e) {
            logging.logToError("Failed to create notes directory: " + e.getMessage());
            // Handle error appropriately, maybe disable saving/loading
        }
    }

    public void saveNote(Note note) {
        if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
            logging.logToOutput("Note title cannot be empty.");
            return;
        }
        // Sanitize title to create a valid filename
        String fileName = sanitizeFilename(note.getTitle()) + ".md";
        Path noteFile = notesDirectory.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(noteFile, StandardCharsets.UTF_8)) {
            writer.write(note.getMarkdownContent());
            logging.logToOutput("Note saved: " + noteFile.toString());
        } catch (IOException e) {
            logging.logToError("Error saving note '" + note.getTitle() + "': " + e.getMessage());
        }
    }

    public Note loadNote(String title) {
        String fileName = sanitizeFilename(title) + ".md";
        Path noteFile = notesDirectory.resolve(fileName);

        if (Files.exists(noteFile)) {
            try {
                String content = Files.readString(noteFile, StandardCharsets.UTF_8);
                logging.logToOutput("Note loaded: " + title);
                return new Note(title, content);
            } catch (IOException e) {
                logging.logToError("Error loading note '" + title + "': " + e.getMessage());
            }
        } else {
            logging.logToOutput("Note not found: " + title);
        }
        return null;
    }

    public List<String> getAllNoteTitles() {
        List<String> titles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(notesDirectory)) {
            titles = stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".md"))
                    .map(name -> name.substring(0, name.length() - 3)) // Remove .md extension
                    // Desanitize filename if necessary, for now assume direct mapping
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logging.logToError("Error listing notes: " + e.getMessage());
        }
        return titles;
    }

    public boolean deleteNote(String title) {
        String fileName = sanitizeFilename(title) + ".md";
        Path noteFile = notesDirectory.resolve(fileName);
        try {
            boolean deleted = Files.deleteIfExists(noteFile);
            if (deleted) {
                logging.logToOutput("Note deleted: " + title);
            } else {
                logging.logToOutput("Note not found for deletion or already deleted: " + title);
            }
            return deleted;
        } catch (IOException e) {
            logging.logToError("Error deleting note '" + title + "': " + e.getMessage());
            return false;
        }
    }

    private String sanitizeFilename(String inputName) {
        // Replace common problematic characters, this might need to be more robust
        return inputName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }
}
