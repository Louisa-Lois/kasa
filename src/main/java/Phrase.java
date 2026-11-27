import java.util.ArrayList;
import java.util.List;

/**
 * A Phrase is a flashcard for a group of words, e.g.
 *     "How are you?" -> "Ɛte sɛn?" (Twi)
 *
 * It "extends" Card, and adds:
 *   - context         when you would use it, e.g. "greeting"     (optional)
 *   - literalMeaning  word-for-word meaning, e.g. "How is it?"   (optional)
 *
 * It also has helper methods for the FILL-IN-THE-BLANK question type:
 *     phrase.getTranslationWords()      -> ["Wo", "din", "de", "sɛn"]
 *     phrase.getTranslationWithBlank(1) -> "Wo ____ de sɛn?"
 *     phrase.getWordAt(1)               -> "din"   (the missing word)
 */
public class Phrase extends Card {

    private static final String BLANK = "____";

    private final String context;
    private final String literalMeaning;

    /** Short constructor: just the phrase and its translation. */
    public Phrase(String english, String translation, Language language) {
        this(english, translation, language, "", "");
    }

    /** Full constructor with the optional extras. Pass "" if you don't have one. */
    public Phrase(String english, String translation, Language language,
                  String context, String literalMeaning) {
        super(english, translation, language);
        this.context = clean(context);
        this.literalMeaning = clean(literalMeaning);
    }

    public String getContext() {
        return context;
    }

    public String getLiteralMeaning() {
        return literalMeaning;
    }

    // ---------- Helpers for Fill-in-the-Blank questions ----------

    /** The words of the translation without punctuation. "Ɛte sɛn?" -> ["Ɛte", "sɛn"] */
    public List<String> getTranslationWords() {
        List<String> words = new ArrayList<>();
        for (String token : splitIntoTokens()) {
            String word = stripPunctuation(token);
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    /** How many words are in the translation. */
    public int getWordCount() {
        return getTranslationWords().size();
    }

    /**
     * The word at a position (starting from 0).
     * @throws IndexOutOfBoundsException if the position does not exist
     */
    public String getWordAt(int index) {
        return getTranslationWords().get(index);
    }

    /**
     * The translation with one word replaced by "____" (punctuation is kept).
     * Example: "Wo din de sɛn?" with index 3 -> "Wo din de ____?"
     *
     * @throws IndexOutOfBoundsException if the position does not exist
     */
    public String getTranslationWithBlank(int index) {
        if (index < 0 || index >= getWordCount()) {
            throw new IndexOutOfBoundsException("No word at position " + index
                    + ". This phrase has " + getWordCount() + " words.");
        }
        StringBuilder result = new StringBuilder();
        int wordNumber = 0;
        for (String token : splitIntoTokens()) {
            String word = stripPunctuation(token);
            String piece = token;
            if (!word.isEmpty()) {
                if (wordNumber == index) {
                    int start = token.indexOf(word);
                    piece = token.substring(0, start) + BLANK + token.substring(start + word.length());
                }
                wordNumber++;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(piece);
        }
        return result.toString();
    }

    private String[] splitIntoTokens() {
        return getTranslation().trim().split("\\s+");
    }

    private static String stripPunctuation(String token) {
        return token.replaceAll("^[\\p{Punct}¿¡’‘“”]+", "")
                    .replaceAll("[\\p{Punct}¿¡’‘“”]+$", "");
    }

    // ---------- Polymorphism: Phrase's own versions of Card's abstract methods ----------

    @Override
    public String getCardType() {
        return "Phrase";
    }

    /** Example hint for "Ɛte sɛn?":  2 words. First word: "Ɛte". */
    @Override
    public String getHint() {
        List<String> words = getTranslationWords();
        if (words.isEmpty()) {
            return "No hint available.";
        }
        return words.size() + " words. First word: \"" + words.get(0) + "\".";
    }

    /** Format: PHRASE|english|translation|context|literalMeaning|box|nextReviewDate */
    @Override
    public String toFileLine() {
        return "PHRASE|" + getEnglish() + "|" + getTranslation() + "|" + context + "|"
                + literalMeaning + "|" + getBox() + "|" + getNextReviewDate();
    }

    /** Example: "[Phrase] How are you? -> Ɛte sɛn? (Twi) - greeting" */
    @Override
    public String toString() {
        String text = super.toString();
        if (!context.isEmpty()) {
            text += " - " + context;
        }
        return text;
    }
}
