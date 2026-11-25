/**
 * A Word is a flashcard for a SINGLE word, e.g.  water -> nsuo (Twi).
 *
 * It "extends" Card, so it automatically has everything Card has
 * (english, translation, language, box, nextReviewDate, checkTranslation, ...).
 * It adds two extra details:
 *   - partOfSpeech    e.g. "noun", "verb", "greeting"   (optional)
 *   - exampleSentence e.g. "Mepɛ nsuo." (I want water)  (optional)
 */
public class Word extends Card {

    private final String partOfSpeech;
    private final String exampleSentence;

    /** Short constructor: just the word and its translation. */
    public Word(String english, String translation, Language language) {
        this(english, translation, language, "", "");
    }

    /** Full constructor with the optional extras. Pass "" if you don't have one. */
    public Word(String english, String translation, Language language,
                String partOfSpeech, String exampleSentence) {
        super(english, translation, language); // runs Card's constructor first
        this.partOfSpeech = clean(partOfSpeech);
        this.exampleSentence = clean(exampleSentence);
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public boolean hasExampleSentence() {
        return !exampleSentence.isEmpty();
    }

    // ---------- Polymorphism: Word's own versions of Card's abstract methods ----------

    @Override
    public String getCardType() {
        return "Word";
    }

    /** Example hint for "nsuo":  Starts with "n" and has 4 letters. */
    @Override
    public String getHint() {
        String answer = getTranslation();
        int letterCount = answer.replace(" ", "").length();
        return "Starts with \"" + answer.charAt(0) + "\" and has " + letterCount + " letters.";
    }

    /** Format: WORD|english|translation|partOfSpeech|exampleSentence|box|nextReviewDate */
    @Override
    public String toFileLine() {
        return "WORD|" + getEnglish() + "|" + getTranslation() + "|" + partOfSpeech + "|"
                + exampleSentence + "|" + getBox() + "|" + getNextReviewDate();
    }

    /** Example: "[Word] water -> nsuo (Twi) - noun" */
    @Override
    public String toString() {
        String text = super.toString();
        if (!partOfSpeech.isEmpty()) {
            text += " - " + partOfSpeech;
        }
        return text;
    }
}
