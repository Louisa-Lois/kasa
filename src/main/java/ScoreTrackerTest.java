import java.util.List;

/**
 * Tests for ScoreTracker. A plain class with a main method that prints
 * PASS or FAIL, so it runs with or without Maven:
 *
 *   java -Dfile.encoding=UTF-8 -cp out ScoreTrackerTest
 */
public class ScoreTrackerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static final Card WATER = new Word("water", "nsuo", Language.TWI);
    private static final Card FOOD = new Word("food", "aduane", Language.TWI);
    private static final Card HOUSE = new Word("house", "fie", Language.TWI);
    private static final Card FRIEND = new Word("friend", "adamfo", Language.TWI);

    public static void main(String[] args) {
        newTrackerIsZeroNotNaN();
        threeOfFourIs75Percent();
        allWrongIsZeroPercent();
        wrongCardsAreExactlyTheMissedOnes();
        resetClearsEverything();
        streaksAndWeakestCards();
        nullCardIsRejected();
        equalButDistinctCardsAreCountedSeparately();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void newTrackerIsZeroNotNaN() {
        ScoreTracker t = new ScoreTracker();
        try {
            double accuracy = t.getAccuracy();
            check("new tracker: 0 attempted, 0 correct", t.getAttempted() == 0 && t.getCorrect() == 0);
            check("new tracker: accuracy is 0.0, not NaN", accuracy == 0.0 && !Double.isNaN(accuracy));
            check("new tracker: percent is 0", t.getAccuracyPercent() == 0);
            check("new tracker: summary has no NaN", !t.getSummary().contains("NaN"));
        } catch (ArithmeticException e) {
            check("new tracker: accuracy does not throw", false);
        }
    }

    static void threeOfFourIs75Percent() {
        ScoreTracker t = new ScoreTracker();
        t.record(WATER, true);
        t.record(FOOD, true);
        t.record(HOUSE, false);
        t.record(FRIEND, true);
        check("3 of 4 gives accuracy 0.75", t.getAccuracy() == 0.75);
        check("3 of 4 gives 75%", t.getAccuracyPercent() == 75);
        check("summary reads \"3 of 4 correct, 75%\"", t.getSummary().equals("3 of 4 correct, 75%"));
    }

    static void allWrongIsZeroPercent() {
        ScoreTracker t = new ScoreTracker();
        t.record(WATER, false);
        t.record(FOOD, false);
        check("all wrong gives 0%", t.getAccuracyPercent() == 0 && t.getAccuracy() == 0.0);
    }

    static void wrongCardsAreExactlyTheMissedOnes() {
        ScoreTracker t = new ScoreTracker();
        t.record(WATER, false);
        t.record(FOOD, true);
        t.record(HOUSE, false);
        t.record(WATER, false);   // missed again - still listed once
        List<Card> wrong = t.getWrongCards();
        check("wrong cards: exactly water and house, in order",
                wrong.size() == 2 && wrong.get(0) == WATER && wrong.get(1) == HOUSE);
        check("miss count is kept per card", t.getMissCount(WATER) == 2 && t.getMissCount(FOOD) == 0);
    }

    static void resetClearsEverything() {
        ScoreTracker t = new ScoreTracker();
        t.record(WATER, true);
        t.record(FOOD, false);
        t.reset();
        check("after reset everything is zero",
                t.getAttempted() == 0 && t.getCorrect() == 0 && t.getAccuracy() == 0.0
                        && t.getWrongCards().isEmpty() && t.getBestStreak() == 0);
    }

    static void streaksAndWeakestCards() {
        ScoreTracker t = new ScoreTracker();
        t.record(WATER, true);
        t.record(FOOD, true);
        t.record(HOUSE, true);
        t.record(FRIEND, false);
        t.record(WATER, true);
        check("best streak is 3", t.getBestStreak() == 3);
        check("current streak restarted after a miss", t.getCurrentStreak() == 1);

        t.record(HOUSE, false);
        t.record(HOUSE, false);
        check("weakest card is the most missed", t.getWeakestCards(1).get(0) == HOUSE);
        check("detailed summary names the weak card", t.getDetailedSummary().contains("fie"));
    }

    static void nullCardIsRejected() {
        try {
            new ScoreTracker().record(null, true);
            check("null card is rejected", false);
        } catch (IllegalArgumentException e) {
            check("null card is rejected", true);
        }
    }

    /**
     * Regression test. Two separate cards with the same text are "equal" by
     * Card.equals(), but they are two cards. Missing each once must give two
     * entries with one miss each - not one entry with two misses.
     */
    static void equalButDistinctCardsAreCountedSeparately() {
        Card c1 = new Word("water", "nsuo", Language.TWI);
        Card c2 = new Word("water", "nsuo", Language.TWI);
        ScoreTracker t = new ScoreTracker();
        t.record(c1, false);
        t.record(c2, false);
        List<Card> wrong = t.getWrongCards();
        check("two equal-but-distinct missed cards are both listed",
                wrong.size() == 2 && wrong.get(0) == c1 && wrong.get(1) == c2);
        check("each has its own miss count of 1", t.getMissCount(c1) == 1 && t.getMissCount(c2) == 1);
        check("an equal card never answered has no misses",
                t.getMissCount(new Word("water", "nsuo", Language.TWI)) == 0);
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
