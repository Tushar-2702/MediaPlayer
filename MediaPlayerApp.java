package main.java.com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerApp extends Application {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private final boolean repeat = false;
    private boolean stopRequested = false;
    private boolean atEndOfMedia = false;
    private Duration duration;
    private Slider timeSlider;
    private Label playTime;
    private Slider volumeSlider;
    private VBox mediaBar;
    private final List<String> playlist = new ArrayList<>();
    private int currentTrackIndex = 0;
    private Label currentlyPlaying;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        // Center content - Media view
        StackPane mediaViewPane = new StackPane();
        mediaView = new MediaView();
        mediaViewPane.getChildren().add(mediaView);
        mediaViewPane.setStyle("-fx-background-color: black;");
        root.setCenter(mediaViewPane);

        // Add playlist view on right
        VBox playlistPane = createPlaylistPane();
        root.setRight(playlistPane);

        // Add control bar
        mediaBar = createMediaBar();
        root.setBottom(mediaBar);

        // Create menu
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        // Create scene
        Scene scene = new Scene(root, 1000, 600);
        try {
            // First try with package path
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            try {
                // Then try with direct path (for working directory)
                String css = new File("style.css").toURI().toString();
                scene.getStylesheets().add(css);
                System.out.println("Loaded CSS from: " + css);
            } catch (Exception ex) {
                System.out.println("Style sheet not found, using inline styles.");
            }
        }

        // Apply inline styles as fallback
        applyInlineStyles(root);

        // Configure stage
        primaryStage.setTitle("JavaFX Media Player");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Apply inline styles as a fallback if CSS file is not found
    private void applyInlineStyles(BorderPane root) {
        // You can add specific inline styles here if needed
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #1e1e1e;");

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open File...");
        openItem.setOnAction(e -> openFile(primaryStage));
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        
        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), exitItem);

        // Playback menu
        Menu playbackMenu = new Menu("Playback");
        MenuItem playItem = new MenuItem("Play");
        playItem.setOnAction(e -> {
            if (mediaPlayer != null) {
                MediaPlayer.Status status = mediaPlayer.getStatus();
                if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) {
                    return;
                }
                if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.READY || status == MediaPlayer.Status.STOPPED) {
                    mediaPlayer.play();
                }
            }
        });
        
        MenuItem pauseItem = new MenuItem("Pause");
        pauseItem.setOnAction(e -> {
            if (mediaPlayer != null) {
                MediaPlayer.Status status = mediaPlayer.getStatus();
                if (status == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                }
            }
        });
        
        MenuItem stopItem = new MenuItem("Stop");
        stopItem.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
        });
        
        playbackMenu.getItems().addAll(playItem, pauseItem, stopItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("About JavaFX Media Player");
            alert.setHeaderText("JavaFX Media Player");
            alert.setContentText("A simple media player built with JavaFX and Java 8.");
            alert.showAndWait();
        });
        
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, playbackMenu, helpMenu);
        return menuBar;
    }

    private VBox createMediaBar() {
        HBox mediaBar = new HBox();
        mediaBar.setAlignment(Pos.CENTER);
        mediaBar.setPadding(new Insets(5, 10, 5, 10));
        mediaBar.setStyle("-fx-background-color: #1e1e1e;");
        
        // Play/Pause Button
        final Button playButton = new Button(">");
        playButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 40px; -fx-min-height: 30px; -fx-background-radius: 3;");
        playButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                MediaPlayer.Status status = mediaPlayer.getStatus();
                
                if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) {
                    return;
                }
                
                if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.READY || status == MediaPlayer.Status.STOPPED) {
                    mediaPlayer.play();
                    playButton.setText("||");
                } else {
                    mediaPlayer.pause();
                    playButton.setText(">");
                }
            }
        });
        
        // Previous track button
        Button prevButton = new Button("<<");
        prevButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 40px; -fx-min-height: 30px; -fx-background-radius: 3;");
        prevButton.setOnAction(e -> playPreviousTrack());
        
        // Next track button
        Button nextButton = new Button(">>");
        nextButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 40px; -fx-min-height: 30px; -fx-background-radius: 3;");
        nextButton.setOnAction(e -> playNextTrack());
        
        // Time label
        playTime = new Label("00:00/00:00");
        playTime.setPrefWidth(130);
        playTime.setStyle("-fx-text-fill: white;");
        
        // Time slider
        timeSlider = new Slider();
        timeSlider.setStyle("-fx-control-inner-background: #555555;");
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (timeSlider.isValueChanging()) {
                if (mediaPlayer != null && duration != null) {
                    mediaPlayer.seek(duration.multiply(newValue.doubleValue() / 100.0));
                }
            }
        });
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        
        // Volume slider
        volumeSlider = new Slider();
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        volumeSlider.setValue(100);
        volumeSlider.setStyle("-fx-control-inner-background: #555555;");
        
        // Volume label
        Label volumeLabel = new Label("Volume: ");
        volumeLabel.setStyle("-fx-text-fill: white;");
        
        // Currently playing label
        currentlyPlaying = new Label("No media loaded");
        currentlyPlaying.setStyle("-fx-text-fill: white;");
        HBox.setHgrow(currentlyPlaying, Priority.ALWAYS);
        currentlyPlaying.setMaxWidth(Double.MAX_VALUE);
        
        mediaBar.getChildren().addAll(
                prevButton, playButton, nextButton, 
                timeSlider, playTime, 
                volumeLabel, volumeSlider);
                
        // Add currently playing to a separate bar
        HBox infoBar = new HBox(currentlyPlaying);
        infoBar.setPadding(new Insets(5));
        infoBar.setStyle("-fx-background-color: #333333;");
        
        VBox controlsContainer = new VBox(infoBar, mediaBar);
        return controlsContainer;
    }

    private VBox createPlaylistPane() {
        VBox playlistPane = new VBox(10);
        playlistPane.setPadding(new Insets(10));
        playlistPane.setStyle("-fx-background-color: #333333;");
        playlistPane.setPrefWidth(200);
        
        Label playlistLabel = new Label("Playlist");
        playlistLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        
        ListView<String> playlistView = new ListView<>();
        playlistView.setStyle("-fx-control-inner-background: #444444; -fx-text-fill: white;");
        VBox.setVgrow(playlistView, Priority.ALWAYS);
        
        // Bind playlist items
        playlistView.getItems().addAll(playlist);
        
        // Double click to play item
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = playlistView.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    currentTrackIndex = selectedIndex;
                    playMediaFile(playlist.get(currentTrackIndex));
                }
            }
        });
        
        Button addButton = new Button("Add to Playlist");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-background-radius: 3;");
        addButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            configureFileChooser(fileChooser);
            List<File> files = fileChooser.showOpenMultipleDialog(null);
            
            if (files != null) {
                for (File file : files) {
                    String mediaPath = file.toURI().toString();
                    playlist.add(mediaPath);
                    String fileName = file.getName();
                    playlistView.getItems().add(fileName);
                }
            }
        });
        
        Button removeButton = new Button("Remove Selected");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-background-radius: 3;");
        removeButton.setOnAction(e -> {
            int selectedIndex = playlistView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                playlist.remove(selectedIndex);
                playlistView.getItems().remove(selectedIndex);
            }
        });
        
        HBox buttonBar = new HBox(10, addButton, removeButton);
        buttonBar.setAlignment(Pos.CENTER);
        
        playlistPane.getChildren().addAll(playlistLabel, playlistView, buttonBar);
        return playlistPane;
    }

    private void openFile(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        configureFileChooser(fileChooser);
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            String mediaPath = file.toURI().toString();
            playlist.add(mediaPath);
            currentTrackIndex = playlist.size() - 1;
            playMediaFile(mediaPath);
        }
    }

    private void configureFileChooser(FileChooser fileChooser) {
        fileChooser.setTitle("Select Media File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files", "*.mp3", "*.mp4", "*.wav", "*.aac"),
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac"),
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
    }

    private void playMediaFile(String mediaPath) {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
        
        try {
            Media media = new Media(mediaPath);
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            
            // Make the video fit the pane
            DoubleProperty mvw = mediaView.fitWidthProperty();
            DoubleProperty mvh = mediaView.fitHeightProperty();
            mvw.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width").subtract(200)); // Subtract playlist width
            mvh.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height").subtract(100)); // Subtract controls height
            mediaView.setPreserveRatio(true);
            
            // Update time slider and label as media plays
            mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                if (!timeSlider.isValueChanging()) {
                    timeSlider.setValue(newValue.toSeconds() / duration.toSeconds() * 100.0);
                }
                updateValues();
            });
            
            mediaPlayer.setOnReady(() -> {
                duration = mediaPlayer.getMedia().getDuration();
                updateValues();
            });
            
            mediaPlayer.setOnEndOfMedia(() -> {
                playNextTrack();
            });
            
            // Bind volume slider
            mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty().divide(100));
            
            // Display file name
            File file = new File(mediaPath);
            currentlyPlaying.setText("Now Playing: " + file.getName());
            
            mediaPlayer.play();
        } catch (Exception e) {
            System.out.println("Error playing media: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Media Error");
            alert.setHeaderText("Could not play media");
            alert.setContentText("The selected file could not be played.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private void updateValues() {
        if (playTime != null && timeSlider != null && volumeSlider != null && mediaPlayer != null) {
            Platform.runLater(() -> {
                Duration currentTime = mediaPlayer.getCurrentTime();
                playTime.setText(formatTime(currentTime, duration));
                timeSlider.setDisable(duration.isUnknown());
                
                if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !timeSlider.isValueChanging()) {
                    timeSlider.setValue(currentTime.divide(duration).toMillis() * 100.0);
                }
            });
        }
    }

    private String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        int elapsedMinutes = (intElapsed - elapsedHours * 60 * 60) / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;
        
        int intDuration = (int) Math.floor(duration.toSeconds());
        int durationHours = intDuration / (60 * 60);
        int durationMinutes = (intDuration - durationHours * 60 * 60) / 60;
        int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;
        
        if (durationHours > 0) {
            return String.format("%d:%02d:%02d/%d:%02d:%02d",
                    elapsedHours, elapsedMinutes, elapsedSeconds,
                    durationHours, durationMinutes, durationSeconds);
        } else {
            return String.format("%02d:%02d/%02d:%02d",
                    elapsedMinutes, elapsedSeconds, durationMinutes, durationSeconds);
        }
    }

    private void playNextTrack() {
        if (playlist.size() > 0) {
            currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
            playMediaFile(playlist.get(currentTrackIndex));
        }
    }

    private void playPreviousTrack() {
        if (playlist.size() > 0) {
            currentTrackIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
            playMediaFile(playlist.get(currentTrackIndex));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}