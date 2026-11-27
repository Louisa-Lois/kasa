import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for ReviewScheduler. A plain class with a main method that prints
 * PASS or FAIL, so it runs with or without Maven:
 *
 *   javac -encoding UTF-8 -d out src/*.java
 *   java -Dfile.encoding=UTF-8 -cp out ReviewSchedulerTest
 *
 * Every test uses a fixed date, so an 8-day interval is checked without
 * waiting 8 days.
 */
public class ReviewSchedulerTest {

    private static final LocalDate DAY_0 = LocalDate.of(2026, 12, 1);
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        newCardIsInBox1AndDueToday();
        correctOnBox1MovesToBox2InTwoDays();
        threeCorrectInARowFollowsDoublingIntervals();
        correctOnBox5StaysAtBox5();
        wrongOnBox4DropsToBox1DueTomorrow();
        wrongOnBox1StaysAtBox1();
        cardEightDaysOutIsDueOnDayEightNotToday();
        intervalsMatchTheTable();
        badInputGivesReadableErrors();
        queueReturnsSoonestFirstAndStaysCorrectAfterAnswers();
        equalButDistinctCardsAreKeptApart();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- the seven from the brief

    static void newCardIsInBox1AndDueToday() {
        Card card = new Word("water", "nsuo", Language.TWI);
        check("new card is in box 1", card.getBox() == 1);
        check("new card is due today", card.isDue(LocalDate.now()));
    }

