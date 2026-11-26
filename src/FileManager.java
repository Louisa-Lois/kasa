import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * FileManager saves and loads the whole DeckLibrary to one file.
 *
 * It wraps DeckLibrary.saveToFile() and loadFromFile() (Josephine's code),
 * so the file format lives in ONE place only. FileManager adds:
 *   - one place that knows the file path
 *   - friendly messages instead of crashes
 *   - sample decks on the first run, when no saved file exists yet
 */
public class FileManager {

    public static final String DEFAULT_FILE = "decks.txt";

    private final String filePath;
    private String lastMessage = "";

    public FileManager() {
        this(DEFAULT_FILE);
    }

    public FileManager(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty.");
        }
        this.filePath = filePath.trim();
    }

    public String getFilePath() {
        return filePath;
    }

    /** A short message about the last load or save, ready to show on screen. */
    public String getLastMessage() {
        return lastMessage;
    }

    public boolean fileExists() {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Loads the library from the file.
     * If the file is missing, empty or unreadable, the sample decks are
     * loaded instead, and getLastMessage() says why.
     */
    public DeckLibrary load() {
        DeckLibrary library = new DeckLibrary();

        if (!fileExists()) {
            library.loadSampleDecks();
            lastMessage = "No saved file found, so the sample decks were loaded.";
            return library;
        }

        try {
            int cardCount = library.loadFromFile(filePath);
            if (library.isEmpty()) {
                library.loadSampleDecks();
                lastMessage = "The saved file had no decks, so the sample decks were loaded.";
            } else {
                lastMessage = "Loaded " + library.size() + " decks and "
                        + cardCount + " cards from " + filePath + ".";
            }
        } catch (IOException e) {
            library = new DeckLibrary();
            library.loadSampleDecks();
            lastMessage = "Could not read " + filePath + " (" + e.getMessage()
                    + "). The sample decks were loaded instead.";
        }
        return library;
    }

    /**
     * Saves the library to the file.
     *
     * @return true if it worked, false if it failed (see getLastMessage())
     */
    public boolean save(DeckLibrary library) {
        if (library == null) {
            lastMessage = "Nothing to save.";
            return false;
        }
        try {
            library.saveToFile(filePath);
            lastMessage = "Saved " + library.size() + " decks and "
                    + library.getTotalCardCount() + " cards to " + filePath + ".";
            return true;
        } catch (IOException e) {
            lastMessage = "Could not save to " + filePath + " (" + e.getMessage() + ").";
            return false;
        }
    }
}