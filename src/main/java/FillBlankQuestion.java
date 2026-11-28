import java.util.Random;

/**
 * Shows a local-language phrase with one word replaced by a blank, e.g.
 *     "Wo din de ____?"
 * The learner types the missing word.
 */
public class FillBlankQuestion extends Question {

    private final Phrase phrase;
    private final int blankIndex;

    /** Hides the word at a chosen position (the first word is position 0). */
    public FillBlankQuestion(Phrase phrase, int blankIndex) {
        super(phrase);
        if (blankIndex < 0 || blankIndex >= phrase.getWordCount()) {
            throw new IllegalArgumentException("No word at position " + blankIndex
                    + ". This phrase has " + phrase.getWordCount() + " words.");
        }
        this.phrase = phrase;
        this.blankIndex = blankIndex;
    }

    /** Hides a randomly chosen word. */
    public FillBlankQuestion(Phrase phrase) {
        this(phrase, new Random().nextInt(Math.max(1, phrase.getWordCount())));
    }

    @Override
    public String getPrompt() {
        return "Complete the phrase: " + phrase.getTranslationWithBlank(blankIndex)
                + "   (" + englishWithoutAnswer() + ")";
    }

    /**
     * The English meaning is shown as context. But for names and numbers the
     * hidden word can also appear in the English (e.g. "My name is Kofi"),
     * which would give the answer away. So any English word that matches the
     * hidden word is replaced by "____" too.
     */
    private String englishWithoutAnswer() {
        String answer = phrase.getWordAt(blankIndex);
        StringBuilder result = new StringBuilder();
        for (String token : phrase.getEnglish().split("\\s+")) {
            // the word itself, without punctuation around it ("Kofi." -> "Kofi")
            String word = token.replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
            String shown = token;
            if (!word.isEmpty() && Card.answersMatch(word, answer)) {
                shown = token.replace(word, "____");
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(shown);
        }
        return result.toString();
    }

    @Override
    public boolean checkAnswer(String answer) {
        return Card.answersMatch(answer, phrase.getWordAt(blankIndex));
    }

    @Override
    public String getCorrectAnswer() {
        return phrase.getWordAt(blankIndex);
    }

    @Override
    public String getQuestionType() {
        return "Fill in the blank";
    }
}
