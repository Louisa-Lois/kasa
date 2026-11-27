import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionTest {

    private final Word water = new Word("water", "nsuo", Language.TWI);
    private final Phrase name = new Phrase("What is your name?", "Wo din de sɛn?", Language.TWI);
    private final Phrase howAreYou = new Phrase("How are you?", "Ɛte sɛn?", Language.TWI);

    // ---------- TranslationQuestion ----------
    @Test
    void translationAcceptsCorrectAnswer() {
        assertTrue(new TranslationQuestion(water).checkAnswer("water"));
    }

    @Test
    void translationIgnoresCaseAndExtraSpaces() {
        assertTrue(new TranslationQuestion(water).checkAnswer("  WATER  "));
    }

    @Test
    void translationRejectsWrongAnswer() {
        assertFalse(new TranslationQuestion(water).checkAnswer("fire"));
    }

    @Test
    void translationRejectsEmptyAndNull() {
        TranslationQuestion q = new TranslationQuestion(water);
        assertFalse(q.checkAnswer(""));
        assertFalse(q.checkAnswer(null));
    }

    @Test
    void translationPromptShowsLocalWordAndGivesCorrectAnswer() {
        TranslationQuestion q = new TranslationQuestion(water);
        assertTrue(q.getPrompt().contains("nsuo"));
        assertEquals("water", q.getCorrectAnswer());
    }

    @Test
    void nullCardIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TranslationQuestion(null));
    }

    // ---------- MatchingQuestion ----------
    @Test
    void matchingContainsCorrectOptionAndDistractors() {
        MatchingQuestion q = new MatchingQuestion(water,
                Arrays.asList("fire", "house", "food"));
        assertEquals(4, q.getOptions().size());
        assertTrue(q.getOptions().contains("water"));
    }

    @Test
    void matchingChecksChosenOption() {
        MatchingQuestion q = new MatchingQuestion(water, Arrays.asList("fire", "house"));
        assertTrue(q.checkAnswer("water"));
        assertFalse(q.checkAnswer("fire"));
    }

    @Test
    void matchingDropsDistractorsThatEqualTheAnswerOrRepeat() {
        MatchingQuestion q = new MatchingQuestion(water,
                Arrays.asList("Water", "fire", "fire"));
        assertEquals(2, q.getOptions().size());
    }

    @Test
    void matchingNeedsAtLeastOneWrongOption() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchingQuestion(water, new ArrayList<String>()));
        assertThrows(IllegalArgumentException.class,
                () -> new MatchingQuestion(water, null));
    }

    @Test
    void matchingOptionsListCannotBeChangedFromOutside() {
        MatchingQuestion q = new MatchingQuestion(water, Arrays.asList("fire"));
        List<String> copy = q.getOptions();
        copy.clear();
        assertEquals(2, q.getOptions().size());
    }

    // ---------- FillBlankQuestion ----------
    @Test
    void fillBlankHidesTheChosenWord() {
        String prompt = new FillBlankQuestion(name, 1).getPrompt();
        assertTrue(prompt.contains("Wo ____ de sɛn?"));
        assertFalse(prompt.contains("din"));
    }

    @Test
    void fillBlankAcceptsMissingWord() {
        FillBlankQuestion q = new FillBlankQuestion(name, 1);
        assertTrue(q.checkAnswer("din"));
        assertTrue(q.checkAnswer("  DIN "));
        assertEquals("din", q.getCorrectAnswer());
    }

    @Test
    void fillBlankAcceptsKeyboardLettersForSpecialOnes() {
        // the hidden word is "sɛn": typing "sen" should be accepted
        assertTrue(new FillBlankQuestion(name, 3).checkAnswer("sen"));
    }

    @Test
    void fillBlankRejectsWrongWord() {
        assertFalse(new FillBlankQuestion(name, 1).checkAnswer("de"));
    }

    @Test
    void fillBlankRejectsPositionThatDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> new FillBlankQuestion(name, 4));
        assertThrows(IllegalArgumentException.class, () -> new FillBlankQuestion(name, -1));
    }

    @Test
    void fillBlankRandomVersionAlwaysMakesAWorkingQuestion() {
        for (int i = 0; i < 30; i++) {
            FillBlankQuestion q = new FillBlankQuestion(howAreYou);
            assertTrue(q.checkAnswer(q.getCorrectAnswer()));
            assertTrue(q.getPrompt().contains("____"));
        }
    }

    // ---------- Polymorphism ----------
    @Test
    void allQuestionTypesWorkThroughTheBaseClass() {
        Question[] questions = {
            new TranslationQuestion(water),
            new MatchingQuestion(water, Arrays.asList("fire")),
            new FillBlankQuestion(name, 1)
        };
        for (Question q : questions) {
            assertNotNull(q.getPrompt());
            assertNotNull(q.getQuestionType());
            assertFalse(q.checkAnswer("definitely wrong"));
            assertTrue(q.checkAnswer(q.getCorrectAnswer()));
        }
    }
}
