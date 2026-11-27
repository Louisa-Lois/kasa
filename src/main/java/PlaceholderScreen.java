import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class PlaceholderScreen implements Screen {

    private final String name;

    public PlaceholderScreen(String name) {
        this.name = name;
    }

    @Override
    public Parent getView() {
        Label label = new Label(name + " screen goes here");
        return new StackPane(label);
    }
}