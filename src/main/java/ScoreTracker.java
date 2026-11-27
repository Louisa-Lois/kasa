import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * ScoreTracker records what happened in ONE quiz session and reports it.
 *
 * <p>It counts attempts and correct answers, works out accuracy, keeps the
 * current and best streak of correct answers, and remembers which cards were
 * missed (and how often) so the learner can see what needs work.
 *
 * <h2>Why a class and not two counters inside QuizSession?</h2>
 * Scoring has its own rules - what accuracy means before any answer, how a
 * streak breaks, which cards are weakest, how the summary is worded. Keeping
 * them here gives each class one job, and lets these rules be tested without
 * building a deck or a session.
 *
 * <h2>Accuracy with no answers</h2>
 * Before any question is answered, accuracy is 0.0. Dividing 0 by 0 would
 * throw with integers or print "NaN%" with doubles, so that case is guarded.
 * 0.0 is chosen because the learner has not yet got anything right.
 *
 * <h2>Collections: IdentityHashMap&lt;Card, Integer&gt; plus ArrayList&lt;Card&gt;</h2>
 * Miss counts are kept in a map from card to how many times it was missed, so
 * a card missed twice is stored once with a count of 2, and "has this card
 * been missed already?" is O(1).
 *
 * <p>It is an IdentityHashMap, not a HashMap, on purpose. Card.equals()
 * compares text, so two separate cards reading "water / nsuo" are equal, and
 * a HashMap would merge them into one entry with both misses. Those are two
 * different cards with their own boxes, so the map must compare with
 * {@code ==}. An IdentityHashMap does exactly that.
 *
 * <p>An IdentityHashMap has no order, so a separate ArrayList records the
 * cards in the order they were first missed. That keeps the list shown on
 * screen stable and sensible. It only grows when the map gains a new key, so
 * it never holds the same card twice.
 */
public class ScoreTracker {

    private int attempted;
    private int correct;
    private int currentStreak;
    private int bestStreak;
    private final Map<Card, Integer> missCounts = new IdentityHashMap<>();
    private final List<Card> missedInOrder = new ArrayList<>();

    /** Creates a tracker with nothing recorded. */
    public ScoreTracker() {
    }

    // =====================================================================
    //  RECORDING
    // =====================================================================

    /**
     * Records one answered question.
     *
     * @param card      the card the question was about
     * @param wasCorrect whether the learner answered correctly
     * @throws IllegalArgumentException if card is null
     */
    public void record(Card card, boolean wasCorrect) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot record an answer for an empty card.");
        }
        attempted++;
        if (wasCorrect) {
            correct++;
            currentStreak++;
            bestStreak = Math.max(bestStreak, currentStreak);
        } else {
            currentStreak = 0;
            Integer previous = missCounts.put(card, getMissCount(card) + 1);
            if (previous == null) {
                missedInOrder.add(card);   // first miss for this card object
            }
        }
    }

    /** Clears everything, ready for a new session. */
    public void reset() {
        attempted = 0;
        correct = 0;
        currentStreak = 0;
        bestStreak = 0;
        missCounts.clear();
        missedInOrder.clear();
    }

    // =====================================================================
    //  COUNTS
    // =====================================================================

    /** @return how many questions have been answered */
    public int getAttempted() {
        return attempted;
    }

    /** @return how many answers were right */
    public int getCorrect() {
        return correct;
    }

    /** @return how many answers were wrong */
    public int getIncorrect() {
        return attempted - correct;
    }

    /** @return how many right answers in a row, ending with the latest answer */
    public int getCurrentStreak() {
        return currentStreak;
    }

    /** @return the longest run of right answers this session */
    public int getBestStreak() {
        return bestStreak;
    }

    /** @return true if nothing has been recorded yet */
    public boolean isEmpty() {
        return attempted == 0;
    }

    // =====================================================================
    //  ACCURACY
    // =====================================================================

    /**
     * The proportion of answers that were right, from 0.0 to 1.0.
     *
     * @return correct / attempted, or 0.0 if nothing has been answered yet
     *         (never NaN, never throws)
     */
    public double getAccuracy() {
        if (attempted == 0) {
            return 0.0;
        }
        return (double) correct / attempted;
    }

    /**
     * Accuracy as a whole-number percentage, rounded to the nearest percent.
     *
     * @return 0 to 100; 0 if nothing has been answered yet
     */
    public int getAccuracyPercent() {
        return (int) Math.round(getAccuracy() * 100);
    }

    // =====================================================================
    //  MISSED CARDS
    // =====================================================================

    /**
     * Every card answered wrongly at least once, in the order first missed.
     * Each card appears once, however many times it was missed.
     *
     * @return a new list the caller may change freely
     */
    public List<Card> getWrongCards() {
        return new ArrayList<>(missedInOrder);
    }

    /**
     * How many times this exact card object was answered wrongly this
     * session. A different card with the same text is counted separately.
     *
     * @param card the card to look up
     * @return the number of misses, 0 if never missed or if card is null
     */
    public int getMissCount(Card card) {
        if (card == null) {
            return 0;
        }
        return missCounts.getOrDefault(card, 0);
    }

    /**
     * The cards that need the most work: most-missed first, and among cards
     * missed equally often, the one in the lower box first.
     *
     * @param count the most cards to return
     * @return a new list of up to count cards
     * @throws IllegalArgumentException if count is negative
     */
    public List<Card> getWeakestCards(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "Number of cards cannot be negative, but was " + count);
        }
        List<Card> cards = new ArrayList<>(missedInOrder);
        cards.sort((a, b) -> {
            int byMisses = Integer.compare(missCounts.get(b), missCounts.get(a));
            if (byMisses != 0) {
                return byMisses;
            }
            return Integer.compare(a.getBox(), b.getBox());
        });
        return new ArrayList<>(cards.subList(0, Math.min(count, cards.size())));
    }

    // =====================================================================
    //  SUMMARY
    // =====================================================================

    /**
     * A short line for the screen, such as "8 of 10 correct, 80%".
     *
     * @return the summary, or "No questions answered yet." before any answer
     */
    public String getSummary() {
        if (attempted == 0) {
            return "No questions answered yet.";
        }
        return correct + " of " + attempted + " correct, " + getAccuracyPercent() + "%";
    }

    /**
     * A longer, friendly end-of-session message: the summary, the best
     * streak, and which cards to look at again.
     *
     * @return several lines of text separated by newlines
     */
    public String getDetailedSummary() {
        if (attempted == 0) {
            return getSummary();
        }
        StringBuilder text = new StringBuilder(getSummary()).append('.');

        if (correct == attempted) {
            text.append("\nA perfect session!");
        } else if (bestStreak >= 3) {
            text.append("\nBest streak: ").append(bestStreak).append(" in a row.");
        }

        List<Card> weakest = getWeakestCards(3);
        if (!weakest.isEmpty()) {
            text.append("\nWorth another look: ");
            for (int i = 0; i < weakest.size(); i++) {
                if (i > 0) {
                    text.append(", ");
                }
                Card card = weakest.get(i);
                text.append(card.getTranslation()).append(" (").append(card.getEnglish()).append(')');
            }
        }
        return text.toString();
    }

    /** Example: "ScoreTracker[3 of 4 correct, 75%]" */
    @Override
    public String toString() {
        return "ScoreTracker[" + getSummary() + "]";
    }
}
