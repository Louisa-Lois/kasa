import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Starter Decks screen. Shows every deck, and the cards of the selected deck.
 *
 * HOW TO USE THIS FILE AS A TEMPLATE FOR YOUR OWN SCREEN:
 *   1. Copy it and rename the file and the class (for example QuizScreen).
 *   2. Keep "implements Screen" and the getView() method.
 *   3. Do not add a "package" line at the top.
 *   4. Change what getView() builds and returns.
 *
 * Josephine owns the Decks screen, so she can change or replace this one.
 */
public class DecksScreen implements Screen {

    private final DeckLibrary library;

    public DecksScreen(DeckLibrary library) {
        this.library = library;
    }

    @Override
    public Parent getView() {
        Label decksTitle = new Label("Decks");
        decksTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ListView<Deck> deckList = new ListView<>(
                FXCollections.observableArrayList(library.getAllDecks()));

        Label cardsTitle = new Label("Select a deck to see its cards");
        cardsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ListView<Card> cardList = new ListView<>();

        // When a deck is clicked, show its cards on the right
        deckList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldDeck, newDeck) -> {
                    if (newDeck == null) {
                        cardsTitle.setText("Select a deck to see its cards");
                        cardList.getItems().clear();
                    } else {
                        cardsTitle.setText(newDeck.getName() + " (" + newDeck.getLanguage() + ")");
                        cardList.getItems().setAll(newDeck.getCards());
                    }
                });

        VBox left = new VBox(10, decksTitle, deckList);
        left.setPrefWidth(300);
        VBox.setVgrow(deckList, Priority.ALWAYS);

        VBox right = new VBox(10, cardsTitle, cardList);
        VBox.setVgrow(cardList, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox layout = new HBox(20, left, right);
        layout.setPadding(new Insets(20));
        return layout;
    }
}
