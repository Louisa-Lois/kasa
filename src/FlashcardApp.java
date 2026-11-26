import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class FlashcardApp extends Application {

    private BorderPane root;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();

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

        root.setTop(menu);
        showScreen(decks);

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

    public static void main(String[] args) {
        launch(args);
    }
}