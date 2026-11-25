# Kasa

A desktop flashcard trainer for Twi, Ewe, Ga and Swahili, built for CS 213
Object-Oriented Programming at Ashesi University by Group B12.

Kasa helps students at Ashesi University learn everyday words and phrases in
each other's languages, so Ghanaian and international students can meet each
other halfway.

## Team

| Member | Part they own |
|---|---|
| Josephine Yeboah-Kodie | Cards and decks |
| Emmanuel Tayviah | Question types |
| Louisa-Lois Adjoka | Quiz, scoring and spaced repetition |
| Gerald Boateng Boaitey | Saving, loading and the main window |

## Running it

Requires JDK 17 or later.

```
javac -encoding UTF-8 -d out src/*.java
java -Dfile.encoding=UTF-8 -cp out CardsAndDecksDemo
```

Everything must be UTF-8, or ɛ, ɔ, ŋ, ƒ and ɖ corrupt. Set your editor to
UTF-8 as well. If the Windows console prints `?` instead of ɛ, that is the
console font rather than the data.

## Branches

`main` always compiles. Everything else is a short branch named
`<name>/<what-it-does>`, merged through a reviewed pull request.
