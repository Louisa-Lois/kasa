/**
 * Base class for every kind of quiz question.
 *
 * Each subclass decides how it shows the question (getPrompt)
 * and how it checks the answer (checkAnswer). This is the polymorphism
 * in the project: QuizSession only ever calls these two methods and
 * does not care which kind of question it has.
 *
 * Answer comparison is done by Card.answersMatch(), so it ignores capital
 * letters, spaces and punctuation, and accepts keyboard letters for special
 * ones (typing "ete sen" is accepted for "Ɛte sɛn?").
 */
public abstract class Question {

    private final Card card;

    protected Question(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("A question needs a card.");
        }
        this.card = card;
    }

    /** The card this question is about (QuizSession uses it to update the box). */
    public Card getCard() {
        return card;
    }

    /** The text shown to the learner. */
    public abstract String getPrompt();

    /** Returns true if the learner's answer is correct. */
    public abstract boolean checkAnswer(String answer);

    /** The correct answer as text, to show the learner after a wrong try. */
    public abstract String getCorrectAnswer();

    /** The kind of question, e.g. "Translation". The GUI can use it as a title. */
    public abstract String getQuestionType();
}
