# Cards and Decks — Josephine Yeboah-Kodie

My part of the Local Language Flashcard Trainer (Twi, Ewe, Ga, Swahili).

## Files

| File | What it is |
|---|---|
| `Language.java` | Enum of the 4 languages: `TWI`, `EWE`, `GA`, `SWAHILI` |
| `Card.java` | **Abstract** parent class for every flashcard |
| `Word.java` | A single word card (extends `Card`) |
| `Phrase.java` | A phrase/sentence card (extends `Card`), with fill-in-the-blank helpers |
| `Deck.java` | A named group of cards in one language |
| `DeckLibrary.java` | Holds all decks; sample decks; save/load to a text file |
| `CardsAndDecksDemo.java` | Runs 31 PASS/FAIL checks on everything above |

## How the classes connect

```
                 Card  (abstract)
          ┌───────┴────────┐
        Word             Phrase
          └───────┬────────┘
                  │ many cards in
                Deck  (one Language)
                  │ many decks in
             DeckLibrary
```

## Run it

From inside this folder:

```
javac -encoding UTF-8 *.java
java CardsAndDecksDemo
```

(`-encoding UTF-8` makes sure letters like ɛ, ɔ, ŋ, ƒ, ɖ compile properly on Windows.
In IntelliJ, set File → Settings → Editor → File Encodings to UTF-8.)
If the console shows `?` instead of ɛ, that is only the Windows console font — the data is fine.

## OOP ideas used (for the report)

- **Abstraction** – `Card` is abstract; it has abstract methods `getCardType()`, `getHint()`, `toFileLine()`.
- **Inheritance** – `Word` and `Phrase` extend `Card` and reuse its fields and methods.
- **Polymorphism** – a `List<Card>` holds Words and Phrases; calling `getHint()` gives a different hint for each. `DeckLibrary.saveToFile` calls `card.toFileLine()` without knowing the type.
- **Encapsulation** – all fields are private; `Deck.getCards()` returns a read-only list so cards can only be changed through `addCard`/`removeCard`, which check the rules (same language, no duplicates).

## How teammates use my classes

```java
DeckLibrary library = new DeckLibrary();
library.loadSampleDecks();                       // Twi, Ewe, Ga, Swahili starter decks
Deck deck = library.getDeck("Twi Basics");

// QuizSession
List<Card> toReview = deck.getDueCards();        // due today, hardest (lowest box) first
List<Card> all = deck.getShuffledCards();        // random order, deck not changed

// Translation question
Card c = deck.getCard(0);
c.getEnglish();                                  // "water"
c.checkTranslation(userAnswer);                  // forgiving: "ete sen" == "Ɛte sɛn?"
c.getHint();

// Matching question
List<Card> four = deck.getRandomCards(4);
List<Card> wrongOptions = deck.getOtherRandomCards(c, 3);

// FillBlank question (only for Phrases)
for (Phrase p : deck.getPhrases()) {
    p.getTranslationWithBlank(1);                // "Wo ____ de sɛn?"
    p.getWordAt(1);                              // "din"  (the answer)
}

// Spaced-repetition scheduler
c.getBox();  c.setBox(3);                        // box 1 (new) to 5 (well known)
c.getNextReviewDate();  c.setNextReviewDate(LocalDate.now().plusDays(4));
c.isDue();

// Saving progress
library.saveToFile("decks.txt");
library.loadFromFile("decks.txt");
```

All errors (empty text, wrong language, box outside 1–5) throw `IllegalArgumentException` with a clear message.

## Translations

The sample decks in `DeckLibrary` were checked against published sources
(Harvard ELIAS Twi lessons, Omniglot, Wikipedia, CIEE Ghana, Ghana curriculum materials,
Northwestern and Baylor Swahili courses). Spellings vary between dialects, so a quick
check by a native speaker (especially for Ga) is still a good idea.
To change one, edit the matching line in `createTwiDeck()`, `createEweDeck()`, etc.
