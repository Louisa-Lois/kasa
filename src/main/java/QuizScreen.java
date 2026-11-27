import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Puts a QuizSession on screen: shows a question, takes an answer, shows
 * feedback, and moves on.
 *
 * <h2>Navigating away mid-quiz (decided deliberately)</h2>
 * The application creates this screen once and calls {@link #getView()} every
 * time the Quiz button is clicked. The running {@link QuizSession} is kept in a
 * FIELD, not built inside getView(), so getView() only decides what to show:
 * <ul>
 *   <li>no session yet: a deck chooser and a Start button;</li>
 *   <li>a session running: the current question (or feedback on the last one);</li>
 *   <li>a session finished: the summary and a "New quiz" button;</li>
 *   <li>nothing due: a message saying when the next card is due.</li>
 * </ul>
 * So a learner who clicks Progress three questions in and then comes back
 * resumes exactly where they were. "End quiz" abandons a session on purpose.
 *
 * <h2>Text box or buttons?</h2>
 * A MatchingQuestion offers fixed options, so it gets one button per option.
 * Every other question type gets a text box. This is the one place that
 * checks the question's type, and only to choose the INPUT - grading is
 * always left to the question itself.
 */
public class QuizScreen implements Screen {

    private static final String TITLE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold;";
    private static final String TYPE_STYLE = "-fx-text-fill: #8C2B3A; -fx-font-weight: bold;";
    private static final String PROMPT_STYLE = "-fx-font-size: 22px;";
    private static final String MUTED_STYLE = "-fx-text-fill: #555555;";
    private static final String RIGHT_STYLE = "-fx-text-fill: #2E7D32; -fx-font-size: 18px; -fx-font-weight: bold;";
    private static final String WRONG_STYLE = "-fx-text-fill: #B23A3A; -fx-font-size: 18px; -fx-font-weight: bold;";
    private static final String PRIMARY_STYLE = "-fx-background-color: #8C2B3A; -fx-text-fill: white;"
            + " -fx-font-size: 14px; -fx-padding: 8 18 8 18; -fx-background-radius: 6; -fx-cursor: hand;";
    private static final String OPTION_STYLE = "-fx-font-size: 15px; -fx-padding: 10 18 10 18;"
            + " -fx-background-radius: 6; -fx-cursor: hand;";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM");

    /** The five things this screen can show. Returned by {@link #viewFor}. */
    public static final String VIEW_START = "start";
    public static final String VIEW_QUESTION = "question";
    public static final String VIEW_FEEDBACK = "feedback";
    public static final String VIEW_SUMMARY = "summary";
    public static final String VIEW_NOTHING_DUE = "nothing due";

    private final DeckLibrary library;

    /** The quiz in progress, kept between clicks. Null when none has been started. */
    private QuizSession session;
    /** Name of the deck last chosen, so the chooser remembers it. */
    private String selectedDeckName;
    /** True while showing "right / wrong" for the answer just given. */
    private boolean showingFeedback;
    /** What the learner submitted last, for the feedback message. */
    private String lastGivenAnswer;
    /** Whether the hint is showing for the current question. */
    private boolean hintShown;

    /** The container everything is drawn into; rebuilt on each getView(). */
    private VBox root;

    /**
     * @param library the decks the learner can be quizzed on
     * @throws IllegalArgumentException if library is null
     */
    public QuizScreen(DeckLibrary library) {
        if (library == null) {
            throw new IllegalArgumentException("The Quiz screen needs a deck library.");
        }
        this.library = library;
    }

    /**
     * Builds the screen for whatever state the quiz is in. Does not start or
     * restart a quiz.
     *
     * @return the screen's layout
     */
    @Override
    public Parent getView() {
        root = new VBox(16);
        root.setPadding(new Insets(24));
        render();
        return root;
    }

    // =====================================================================
    //  CHOOSING WHAT TO SHOW
    // =====================================================================

    /**
     * Decides which view to show for a given state. Kept separate from the
     * drawing, and static, so the decision can be tested without a window.
     *
     * @param session         the quiz in progress, or null if none was started
     * @param showingFeedback true right after an answer, before "Next" is pressed
     * @return one of the VIEW_ constants
     */
    public static String viewFor(QuizSession session, boolean showingFeedback) {
        if (session == null) {
            return VIEW_START;
        }
        if (showingFeedback && session.getLastQuestion() != null) {
            return VIEW_FEEDBACK;
        }
        if (session.hadNothingDue()) {
            return VIEW_NOTHING_DUE;
        }
        if (session.isFinished()) {
            return VIEW_SUMMARY;
        }
        return VIEW_QUESTION;
    }

    /**
     * Whether a question is answered by clicking an option button rather than
     * typing. Only Matching questions have fixed options.
     *
     * @param question the question to be shown
     * @return true for option buttons, false for a text box
     */
    public static boolean usesOptionButtons(Question question) {
        return question instanceof MatchingQuestion;
    }

    /**
     * The message for a deck with nothing due, naming the next review date.
     *
     * @param session a session that had nothing due
     * @return a sentence for the screen
     * @throws IllegalArgumentException if session is null
     */
    public static String describeNothingDue(QuizSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Cannot describe an empty quiz session.");
        }
        LocalDate next = session.getNextReviewDate();
        if (next == null) {
            return session.getDeck().getName() + " has no cards yet.";
        }
        return "Every card in " + session.getDeck().getName() + " is scheduled for later. "
                + "The next one is due on " + next.format(DATE_FORMAT) + ".";
    }

    /** Redraws the screen from the current state. Called after every action. */
    private void render() {
        root.getChildren().clear();
        switch (viewFor(session, showingFeedback)) {
            case VIEW_START:
                showStart();
                break;
            case VIEW_FEEDBACK:
                showFeedback();
                break;
            case VIEW_NOTHING_DUE:
                showNothingDue();
                break;
            case VIEW_SUMMARY:
                showSummary();
                break;
            default:
                showQuestion();
        }
    }

    // =====================================================================
    //  START: choose a deck
    // =====================================================================

    private void showStart() {
        Label title = new Label("Quiz");
        title.setStyle(TITLE_STYLE);

        List<Deck> decks = library.getAllDecks();
        if (decks.isEmpty()) {
            Label none = new Label("There are no decks to quiz on yet.");
            none.setStyle(MUTED_STYLE);
            root.getChildren().addAll(title, none);
            return;
        }

        Label intro = new Label("Choose a deck. You will be asked about the cards that are due today, "
                + "hardest first.");
        intro.setWrapText(true);
        intro.setStyle(MUTED_STYLE);

        LocalDate today = LocalDate.now();
        ComboBox<Deck> chooser = new ComboBox<>();
        chooser.getItems().setAll(decks);
        chooser.setCellFactory(list -> new DeckCell(today));
        chooser.setButtonCell(new DeckCell(today));
        chooser.setPrefWidth(320);
        chooser.getSelectionModel().select(findDeck(decks, selectedDeckName));

        Button start = new Button("Start quiz");
        start.setStyle(PRIMARY_STYLE);
        start.setDefaultButton(true);
        start.setOnAction(event -> {
            Deck deck = chooser.getValue();
            if (deck == null) {
                return;
            }
            selectedDeckName = deck.getName();
            startQuiz(deck);
        });

        root.getChildren().addAll(title, intro, new HBox(12, chooser, start));
    }

    /** The deck with that name, or the first deck if none matches. */
    private static Deck findDeck(List<Deck> decks, String name) {
        for (Deck deck : decks) {
            if (deck.getName().equals(name)) {
                return deck;
            }
        }
        return decks.get(0);
    }

    private void startQuiz(Deck deck) {
        session = new QuizSession(deck);
        showingFeedback = false;
        hintShown = false;
        render();
    }

    // =====================================================================
    //  A QUESTION
    // =====================================================================

    private void showQuestion() {
        Question question = session.getCurrentQuestion();

        Label progress = new Label(progressText());
        progress.setStyle(MUTED_STYLE);

        Label type = new Label(question.getQuestionType().toUpperCase()
                + (session.isCurrentRetry() ? "  ·  SECOND TRY (NOT SCORED)" : ""));
        type.setStyle(TYPE_STYLE);

        Label prompt = new Label(question.getPrompt());
        prompt.setStyle(PROMPT_STYLE);
        prompt.setWrapText(true);

        Label hint = new Label("Hint: " + question.getCard().getHint());
        hint.setStyle(MUTED_STYLE);
        hint.setWrapText(true);
        hint.setVisible(hintShown);
        hint.setManaged(hintShown);

        // Polymorphism: Word and Phrase each give their own kind of hint.
        Button hintButton = new Button(hintShown ? "Hide hint" : "Show hint");
        hintButton.setOnAction(event -> {
            hintShown = !hintShown;
            render();
        });

        Button endButton = new Button("End quiz");
        endButton.setOnAction(event -> {
            session.endEarly();
            render();
        });

        root.getChildren().addAll(progress, type, prompt, buildAnswerInput(question), hint,
                new HBox(10, hintButton, endButton));
    }

    /** One button per option for Matching; a text box for everything else. */
    private Parent buildAnswerInput(Question question) {
        if (usesOptionButtons(question)) {
            FlowPane options = new FlowPane(10, 10);
            for (String option : ((MatchingQuestion) question).getOptions()) {
                Button button = new Button(option);
                button.setStyle(OPTION_STYLE);
                button.setOnAction(event -> submit(option));
                options.getChildren().add(button);
            }
            return options;
        }

        TextField field = new TextField();
        field.setPromptText("Type your answer");
        field.setPrefWidth(320);
        field.setStyle("-fx-font-size: 15px;");

        Label warning = new Label("");
        warning.setStyle("-fx-text-fill: #B23A3A;");

        Button check = new Button("Check");
        check.setStyle(PRIMARY_STYLE);

        Runnable submitTyped = () -> {
            if (field.getText() == null || field.getText().trim().isEmpty()) {
                warning.setText("Type an answer first, or use the hint.");
                return;
            }
            submit(field.getText());
        };
        check.setOnAction(event -> submitTyped.run());
        field.setOnAction(event -> submitTyped.run());   // Enter key
        Platform.runLater(field::requestFocus);

        HBox row = new HBox(10, field, check);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, row, warning);
    }

    private void submit(String answer) {
        lastGivenAnswer = answer;
        session.submitAnswer(answer);
        showingFeedback = true;
        hintShown = false;
        render();
    }

    // =====================================================================
    //  FEEDBACK after an answer
    // =====================================================================

    private void showFeedback() {
        Question answered = session.getLastQuestion();
        boolean correct = session.wasLastCorrect();

        Label verdict = new Label(correct ? "Correct!" : "Not quite.");
        verdict.setStyle(correct ? RIGHT_STYLE : WRONG_STYLE);

        Label prompt = new Label(answered.getPrompt());
        prompt.setStyle(MUTED_STYLE);
        prompt.setWrapText(true);

        VBox details = new VBox(6, prompt);
        if (!correct) {
            details.getChildren().add(new Label("You answered: " + lastGivenAnswer));
            Label right = new Label("The answer is: " + answered.getCorrectAnswer());
            right.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            details.getChildren().add(right);
        }
        Card card = answered.getCard();
        Label full = new Label(card.getTranslation() + "  =  " + card.getEnglish());
        full.setStyle(MUTED_STYLE);
        details.getChildren().add(full);

        Label score = new Label("Score: " + session.getScoreTracker().getSummary());
        score.setStyle(MUTED_STYLE);

        Button next = new Button(session.isFinished() ? "See results" : "Next question");
        next.setStyle(PRIMARY_STYLE);
        next.setDefaultButton(true);   // Enter moves on
        next.setOnAction(event -> {
            showingFeedback = false;
            render();
        });
        Platform.runLater(next::requestFocus);

        root.getChildren().addAll(verdict, details, score, next);
    }

    // =====================================================================
    //  END STATES
    // =====================================================================

    private void showSummary() {
        Label title = new Label("Quiz finished: " + session.getDeck().getName());
        title.setStyle(TITLE_STYLE);

        ScoreTracker tracker = session.getScoreTracker();
        Label summary = new Label(tracker.getDetailedSummary());
        summary.setStyle("-fx-font-size: 16px;");
        summary.setWrapText(true);

        Label skipped = new Label("");
        int unanswered = session.getTotalQuestions() - session.getAnsweredCount();
        if (unanswered > 0) {
            skipped.setText(unanswered + (unanswered == 1 ? " card was" : " cards were")
                    + " not reached and will still be due.");
            skipped.setStyle(MUTED_STYLE);
        }

        Label note = new Label("Cards you got right will come back later; cards you missed "
                + "are back in box 1 and due tomorrow.");
        note.setStyle(MUTED_STYLE);
        note.setWrapText(true);

        root.getChildren().addAll(title, summary, skipped, note, newQuizButton());
    }

    private void showNothingDue() {
        Label title = new Label("Nothing due today");
        title.setStyle(TITLE_STYLE);

        Label message = new Label(describeNothingDue(session));
        message.setWrapText(true);

        Label tip = new Label("Try another deck, or come back then. Spacing reviews out is "
                + "what makes them stick.");
        tip.setStyle(MUTED_STYLE);
        tip.setWrapText(true);

        root.getChildren().addAll(title, message, tip, newQuizButton());
    }

    private Button newQuizButton() {
        Button again = new Button("Choose a deck");
        again.setStyle(PRIMARY_STYLE);
        again.setOnAction(event -> {
            session = null;
            render();
        });
        return again;
    }

    private String progressText() {
        int done = session.getAnsweredCount();
        int total = session.getTotalQuestions();
        String text = session.getDeck().getName() + "  ·  ";
        if (session.isCurrentRetry()) {
            text += "second tries";
        } else {
            text += "question " + (done + 1) + " of " + total;
        }
        if (done > 0) {
            text += "  ·  " + session.getScoreTracker().getSummary();
        }
        return text;
    }

    // =====================================================================
    //  DECK CHOOSER CELL
    // =====================================================================

    /** Shows "Twi Basics (Twi) - 12 due" in the deck chooser. */
    private static class DeckCell extends ListCell<Deck> {
        private final LocalDate today;

        DeckCell(LocalDate today) {
            this.today = today;
        }

        @Override
        protected void updateItem(Deck deck, boolean empty) {
            super.updateItem(deck, empty);
            if (empty || deck == null) {
                setText(null);
            } else {
                int due = deck.countDueCards(today);
                setText(deck.getName() + " (" + deck.getLanguage() + ")  -  "
                        + (due == 0 ? "nothing due" : due + " due"));
            }
        }
    }
}
