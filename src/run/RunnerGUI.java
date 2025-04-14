package run;

/*
 * Do what you want with this file
 */

/**
 *
 * @author jstar with ChatGPT
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RunnerGUI {

    private final static Font FONT = new Font("Arial", Font.PLAIN, 24);
    private final static List<String> classes = new ArrayList<>();

    public static void main(String[] args) {
        File here = new File(".");
        findClassFiles(classes, here);
        classes.remove("run.RunnerGUI");
        Collections.sort(classes);
        System.err.println(here.getAbsolutePath());

        SwingUtilities.invokeLater(RunnerGUI::createMainWindow);
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

    private static void createMainWindow() {
        JPanel panel = new JPanel(new BorderLayout());

        JList<String> classNameField = new JList<>(classes.toArray(new String[0]));
        classNameField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(classNameField);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JButton runButton = new JButton("Run");
        runButton.addActionListener((ActionEvent e) -> {
            String className = classNameField.getSelectedValue();
            if ( className != null && !className.isEmpty()) {
                runClassInNewWindow(className);
            }
        });
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener((ActionEvent e) -> System.exit(0));
        buttonPanel.add(runButton, BorderLayout.WEST);
        buttonPanel.add(closeButton, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        Dimension size = new Dimension(600, (4 + classes.size()) * scrollPane.getFontMetrics(FONT).getHeight());
        JFrame frame = new JFrame("Class Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(size);

        frame.add(panel);
        setFontRecursively(frame, FONT);
        frame.setVisible(true);
    }

    private static void setFontRecursively(Component comp, Font font) {
        if (comp == null) {
            return;
        }
        comp.setFont(font);
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                setFontRecursively(child, font);
            }
        }
        // Needs specific navigation, since JMenu does not show menu components as Components
        if (comp instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                setFontRecursively(menu.getItem(i), font);
            }
        }
    }

    private static void runClassInNewWindow(String className) {
        JFrame outputFrame = new JFrame("Output: " + className);
        outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        outputFrame.setSize(800, 600);
        outputFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton closeButton = new JButton("Close");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(closeButton, BorderLayout.EAST);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        outputFrame.add(panel);
        setFontRecursively(outputFrame, FONT);
        outputFrame.setVisible(true);

        closeButton.addActionListener(e -> outputFrame.dispose());

        // Przechwytywanie System.out i System.err
        PipedOutputStream pos = new PipedOutputStream();
        try {
            PipedInputStream pis = new PipedInputStream(pos);
            BufferedReader reader = new BufferedReader(new InputStreamReader(pis));

            PrintStream ps = new PrintStream(pos, true);
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            System.setOut(ps);
            System.setErr(ps);

            // Czytanie wątku wyjścia
            new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> outputArea.append(finalLine + "\n"));
                    }
                } catch (IOException ex) {
                }
            }).start();

            // Uruchamianie klasy
            new Thread(() -> {
                try {
                    Class<?> cls = Class.forName(className);
                    Method mainMethod = cls.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[0]);
                } catch (Exception ex) {
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }
            }).start();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
