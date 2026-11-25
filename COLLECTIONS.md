# Collections, and why each one

Collection choice and efficiency rationale is 15% of the project grade.
Add to this as each collection goes in — reconstructing the reasoning in
December is harder and sounds rehearsed.

| Where | Collection | Why this one |
|---|---|---|
| `Deck.cards` | `ArrayList<Card>` | Insertion order matters for display, and index access is needed when drawing random cards for a Matching question. |
| `DeckLibrary.decks` | `LinkedHashMap<String, Deck>` | Decks are looked up by name constantly — O(1) here instead of scanning a list. LinkedHashMap also preserves insertion order, so the library always displays the same way. |

## Still to justify

- **Finding due cards.** `Deck.getDueCards()` scans every card then sorts by
  box. A `PriorityQueue<Card>` ordered by next review date answers "what is
  due soonest" in O(1) instead of O(n log n) per lookup. Worth doing if there
  is time, and worth being able to discuss either way.
- **An `EnumMap<Language, List<Deck>>` in `DeckLibrary`**, alongside the name
  map. The key set is fixed at compile time, so an EnumMap is array-backed
  and indexed by ordinal, with no hashing at all.
- **A `HashSet`** when building Matching questions, so the same distractor
  cannot be drawn twice and the answer cannot appear among its own options.
