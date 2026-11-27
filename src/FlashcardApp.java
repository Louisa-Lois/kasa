import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class FlashcardApp extends Application {

    // Colours and styles in one place, so they are easy to change
    private static final String MAROON = "#8C2B3A";

    private static final String BAR_STYLE =
            "-fx-background-color: " + MAROON + ";";

    private static final String BUTTON_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: white;"
            + " -fx-font-size: 14px; -fx-padding: 8 16 8 16;"
            + " -fx-background-radius: 6; -fx-cursor: hand;";

    private static final String HOVER_STYLE =
            "-fx-background-color: rgba(255,255,255,0.22); -fx-text-fill: white;"
            + " -fx-font-size: 14px; -fx-padding: 8 16 8 16;"
            + " -fx-background-radius: 6; -fx-cursor: hand;";

    private static final String ACTIVE_STYLE =
            "-fx-background-color: white; -fx-text-fill: " + MAROON + ";"
            + " -fx-font-size: 14px; -fx-font-weight: bold;"
            + " -fx-padding: 8 16 8 16; -fx-background-radius: 6; -fx-cursor: hand;";

    private static final String STATUS_STYLE =
            "-fx-background-color: #F6EEEF; -fx-text-fill: #555555;"
            + " -fx-font-size: 12px; -fx-padding: 8 16 8 16;";

    private BorderPane root;
    private FileManager fileManager;
    private DeckLibrary library;
    private Button activeButton;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        // Load the saved decks (or the sample decks on the first run)
        fileManager = new FileManager();
        library = fileManager.load();
        System.out.println(fileManager.getLastMessage());

        Screen decks = new DecksScreen(library);
        Screen study = new PlaceholderScreen("Study");
        Screen quiz = new PlaceholderScreen("Quiz");
        Screen progress = new PlaceholderScreen("Progress");
        Screen settings = new PlaceholderScreen("Settings");

        Label title = new Label("Flashcard Trainer");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        title.setPadding(new Insets(0, 20, 0, 0));

        Button decksButton = createMenuButton("Decks", decks);

        HBox menu = new HBox(8);
        menu.setPadding(new Insets(12, 16, 12, 16));
        menu.setAlignment(Pos.CENTER_LEFT);
        menu.setStyle(BAR_STYLE);
        menu.getChildren().addAll(
                title,
                decksButton,
                createMenuButton("Study", study),
                createMenuButton("Quiz", quiz),
                createMenuButton("Progress", progress),
                createMenuButton("Settings", settings)
        );

        // A status line at the bottom shows what happened with the file
        Label status = new Label(fileManager.getLastMessage());
        status.setMaxWidth(Double.MAX_VALUE);
        status.setStyle(STATUS_STYLE);

        root.setTop(menu);
        root.setBottom(status);
        showScreen(decks);
        setActive(decksButton);

        // Save when the window is closed
        stage.setOnCloseRequest(event -> {
            fileManager.save(library);
            System.out.println(fileManager.getLastMessage());
        });

        Scene scene = new Scene(root, 900, 560);
        stage.setTitle("Local Language Flashcard Trainer");
        stage.setScene(scene);
        stage.show();
    }

    private Button createMenuButton(String text, Screen screen) {
        Button button = new Button(text);
        button.setStyle(BUTTON_STYLE);

        button.setOnAction(event -> {
            showScreen(screen);
            setActive(button);
        });

        // Hover effect, but never on the active button
        button.setOnMouseEntered(event -> {
            if (button != activeButton) {
                button.setStyle(HOVER_STYLE);
            }
        });
        button.setOnMouseExited(event -> {
            if (button != activeButton) {
                button.setStyle(BUTTON_STYLE);
            }
        });
        return button;
    }

    /** Highlights the button of the screen that is showing. */
    private void setActive(Button button) {
        if (activeButton != null) {
            activeButton.setStyle(BUTTON_STYLE);
        }
        activeButton = button;
        activeButton.setStyle(ACTIVE_STYLE);
    }

    private void showScreen(Screen screen) {
        root.setCenter(screen.getView());
    }

    /** Teammates' screens can use this to read and change the decks. */
    public DeckLibrary getLibrary() {
        return library;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
