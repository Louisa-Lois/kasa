import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DeckLibrary keeps ALL the decks in the app, like a bookshelf of decks.
 *
 * Decks are stored in a Map so we can find one quickly by its name
 * (names are matched without caring about capital letters).
 *
 * Main things you can do:
 *   library.loadSampleDecks()             ready-made decks for Twi, Ewe, Ga, Swahili
 *   library.addDeck(deck)                 add your own deck
 *   library.getDeck("Twi Basics")         find a deck by name
 *   library.getDecksByLanguage(Language.GA)
 *   library.saveToFile("decks.txt")       save every deck + progress
 *   library.loadFromFile("decks.txt")     load them back next time
 */
public class DeckLibrary {

    private static final String SEPARATOR_REGEX = "\\|";

    // LinkedHashMap keeps decks in the order they were added.
    private final Map<String, Deck> decks = new LinkedHashMap<>();

    // =====================================================================
    //  MANAGING DECKS
    // =====================================================================

    /**
     * @return true if added, false if a deck with the same name already exists
     */
    public boolean addDeck(Deck deck) {
        if (deck == null) {
            throw new IllegalArgumentException("Cannot add an empty deck.");
        }
        String key = keyFor(deck.getName());
        if (decks.containsKey(key)) {
            return false;
        }
        decks.put(key, deck);
        return true;
    }

    /**
     * Creates a new empty deck, adds it, and returns it so you can add cards.
     * @throws IllegalArgumentException if the name is already used
     */
    public Deck createDeck(String name, Language language, String description) {
        Deck deck = new Deck(name, language, description);
        if (!addDeck(deck)) {
            throw new IllegalArgumentException("A deck called \"" + name + "\" already exists.");
        }
        return deck;
    }

    /** Finds a deck by name (ignores capital letters). Returns null if not found. */
    public Deck getDeck(String name) {
        if (name == null) {
            return null;
        }
        return decks.get(keyFor(name));
    }

    public boolean hasDeck(String name) {
        return getDeck(name) != null;
    }

    /** @return true if the deck existed and was removed */
    public boolean removeDeck(String name) {
        if (name == null) {
            return false;
        }
        return decks.remove(keyFor(name)) != null;
    }

    /** Every deck, in the order they were added. */
    public List<Deck> getAllDecks() {
        return new ArrayList<>(decks.values());
    }

    /** Only the decks for one language. */
    public List<Deck> getDecksByLanguage(Language language) {
        List<Deck> result = new ArrayList<>();
        for (Deck deck : decks.values()) {
            if (deck.getLanguage() == language) {
                result.add(deck);
            }
        }
        return result;
    }

    public List<String> getDeckNames() {
        List<String> names = new ArrayList<>();
        for (Deck deck : decks.values()) {
            names.add(deck.getName());
        }
        return names;
    }

    /** Number of decks. */
    public int size() {
        return decks.size();
    }

    public boolean isEmpty() {
        return decks.isEmpty();
    }

    public void clear() {
        decks.clear();
    }

    /** Total number of cards across every deck. */
    public int getTotalCardCount() {
        int total = 0;
        for (Deck deck : decks.values()) {
            total += deck.size();
        }
        return total;
    }

    /** Every card (from every deck) that is due on the given date. */
    public List<Card> getAllDueCards(LocalDate today) {
        List<Card> due = new ArrayList<>();
        for (Deck deck : decks.values()) {
            due.addAll(deck.getDueCards(today));
        }
        return due;
    }

    /** Prints a short list of all decks. */
    public void printSummary() {
        System.out.println("=== Deck Library: " + size() + " decks, "
                + getTotalCardCount() + " cards ===");
        int number = 1;
        for (Deck deck : decks.values()) {
            System.out.println("  " + number + ". " + deck);
            number++;
        }
    }

