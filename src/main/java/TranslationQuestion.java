/**
 * Shows a word or phrase in the local language.
 * The learner types the English meaning.
 */
public class TranslationQuestion extends Question {

    public TranslationQuestion(Card card) {
        super(card);
    }

    @Override
    public String getPrompt() {
        return "What does \"" + getCard().getTranslation() + "\" mean in English?";
    }

    @Override
    public boolean checkAnswer(String answer) {
        return getCard().checkEnglish(answer);
    }

    @Override
    public String getCorrectAnswer() {
        return getCard().getEnglish();
    }

    @Override
    public String getQuestionType() {
        return "Translation";
    }
}
