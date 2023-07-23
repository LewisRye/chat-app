import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GUI extends Application {
    private Scene mainScene;
    private Stage stage;
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;
    private TextArea displayMessage;
    private TextArea displayUsers;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            new Thread(() -> { // thread responsible for the data handling
                try {
                    client = new Socket("localhost", 65000);
                    out = new PrintWriter(client.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    while (client.isConnected()) {
                        String data = in.readLine();

                        if (data.startsWith("/userlist")) {
                            displayUsers(data); // changes the user list when server suggests a user has joined / left
                        }

                        else {
                            displayMessage(data);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not establish a connection with the server. " +
                            "If you are running this server locally, try running the 'Server.java' file. ");
                    Platform.exit();
                }
            }).start();

            Platform.runLater(() -> { // thread responsible for UI
                stage = primaryStage;

                BorderPane loginPane = new BorderPane();
                VBox items = new VBox();
                Scene loginScene = new Scene(loginPane);
                MenuBar menuBar = new MenuBar();
                Menu file = new Menu("File");
                MenuItem exit = new MenuItem("Exit"); exit.setOnAction(e -> Platform.exit());
                file.getItems().addAll(exit);
                menuBar.getMenus().addAll(file);

                Label welcome = new Label("Welcome, choose a username.");
                TextField username = new TextField(); username.setPromptText("Username"); username.setMaxSize(150, 10);
                username.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        String input = username.getText();

                        if (input.length() < 3 || input.length() > 12 || input.startsWith("/")) {
                            Alert a = new Alert(Alert.AlertType.ERROR, "Your username must be between 3 and 12 characters \n" +
                                    "and must not begin with '/'.");
                            a.showAndWait();
                        } else {
                            sendMessage(input);
                            stage.setScene(mainScene);
                        }
                    }
                });
                Button set = new Button("Set Username"); set.setOnAction(e -> {
                    String input = username.getText();

                    if (input.length() < 3 || input.length() > 12 || input.startsWith("/")) {
                        Alert a = new Alert(Alert.AlertType.ERROR, "Your username must be between 3 and 12 characters \n" +
                                "and must not begin with '/'.");
                        a.showAndWait();
                    } else {
                        sendMessage(input);
                        stage.setScene(mainScene);
                    }
                });

                loginPane.setTop(menuBar);
                items.getChildren().addAll(welcome, username, set);
                items.setAlignment(Pos.CENTER);
                items.setSpacing(10);
                loginPane.setCenter(items);

                stage.setWidth(1000);
                stage.setHeight(750);
                stage.setMinWidth(750);
                stage.setMinHeight(500);

                stage.setTitle("Chat App");
                stage.getIcons().add(new Image("icon.png"));
                stage.setScene(loginScene);
                stage.show();

                stage.setOnCloseRequest(e -> shutdown());

                BorderPane mainPane = new BorderPane();
                mainScene = new Scene(mainPane);
                mainPane.setTop(createMenu());
                mainPane.setCenter(createContent());
            });
        } catch (Exception e) {
            System.err.println("Unable to establish a connection to the server.");
            Platform.exit();
        }
    }

    private MenuBar createMenu() {
        MenuBar menu = new MenuBar();

        Menu file = new Menu("File");
        MenuItem exit = new MenuItem("Leave"); exit.setOnAction(e -> sendMessage("/quit"));
        file.getItems().addAll(exit);

        menu.getMenus().addAll(file);
        return menu;
    }

    private BorderPane createContent() {
        BorderPane pane = new BorderPane();

        VBox leftContent = new VBox();
        Label header = new Label("Dashboard");
        header.setStyle("-fx-font-weight: bold");
        Button home = new Button("Home");
        Button friends = new Button("Friends");
        leftContent.getChildren().addAll(header, home, friends);
        leftContent.setPrefWidth(150);
        leftContent.setSpacing(10);

        BorderPane centerContent = new BorderPane();
        TextField input = new TextField(); input.setPromptText("Message the chat");
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage(input.getText());
                input.clear();
            }
        });
        displayMessage = new TextArea("Welcome. Type /quit to leave.\n\n");
        displayMessage.setEditable(false);
        ScrollPane scroll = new ScrollPane(displayMessage);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setCursor(Cursor.DEFAULT);
        centerContent.setTop(input);
        centerContent.setCenter(scroll);
        centerContent.setPrefWidth(650);

        VBox rightContent = new VBox();
        Label connected = new Label("Connected Users:");
        connected.setStyle("-fx-font-weight: bold");
        displayUsers = new TextArea("{users}");
        displayUsers.setEditable(false);
        ScrollPane scroll2 = new ScrollPane(displayUsers);
        scroll2.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll2.setFitToWidth(true);
        scroll2.setFitToHeight(true);
        scroll2.setCursor(Cursor.DEFAULT);
        scroll2.setStyle("-fx-background-color: transparent");
        rightContent.getChildren().addAll(connected, scroll2);
        rightContent.setPrefWidth(200);
        rightContent.setSpacing(10);

        HBox content = new HBox();
        content.setSpacing(10);
        content.getChildren().addAll(leftContent, centerContent, rightContent);
        pane.setCenter(content);

        stage.widthProperty().addListener((obs, oldVal, newVal) ->
            centerContent.setPrefWidth(centerContent.getPrefWidth() + (newVal.doubleValue() - oldVal.doubleValue())));

        return pane;
    }

    private void sendMessage(String message) {
        if (message.equals("/quit")) {
            out.println(message);
            Platform.exit();
            System.exit(0);
        } else {
            out.println(message);
        }
    }

    private void shutdown() {
        try {
            if (!client.isClosed()) {
                sendMessage("/quit");
                client.close();
            }
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Could not establish a connection with the server.");
            a.showAndWait();
            Platform.exit();
        }
        Platform.exit();
    }

    public void displayMessage(String message) {
        displayMessage.appendText("\n" + message);
        displayMessage.setScrollTop(Double.MAX_VALUE); // scrolls to the bottom when a new message is sent
    }

    public void displayUsers(String message) {
        displayUsers.setText("");
        String[] split = message.split(", "); // finds each username in the list

        for (int i = 1; i < split.length; i++) {
            displayUsers.appendText(split[i] + "\n"); // adds each username to the user list UI
        }
    }
}