    private static String keyFor(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    // =====================================================================
    //  SAVING AND LOADING
    //
    //  File format (one item per line, fields separated by "|"):
    //
    //    # comment lines start with #
    //    DECK|Twi Basics|TWI|Everyday Twi words
    //    WORD|water|nsuo|noun|Mepɛ nsuo.|1|2026-11-25
    //    PHRASE|How are you?|Ɛte sɛn?|greeting||1|2026-11-25
    //    END
    //
    //  Files are read and written as UTF-8 so letters like ɛ, ɔ, ŋ, ƒ, ɖ survive.
    // =====================================================================

    /** Saves every deck and every card (including spaced-repetition progress). */
    public void saveToFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("# Local Language Flashcard Trainer - saved decks");
            writer.newLine();
            for (Deck deck : decks.values()) {
                writer.write("DECK|" + deck.getName() + "|" + deck.getLanguage().name()
                        + "|" + deck.getDescription());
                writer.newLine();
                for (Card card : deck.getCards()) {
                    writer.write(card.toFileLine()); // polymorphism: Word or Phrase decides
                    writer.newLine();
                }
                writer.write("END");
                writer.newLine();
            }
        }
    }

    /**
     * Loads decks from a file and adds them to this library.
     * Bad lines are skipped with a warning instead of crashing the program.
     * If a deck with the same name already exists, the cards are added to it.
     *
     * @return the number of cards loaded
     */
    public int loadFromFile(String filePath) throws IOException {
        int cardsLoaded = 0;
        int lineNumber = 0;
        Deck currentDeck = null;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.replace("﻿", "").trim(); // remove Windows "BOM" mark if present
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR_REGEX, -1);
                String type = parts[0].trim().toUpperCase(Locale.ROOT);

                try {
                    if (type.equals("DECK")) {
                        currentDeck = readDeckLine(parts);
                    } else if (type.equals("END")) {
                        currentDeck = null;
                    } else if (type.equals("WORD") || type.equals("PHRASE")) {
                        if (currentDeck == null) {
                            throw new IllegalArgumentException("card is not inside a DECK");
                        }
                        Card card = parseCard(parts, currentDeck.getLanguage());
                        if (currentDeck.addCard(card)) {
                            cardsLoaded++;
                        }
                    } else {
                        throw new IllegalArgumentException("unknown line type \"" + parts[0] + "\"");
                    }
                } catch (IllegalArgumentException | DateTimeParseException e) {
                    System.out.println("Warning: skipped line " + lineNumber + " (" + e.getMessage() + ")");
                }
            }
        }
        return cardsLoaded;
    }

    private Deck readDeckLine(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("DECK line needs at least a name and a language");
        }
        String name = parts[1];
        Language language = Language.fromString(parts[2]);
        String description = parts.length > 3 ? parts[3] : "";

        Deck existing = getDeck(name);
        if (existing != null) {
            if (existing.getLanguage() != language) {
                throw new IllegalArgumentException("deck \"" + name
                        + "\" already exists in a different language");
            }
            return existing;
        }
        return createDeck(name, language, description);
    }

    /**
     * Turns one saved line back into a Word or Phrase.
     * Format: TYPE|english|translation|extra1|extra2|box|nextReviewDate
     * (box and date are optional - a new card is made if they are missing)
     */
    public static Card parseCard(String[] parts, Language language) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("a card needs at least English and translation");
        }
        String type = parts[0].trim().toUpperCase(Locale.ROOT);
        String english = parts[1];
        String translation = parts[2];
        String extra1 = parts.length > 3 ? parts[3] : "";
        String extra2 = parts.length > 4 ? parts[4] : "";

        Card card;
        if (type.equals("WORD")) {
            card = new Word(english, translation, language, extra1, extra2);
        } else if (type.equals("PHRASE")) {
            card = new Phrase(english, translation, language, extra1, extra2);
        } else {
            throw new IllegalArgumentException("unknown card type \"" + parts[0] + "\"");
        }

        if (parts.length > 5 && !parts[5].trim().isEmpty()) {
            try {
                card.setBox(Integer.parseInt(parts[5].trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("box must be a number, but was \"" + parts[5] + "\"");
            }
        }
        if (parts.length > 6 && !parts[6].trim().isEmpty()) {
            card.setNextReviewDate(LocalDate.parse(parts[6].trim()));
        }
        return card;
    }

    // =====================================================================
    //  SAMPLE DECKS
    //  Translations were checked against published sources (Nov 2026):
    //  Harvard ELIAS Twi lessons, Omniglot, Wikipedia, CIEE Ghana, Ghana
    //  curriculum materials, Northwestern/Baylor Swahili courses.
    //  Spellings still vary between dialects - a native speaker check is a bonus.
    // =====================================================================

    /** Adds one starter deck for each language (skips any that already exist). */
    public void loadSampleDecks() {
        addDeck(createTwiDeck());
        addDeck(createEweDeck());
        addDeck(createGaDeck());
        addDeck(createSwahiliDeck());
    }

    private static Deck createTwiDeck() {
        Language l = Language.TWI;
        Deck deck = new Deck("Twi Basics", l, "Everyday Twi words and greetings");
        deck.addCard(new Word("water", "nsuo", l, "noun", "Mepɛ nsuo."));
        deck.addCard(new Word("food", "aduane", l, "noun", ""));
        deck.addCard(new Word("house", "fie", l, "noun", ""));
        deck.addCard(new Word("friend", "adamfo", l, "noun", ""));
        deck.addCard(new Word("thank you", "medaase", l, "expression", ""));
        deck.addCard(new Word("welcome", "akwaaba", l, "greeting", ""));
        deck.addCard(new Word("good morning", "maakye", l, "greeting", ""));
        deck.addCard(new Phrase("How are you?", "Ɛte sɛn?", l, "greeting", "How is it?"));
        deck.addCard(new Phrase("What is your name?", "Wo din de sɛn?", l, "introductions", "Your name is what?"));
        deck.addCard(new Phrase("My name is Kofi", "Me din de Kofi", l, "introductions", ""));
        deck.addCard(new Phrase("Thank you very much", "Medaase paa", l, "polite", ""));
        deck.addCard(new Phrase("Please", "Mepa wo kyɛw", l, "polite", "I beg you"));
        return deck;
    }

    private static Deck createEweDeck() {
        Language l = Language.EWE;
        Deck deck = new Deck("Ewe Basics", l, "Everyday Ewe words and greetings");
        deck.addCard(new Word("water", "tsi", l, "noun", ""));
        deck.addCard(new Word("food", "nuɖuɖu", l, "noun", ""));
        deck.addCard(new Word("house", "aƒe", l, "noun", ""));
        deck.addCard(new Word("friend", "xɔlɔ̃", l, "noun", ""));
        deck.addCard(new Word("thank you", "akpe", l, "expression", ""));
        deck.addCard(new Word("welcome", "woezɔ", l, "greeting", ""));
        deck.addCard(new Word("good morning", "ŋdi", l, "greeting", ""));
        deck.addCard(new Word("please", "meɖekuku", l, "expression", ""));
        deck.addCard(new Phrase("How are you?", "Efoa?", l, "greeting", "Did you wake up well?"));
        deck.addCard(new Phrase("Good morning to you all", "Ŋdi na mi", l, "greeting", ""));
        deck.addCard(new Phrase("My name is Kofi", "Ŋkɔnye nye Kofi", l, "introductions", ""));
        deck.addCard(new Phrase("Thank you all", "Akpe na mi katã", l, "polite", ""));
        return deck;
    }

    private static Deck createGaDeck() {
        Language l = Language.GA;
        Deck deck = new Deck("Ga Basics", l, "Everyday Ga words and greetings");
        deck.addCard(new Word("water", "nu", l, "noun", ""));
        deck.addCard(new Word("food", "niyenii", l, "noun", ""));
        deck.addCard(new Word("house", "shia", l, "noun", ""));
        deck.addCard(new Word("welcome", "atuu", l, "greeting", ""));
        deck.addCard(new Word("come", "ba", l, "verb", ""));
        deck.addCard(new Word("thank you", "oyiwaladɔŋ", l, "expression", ""));
        deck.addCard(new Word("please", "ofainɛ", l, "expression", ""));
        deck.addCard(new Phrase("How are you?", "Te oyɔɔ tɛŋŋ?", l, "greeting", ""));
        deck.addCard(new Phrase("What is your name?", "Mɛni ji ogbɛi?", l, "introductions", ""));
        deck.addCard(new Phrase("My name is Kofi", "Atsɛɔ mi Kofi", l, "introductions", "They call me Kofi"));
        return deck;
    }

    private static Deck createSwahiliDeck() {
        Language l = Language.SWAHILI;
        Deck deck = new Deck("Swahili Basics", l, "Everyday Swahili words and greetings");
        deck.addCard(new Word("water", "maji", l, "noun", "Ninataka maji."));
        deck.addCard(new Word("food", "chakula", l, "noun", ""));
        deck.addCard(new Word("house", "nyumba", l, "noun", ""));
        deck.addCard(new Word("friend", "rafiki", l, "noun", ""));
        deck.addCard(new Word("thank you", "asante", l, "expression", ""));
        deck.addCard(new Word("welcome", "karibu", l, "greeting", ""));
        deck.addCard(new Word("please", "tafadhali", l, "expression", ""));
        deck.addCard(new Phrase("How are you?", "Habari yako?", l, "greeting", "Your news?"));
        deck.addCard(new Phrase("Good morning", "Habari za asubuhi", l, "greeting", "News of the morning"));
        deck.addCard(new Phrase("My name is Kofi", "Jina langu ni Kofi", l, "introductions", ""));
        deck.addCard(new Phrase("Thank you very much", "Asante sana", l, "polite", ""));
        deck.addCard(new Phrase("Goodbye", "Kwa heri", l, "farewell", ""));
        return deck;
    }
}
