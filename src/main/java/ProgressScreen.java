import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shows the learner how far along they are, per deck: how many cards each
 * deck has, how many are due today, and how many sit in each Leitner box.
 *
 * <p><b>When the numbers are worked out:</b> every time the Progress button
 * is clicked. The application creates this screen once but calls
 * {@link #getView()} on every click, and all counting happens inside
 * getView() - so after a quiz, clicking away and back shows the new numbers.
 * Nothing is counted in the constructor, or the numbers would freeze at
 * application start.
 *
 * <p><b>Reading the boxes:</b> cards piling up in box 1 means the learner is
 * struggling with them; cards reaching box 5 means the spacing is working.
 */
public class ProgressScreen implements Screen {

    private static final String TITLE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold;";
    private static final String HEADER_STYLE = "-fx-font-weight: bold; -fx-text-fill: #555555;";
    private static final String TOTAL_STYLE = "-fx-font-weight: bold;";
    private static final String NOTE_STYLE = "-fx-text-fill: #555555; -fx-font-size: 13px;";

    /** One colour per box: warm (still learning) to cool (well known). */
    private static final String[] BOX_COLOURS =
            {"#C0504D", "#E08E45", "#E6C14A", "#7FAF5A", "#3F8F6B"};

    private static final double BAR_WIDTH = 160;

    private final DeckLibrary library;

    /**
     * @param library the decks to report on
     * @throws IllegalArgumentException if library is null
     */
    public ProgressScreen(DeckLibrary library) {
        if (library == null) {
            throw new IllegalArgumentException("The Progress screen needs a deck library.");
        }
        this.library = library;
    }

    /**
     * Builds the screen from the library as it is right now.
     *
     * @return the screen's layout
     */
    @Override
    public Parent getView() {
        LocalDate today = LocalDate.now();
        List<Deck> decks = library.getAllDecks();

        Label title = new Label("Your progress");
        title.setStyle(TITLE_STYLE);

        VBox layout = new VBox(16, title);
        layout.setPadding(new Insets(20));

        if (decks.isEmpty()) {
            Label empty = new Label("There are no decks yet, so there is no progress to show.");
            empty.setStyle(NOTE_STYLE);
            layout.getChildren().add(empty);
            return layout;
        }

        // Overall numbers come straight from the library.
        int totalCards = library.getTotalCardCount();
        int totalDue = library.getAllDueCards(today).size();
        List<Card> allCards = new ArrayList<>();
        for (Deck deck : decks) {
            allCards.addAll(deck.getCards());
        }
        int[] totalBoxes = countCardsPerBox(allCards);

        Label overall = new Label(totalCards + " cards in " + decks.size()
                + (decks.size() == 1 ? " deck" : " decks") + "  ·  "
                + totalDue + " due today");
        overall.setStyle("-fx-font-size: 15px;");

        Label note = new Label(describeProgress(totalBoxes, totalCards));
        note.setStyle(NOTE_STYLE);
        note.setWrapText(true);

        layout.getChildren().addAll(overall, note, buildTable(decks, today, totalBoxes, totalCards),
                buildLegend());

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: white;");
        return scroll;
    }

    // =====================================================================
    //  COUNTING (public and static so they can be tested without opening a
    //  window; ProgressScreenTest calls them directly)
    // =====================================================================

    /**
     * Counts how many cards are in each Leitner box.
     *
     * @param cards the cards to count
     * @return an array of five counts: index 0 is box 1, index 4 is box 5
     * @throws IllegalArgumentException if cards is null
     */
    public static int[] countCardsPerBox(Collection<? extends Card> cards) {
        if (cards == null) {
            throw new IllegalArgumentException("Cannot count boxes for an empty list of cards.");
        }
        int[] counts = new int[Card.MAX_BOX - Card.MIN_BOX + 1];
        for (Card card : cards) {
            counts[card.getBox() - Card.MIN_BOX]++;
        }
        return counts;
    }

    /**
     * A one-line, plain-language reading of the box counts.
     *
     * @param boxCounts five counts, as returned by {@link #countCardsPerBox}
     * @param totalCards the number of cards counted
     * @return a sentence for the screen
     * @throws IllegalArgumentException if boxCounts is not five counts
     */
    public static String describeProgress(int[] boxCounts, int totalCards) {
        int boxes = Card.MAX_BOX - Card.MIN_BOX + 1;
        if (boxCounts == null || boxCounts.length != boxes) {
            throw new IllegalArgumentException("Expected " + boxes + " box counts, but got "
                    + (boxCounts == null ? "none" : String.valueOf(boxCounts.length)));
        }
        if (totalCards == 0) {
            return "No cards yet.";
        }
        if (boxCounts[0] == totalCards) {
            return "Every card is still in box 1. Take a quiz to start moving them up.";
        }
        int learnt = boxCounts[boxCounts.length - 1];
        int percentPastBox1 = (int) Math.round(100.0 * (totalCards - boxCounts[0]) / totalCards);
        String text = percentPastBox1 + "% of your cards have moved past box 1.";
        if (learnt > 0) {
            text += " " + learnt + (learnt == 1 ? " card has" : " cards have") + " reached box 5.";
        }
        return text;
    }

    // =====================================================================
    //  LAYOUT
    // =====================================================================

    private GridPane buildTable(List<Deck> decks, LocalDate today, int[] totalBoxes, int totalCards) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(8);

        ColumnConstraints nameColumn = new ColumnConstraints();
        nameColumn.setMinWidth(150);
        grid.getColumnConstraints().add(nameColumn);

        String[] headers = {"Deck", "Cards", "Due", "Box 1", "Box 2", "Box 3", "Box 4", "Box 5", ""};
        for (int col = 0; col < headers.length; col++) {
            Label header = new Label(headers[col]);
            header.setStyle(HEADER_STYLE);
            grid.add(header, col, 0);
            if (col > 0) {
                GridPane.setHalignment(header, HPos.CENTER);
            }
        }

        int row = 1;
        for (Deck deck : decks) {
            int[] boxes = countCardsPerBox(deck.getCards());
            addRow(grid, row++, deck.getName() + " (" + deck.getLanguage() + ")",
                    deck.size(), deck.getDueCards(today).size(), boxes, false);
        }
        addRow(grid, row, "All decks", totalCards,
                library.getAllDueCards(today).size(), totalBoxes, true);
        return grid;
    }

    private void addRow(GridPane grid, int row, String name, int cards, int due,
                        int[] boxes, boolean isTotal) {
        List<Label> cells = new ArrayList<>();
        cells.add(new Label(name));
        cells.add(new Label(String.valueOf(cards)));
        cells.add(new Label(String.valueOf(due)));
        for (int count : boxes) {
            cells.add(new Label(String.valueOf(count)));
        }
        for (int col = 0; col < cells.size(); col++) {
            Label cell = cells.get(col);
            if (isTotal) {
                cell.setStyle(TOTAL_STYLE);
            }
            grid.add(cell, col, row);
            if (col > 0) {
                GridPane.setHalignment(cell, HPos.CENTER);
            }
        }
        grid.add(buildBoxBar(boxes, cards), cells.size(), row);
    }

    /** A thin bar split into five coloured parts, one per box, sized by count. */
    private HBox buildBoxBar(int[] boxes, int total) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinWidth(BAR_WIDTH);
        bar.setMaxWidth(BAR_WIDTH);
        bar.setStyle("-fx-background-color: #EEEEEE; -fx-background-radius: 4;");
        if (total == 0) {
            bar.setPrefHeight(10);
            return bar;
        }
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == 0) {
                continue;
            }
            Region part = new Region();
            double width = BAR_WIDTH * boxes[i] / total;
            part.setMinSize(width, 10);
            part.setPrefSize(width, 10);
            part.setStyle("-fx-background-color: " + BOX_COLOURS[i] + ";");
            bar.getChildren().add(part);
        }
        HBox.setHgrow(bar, Priority.NEVER);
        return bar;
    }

    private HBox buildLegend() {
        HBox legend = new HBox(14);
        legend.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < BOX_COLOURS.length; i++) {
            Region swatch = new Region();
            swatch.setMinSize(12, 12);
            swatch.setStyle("-fx-background-color: " + BOX_COLOURS[i] + "; -fx-background-radius: 2;");
            Label label = new Label("Box " + (i + 1)
                    + (i == 0 ? " (learning)" : i == BOX_COLOURS.length - 1 ? " (known)" : ""));
            label.setStyle(NOTE_STYLE);
            legend.getChildren().add(new HBox(5, swatch, label));
        }
        return legend;
    }
}
