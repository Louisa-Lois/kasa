import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;

/**
 * Tests for QuizScreen's decisions: which view to show, which input a
 * question gets, and the "nothing due" message. A plain class with a main
 * method that prints PASS or FAIL. No window is opened; the JavaFX library
 * must be on the classpath (as it is under Maven).
 *
 *   java -Dfile.encoding=UTF-8 -cp out QuizScreenTest
 */
public class QuizScreenTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        noSessionShowsStart();
        runningSessionShowsQuestionThenFeedback();
        navigatingAwayAndBackResumes();
        finishedSessionShowsSummary();
        nothingDueShowsMessageNotCrash();
        matchingGetsButtonsOthersGetTextBox();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void noSessionShowsStart() {
        check("no session: start view", QuizScreen.VIEW_START.equals(QuizScreen.viewFor(null, false)));
        check("no session, stale feedback flag: still start view",
                QuizScreen.VIEW_START.equals(QuizScreen.viewFor(null, true)));
    }

    static void runningSessionShowsQuestionThenFeedback() {
        QuizSession s = session(twoCardDeck());
        check("running session: question view", QuizScreen.VIEW_QUESTION.equals(QuizScreen.viewFor(s, false)));
        s.submitAnswer(s.getCurrentQuestion().getCorrectAnswer());
        check("just answered: feedback view", QuizScreen.VIEW_FEEDBACK.equals(QuizScreen.viewFor(s, true)));
        check("feedback flag before any answer is ignored",
                QuizScreen.VIEW_QUESTION.equals(QuizScreen.viewFor(session(twoCardDeck()), true)));
    }

    /**
     * The screen keeps its session in a field, and the view depends only on
     * that session's state. So deciding the view again (what happens when the
     * learner clicks away and back) shows the same question, not a restart.
     */
    static void navigatingAwayAndBackResumes() {
        QuizSession s = session(twoCardDeck());
        s.submitAnswer(s.getCurrentQuestion().getCorrectAnswer());
        Question second = s.getCurrentQuestion();
        String before = QuizScreen.viewFor(s, false);
        String after = QuizScreen.viewFor(s, false);   // "clicked Progress, then Quiz"
        check("coming back shows the same view", before.equals(after)
                && QuizScreen.VIEW_QUESTION.equals(after));
        check("coming back shows the same question, not question 1",
                s.getCurrentQuestion() == second && s.getAnsweredCount() == 1);
    }

    static void finishedSessionShowsSummary() {
        QuizSession s = session(twoCardDeck());
        while (!s.isFinished()) {
            s.submitAnswer(s.getCurrentQuestion().getCorrectAnswer());
        }
        check("last answer still shows feedback first",
                QuizScreen.VIEW_FEEDBACK.equals(QuizScreen.viewFor(s, true)));
        check("then the summary", QuizScreen.VIEW_SUMMARY.equals(QuizScreen.viewFor(s, false)));

        QuizSession ended = session(twoCardDeck());
        ended.endEarly();
        check("ending early shows the summary", QuizScreen.VIEW_SUMMARY.equals(QuizScreen.viewFor(ended, false)));
    }

    static void nothingDueShowsMessageNotCrash() {
        Deck deck = twoCardDeck();
        for (Card card : deck.getCards()) {
            card.setNextReviewDate(TODAY.plusDays(2));
        }
        QuizSession s = session(deck);
        check("nothing due: nothing-due view", QuizScreen.VIEW_NOTHING_DUE.equals(QuizScreen.viewFor(s, false)));
        String message = QuizScreen.describeNothingDue(s);
        check("nothing due: message names the deck and the next date",
                message.contains("Test deck") && message.contains(String.valueOf(TODAY.plusDays(2).getDayOfMonth())));

        QuizSession empty = session(new Deck("Empty deck", Language.GA));
        check("empty deck: nothing-due view", QuizScreen.VIEW_NOTHING_DUE.equals(QuizScreen.viewFor(empty, false)));
        check("empty deck: says it has no cards",
                QuizScreen.describeNothingDue(empty).equals("Empty deck has no cards yet."));
    }

    static void matchingGetsButtonsOthersGetTextBox() {
        Word water = new Word("water", "nsuo", Language.TWI);
        Phrase howAreYou = new Phrase("How are you?", "Ɛte sɛn?", Language.TWI);
        check("Matching question: option buttons",
                QuizScreen.usesOptionButtons(new MatchingQuestion(water, Arrays.asList("fire", "food"))));
        check("Translation question: text box",
                !QuizScreen.usesOptionButtons(new TranslationQuestion(water)));
        check("Fill-in-the-blank question: text box",
                !QuizScreen.usesOptionButtons(new FillBlankQuestion(howAreYou, 1)));
    }

    // ---------------------------------------------------------------- helpers

    private static QuizSession session(Deck deck) {
        return new QuizSession(deck, TODAY, new ReviewScheduler(), new ScoreTracker(), new Random(7), false);
    }

    private static Deck twoCardDeck() {
        Deck deck = new Deck("Test deck", Language.TWI);
        deck.addCard(new Word("water", "nsuo", Language.TWI));
        deck.addCard(new Word("food", "aduane", Language.TWI));
        return deck;
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