    static void correctOnBox1MovesToBox2InTwoDays() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card card = new Word("water", "nsuo", Language.TWI);
        int box = scheduler.recordAnswer(card, true, DAY_0);
        check("correct on box 1 -> box 2", box == 2 && card.getBox() == 2);
        check("box 2 is due in 2 days", card.getNextReviewDate().equals(DAY_0.plusDays(2)));
    }

    static void threeCorrectInARowFollowsDoublingIntervals() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card card = new Word("food", "aduane", Language.TWI);
        int[] expectedBoxes = {2, 3, 4};
        int[] expectedDays = {2, 4, 8};
        LocalDate day = DAY_0;
        boolean allRight = true;
        for (int i = 0; i < 3; i++) {
            scheduler.recordAnswer(card, true, day);
            allRight &= card.getBox() == expectedBoxes[i];
            allRight &= card.getNextReviewDate().equals(day.plusDays(expectedDays[i]));
            day = card.getNextReviewDate();   // answer it again on the day it is due
        }
        check("three correct: box 1 -> 2 -> 3 -> 4, intervals 2, 4, 8", allRight);
    }

    static void correctOnBox5StaysAtBox5() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card card = new Word("house", "fie", Language.TWI);
        card.setBox(5);
        try {
            scheduler.recordAnswer(card, true, DAY_0);
            check("correct on box 5 stays in box 5", card.getBox() == 5);
            check("box 5 is due in 16 days", card.getNextReviewDate().equals(DAY_0.plusDays(16)));
        } catch (RuntimeException e) {
            check("correct on box 5 does not throw (threw " + e + ")", false);
        }
    }

    static void wrongOnBox4DropsToBox1DueTomorrow() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card card = new Word("friend", "adamfo", Language.TWI);
        card.setBox(4);
        scheduler.recordAnswer(card, false, DAY_0);
        check("wrong on box 4 drops to box 1", card.getBox() == 1);
        check("box 1 is due tomorrow", card.getNextReviewDate().equals(DAY_0.plusDays(1)));
    }

    static void wrongOnBox1StaysAtBox1() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card card = new Word("welcome", "akwaaba", Language.TWI);
        try {
            scheduler.recordAnswer(card, false, DAY_0);
            check("wrong on box 1 stays in box 1, never 0", card.getBox() == 1);
        } catch (RuntimeException e) {
            check("wrong on box 1 does not throw (threw " + e + ")", false);
        }
    }

    static void cardEightDaysOutIsDueOnDayEightNotToday() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card card = new Word("thank you", "medaase", Language.TWI);
        card.setBox(3);
        scheduler.recordAnswer(card, true, DAY_0);   // -> box 4, 8 days
        check("8-day card is not due today", !card.isDue(DAY_0));
        check("8-day card is not due on day 7", !card.isDue(DAY_0.plusDays(7)));
        check("8-day card is due on day 8", card.isDue(DAY_0.plusDays(8)));
    }

    // ---------------------------------------------------------------- extras

    static void intervalsMatchTheTable() {
        ReviewScheduler scheduler = new ReviewScheduler();
        int[] expected = {1, 2, 4, 8, 16};
        boolean ok = true;
        for (int box = 1; box <= 5; box++) {
            ok &= scheduler.getIntervalDays(box) == expected[box - 1];
        }
        check("intervals are 1, 2, 4, 8, 16", ok);
    }

    static void badInputGivesReadableErrors() {
        ReviewScheduler scheduler = new ReviewScheduler();
        checkThrows("box 0 is rejected", () -> scheduler.getIntervalDays(0));
        checkThrows("box 6 is rejected", () -> scheduler.getIntervalDays(6));
        checkThrows("null card is rejected", () -> scheduler.recordAnswer(null, true, DAY_0));
        checkThrows("null date is rejected",
                () -> scheduler.recordAnswer(new Word("a", "b", Language.GA), true, null));
        try {
            scheduler.getIntervalDays(9);
        } catch (IllegalArgumentException e) {
            check("error message is readable", e.getMessage().equals("Box must be between 1 and 5, but was 9"));
        }
    }

    static void queueReturnsSoonestFirstAndStaysCorrectAfterAnswers() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card a = new Word("water", "nsuo", Language.TWI);
        Card b = new Word("food", "aduane", Language.TWI);
        Card c = new Word("house", "fie", Language.TWI);
        a.setNextReviewDate(DAY_0.plusDays(3));
        b.setNextReviewDate(DAY_0.plusDays(1));
        c.setNextReviewDate(DAY_0.plusDays(2));
        scheduler.trackAll(Arrays.asList(a, b, c));

        check("tracking the same card twice is ignored", !scheduler.track(a) && scheduler.getTrackedCount() == 3);
        check("peekSoonest gives the earliest card", scheduler.peekSoonest() == b);

        // b is answered right on day 1 -> box 2, due day 3. Now c (day 2) is soonest.
        scheduler.recordAnswer(b, true, DAY_0.plusDays(1));
        List<Card> order = scheduler.getSoonestCards(3);
        check("queue reorders after an answer", order.get(0) == c);
        check("getSoonestCards does not empty the queue", scheduler.getTrackedCount() == 3);
        check("getDueCards stops at the first card not due",
                scheduler.getDueCards(DAY_0.plusDays(2)).size() == 1);
        check("earliest review date is reported", scheduler.getEarliestReviewDate().equals(DAY_0.plusDays(2)));
    }

    /**
     * Regression test. Card.equals() compares TEXT, so two separate cards with
     * the same English, translation and language are "equal". The scheduler
     * must still treat them as two cards, because each has its own box and date.
     * The bug: answering c2 evicted c1 and left c2 in the queue twice.
     */
    static void equalButDistinctCardsAreKeptApart() {
        ReviewScheduler scheduler = new ReviewScheduler();
        Card head = new Word("food", "aduane", Language.TWI);
        Card c1 = new Word("water", "nsuo", Language.TWI);
        Card c2 = new Word("water", "nsuo", Language.TWI);
        check("setup: c1 and c2 are equal but not the same object", c1.equals(c2) && c1 != c2);

        head.setNextReviewDate(DAY_0);               // keeps c1 and c2 off the head
        c1.setNextReviewDate(DAY_0.plusDays(5));
        c2.setNextReviewDate(DAY_0.plusDays(6));
        scheduler.trackAll(Arrays.asList(head, c1, c2));
        check("both equal cards are tracked", scheduler.getTrackedCount() == 3);

        scheduler.recordAnswer(c2, false, DAY_0);    // c2 -> box 1, due DAY_0 + 1

        List<Card> all = scheduler.getSoonestCards(10);
        int c1Count = 0;
        int c2Count = 0;
        for (Card card : all) {
            if (card == c1) c1Count++;
            if (card == c2) c2Count++;
        }
        check("answering c2 does not evict c1", c1Count == 1 && scheduler.isTracked(c1));
        check("c2 is in the queue exactly once", c2Count == 1);
        check("queue still holds three cards", all.size() == 3);
        check("c2 (due tomorrow) now comes before c1", all.get(1) == c2 && all.get(2) == c1);

        check("untracking c1 leaves c2 tracked", scheduler.untrack(c1) && scheduler.isTracked(c2)
                && !scheduler.isTracked(c1));
    }

    // ---------------------------------------------------------------- helpers

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }

    private static void checkThrows(String name, Runnable action) {
        try {
            action.run();
            check(name + " (nothing was thrown)", false);
        } catch (IllegalArgumentException e) {
            check(name, true);
        }
    }
}
