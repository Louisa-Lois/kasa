import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Tests for SettingsScreen's file description and its destructive reset.
 * A plain class with a main method that prints PASS or FAIL. No window is
 * opened; the JavaFX library must be on the classpath (as it is under Maven).
 *
 *   java -Dfile.encoding=UTF-8 -cp out SettingsScreenTest
 *
 * The save-file tests use a temporary folder, never the real decks.txt.
 */
public class SettingsScreenTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        describesMissingFile();
        describesSavedFileAfterSave();
        resetOneDeckLeavesOthersAlone();
        resetAllDecksResetsEveryCard();
        resetUnknownDeckChangesNothing();
        badInputGivesReadableErrors();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void describesMissingFile() throws IOException {
        Path folder = Files.createTempDirectory("kasa-settings-test");
        FileManager manager = new FileManager(folder.resolve("decks.txt").toString());
        String text = SettingsScreen.describeLocation(manager);
        check("missing file: says it will be saved there",
                text.startsWith("Will be saved to ") && text.contains("no file there yet"));
        check("missing file: shows the full path", text.contains(folder.toAbsolutePath().toString()));
    }

    static void describesSavedFileAfterSave() throws IOException {
        Path folder = Files.createTempDirectory("kasa-settings-test");
        Path file = folder.resolve("decks.txt");
        FileManager manager = new FileManager(file.toString());
        DeckLibrary library = new DeckLibrary();
        library.loadSampleDecks();

        boolean saved = manager.save(library);   // what "Save now" does
        check("save now writes the file", saved && Files.exists(file));
        check("after saving: says \"Saved to\" the full path",
                SettingsScreen.describeLocation(manager).equals("Saved to " + file.toAbsolutePath()));
        Files.deleteIfExists(file);
        Files.deleteIfExists(folder);
    }

    static void resetOneDeckLeavesOthersAlone() {
        DeckLibrary library = progressedLibrary();
        Deck first = library.getAllDecks().get(0);
        Deck second = library.getAllDecks().get(1);

        int count = SettingsScreen.resetProgress(library, first.getName());
        check("reset one deck: returns that deck's card count", count == first.size());
        check("reset one deck: its cards are back in box 1 and due today", allFresh(first));
        check("reset one deck: other decks are untouched", noneFresh(second));
    }

    static void resetAllDecksResetsEveryCard() {
        DeckLibrary library = progressedLibrary();
        int count = SettingsScreen.resetProgress(library, SettingsScreen.ALL_DECKS);
        boolean allReset = true;
        for (Deck deck : library.getAllDecks()) {
            allReset &= allFresh(deck);
        }
        check("reset all: every card in every deck is reset", allReset);
        check("reset all: returns the total card count", count == library.getTotalCardCount());
    }

    static void resetUnknownDeckChangesNothing() {
        DeckLibrary library = progressedLibrary();
        check("unknown deck name: 0 cards reset", SettingsScreen.resetProgress(library, "No such deck") == 0);
        check("null deck name: 0 cards reset", SettingsScreen.resetProgress(library, null) == 0);
        boolean untouched = true;
        for (Deck deck : library.getAllDecks()) {
            untouched &= noneFresh(deck);
        }
        check("unknown deck name: nothing was changed", untouched);
    }

    static void badInputGivesReadableErrors() {
        try {
            SettingsScreen.resetProgress(null, SettingsScreen.ALL_DECKS);
            check("null library is rejected", false);
        } catch (IllegalArgumentException e) {
            check("null library is rejected", true);
        }
        try {
            SettingsScreen.describeLocation(null);
            check("null file manager is rejected", false);
        } catch (IllegalArgumentException e) {
            check("null file manager is rejected", true);
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Sample decks with every card moved to box 3, due in four days. */
    private static DeckLibrary progressedLibrary() {
        DeckLibrary library = new DeckLibrary();
        library.loadSampleDecks();
        for (Deck deck : library.getAllDecks()) {
            for (Card card : deck.getCards()) {
                card.setBox(3);
                card.setNextReviewDate(LocalDate.now().plusDays(4));
            }
        }
        return library;
    }

    private static boolean allFresh(Deck deck) {
        for (Card card : deck.getCards()) {
            if (card.getBox() != 1 || !card.isDue(LocalDate.now())) {
                return false;
            }
        }
        return true;
    }

    private static boolean noneFresh(Deck deck) {
        for (Card card : deck.getCards()) {
            if (card.getBox() == 1) {
                return false;
            }
        }
        return true;
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
