package com.pro.quizgame1;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.sql.*;

public class MainApp extends Application {

    private int currentUserId = -1; // Stores the ID of the currently logged-in user

    public static void main(String[] args) {
        launch(args); // Launches the JavaFX application
    }

    // Connects to the SQLite database
    private Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:quiz_game.db");
        } catch (Exception e) {
            showAlert("Database Error", e.getMessage());
            return null;
        }
    }

    // Creates database tables if they do not exist
    private void setupDatabase() {
        try (Connection conn = connect()) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS scores (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, level TEXT, score INTEGER, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");
        } catch (Exception e) {
            showAlert("DB Setup Error", e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        setupDatabase(); // Set up database on start

        // UI elements for login and registration
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");

        VBox layout = new VBox(10, usernameField, passwordField, loginButton, registerButton);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        // Set button actions
        loginButton.setOnAction(e -> handleLogin(usernameField.getText(), passwordField.getText(), primaryStage));
        registerButton.setOnAction(e -> handleRegister(usernameField.getText(), passwordField.getText()));

        Scene scene = new Scene(layout, 300, 250);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Handles user login logic
    private void handleLogin(String username, String password, Stage stage) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Username and Password cannot be empty.");
            return;
        }
        try (Connection conn = connect()) {
            String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("id"); // Save logged-in user ID
                showGameMenu(stage); // Show main menu
            } else {
                showAlert("Login Failed", "Incorrect username or password.");
            }
        } catch (Exception e) {
            showAlert("Login Error", e.getMessage());
        }
    }

    // Handles user registration logic
    private void handleRegister(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Username and Password cannot be empty.");
            return;
        }
        try (Connection conn = connect()) {
            String sql = "INSERT INTO users(username, password) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            showAlert("Success", "Registration successful. Please login.");
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                showAlert("Registration Error", "Username already exists.");
            } else {
                showAlert("Registration Error", e.getMessage());
            }
        } catch (Exception e) {
            showAlert("Registration Error", e.getMessage());
        }
    }

    // Displays the difficulty selection menu
    private void showGameMenu(Stage stage) {
        Button easyBtn = new Button("Easy");
        Button mediumBtn = new Button("Medium");
        Button hardBtn = new Button("Hard");
        Button logoutBtn = new Button("Logout");
        Button showScoresBtn = new Button("Show Scores"); // New button

        // Set actions for difficulty buttons
        easyBtn.setOnAction(e -> startQuiz(stage, "Easy"));
        mediumBtn.setOnAction(e -> startQuiz(stage, "Medium"));
        hardBtn.setOnAction(e -> startQuiz(stage, "Hard"));
        logoutBtn.setOnAction(e -> {
            currentUserId = -1; // Reset user ID on logout
            start(stage); // Return to login screen
        });
        showScoresBtn.setOnAction(e -> showScoresWindow()); // Action for new button

        VBox menuLayout = new VBox(15, new Label("Select Difficulty"), easyBtn, mediumBtn, hardBtn, showScoresBtn, logoutBtn);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(20));

        Scene menuScene = new Scene(menuLayout, 300, 300);
        stage.setScene(menuScene);
    }

    // Starts the quiz
    private void startQuiz(Stage stage, String level) {
        playQuiz(stage, level);
    }

    // Displays and runs the quiz
    private void playQuiz(Stage stage, String level) {
        // Define questions for each difficulty
        String[][] easyQuestions = {
                {"What is 2 + 2?", "3", "4", "5", "6", "4"},
                {"What color is the sky?", "Blue", "Green", "Red", "Yellow", "Blue"},
                {"What is the capital of the USA?", "New York", "Washington DC", "Los Angeles", "Chicago", "Washington DC"},
                {"How many days are there in a week?", "5", "6", "7", "8", "7"},
                {"Which animal barks?", "Cat", "Dog", "Cow", "Horse", "Dog"},
                {"Which fruit is yellow and sour?", "Apple", "Banana", "Lemon", "Orange", "Lemon"},
                {"How many legs does a spider have?", "6", "8", "10", "12", "8"},
                {"What do bees produce?", "Milk", "Honey", "Wax", "Silk", "Honey"}
        };
        String[][] mediumQuestions = {
                {"What is the capital of France?", "Berlin", "Madrid", "Paris", "Rome", "Paris"},
                {"Which planet is known as the Red Planet?", "Earth", "Venus", "Mars", "Jupiter", "Mars"},
                {"What is the chemical symbol for water?", "O2", "CO2", "H2O", "NaCl", "H2O"},
                {"Who painted the Mona Lisa?", "Van Gogh", "Picasso", "Da Vinci", "Michelangelo", "Da Vinci"},
                {"What is the largest ocean on Earth?", "Atlantic", "Indian", "Pacific", "Arctic", "Pacific"},
                {"What gas do plants absorb?", "Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen", "Carbon Dioxide"},
                {"How many continents are there?", "5", "6", "7", "8", "7"}
        };
        String[][] hardQuestions = {
                {"What is the square root of 144?", "10", "11", "12", "13", "12"},
                {"Who developed general relativity?", "Newton", "Einstein", "Tesla", "Hawking", "Einstein"},
                {"What is the chemical formula of table salt?", "NaCl", "KCl", "NaOH", "HCl", "NaCl"},
                {"Which element has the atomic number 26?", "Iron", "Gold", "Silver", "Copper", "Iron"},
                {"What is the name of the longest river in the world?", "Amazon", "Nile", "Yangtze", "Mississippi", "Nile"},
                {"In which year did World War II end?", "1942", "1945", "1939", "1950", "1945"},
                {"Who is known as the father of modern computers?", "Charles Babbage", "Alan Turing", "John Von Neumann", "Bill Gates", "Charles Babbage"}
        };

        // Select questions based on chosen level
        String[][] selectedQuestions = switch (level) {
            case "Easy" -> easyQuestions;
            case "Medium" -> mediumQuestions;
            case "Hard" -> hardQuestions;
            default -> easyQuestions;
        };

        // Shuffle questions randomly
        List<String[]> questionList = new ArrayList<>(Arrays.asList(selectedQuestions));
        Collections.shuffle(questionList);
        selectedQuestions = questionList.toArray(new String[0][]);

        final int[] index = {0}; // Current question index
        final int[] score = {0}; // User score

        Label questionLabel = new Label();
        ToggleGroup optionsGroup = new ToggleGroup();
        VBox optionsBox = new VBox(5);
        Button nextBtn = new Button("Next");
        nextBtn.setDisable(true); // Disable until an option is selected

        VBox quizBox = new VBox(10, questionLabel, optionsBox, nextBtn);
        quizBox.setPadding(new Insets(20));
        quizBox.setAlignment(Pos.CENTER);

        Scene quizScene = new Scene(quizBox, 400, 300);

        String[][] finalSelectedQuestions = selectedQuestions;

        // Loads a new question to the screen
        Runnable loadQuestion = () -> {
            if (index[0] < finalSelectedQuestions.length) {
                String[] q = finalSelectedQuestions[index[0]];
                questionLabel.setText("Question " + (index[0] + 1) + ": " + q[0]);
                optionsBox.getChildren().clear();
                optionsGroup.getToggles().clear();
                nextBtn.setDisable(true);

                for (int i = 1; i <= 4; i++) {
                    RadioButton rb = new RadioButton(q[i]);
                    rb.setToggleGroup(optionsGroup);
                    optionsBox.getChildren().add(rb);
                }

                optionsGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                    nextBtn.setDisable(newToggle == null);
                });
            } else {
                showFinalScore(stage, score[0], level); // Show final score when done
            }
        };

        String[][] finalSelectedQuestions1 = selectedQuestions;
        nextBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) optionsGroup.getSelectedToggle();
            if (selected != null && selected.getText().equals(finalSelectedQuestions1[index[0]][5])) {
                score[0]++; // Increase score for correct answer
            }
            index[0]++;
            loadQuestion.run();
        });

        stage.setScene(quizScene);
        loadQuestion.run(); // Start quiz
    }

    // Shows final score and saves it to the database
    private void showFinalScore(Stage stage, int score, String level) {
        Label label = new Label("You scored: " + score);
        Button backButton = new Button("Back to Menu");

        backButton.setOnAction(e -> showGameMenu(stage));

        VBox layout = new VBox(10, label, backButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        // Save score with user id
        if (currentUserId != -1) {
            try (Connection conn = connect()) {
                String sql = "INSERT INTO scores(user_id, level, score) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, currentUserId);
                stmt.setString(2, level);
                stmt.setInt(3, score);
                stmt.executeUpdate();
            } catch (Exception e) {
                showAlert("DB Error", e.getMessage());
            }
        }

        stage.setScene(new Scene(layout, 300, 200));
    }

    // Displays alerts to the user
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Method to show all users and scores in a new window
    private void showScoresWindow() {
        Stage scoreStage = new Stage();
        scoreStage.setTitle("All User Scores");

        TableView<ScoreEntry> table = new TableView<>();

        TableColumn<ScoreEntry, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<ScoreEntry, String> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));

        TableColumn<ScoreEntry, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));

        table.getColumns().addAll(userCol, levelCol, scoreCol);

        // Load data from DB
        try (Connection conn = connect()) {
            String sql = "SELECT users.username, scores.level, scores.score FROM scores " +
                    "JOIN users ON users.id = scores.user_id " +
                    "ORDER BY users.username";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String level = rs.getString("level");
                int score = rs.getInt("score");
                table.getItems().add(new ScoreEntry(username, level, score));
            }
        } catch (Exception e) {
            showAlert("DB Error", e.getMessage());
        }

        VBox root = new VBox(table);
        root.setPadding(new Insets(10));
        root.setPrefSize(400, 300);

        scoreStage.setScene(new Scene(root));
        scoreStage.show();
    }

    // Helper class for table entries
    public static class ScoreEntry {
        private final SimpleStringProperty username;
        private final SimpleStringProperty level;
        private final SimpleIntegerProperty score;

        public ScoreEntry(String username, String level, int score) {
            this.username = new SimpleStringProperty(username);
            this.level = new SimpleStringProperty(level);
            this.score = new SimpleIntegerProperty(score);
        }

        public String getUsername() {
            return username.get();
        }

        public void setUsername(String username) {
            this.username.set(username);
        }

        public StringProperty usernameProperty() {
            return username;
        }

        public String getLevel() {
            return level.get();
        }

        public void setLevel(String level) {
            this.level.set(level);
        }

        public StringProperty levelProperty() {
            return level;
        }

        public int getScore() {
            return score.get();
        }

        public void setScore(int score) {
            this.score.set(score);
        }

        public IntegerProperty scoreProperty() {
            return score;
        }
    }
}
