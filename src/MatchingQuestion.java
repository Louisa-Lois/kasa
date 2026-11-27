import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows a local-language word and a list of English options.
 * The learner picks the option that matches. The wrong options
 * (distractors) are English meanings taken from other cards in the deck.
 */
public class MatchingQuestion extends Question {

    private final List<String> options = new ArrayList<>();

    /**
     * @param card        the card being asked
     * @param distractors English meanings of OTHER cards, used as wrong options
     * @throws IllegalArgumentException if there is not at least one usable wrong option
     */
    public MatchingQuestion(Card card, List<String> distractors) {
        super(card);
        if (distractors == null) {
            throw new IllegalArgumentException("Matching needs at least one wrong option.");
        }
        options.add(card.getEnglish());
        for (String d : distractors) {
            if (d == null || d.trim().isEmpty()) {
                continue;
            }
            // skip the right answer and repeats, so exactly one option is correct
            boolean alreadyThere = false;
            for (String existing : options) {
                if (Card.answersMatch(d, existing)) {
                    alreadyThere = true;
                    break;
                }
            }
            if (!alreadyThere) {
                options.add(d.trim());
            }
        }
        if (options.size() < 2) {
            throw new IllegalArgumentException("Matching needs at least one wrong option.");
        }
        Collections.shuffle(options);
    }

    /** The choices to show on screen, already shuffled. */
    public List<String> getOptions() {
        return new ArrayList<>(options);
    }

    @Override
    public String getPrompt() {
        return "Match \"" + getCard().getTranslation() + "\" with its meaning.";
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
        return "Matching";
    }
}
