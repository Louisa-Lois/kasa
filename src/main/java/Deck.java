import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A Deck is a named group of flashcards in ONE language,
 * e.g. "Twi Greetings" with 10 cards.
 *
 * It can hold both Words and Phrases together in one list, because both
 * ARE Cards (polymorphism: a List<Card> can store any kind of Card).
 *
 * Main things you can do:
 *   deck.addCard(card)          add a card (same language only, no duplicates)
 *   deck.removeCard(card)       take a card out
 *   deck.getCards()             all cards (read-only list)
 *   deck.getWords() / getPhrases()   only one type
 *   deck.getDueCards()          cards to review today (spaced repetition)
 *   deck.getShuffledCards()     cards in random order (for a quiz)
 *   deck.getRandomCards(4)      4 random cards (e.g. for a Matching question)
 */
public class Deck {

    private static final Random RANDOM = new Random();

    private final String name;
    private final Language language;
    private String description;
    private final List<Card> cards = new ArrayList<>();

    public Deck(String name, Language language) {
        this(name, language, "");
    }

    public Deck(String name, Language language, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Deck name cannot be empty.");
        }
        if (language == null) {
            throw new IllegalArgumentException("Deck language cannot be empty.");
        }
        this.name = name.trim().replace("|", "/");
        this.language = language;
        setDescription(description);
    }

    // =====================================================================
    //  GETTERS / SETTERS
    // =====================================================================

    public String getName() {
        return name;
    }

    public Language getLanguage() {
        return language;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = (description == null) ? "" : description.trim().replace("|", "/");
    }

    // =====================================================================
    //  ADDING AND REMOVING CARDS
    // =====================================================================

    /**
     * Adds a card to the deck.
     *
     * @return true if it was added, false if the same card is already in the deck
     * @throws IllegalArgumentException if card is null or in a different language
     */
    public boolean addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot add an empty card.");
        }
        if (card.getLanguage() != language) {
            throw new IllegalArgumentException("Cannot add a " + card.getLanguage()
                    + " card to the " + language + " deck \"" + name + "\".");
        }
        if (cards.contains(card)) {   // uses Card.equals()
            return false;
        }
        cards.add(card);
        return true;
    }

    /** Adds many cards. Returns how many were actually added (duplicates are skipped). */
    public int addCards(List<? extends Card> newCards) {
        int added = 0;
        for (Card card : newCards) {
            if (addCard(card)) {
                added++;
            }
        }
        return added;
    }

    /** @return true if the card was found and removed */
    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    /** Removes the card whose English text matches. Returns the removed card, or null. */
    public Card removeCardByEnglish(String english) {
        Card card = findByEnglish(english);
        if (card != null) {
            cards.remove(card);
        }
        return card;
    }

    public void clear() {
        cards.clear();
    }

    // =====================================================================
    //  LOOKING AT CARDS
    // =====================================================================

    /** All cards. The list is read-only - use addCard/removeCard to change the deck. */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /** The card at a position (starting from 0). */
    public Card getCard(int index) {
        return cards.get(index);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public boolean contains(Card card) {
        return cards.contains(card);
    }

    /** Finds a card by its English text (ignores capitals/punctuation). Returns null if not found. */
    public Card findByEnglish(String english) {
        for (Card card : cards) {
            if (card.checkEnglish(english)) {
                return card;
            }
        }
        return null;
    }

    /** Finds a card by its local-language text. Returns null if not found. */
    public Card findByTranslation(String translation) {
        for (Card card : cards) {
            if (card.checkTranslation(translation)) {
                return card;
            }
        }
        return null;
    }

    /** Only the Word cards. */
    public List<Word> getWords() {
        List<Word> words = new ArrayList<>();
        for (Card card : cards) {
            if (card instanceof Word) {
                words.add((Word) card);
            }
        }
        return words;
    }

    /** Only the Phrase cards. */
    public List<Phrase> getPhrases() {
        List<Phrase> phrases = new ArrayList<>();
        for (Card card : cards) {
            if (card instanceof Phrase) {
                phrases.add((Phrase) card);
            }
        }
        return phrases;
    }

    // =====================================================================
    //  FOR QUIZZES AND SPACED REPETITION
    // =====================================================================

    /** Cards due for review on the given date, lowest box (hardest) first. */
    public List<Card> getDueCards(LocalDate today) {
        List<Card> due = new ArrayList<>();
        for (Card card : cards) {
            if (card.isDue(today)) {
                due.add(card);
            }
        }
        due.sort((a, b) -> Integer.compare(a.getBox(), b.getBox()));
        return due;
    }

    /** Cards due for review today. */
    public List<Card> getDueCards() {
        return getDueCards(LocalDate.now());
    }

    public int countDueCards(LocalDate today) {
        return getDueCards(today).size();
    }

    /** All cards in a random order. The deck itself is NOT changed. */
    public List<Card> getShuffledCards() {
        List<Card> copy = new ArrayList<>(cards);
        Collections.shuffle(copy, RANDOM);
        return copy;
    }

    /**
     * Up to 'count' different random cards (fewer if the deck is smaller).
     * Handy for Matching questions or picking wrong options.
     */
    public List<Card> getRandomCards(int count) {
        List<Card> shuffled = getShuffledCards();
        return new ArrayList<>(shuffled.subList(0, Math.min(count, shuffled.size())));
    }

    /**
     * Random cards that are NOT the given card - useful as wrong answer choices.
     */
    public List<Card> getOtherRandomCards(Card exclude, int count) {
        List<Card> others = new ArrayList<>();
        for (Card card : getShuffledCards()) {
            if (!card.equals(exclude)) {
                others.add(card);
            }
            if (others.size() == count) {
                break;
            }
        }
        return others;
    }

    /** Sets every card back to box 1, due today. */
    public void resetAllProgress() {
        for (Card card : cards) {
            card.resetProgress();
        }
    }

    // =====================================================================
    //  DISPLAY
    // =====================================================================

    /** Prints every card in the deck, numbered. */
    public void printCards() {
        System.out.println("=== " + this + " ===");
        if (cards.isEmpty()) {
            System.out.println("  (no cards yet)");
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + cards.get(i));
        }
    }

    /** Example: "Twi Basics (Twi) - 12 cards" */
    @Override
    public String toString() {
        return name + " (" + language + ") - " + cards.size()
                + (cards.size() == 1 ? " card" : " cards");
    }
}
