import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class FlashcardApp extends Application {

    private BorderPane root;
    private FileManager fileManager;
    private DeckLibrary library;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();

        // Load the saved decks (or the sample decks on the first run)
        fileManager = new FileManager();
        library = fileManager.load();
        System.out.println(fileManager.getLastMessage());

        Screen decks = new WelcomeScreen();
        Screen study = new PlaceholderScreen("Study");
        Screen quiz = new PlaceholderScreen("Quiz");
        Screen progress = new PlaceholderScreen("Progress");
        Screen settings = new PlaceholderScreen("Settings");

        HBox menu = new HBox(10);
        menu.setPadding(new Insets(10));
        menu.getChildren().addAll(
                createMenuButton("Decks", decks),
                createMenuButton("Study", study),
                createMenuButton("Quiz", quiz),
                createMenuButton("Progress", progress),
                createMenuButton("Settings", settings)
        );

        // A status line at the bottom shows what happened with the file
        Label status = new Label(fileManager.getLastMessage());
        status.setPadding(new Insets(8));

        root.setTop(menu);
        root.setBottom(status);
        showScreen(decks);

        // Save when the window is closed
        stage.setOnCloseRequest(event -> {
            fileManager.save(library);
            System.out.println(fileManager.getLastMessage());
        });

        Scene scene = new Scene(root, 800, 500);
        stage.setTitle("Local Language Flashcard Trainer");
        stage.setScene(scene);
        stage.show();
    }

    private Button createMenuButton(String text, Screen screen) {
        Button button = new Button(text);
        button.setOnAction(event -> showScreen(screen));
        return button;
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