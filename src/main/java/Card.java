import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * Card is the PARENT (abstract) class for every flashcard in the app.
 *
 * "abstract" means you can never write  new Card(...)  directly.
 * You always create one of the child classes instead:
 *     new Word("water", "nsuo", Language.TWI)
 *     new Phrase("How are you?", "Ɛte sɛn?", Language.TWI)
 *
 * Every card stores:
 *   - the English text            (front of the card)
 *   - the local-language text     (back of the card)
 *   - which language it is in
 *   - spaced-repetition progress  (box number + next review date)
 *
 * OOP ideas shown here:
 *   - Abstraction:   Card is abstract, with abstract methods the children must write.
 *   - Encapsulation: all fields are private; other classes use getters/setters.
 *   - Inheritance:   Word and Phrase "extend" Card and reuse all of this code.
 *   - Polymorphism:  getCardType(), getHint() and toFileLine() behave differently
 *                    for a Word and a Phrase, even when stored as a Card variable.
 */
public abstract class Card {

    // ---- Spaced-repetition box limits (Leitner system) ----
    // Box 1 = new / hard card (review often). Box 5 = well known (review rarely).
    public static final int MIN_BOX = 1;
    public static final int MAX_BOX = 5;

    // Shared by ALL cards: used to give every card a unique id number.
    private static int nextId = 1;

    private final int id;
    private final String english;
    private final String translation;
    private final Language language;

    private int box;
    private LocalDate nextReviewDate;

    /**
     * "protected" means only Card's child classes (Word, Phrase) can call this.
     */
    protected Card(String english, String translation, Language language) {
        if (isBlank(english)) {
            throw new IllegalArgumentException("English text cannot be empty.");
        }
        if (isBlank(translation)) {
            throw new IllegalArgumentException("Translation cannot be empty.");
        }
        if (language == null) {
            throw new IllegalArgumentException("Language cannot be empty.");
        }
        this.id = nextId++;
        this.english = clean(english);
        this.translation = clean(translation);
        this.language = language;
        this.box = MIN_BOX;                    // every new card starts in box 1
        this.nextReviewDate = LocalDate.now(); // and is due for review today
    }

    // =====================================================================
    //  ABSTRACT METHODS - Word and Phrase MUST each write their own version
    // =====================================================================

    /** Returns "Word" or "Phrase". */
    public abstract String getCardType();

    /** A small clue to help the learner when they are stuck. */
    public abstract String getHint();

    /** One line of text used by DeckLibrary to save this card to a file. */
    public abstract String toFileLine();

    // =====================================================================
    //  GETTERS
    // =====================================================================

    public int getId() {
        return id;
    }

    public String getEnglish() {
        return english;
    }

    public String getTranslation() {
        return translation;
    }

    public Language getLanguage() {
        return language;
    }

    // =====================================================================
    //  SPACED REPETITION DATA
    //  (The scheduler / QuizSession decides WHEN to change these.
    //   Card just stores them safely.)
    // =====================================================================

    public int getBox() {
        return box;
    }

    /** @throws IllegalArgumentException if box is not between 1 and 5 */
    public void setBox(int box) {
        if (box < MIN_BOX || box > MAX_BOX) {
            throw new IllegalArgumentException("Box must be between "
                    + MIN_BOX + " and " + MAX_BOX + ", but was " + box);
        }
        this.box = box;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        if (nextReviewDate == null) {
            throw new IllegalArgumentException("Next review date cannot be empty.");
        }
        this.nextReviewDate = nextReviewDate;
    }

    /** True if the card should be reviewed on the given day (today or overdue). */
    public boolean isDue(LocalDate today) {
        return !nextReviewDate.isAfter(today);
    }

    /** True if the card should be reviewed today. */
    public boolean isDue() {
        return isDue(LocalDate.now());
    }

    /** Puts the card back to "brand new": box 1, due today. */
    public void resetProgress() {
        this.box = MIN_BOX;
        this.nextReviewDate = LocalDate.now();
    }

    // =====================================================================
    //  ANSWER CHECKING (useful for the question classes)
    // =====================================================================

    /**
     * Checks a learner's answer against the local-language translation.
     * It is forgiving: ignores capital letters, extra spaces, ending punctuation,
     * and special letters. So typing "ete sen" is accepted for "Ɛte sɛn?".
     */
    public boolean checkTranslation(String answer) {
        return answersMatch(answer, translation);
    }

    /** Same as checkTranslation, but checks against the English side. */
    public boolean checkEnglish(String answer) {
        return answersMatch(answer, english);
    }

    /** Compares two pieces of text in the same forgiving way. */
    public static boolean answersMatch(String given, String expected) {
        if (given == null || expected == null) {
            return false;
        }
        return simplify(given).equals(simplify(expected));
    }

    /**
     * Makes text easy to compare:
     *  "  Ɛte  sɛn? " -> "ete sen"
     * Maps Ghanaian special letters to keyboard letters (ɛ->e, ɔ->o, ŋ->ng, ƒ->f, ɖ->d, ʋ->v),
     * removes accents/tone marks, lower-cases, removes punctuation and extra spaces.
     */
    public static String simplify(String text) {
        String result = text.toLowerCase(Locale.ROOT);
        result = result.replace('ɛ', 'e')
                       .replace('ɔ', 'o')
                       .replace('ƒ', 'f')
                       .replace('ɖ', 'd')
                       .replace('ʋ', 'v')
                       .replace('ɣ', 'g')
                       .replace("ŋ", "ng");
        // Remove accents and tone marks, e.g. "è" -> "e", "ẽ" -> "e"
        result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        // Remove punctuation such as ? ! . , ' "
        result = result.replaceAll("[\\p{Punct}¿¡’‘“”]", "");
        // Squash many spaces into one
        result = result.trim().replaceAll("\\s+", " ");
        return result;
    }

    // =====================================================================
    //  HELPERS FOR CHILD CLASSES
    // =====================================================================

    /** True if text is null or only spaces. */
    protected static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Tidies text before storing it: null becomes "", spaces are trimmed,
     * and "|" is replaced (because "|" separates fields in our save file).
     */
    protected static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replace("|", "/");
    }

    // =====================================================================
    //  equals / hashCode / toString
    // =====================================================================

    /**
     * Two cards are "the same" if they are the same type, same language,
     * and have the same English and translation (ignoring capital letters).
     * Deck uses this to stop duplicate cards being added.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Card that = (Card) other;
        return language == that.language
                && english.equalsIgnoreCase(that.english)
                && translation.equalsIgnoreCase(that.translation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), language,
                english.toLowerCase(Locale.ROOT), translation.toLowerCase(Locale.ROOT));
    }

    /** Example: "[Word] water -> nsuo (Twi)" */
    @Override
    public String toString() {
        return "[" + getCardType() + "] " + english + " -> " + translation + " (" + language + ")";
    }
}
