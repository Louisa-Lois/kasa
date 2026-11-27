import java.nio.file.Paths;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Lets the learner see where their data lives, save it on demand, and reset
 * their progress.
 *
 * <p>Built only from methods that already exist: FileManager's
 * {@code getFilePath()}, {@code fileExists()}, {@code getLastMessage()} and
 * {@code save()}, and Deck's {@code resetAllProgress()}.
 *
 * <h2>The reset is two steps on purpose</h2>
 * Resetting wipes every box number and review date, so the Reset button stays
 * disabled until the learner ticks a box saying they understand. After a
 * reset the tick is cleared again, so a second reset needs a second
 * confirmation. This is also the quickest way to put the decks in a known
 * state before a demonstration.
 *
 * <h2>Why no "default language" setting</h2>
 * Nothing else in the application reads such a setting yet, so a control for
 * it would do nothing. It is left out rather than shipped as a dead button.
 */
public class SettingsScreen implements Screen {

    private static final String TITLE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold;";
    private static final String SECTION_STYLE = "-fx-font-size: 15px; -fx-font-weight: bold;";
    private static final String MUTED_STYLE = "-fx-text-fill: #555555;";
    private static final String OK_STYLE = "-fx-text-fill: #2E7D32;";
    private static final String ERROR_STYLE = "-fx-text-fill: #B23A3A;";
    private static final String DANGER_STYLE = "-fx-background-color: #B23A3A; -fx-text-fill: white;"
            + " -fx-padding: 6 14 6 14; -fx-background-radius: 6;";

    /** Label for the "every deck" choice in the reset chooser. */
    public static final String ALL_DECKS = "All decks";

    private final DeckLibrary library;
    private final FileManager fileManager;

    /**
     * @param library     the decks being studied
     * @param fileManager the object that saves and loads them
     * @throws IllegalArgumentException if either argument is null
     */
    public SettingsScreen(DeckLibrary library, FileManager fileManager) {
        if (library == null) {
            throw new IllegalArgumentException("The Settings screen needs a deck library.");
        }
        if (fileManager == null) {
            throw new IllegalArgumentException("The Settings screen needs a file manager.");
        }
        this.library = library;
        this.fileManager = fileManager;
    }

    /**
     * Builds the screen. Figures are read fresh on every call, so they are
     * always current.
     *
     * @return the screen's layout
     */
    @Override
    public Parent getView() {
        Label title = new Label("Settings");
        title.setStyle(TITLE_STYLE);

        VBox layout = new VBox(14, title,
                buildDataSection(), new Separator(),
                buildResetSection());
        layout.setPadding(new Insets(24));
        layout.setMaxWidth(640);
        return layout;
    }

    // =====================================================================
    //  YOUR DATA: where it is, and Save now
    // =====================================================================

    private VBox buildDataSection() {
        Label heading = new Label("Your data");
        heading.setStyle(SECTION_STYLE);

        Label location = new Label();
        location.setWrapText(true);
        Label counts = new Label();
        Label lastMessage = new Label();
        lastMessage.setWrapText(true);
        lastMessage.setStyle(MUTED_STYLE);
        refreshDataLabels(location, counts, lastMessage);

        Label result = new Label();
        result.setWrapText(true);

        Button save = new Button("Save now");
        save.setOnAction(event -> {
            boolean saved = fileManager.save(library);
            result.setText(fileManager.getLastMessage());
            result.setStyle(saved ? OK_STYLE : ERROR_STYLE);
            refreshDataLabels(location, counts, lastMessage);
        });

        Label note = new Label("Progress is also saved automatically when you close the window.");
        note.setStyle(MUTED_STYLE);
        note.setWrapText(true);

        return new VBox(8, heading, location, counts, lastMessage, save, result, note);
    }

    private void refreshDataLabels(Label location, Label counts, Label lastMessage) {
        location.setText(describeLocation(fileManager));
        counts.setText(library.size() + (library.size() == 1 ? " deck, " : " decks, ")
                + library.getTotalCardCount() + " cards loaded.");
        String message = fileManager.getLastMessage();
        lastMessage.setText(message == null || message.isEmpty() ? "" : "Last file action: " + message);
    }

    /**
     * Says where the save file is and whether it exists yet.
     *
     * @param fileManager the file manager to describe
     * @return a sentence such as "Saved to /home/ama/kasa/decks.txt"
     * @throws IllegalArgumentException if fileManager is null
     */
    public static String describeLocation(FileManager fileManager) {
        if (fileManager == null) {
            throw new IllegalArgumentException("Cannot describe an empty file manager.");
        }
        String fullPath = Paths.get(fileManager.getFilePath()).toAbsolutePath().toString();
        if (fileManager.fileExists()) {
            return "Saved to " + fullPath;
        }
        return "Will be saved to " + fullPath + " (no file there yet).";
    }

    // =====================================================================
    //  RESET PROGRESS (two steps)
    // =====================================================================

    private VBox buildResetSection() {
        Label heading = new Label("Reset progress");
        heading.setStyle(SECTION_STYLE);

        Label explain = new Label("Puts cards back in box 1 and makes them due today. "
                + "The cards themselves are not deleted.");
        explain.setWrapText(true);
        explain.setStyle(MUTED_STYLE);

        ComboBox<String> scope = new ComboBox<>();
        scope.getItems().add(ALL_DECKS);
        scope.getItems().addAll(library.getDeckNames());
        scope.getSelectionModel().selectFirst();

        CheckBox confirm = new CheckBox("I understand this cannot be undone");
        Button reset = new Button("Reset");
        reset.setStyle(DANGER_STYLE);
        reset.setDisable(true);
        confirm.selectedProperty().addListener(
                (observable, wasTicked, isTicked) -> reset.setDisable(!isTicked));

        Label result = new Label();
        result.setWrapText(true);

        reset.setOnAction(event -> {
            int cards = resetProgress(library, scope.getValue());
            String what = ALL_DECKS.equals(scope.getValue()) ? "every deck" : scope.getValue();
            result.setText("Reset " + cards + (cards == 1 ? " card" : " cards") + " in " + what
                    + ". Press Save now to keep this, or it will be saved when you close.");
            result.setStyle(OK_STYLE);
            confirm.setSelected(false);   // the next reset needs a fresh confirmation
        });

        HBox row = new HBox(10, scope, confirm, reset);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, heading, explain, row, result);
    }

    /**
     * Resets every card in one deck, or in all decks. Static so the
     * destructive action can be tested without a window.
     *
     * @param library  the decks to reset
     * @param deckName a deck name, or {@link #ALL_DECKS}
     * @return how many cards were reset (0 if no deck has that name)
     * @throws IllegalArgumentException if library is null
     */
    public static int resetProgress(DeckLibrary library, String deckName) {
        if (library == null) {
            throw new IllegalArgumentException("Cannot reset an empty deck library.");
        }
        if (deckName == null) {
            return 0;
        }
        if (ALL_DECKS.equals(deckName)) {
            List<Deck> decks = library.getAllDecks();
            int count = 0;
            for (Deck deck : decks) {
                deck.resetAllProgress();
                count += deck.size();
            }
            return count;
        }
        Deck deck = library.getDeck(deckName);
        if (deck == null) {
            return 0;
        }
        deck.resetAllProgress();
        return deck.size();
    }
}
