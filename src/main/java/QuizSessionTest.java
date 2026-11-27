import java.time.LocalDate;
import java.util.Random;

/**
 * Tests for QuizSession. A plain class with a main method that prints
 * PASS or FAIL, so it runs with or without Maven:
 *
 *   java -Dfile.encoding=UTF-8 -cp out QuizSessionTest
 *
 * A whole session is run from start to finish here with no screen involved.
 */
public class QuizSessionTest {

    /** Cards are created due on LocalDate.now(), so "today" must be today. */
    private static final LocalDate TODAY = LocalDate.now();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        fiveDueCardsGiveFiveQuestions();
        nothingDueIsFinishedImmediately();
        correctAnswerScoresAndMovesCardUp();
        wrongAnswerDropsCardToBox1();
        answeringEverythingFinishes();
        oneCardDeckNeverBuildsMatching();
        everyQuestionAcceptsItsOwnAnswer();
        allThreeQuestionTypesAppear();
        secondTryIsNotScoredOrRescheduled();
        answeringAfterFinishGivesClearError();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- the seven from the brief

    static void fiveDueCardsGiveFiveQuestions() {
        Deck deck = deckOf(5);
        QuizSession s = session(deck, 1, false);
        check("5 due cards give 5 questions", s.getTotalQuestions() == 5 && s.getRemainingCount() == 5);
    }

    static void nothingDueIsFinishedImmediately() {
        Deck deck = deckOf(3);
        for (Card card : deck.getCards()) {
            card.setNextReviewDate(TODAY.plusDays(4));
        }
        QuizSession s = session(deck, 1, false);
        check("nothing due: finished at once", s.isFinished() && s.hadNothingDue());
        check("nothing due: no current question", s.getCurrentQuestion() == null);
        check("nothing due: reports next review date", TODAY.plusDays(4).equals(s.getNextReviewDate()));
    }

    static void correctAnswerScoresAndMovesCardUp() {
        QuizSession s = session(deckOf(3), 2, false);
        Question q = s.getCurrentQuestion();
        int boxBefore = q.getCard().getBox();
        boolean result = s.submitAnswer(q.getCorrectAnswer());
        check("correct answer returns true", result);
        check("correct answer increases the score", s.getScoreTracker().getCorrect() == 1);
        check("correct answer moves the card up a box", q.getCard().getBox() == boxBefore + 1);
    }

    static void wrongAnswerDropsCardToBox1() {
        Deck deck = deckOf(3);
        for (Card card : deck.getCards()) {
            card.setBox(3);
        }
        QuizSession s = session(deck, 3, false);
        Question q = s.getCurrentQuestion();
        boolean result = s.submitAnswer("definitely not the answer");
        check("wrong answer returns false", !result);
        check("wrong answer leaves the score at 0 correct",
                s.getScoreTracker().getCorrect() == 0 && s.getScoreTracker().getAttempted() == 1);
        check("wrong answer drops the card to box 1", q.getCard().getBox() == 1);
        check("wrong answer makes the card due tomorrow",
                q.getCard().getNextReviewDate().equals(TODAY.plusDays(1)));
    }

    static void answeringEverythingFinishes() {
        QuizSession s = session(deckOf(4), 4, true);
        int guard = 0;
        while (!s.isFinished() && guard++ < 100) {
            // alternate right and wrong, so second tries are exercised too
            Question q = s.getCurrentQuestion();
            s.submitAnswer(guard % 2 == 0 ? q.getCorrectAnswer() : "wrong");
        }
        check("after answering every question the session is finished", s.isFinished());
        check("every first-time question was answered", s.getAnsweredCount() == 4);
    }

    static void oneCardDeckNeverBuildsMatching() {
        boolean everMatching = false;
        boolean built = true;
        for (int seed = 0; seed < 50; seed++) {
            Deck deck = new Deck("Tiny", Language.TWI);
            deck.addCard(new Word("water", "nsuo", Language.TWI));
            try {
                QuizSession s = session(deck, seed, false);
                everMatching |= s.getCurrentQuestion() instanceof MatchingQuestion;
            } catch (RuntimeException e) {
                built = false;
            }
        }
        check("a one-card deck still builds a session", built);
        check("a one-card deck never gives a Matching question", !everMatching);
    }

    static void everyQuestionAcceptsItsOwnAnswer() {
        boolean allOk = true;
        for (int seed = 0; seed < 30; seed++) {
            QuizSession s = session(sampleTwiDeck(), seed, false);
            while (!s.isFinished()) {
                Question q = s.getCurrentQuestion();
                allOk &= q != null && q.checkAnswer(q.getCorrectAnswer());
                s.submitAnswer(q.getCorrectAnswer());
            }
        }
        check("every question is non-null and accepts its own correct answer", allOk);
    }

    // ---------------------------------------------------------------- extras

    static void allThreeQuestionTypesAppear() {
        boolean translation = false, matching = false, fillBlank = false;
        for (int seed = 0; seed < 20; seed++) {
            QuizSession s = session(sampleTwiDeck(), seed, false);
            while (!s.isFinished()) {
                Question q = s.getCurrentQuestion();
                translation |= q instanceof TranslationQuestion;
                matching |= q instanceof MatchingQuestion;
                fillBlank |= q instanceof FillBlankQuestion;
                if (q instanceof FillBlankQuestion) {
                    fillBlank &= q.getCard() instanceof Phrase;
                }
                s.submitAnswer(q.getCorrectAnswer());
            }
        }
        check("all three question types are built", translation && matching && fillBlank);
    }

    static void secondTryIsNotScoredOrRescheduled() {
        Deck deck = deckOf(1);
        QuizSession s = session(deck, 5, true);
        Card card = s.getCurrentQuestion().getCard();
        s.submitAnswer("wrong");
        check("a missed question comes back once", !s.isFinished() && s.isCurrentRetry());
        s.submitAnswer(s.getCurrentQuestion().getCorrectAnswer());
        check("the second try is not scored", s.getScoreTracker().getAttempted() == 1
                && s.getScoreTracker().getCorrect() == 0);
        check("the second try does not move the card", card.getBox() == 1);
        check("the session ends after the second try", s.isFinished());
    }

    static void answeringAfterFinishGivesClearError() {
        QuizSession s = session(deckOf(1), 6, false);
        s.submitAnswer("anything");
        try {
            s.submitAnswer("again");
            check("answering a finished quiz throws", false);
        } catch (IllegalStateException e) {
            check("answering a finished quiz throws a clear error", e.getMessage().contains("finished"));
        }
        try {
            new QuizSession(null);
            check("null deck is rejected", false);
        } catch (IllegalArgumentException e) {
            check("null deck is rejected", true);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static QuizSession session(Deck deck, long seed, boolean retry) {
        return new QuizSession(deck, TODAY, new ReviewScheduler(), new ScoreTracker(),
                new Random(seed), retry);
    }

    /** A Twi deck of n distinct words, all due today. */
    private static Deck deckOf(int n) {
        String[][] words = {{"water", "nsuo"}, {"food", "aduane"}, {"house", "fie"},
                {"friend", "adamfo"}, {"welcome", "akwaaba"}, {"good morning", "maakye"}};
        Deck deck = new Deck("Test deck " + n, Language.TWI);
        for (int i = 0; i < n; i++) {
            deck.addCard(new Word(words[i][0], words[i][1], Language.TWI));
        }
        return deck;
    }

    /** The real sample Twi deck: Words and Phrases mixed. */
    private static Deck sampleTwiDeck() {
        DeckLibrary library = new DeckLibrary();
        library.loadSampleDecks();
        return library.getDeck("Twi Basics");
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
