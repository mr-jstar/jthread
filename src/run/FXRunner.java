package run;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 *
 * @author jstar
 */
public class FXRunner extends Application {

    private static final List<String> classes = new ArrayList<>();
    private static final Font[] FONTS = {
        Font.font("Courier New", 12),
        Font.font("Courier New", 18),
        Font.font("Courier New", 24)
    };
    private static Font currentFont = FONTS[0];

    public static void main(String[] args) {
        File here = new File(".");
        findClassFiles(classes, here);
        classes.remove("run.RunnerFX");
        Collections.sort(classes);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        ListView<String> classList = new ListView<>();
        classList.setItems(FXCollections.observableArrayList(classes));
        classList.setStyle("-fx-font-family: 'Courier New';-fx-text-fill: black; -fx-background-color: white;");
        TextField argsField = new TextField();
        argsField.setPromptText("Optional arguments");

        Button runButton = new Button("Run");
        Button closeButton = new Button("Close");

        HBox bottomBox = new HBox(10, new Label("Args:"), argsField, runButton, closeButton);
        bottomBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(classList);
        root.setBottom(bottomBox);

        MenuBar menuBar = new MenuBar();
        Menu fontMenu = new Menu("Font Size");
        ToggleGroup fontToggle = new ToggleGroup();

        for (Font font : FONTS) {
            RadioMenuItem item = new RadioMenuItem("Size " + (int) font.getSize());
            item.setToggleGroup(fontToggle);
            item.setSelected(font == currentFont);
            item.setOnAction(e -> {
                currentFont = font;
                classList.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: " + (int) font.getSize() + ";");
                argsField.setFont(currentFont);
            });
            fontMenu.getItems().add(item);
        }

        menuBar.getMenus().add(fontMenu);
        root.setTop(menuBar);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Class Runner FX");
        primaryStage.setScene(scene);
        primaryStage.show();

        runButton.setOnAction(e -> runSelectedClass(classList.getSelectionModel().getSelectedItem(), argsField.getText()));
        closeButton.setOnAction(e -> Platform.exit());

        argsField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                runButton.fire();
            }
        });
    }

    private void runSelectedClass(String className, String argsText) {
        if (className == null || className.isEmpty()) {
            return;
        }
        String[] args = argsText.trim().isEmpty() ? new String[0] : argsText.trim().split("\\s+");
        showOutputWindow(className, args);
    }

    private void showOutputWindow(String className, String[] args) {
        Stage stage = new Stage();
        stage.setTitle("Output: " + className);

        TextArea outputArea = new TextArea();
        outputArea.setFont(currentFont);
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-text-fill: black; -fx-background-color: white; -fx-control-inner-background: white;");

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());

        VBox vbox = new VBox(outputArea, closeButton);
        VBox.setVgrow(outputArea, Priority.ALWAYS);
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox, 800, 600);
        stage.setScene(scene);
        stage.show();

        PipedOutputStream pos = new PipedOutputStream();
        try {
            PipedInputStream pis = new PipedInputStream(pos);
            BufferedReader reader = new BufferedReader(new InputStreamReader(pis));
            PrintStream ps = new PrintStream(pos, true);

            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            System.setOut(ps);
            System.setErr(ps);

            new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        final String finalLine = line;
                        Platform.runLater(() -> outputArea.appendText(finalLine + "\n"));
                    }
                } catch (IOException ignored) {
                }
            }).start();

            new Thread(() -> {
                try {
                    Class<?> cls = Class.forName(className);
                    Method mainMethod = cls.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) args);
                } catch (Exception ex) {
                    System.err.print("Args: ");
                    for (String s : args) {
                        System.err.print("\"" + s + "\" ");
                    }
                    System.err.println();
                    ex.printStackTrace();
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }
            }).start();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void findClassFiles(List<String> classes, File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Nieprawidłowa ścieżka: " + directory.getAbsolutePath());
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findClassFiles(classes, file); // rekurencja
            } else if (file.getName().endsWith(".class")) {
                try {
                    String name = file.getParent().replaceAll(".*/", "") + "." + file.getName().replace(".class", "");
                    Class<?> cls = Class.forName(name);
                    Method mainMethod = cls.getMethod("main", String[].class);
                    if (mainMethod != null) {
                        classes.add(name);
                    }
                } catch (Exception ex) {
                }
            }
        }
    }
}
