import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * ReviewScheduler decides WHEN a card should be seen again, using a
 * five-box Leitner system.
 *
 * <h2>The rule</h2>
 * <ul>
 *   <li>Right answer: the card moves UP one box (a box-5 card stays in box 5).</li>
 *   <li>Wrong answer: the card goes all the way back to box 1, not down one box.
 *       A card the learner has just failed is, by definition, not known, so it
 *       must come back tomorrow whatever box it had reached.</li>
 *   <li>The card's next review date is today plus the interval of its NEW box.</li>
 * </ul>
 *
 * <h2>The intervals, and why</h2>
 * <pre>
 *   Box:                   1   2   3   4   5
 *   Days until next review 1   2   4   8   16
 * </pre>
 * A doubling scale: each correct answer roughly doubles how long the learner
 * can go without seeing the card. It is the usual Leitner scale, simple to
 * explain, and it stretches a fully learnt card to just over two weeks, which
 * suits a one-semester project.
 *
 * <h2>Testing dates without waiting</h2>
 * Every method that depends on the date takes a {@code LocalDate today}
 * parameter. Only the no-argument convenience overloads call
 * {@code LocalDate.now()}. This is the same pattern as
 * {@code Card.isDue()} / {@code Card.isDue(LocalDate)}, and it means a test can
 * check that a box-4 card is due in exactly 8 days in one line.
 *
 * <h2>Collection: PriorityQueue&lt;Card&gt;</h2>
 * The scheduler can track a set of cards in a {@code PriorityQueue} ordered by
 * next review date (then by box, so harder cards win a tie). The question this
 * class is asked most is "which card is due soonest?". A priority queue answers
 * that with {@code peek()} in O(1), and putting a card back after an answer
 * costs O(log n). Re-sorting a list after every answer would cost O(n log n).
 * With 46 cards there is no measurable speed difference; the reason for the
 * choice is that it is the right shape for the problem and stays right as the
 * decks grow.
 *
 * <p>One honest caveat: a PriorityQueue does not notice when a card's date
 * changes. So {@link #recordAnswer} takes the card out BEFORE changing it and
 * puts it back afterwards. Taking out the head of the queue is O(log n); taking
 * out a card from the middle is O(n). In a quiz the card being answered is
 * usually the one due soonest, so the cheap case is the common one.
 *
 * <h2>Cards are tracked by identity, not by equals()</h2>
 * {@code Card.equals()} says two cards with the same text are equal. That is
 * the right meaning for a Deck refusing duplicates, but the scheduler is
 * about each card's own box and date, which equals() ignores. Every lookup
 * and removal in this class therefore uses {@code ==}. Card itself is left
 * alone, because Deck's duplicate check depends on its equals().
 */
public class ReviewScheduler {

    /** Days until the next review, indexed by box - 1. Box 1 is index 0. */
    private static final int[] INTERVAL_DAYS = {1, 2, 4, 8, 16};

    /** Soonest next-review date first; lower box first when dates are equal. */
    private static final Comparator<Card> SOONEST_FIRST =
            Comparator.comparing(Card::getNextReviewDate)
                      .thenComparingInt(Card::getBox);

    private final PriorityQueue<Card> queue = new PriorityQueue<>(SOONEST_FIRST);

    /** Creates a scheduler that is not tracking any cards yet. */
    public ReviewScheduler() {
    }

    // =====================================================================
    //  INTERVALS
    // =====================================================================

    /**
     * How many days a card in the given box waits before its next review.
     *
     * @param box a box number from 1 to 5
     * @return 1, 2, 4, 8 or 16
     * @throws IllegalArgumentException if box is not between 1 and 5
     */
    public int getIntervalDays(int box) {
        checkBox(box);
        return INTERVAL_DAYS[box - Card.MIN_BOX];
    }

    /**
     * The date a card placed in the given box today should next be reviewed.
     *
     * @param box   a box number from 1 to 5
     * @param today the date to count from
     * @return today plus the interval for that box
     * @throws IllegalArgumentException if box is out of range or today is null
     */
    public LocalDate getNextReviewDate(int box, LocalDate today) {
        checkDate(today);
        return today.plusDays(getIntervalDays(box));
    }

    /**
     * The box a card moves to after an answer, without changing the card.
     * Right answers move up one box but never past box 5; wrong answers
     * always go back to box 1.
     *
     * @param currentBox the box the card is in now, 1 to 5
     * @param correct    whether the learner answered correctly
     * @return the new box, always between 1 and 5
     * @throws IllegalArgumentException if currentBox is not between 1 and 5
     */
    public int getNewBox(int currentBox, boolean correct) {
        checkBox(currentBox);
        if (!correct) {
            return Card.MIN_BOX;
        }
        // Clamp: a right answer on a box-5 card leaves it in box 5.
        return Math.min(currentBox + 1, Card.MAX_BOX);
    }

    // =====================================================================
    //  UPDATING A CARD AFTER AN ANSWER
    // =====================================================================

    /**
     * Moves a card to its new box and sets its next review date, based on
     * whether the learner got it right.
     *
     * <p>If this scheduler is tracking the card, the card's place in the
     * queue is updated too.
     *
     * @param card    the card that was just answered
     * @param correct whether the answer was right
     * @param today   the date the answer was given
     * @return the card's new box number
     * @throws IllegalArgumentException if card or today is null
     */
    public int recordAnswer(Card card, boolean correct, LocalDate today) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot schedule an empty card.");
        }
        checkDate(today);

        // The queue orders cards by date, so the card must leave the queue
        // BEFORE its date changes, or the queue's order would be corrupted.
        boolean wasTracked = removeFromQueue(card);

        int newBox = getNewBox(card.getBox(), correct);
        card.setBox(newBox);
        card.setNextReviewDate(getNextReviewDate(newBox, today));

        if (wasTracked) {
            queue.offer(card);
        }
        return newBox;
    }

    /**
     * Same as {@link #recordAnswer(Card, boolean, LocalDate)}, using today's date.
     *
     * @param card    the card that was just answered
     * @param correct whether the answer was right
     * @return the card's new box number
     * @throws IllegalArgumentException if card is null
     */
    public int recordAnswer(Card card, boolean correct) {
        return recordAnswer(card, correct, LocalDate.now());
    }

    // =====================================================================
    //  TRACKING CARDS IN THE PRIORITY QUEUE
    // =====================================================================

    /**
     * Starts tracking a card so it can be returned by the "due soonest"
     * methods. Adding a card that is already tracked does nothing.
     *
     * @param card the card to track
     * @return true if the card was added, false if it was already tracked
     * @throws IllegalArgumentException if card is null
     */
    public boolean track(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot track an empty card.");
        }
        if (isTracked(card)) {
            return false;
        }
        queue.offer(card);
        return true;
    }

    /**
     * Starts tracking every card in a collection, for example
     * {@code deck.getCards()}. Cards already tracked are skipped.
     *
     * @param cards the cards to track
     * @return how many cards were newly added
     * @throws IllegalArgumentException if cards is null or contains null
     */
    public int trackAll(Collection<? extends Card> cards) {
        if (cards == null) {
            throw new IllegalArgumentException("Cannot track an empty list of cards.");
        }
        int added = 0;
        for (Card card : cards) {
            if (track(card)) {
                added++;
            }
        }
        return added;
    }

    /**
     * Stops tracking a card. The card itself is not changed.
     *
     * @param card the card to stop tracking
     * @return true if the card was being tracked
     */
    public boolean untrack(Card card) {
        return card != null && removeFromQueue(card);
    }

    /**
     * Whether this exact card object is being tracked.
     *
     * @param card the card to look for
     * @return true if it is in the queue
     */
    public boolean isTracked(Card card) {
        for (Card tracked : queue) {
            if (tracked == card) {
                return true;
            }
        }
        return false;
    }

    /** @return how many cards are being tracked */
    public int getTrackedCount() {
        return queue.size();
    }

    /** Stops tracking every card. The cards themselves are not changed. */
    public void clear() {
        queue.clear();
    }

    /**
     * The tracked card due soonest, without removing it. O(1).
     *
     * @return the card with the earliest next review date, or null if no
     *         cards are tracked
     */
    public Card peekSoonest() {
        return queue.peek();
    }

    /**
     * The earliest next-review date among the tracked cards. Useful for a
     * "nothing due today - next card due on ..." message.
     *
     * @return the earliest date, or null if no cards are tracked
     */
    public LocalDate getEarliestReviewDate() {
        Card soonest = queue.peek();
        return soonest == null ? null : soonest.getNextReviewDate();
    }

    /**
     * Up to {@code count} tracked cards, soonest first. The tracked cards
     * are not removed or changed.
     *
     * @param count the most cards to return; 0 gives an empty list
     * @return a new list, soonest due first
     * @throws IllegalArgumentException if count is negative
     */
    public List<Card> getSoonestCards(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "Number of cards cannot be negative, but was " + count);
        }
        // Work on a copy so the real queue is untouched.
        PriorityQueue<Card> copy = new PriorityQueue<>(queue);
        List<Card> result = new ArrayList<>();
        while (result.size() < count && !copy.isEmpty()) {
            result.add(copy.poll());
        }
        return result;
    }

    /**
     * Every tracked card that is due on or before the given day, soonest
     * (most overdue) first. Stops as soon as it reaches a card that is not
     * yet due, because everything after it in the queue is later still.
     *
     * @param today the day to check against
     * @return a new list of due cards, most overdue first
     * @throws IllegalArgumentException if today is null
     */
    public List<Card> getDueCards(LocalDate today) {
        checkDate(today);
        PriorityQueue<Card> copy = new PriorityQueue<>(queue);
        List<Card> due = new ArrayList<>();
        while (!copy.isEmpty() && copy.peek().isDue(today)) {
            due.add(copy.poll());
        }
        return due;
    }

    /**
     * Every tracked card due today, most overdue first.
     *
     * @return a new list of due cards
     */
    public List<Card> getDueCards() {
        return getDueCards(LocalDate.now());
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    /**
     * Removes this exact card object, never a look-alike.
     *
     * <p>Card.equals() compares TEXT: two separate "water / nsuo" cards are
     * equal. That is right for Deck, which uses it to refuse duplicates. It is
     * wrong here, because each card object has its own box and review date,
     * so the scheduler must track objects, not text. PriorityQueue.remove(Object)
     * uses equals() and would remove whichever look-alike it meets first -
     * even after we found the right object by identity - so it is never called.
     * removeIf with {@code ==} compares identity instead.
     *
     * @return true if the card was in the queue
     */
    private boolean removeFromQueue(Card card) {
        if (queue.peek() == card) {
            queue.poll();                                    // O(log n)
            return true;
        }
        return queue.removeIf(tracked -> tracked == card);   // O(n), identity only
    }

    private static void checkBox(int box) {
        if (box < Card.MIN_BOX || box > Card.MAX_BOX) {
            throw new IllegalArgumentException("Box must be between "
                    + Card.MIN_BOX + " and " + Card.MAX_BOX + ", but was " + box);
        }
    }

    private static void checkDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("The date cannot be empty.");
        }
    }
}
