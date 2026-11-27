import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class WelcomeScreen implements Screen {

    @Override
    public Parent getView() {
        Label title = new Label("Welcome to the Flashcard Trainer");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Learn Twi, Ewe, Ga and Swahili");
        subtitle.setStyle("-fx-font-size: 16px;");

        VBox box = new VBox(10, title, subtitle);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}