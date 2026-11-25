/**
 * The four local languages our flashcard trainer supports.
 *
 * An enum is a fixed list of values. Using it (instead of plain Strings like "twi")
 * stops typos such as "Twii" from ever getting into our decks.
 *
 * Example:  Language lang = Language.TWI;
 *           System.out.println(lang);   // prints "Twi"
 */
public enum Language {
    TWI("Twi"),
    EWE("Ewe"),
    GA("Ga"),
    SWAHILI("Swahili");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    /** The nice name to show on screen, e.g. "Swahili". */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Turns text such as "twi", "Twi" or "TWI" into Language.TWI.
     * Useful when reading a language from the keyboard or from a file.
     *
     * @throws IllegalArgumentException if the text is not one of our four languages
     */
    public static Language fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Language cannot be empty.");
        }
        String cleaned = text.trim();
        for (Language language : Language.values()) {
            if (language.name().equalsIgnoreCase(cleaned)
                    || language.displayName.equalsIgnoreCase(cleaned)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unknown language: \"" + text
                + "\". Use Twi, Ewe, Ga or Swahili.");
    }

    @Override
    public String toString() {
        return displayName;
    }
}
