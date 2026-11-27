import java.util.Random;

public class FillBlankQuestion extends Question {

    private final Phrase phrase;
    private final int blankIndex;

    public FillBlankQuestion(Phrase phrase, int blankIndex) {
        super(phrase);
        if (blankIndex < 0 || blankIndex >= phrase.getWordCount()) {
            throw new IllegalArgumentException("No word at position " + blankIndex
                    + ". This phrase has " + phrase.getWordCount() + " words.");
        }
        this.phrase = phrase;
        this.blankIndex = blankIndex;
    }

    public FillBlankQuestion(Phrase phrase) {
        this(phrase, new Random().nextInt(Math.max(1, phrase.getWordCount())));
    }

    @Override
    public String getPrompt() {
        return "Complete the phrase: " + phrase.getTranslationWithBlank(blankIndex)
                + "   (" + phrase.getEnglish() + ")";
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
