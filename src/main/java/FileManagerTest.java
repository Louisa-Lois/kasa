import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * Tests FileManager on its own. No JUnit needed.
 *
 * How to run (from the kasa folder):
 *     javac -encoding UTF-8 -d out src/*.java
 *     java -Dfile.encoding=UTF-8 -cp out FileManagerTest
 */
public class FileManagerTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("##### 1. Missing file #####");
        testMissingFile();

        System.out.println("\n##### 2. Save and load round trip #####");
        testRoundTrip();

        System.out.println("\n##### 3. Bad lines are skipped #####");
        testBadLines();

        System.out.println("\n##### 4. Empty file #####");
        testEmptyFile();

        System.out.println("\n##### 5. Saving to an impossible path #####");
        testImpossiblePath();

        System.out.println("\n========================================");
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
    }

    private static void testMissingFile() throws IOException {
        String file = "test_missing_file.txt";
        Files.deleteIfExists(Paths.get(file));

        FileManager manager = new FileManager(file);
        DeckLibrary library = manager.load();

        check("missing file gives the 4 sample decks", library.size() == 4);
        check("message says no saved file", manager.getLastMessage().startsWith("No saved file"));
    }

    private static void testRoundTrip() throws IOException {
        String file = "test_round_trip.txt";

        DeckLibrary original = new DeckLibrary();
        original.loadSampleDecks();
        Card water = original.getDeck("Twi Basics").findByEnglish("water");
        LocalDate date = LocalDate.now().plusDays(7);
        water.setBox(4);
        water.setNextReviewDate(date);

        FileManager manager = new FileManager(file);
        check("save works", manager.save(original));

        DeckLibrary loaded = manager.load();
        check("same number of decks", loaded.size() == original.size());
        check("same number of cards", loaded.getTotalCardCount() == original.getTotalCardCount());

        Card loadedWater = loaded.getDeck("Twi Basics").findByEnglish("water");
        check("box kept", loadedWater.getBox() == 4);
        check("review date kept", loadedWater.getNextReviewDate().equals(date));

        Card house = loaded.getDeck("Ewe Basics").findByEnglish("house");
        check("special letters kept (aƒe)", house.getTranslation().equals("aƒe"));

        Card phrase = loaded.getDeck("Twi Basics").findByEnglish("How are you?");
        check("a Phrase is still a Phrase", phrase instanceof Phrase);

        Files.deleteIfExists(Paths.get(file));
    }

    private static void testBadLines() throws IOException {
        String file = "test_bad_lines.txt";
        String text = "DECK|Test Deck|TWI|A test deck\n"
                + "WORD|water|nsuo|noun||1|2026-11-25\n"
                + "WORD|food|aduane|noun||9|2026-11-25\n"      // box 9 is not allowed
                + "WORD|house|fie|noun||2|not-a-date\n"        // bad date
                + "PHRASE|How are you?|Ɛte sɛn?|greeting||1|2026-11-25\n"
                + "END\n";
        Files.write(Paths.get(file), text.getBytes(StandardCharsets.UTF_8));

        FileManager manager = new FileManager(file);
        DeckLibrary library = manager.load();

        check("1 deck loaded", library.size() == 1);
        check("only the 2 good cards loaded", library.getDeck("Test Deck").size() == 2);

        Files.deleteIfExists(Paths.get(file));
    }

    private static void testEmptyFile() throws IOException {
        String file = "test_empty_file.txt";
        Files.write(Paths.get(file), new byte[0]);

        FileManager manager = new FileManager(file);
        DeckLibrary library = manager.load();

        check("empty file gives the 4 sample decks", library.size() == 4);
        check("message says the file had no decks", manager.getLastMessage().contains("no decks"));

        Files.deleteIfExists(Paths.get(file));
    }

    private static void testImpossiblePath() throws IOException {
        // A normal file cannot also be a folder, so saving "inside" it must fail.
        String blocker = "test_blocker.txt";
        Files.write(Paths.get(blocker), "x".getBytes(StandardCharsets.UTF_8));

        FileManager manager = new FileManager(blocker + "/decks.txt");
        boolean saved = manager.save(new DeckLibrary());

        check("save to an impossible path returns false", !saved);
        check("message says could not save", manager.getLastMessage().startsWith("Could not save"));

        Files.deleteIfExists(Paths.get(blocker));
    }

    private static void check(String description, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS - " + description);
        } else {
            failed++;
            System.out.println("  FAIL - " + description);
        }
    }
}