import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * QuizSession runs ONE sitting with ONE deck: it picks the cards that are due,
 * builds a question for each, grades the answers, keeps the score, and
 * reschedules the cards.
 *
 * <h2>What happens when the learner answers (see {@link #submitAnswer})</h2>
 * <ol>
 *   <li>The current question grades itself with {@code checkAnswer()}.</li>
 *   <li>The ScoreTracker records the result.</li>
 *   <li>The ReviewScheduler moves the card's box and sets its next date.</li>
 *   <li>The session moves on to the next question.</li>
 *   <li>The result is returned so the screen can show feedback.</li>
 * </ol>
 *
 * <h2>Polymorphism</h2>
 * This class never checks which kind of Question it holds before grading.
 * It calls {@code question.checkAnswer(answer)} and the Translation, Matching
 * or Fill-in-the-blank question grades itself in its own way. The only
 * {@code instanceof} here is on a Card, when deciding whether a
 * Fill-in-the-blank question can be BUILT, because that constructor needs a
 * Phrase.
 *
 * <h2>Collection: ArrayDeque&lt;Question&gt;</h2>
 * Questions are taken from the front and a missed question can be put back
 * on the end for a second try. ArrayDeque does both in O(1). An ArrayList
 * would make taking from the front O(n), because every remaining element
 * shifts down one place.
 *
 * <h2>Second tries</h2>
 * By default, a question answered wrongly is asked once more at the end of
 * the session, for practice. The second try is graded so the learner gets
 * feedback, but it is NOT scored and does NOT move the card again - the
 * first answer is what counts, as in a real Leitner system. Pass
 * {@code retryMissed = false} to the full constructor to turn this off.
 */
public class QuizSession {

    /** How many wrong options a Matching question offers, at most. */
    public static final int MATCHING_DISTRACTORS = 3;

    // The three kinds of question this session can build.
    private static final int TRANSLATION = 0;
    private static final int MATCHING = 1;
    private static final int FILL_BLANK = 2;

    private final Deck deck;
    private final LocalDate today;
    private final ReviewScheduler scheduler;
    private final ScoreTracker tracker;
    private final Random random;
    private final boolean retryMissed;

    private final ArrayDeque<Question> pending = new ArrayDeque<>();
    /** Second-try questions. Identity-based: two questions are never "equal". */
    private final Set<Question> retries = Collections.newSetFromMap(new IdentityHashMap<>());

    private final int totalQuestions;
    private Question current;
    private Question lastQuestion;
    private boolean lastCorrect;
    private int answeredCount;

    // =====================================================================
    //  CONSTRUCTORS
    // =====================================================================

    /**
     * Starts a session for the cards in the deck that are due today.
     *
     * @param deck the deck to study
     * @throws IllegalArgumentException if deck is null
     */
    public QuizSession(Deck deck) {
        this(deck, LocalDate.now());
    }

    /**
     * Starts a session for the cards in the deck that are due on the given day.
     *
     * @param deck  the deck to study
     * @param today the date to treat as today (tests pass a fixed date)
     * @throws IllegalArgumentException if deck or today is null
     */
    public QuizSession(Deck deck, LocalDate today) {
        this(deck, today, new ReviewScheduler(), new ScoreTracker(), new Random(), true);
    }

    /**
     * Full constructor, mainly for tests: every collaborator can be supplied.
     *
     * @param deck        the deck to study
     * @param today       the date to treat as today
     * @param scheduler   moves cards between boxes
     * @param tracker     records the score (it is reset at the start)
     * @param random      chooses question types; pass a seeded Random for
     *                    repeatable tests
     * @param retryMissed whether a missed question is asked once more at the end
     * @throws IllegalArgumentException if any object argument is null
     */
    public QuizSession(Deck deck, LocalDate today, ReviewScheduler scheduler,
                       ScoreTracker tracker, Random random, boolean retryMissed) {
        if (deck == null) {
            throw new IllegalArgumentException("A quiz needs a deck.");
        }
        if (today == null) {
            throw new IllegalArgumentException("A quiz needs today's date.");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("A quiz needs a ReviewScheduler.");
        }
        if (tracker == null) {
            throw new IllegalArgumentException("A quiz needs a ScoreTracker.");
        }
        if (random == null) {
            throw new IllegalArgumentException("A quiz needs a Random to choose question types.");
        }
        this.deck = deck;
        this.today = today;
        this.scheduler = scheduler;
        this.tracker = tracker;
        this.random = random;
        this.retryMissed = retryMissed;

        tracker.reset();
        scheduler.trackAll(deck.getCards());   // lets us report the next due date

        // Already sorted lowest box (hardest) first by Deck - do not sort again.
        for (Card card : deck.getDueCards(today)) {
            pending.addLast(buildQuestion(card));
        }
        totalQuestions = pending.size();
        current = pending.pollFirst();          // null if nothing is due
    }

    // =====================================================================
    //  BUILDING QUESTIONS
    // =====================================================================

    /**
     * Builds one question for a card: works out which question types are
     * possible for it, then picks one of those at random.
     *
     * <ul>
     *   <li>Translation: always possible - the fallback.</li>
     *   <li>Matching: only if the deck has another card whose English can
     *       serve as a wrong option.</li>
     *   <li>Fill in the blank: only if the card is a Phrase with at least
     *       one word.</li>
     * </ul>
     */
    private Question buildQuestion(Card card) {
        List<Integer> possible = new ArrayList<>();
        possible.add(TRANSLATION);

        List<String> distractors = findDistractors(card);
        if (!distractors.isEmpty()) {
            possible.add(MATCHING);
        }
        if (card instanceof Phrase && ((Phrase) card).getWordCount() >= 1) {
            possible.add(FILL_BLANK);
        }

        int choice = possible.get(random.nextInt(possible.size()));
        switch (choice) {
            case MATCHING:
                return new MatchingQuestion(card, distractors);
            case FILL_BLANK:
                Phrase phrase = (Phrase) card;
                return new FillBlankQuestion(phrase, random.nextInt(phrase.getWordCount()));
            default:
                return new TranslationQuestion(card);
        }
    }

    /**
     * English meanings of other cards, usable as wrong options. Any that
     * would read the same as the right answer are left out, because then
     * two options would both be "correct" and MatchingQuestion would throw.
     * On a one-card deck this list is empty, so no Matching question is built.
     */
    private List<String> findDistractors(Card card) {
        List<String> distractors = new ArrayList<>();
        for (Card other : deck.getOtherRandomCards(card, MATCHING_DISTRACTORS)) {
            if (!Card.answersMatch(other.getEnglish(), card.getEnglish())) {
                distractors.add(other.getEnglish());
            }
        }
        return distractors;
    }

    // =====================================================================
    //  ANSWERING
    // =====================================================================

    /**
     * Grades the learner's answer to the current question, records the score,
     * reschedules the card, and moves to the next question.
     *
     * <p>A blank or null answer is graded as wrong; the screen should stop
     * the learner submitting an empty box if that is not wanted.
     *
     * @param answer what the learner typed or clicked
     * @return true if the answer was right
     * @throws IllegalStateException if the session is already finished
     */
    public boolean submitAnswer(String answer) {
        if (current == null) {
            throw new IllegalStateException(
                    "The quiz is finished - there is no question to answer.");
        }

        // 1. The question grades itself. We never look at the card's text here.
        boolean correct = current.checkAnswer(answer);
        boolean isRetry = retries.contains(current);

        if (!isRetry) {
            // 2. Tell the ScoreTracker.
            tracker.record(current.getCard(), correct);
            // 3. Tell the ReviewScheduler to move the card's box and date.
            scheduler.recordAnswer(current.getCard(), correct, today);
            answeredCount++;

            if (!correct && retryMissed) {
                Question secondTry = rebuildForRetry(current);
                retries.add(secondTry);
                pending.addLast(secondTry);
            }
        }

        // 4. Move to the next question.
        lastQuestion = current;
        lastCorrect = correct;
        current = pending.pollFirst();

        // 5. Return the result for the screen.
        return correct;
    }

    /**
     * A fresh question for a second try, so a Matching question's options are
     * reshuffled and a Fill-in-the-blank may blank a different word.
     */
    private Question rebuildForRetry(Question missed) {
        return buildQuestion(missed.getCard());
    }

    /**
     * Ends the session early. Answers already given keep their effect on the
     * cards; the questions not yet asked are dropped.
     */
    public void endEarly() {
        pending.clear();
        current = null;
    }

    // =====================================================================
    //  STATE FOR THE SCREEN
    // =====================================================================

    /** @return the question to show now, or null if the session is finished */
    public Question getCurrentQuestion() {
        return current;
    }

    /** @return true once there are no questions left (immediately, if nothing was due) */
    public boolean isFinished() {
        return current == null;
    }

    /** @return true if no cards were due, so the session had nothing to ask */
    public boolean hadNothingDue() {
        return totalQuestions == 0;
    }

    /** @return true if the current question is a second, unscored try */
    public boolean isCurrentRetry() {
        return current != null && retries.contains(current);
    }

    /**
     * The question answered most recently. Useful for feedback, because
     * {@link #submitAnswer} has already moved on to the next question.
     *
     * @return the last question answered, or null if none has been answered
     */
    public Question getLastQuestion() {
        return lastQuestion;
    }

    /** @return whether the last answer was right (false if none yet) */
    public boolean wasLastCorrect() {
        return lastCorrect;
    }

    /** @return how many questions the session started with (not counting second tries) */
    public int getTotalQuestions() {
        return totalQuestions;
    }

    /** @return how many first-time questions have been answered */
    public int getAnsweredCount() {
        return answeredCount;
    }

    /** @return how many questions are left, including the current one and any second tries */
    public int getRemainingCount() {
        return pending.size() + (current == null ? 0 : 1);
    }

    /**
     * The earliest next-review date of any card in the deck. Lets the screen
     * say "nothing due today - next review on ...".
     *
     * @return the date, or null if the deck is empty
     */
    public LocalDate getNextReviewDate() {
        return scheduler.getEarliestReviewDate();
    }

    /** @return the deck being studied */
    public Deck getDeck() {
        return deck;
    }

    /** @return the date this session treats as today */
    public LocalDate getToday() {
        return today;
    }

    /** @return the score for this session */
    public ScoreTracker getScoreTracker() {
        return tracker;
    }

    /** @return the end-of-session summary, e.g. "8 of 10 correct, 80%" */
    public String getSummary() {
        if (hadNothingDue()) {
            return "Nothing was due in " + deck.getName() + ".";
        }
        return tracker.getSummary();
    }
}
