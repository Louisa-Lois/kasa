import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Runs and tests the "Cards and Decks" part on its own.
 *
 * How to run (from inside this folder):
 *     javac -encoding UTF-8 *.java
 *     java CardsAndDecksDemo
 *
 * Each check prints PASS or FAIL so you can see everything works.
 */
public class CardsAndDecksDemo {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("##### 1. Making cards (Word and Phrase) #####");
        Word water = new Word("water", "nsuo", Language.TWI, "noun", "Mepɛ nsuo.");
        Phrase howAreYou = new Phrase("How are you?", "Ɛte sɛn?", Language.TWI, "greeting", "How is it?");
        System.out.println(water);
        System.out.println(howAreYou);

        System.out.println("\n##### 2. Polymorphism: one list, different behaviour #####");
        Card[] cards = { water, howAreYou };
        for (Card card : cards) {
            System.out.println(card.getCardType() + " hint -> " + card.getHint());
        }
        check("Word type", water.getCardType().equals("Word"));
        check("Phrase type", howAreYou.getCardType().equals("Phrase"));

        System.out.println("\n##### 3. Forgiving answer checking #####");
        check("'NSUO' accepted for nsuo", water.checkTranslation("NSUO"));
        check("'ete sen' accepted for Ɛte sɛn?", howAreYou.checkTranslation("ete sen"));
        check("'Ɛte sɛn' accepted for Ɛte sɛn?", howAreYou.checkTranslation("Ɛte sɛn"));
        check("'aduane' rejected for nsuo", !water.checkTranslation("aduane"));
        check("English check works", water.checkEnglish(" Water "));

        System.out.println("\n##### 4. Phrase fill-in-the-blank helpers #####");
        Phrase name = new Phrase("What is your name?", "Wo din de sɛn?", Language.TWI);
        System.out.println("Words: " + name.getTranslationWords());
        System.out.println("Blank word 1: " + name.getTranslationWithBlank(1) + "   (answer: " + name.getWordAt(1) + ")");
        System.out.println("Blank word 3: " + name.getTranslationWithBlank(3) + "   (answer: " + name.getWordAt(3) + ")");
        check("word count is 4", name.getWordCount() == 4);
        check("blank keeps '?'", name.getTranslationWithBlank(3).equals("Wo din de ____?"));

        System.out.println("\n##### 5. Deck rules #####");
        Deck deck = new Deck("My Twi Deck", Language.TWI, "Practice deck");
        check("add water", deck.addCard(water));
        check("add phrase", deck.addCard(howAreYou));
        check("duplicate is refused", !deck.addCard(new Word("Water", "Nsuo", Language.TWI)));
        try {
            deck.addCard(new Word("water", "maji", Language.SWAHILI));
            check("wrong language is refused", false);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
            check("wrong language is refused", true);
        }
        try {
            new Word("", "nsuo", Language.TWI);
            check("empty English is refused", false);
        } catch (IllegalArgumentException e) {
            check("empty English is refused", true);
        }
        deck.printCards();
        check("deck has 2 cards", deck.size() == 2);
        check("1 word, 1 phrase", deck.getWords().size() == 1 && deck.getPhrases().size() == 1);
        check("find by English", deck.findByEnglish("how are you") == howAreYou);

        System.out.println("\n##### 6. Spaced repetition data #####");
        LocalDate today = LocalDate.now();
        water.setBox(3);
        water.setNextReviewDate(today.plusDays(4));
        check("water not due today", !water.isDue(today));
        check("phrase due today", howAreYou.isDue(today));
        check("1 card due today", deck.getDueCards(today).size() == 1);
        check("water due in 4 days", deck.getDueCards(today.plusDays(4)).size() == 2);
        try {
            water.setBox(9);
            check("box 9 is refused", false);
        } catch (IllegalArgumentException e) {
            check("box 9 is refused", true);
        }

        System.out.println("\n##### 7. Deck library with sample decks #####");
        DeckLibrary library = new DeckLibrary();
        library.loadSampleDecks();
        library.addDeck(deck);
        library.printSummary();
        check("5 decks", library.size() == 5);
        check("find deck ignoring capitals", library.getDeck("twi basics") != null);
        check("2 Twi decks", library.getDecksByLanguage(Language.TWI).size() == 2);
        check("same name refused", !library.addDeck(new Deck("Ga Basics", Language.GA)));

        Deck swahili = library.getDeck("Swahili Basics");
        List<Card> matchingCards = swahili.getRandomCards(4);
        System.out.println("4 random Swahili cards for a Matching question:");
        for (Card card : matchingCards) {
            System.out.println("   " + card.getEnglish() + "  <->  " + card.getTranslation());
        }
        check("random cards = 4", matchingCards.size() == 4);

        System.out.println("\n##### 8. Save and load #####");
        String file = "decks_test_save.txt";
        library.saveToFile(file);
        DeckLibrary loaded = new DeckLibrary();
        int count = loaded.loadFromFile(file);
        System.out.println("Loaded " + count + " cards from " + file);
        check("same number of cards after loading", count == library.getTotalCardCount());
        Card loadedWater = loaded.getDeck("My Twi Deck").findByEnglish("water");
        check("box saved", loadedWater.getBox() == 3);
        check("review date saved", loadedWater.getNextReviewDate().equals(today.plusDays(4)));
        check("special letters saved", loaded.getDeck("Ewe Basics").findByEnglish("house")
                .getTranslation().equals("aƒe"));

        System.out.println("\n========================================");
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
    }

    private static void check(String description, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS - " + description);
        } else {
            failed++;
            System.out.println("  FAIL - " + description);
        }
    }
}
