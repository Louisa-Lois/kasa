import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for ProgressScreen's counting. A plain class with a main method that
 * prints PASS or FAIL. It calls the static methods directly, so no window is
 * opened. The JavaFX library must be on the classpath (as it is under Maven),
 * because ProgressScreen itself refers to it.
 *
 *   java -Dfile.encoding=UTF-8 -cp out ProgressScreenTest
 */
public class ProgressScreenTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        countsEveryBox();
        emptyListGivesFiveZeros();
        freshSampleDecksAreAllInBox1();
        countsMatchAfterAQuiz();
        describesEachStage();
        badInputGivesReadableErrors();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void countsEveryBox() {
        List<Card> cards = new ArrayList<>();
        int[] boxes = {1, 1, 1, 2, 3, 3, 5};
        for (int i = 0; i < boxes.length; i++) {
            Card card = new Word("word " + i, "neno " + i, Language.SWAHILI);
            card.setBox(boxes[i]);
            cards.add(card);
        }
        check("counts per box are 3, 1, 2, 0, 1",
                Arrays.equals(ProgressScreen.countCardsPerBox(cards), new int[] {3, 1, 2, 0, 1}));
    }

    static void emptyListGivesFiveZeros() {
        check("no cards gives five zeros",
                Arrays.equals(ProgressScreen.countCardsPerBox(Collections.emptyList()), new int[5]));
    }

    /** "Renders sensibly before any quiz has been taken." */
    static void freshSampleDecksAreAllInBox1() {
        DeckLibrary library = new DeckLibrary();
        library.loadSampleDecks();
        List<Card> all = new ArrayList<>();
        for (Deck deck : library.getAllDecks()) {
            all.addAll(deck.getCards());
        }
        int[] counts = ProgressScreen.countCardsPerBox(all);
        check("fresh decks: every card counted in box 1",
                counts[0] == library.getTotalCardCount() && counts[0] > 0);
        check("fresh decks: message tells the learner to take a quiz",
                ProgressScreen.describeProgress(counts, all.size()).contains("Take a quiz"));
    }

    /** "Clicking away and back shows updated numbers after a quiz." */
    static void countsMatchAfterAQuiz() {
        DeckLibrary library = new DeckLibrary();
        library.loadSampleDecks();
        Deck ewe = library.getAllDecks().get(1);
        int size = ewe.size();

        QuizSession session = new QuizSession(ewe, LocalDate.now());
        while (!session.isFinished()) {
            session.submitAnswer(session.getCurrentQuestion().getCorrectAnswer());
        }
        // The screen counts fresh on every getView(); counting again must see the change.
        int[] counts = ProgressScreen.countCardsPerBox(ewe.getCards());
        check("after a perfect quiz every card is counted in box 2",
                counts[0] == 0 && counts[1] == size);
        check("after a perfect quiz nothing in that deck is due today",
                ewe.getDueCards(LocalDate.now()).isEmpty());
    }

    static void describesEachStage() {
        check("no cards", ProgressScreen.describeProgress(new int[5], 0).equals("No cards yet."));
        String half = ProgressScreen.describeProgress(new int[] {2, 2, 0, 0, 0}, 4);
        check("half moved: says 50%", half.startsWith("50%") && !half.contains("box 5"));
        String one = ProgressScreen.describeProgress(new int[] {1, 0, 0, 0, 1}, 2);
        check("one card in box 5 uses the singular", one.contains("1 card has reached box 5"));
        String many = ProgressScreen.describeProgress(new int[] {0, 0, 0, 0, 3}, 3);
        check("all known: 100% and plural", many.startsWith("100%") && many.contains("3 cards have"));
    }

    static void badInputGivesReadableErrors() {
        try {
            ProgressScreen.countCardsPerBox(null);
            check("null list is rejected", false);
        } catch (IllegalArgumentException e) {
            check("null list is rejected", true);
        }
        try {
            ProgressScreen.describeProgress(new int[3], 3);
            check("wrong number of box counts is rejected", false);
        } catch (IllegalArgumentException e) {
            check("wrong number of box counts is rejected with a readable message",
                    e.getMessage().equals("Expected 5 box counts, but got 3"));
        }
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
