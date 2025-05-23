package run;

/**
 *
 * @author jstar with ChatGPT
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SwingRunner {

    private final static Font[] fonts = {
        new Font("Courier", Font.PLAIN, 12),
        new Font("Courier", Font.PLAIN, 18),
        new Font("Courier", Font.PLAIN, 24)
    };
    private static Font currentFont = fonts[0];
    private final static List<String> classes = new ArrayList<>();

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        File here = new File(".");
        findClassFiles(classes, here);
        classes.remove("run.RunnerGUI");
        Collections.sort(classes);
        System.err.println(here.getAbsolutePath());

        SwingUtilities.invokeLater(SwingRunner::createMainWindow);
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

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JLabel("Args:"));
        JTextField argsTF = new JTextField();
        argsTF.setAlignmentX(0);
        argsTF.setColumns(35);
        //args.setSize(300, scrollPane.getFontMetrics(FONT24).getHeight());
        buttonPanel.add(argsTF);
        JButton runButton = new JButton("Run");
        runButton.addActionListener((ActionEvent e) -> {
            String className = classNameField.getSelectedValue();
            if (className != null && !className.isEmpty()) {
                String[] args = argsTF.getText().trim().split("\\s+");
                runClassInNewWindow(className, args);
            }
        });
        // To poniżej to tylko kwiatki do korzucha
        /*
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runButton.setForeground(Color.RED);
                System.err.println("Czerwony!");
            }
        });
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                (new Thread() {
                    public void run() {
                        try {
                            System.err.println("Zasypiam");
                            Thread.sleep(2000);
                            System.err.println("Obudzony");
                        } catch (InterruptedException ex) {
                        }
                        runButton.setForeground(Color.GREEN);
                        System.err.println("Zielony?");
                    }
                }).start();
            }
        }
        );
*/
        // koniec kwiatków
        JButton closeButton = new JButton("Close");

        closeButton.addActionListener(
                (ActionEvent e) -> System.exit(0));
        buttonPanel.add(runButton);

        buttonPanel.add(
                new JSeparator());
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        Dimension size = new Dimension(600, (4 + classes.size()) * scrollPane.getFontMetrics(currentFont).getHeight());
        JFrame frame = new JFrame("Class Runner");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLocationRelativeTo(null);
        frame.setSize(size);

        frame.add(panel);

        JMenu guiOpts = new JMenu("Options");
        ButtonGroup fgroup = new ButtonGroup();

        guiOpts.add( new JMenuItem("Font size"));
        for (Font f : fonts) {
            JRadioButtonMenuItem fontOpt = new JRadioButtonMenuItem("\t\t\t" + String.valueOf(f.getSize()));
            final Font cf = f;
            fontOpt.addActionListener(e -> {
                currentFont = cf;
                setFontRecursively(frame, currentFont);
                UIManager.put("OptionPane.messageFont", currentFont);
                UIManager.put("OptionPane.buttonFont", currentFont);
                UIManager.put("OptionPane.messageFont", currentFont);
            });
            fontOpt.setSelected(f == currentFont);
            fgroup.add(fontOpt);
            guiOpts.add(fontOpt);
        }
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(guiOpts);

        frame.setJMenuBar(menuBar);

        setFontRecursively(frame, currentFont);

        frame.setVisible(
                true);
    }

    private static void setFontRecursively(Component comp, Font font) {
        if (comp == null) {
            return;
        }
        comp.setFont(font);
        if (comp instanceof Container container ) {
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

    private static void runClassInNewWindow(String className, String[] pargs) {
        JFrame outputFrame = new JFrame("Output: " + className);
        outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        outputFrame.setSize(800, 600);
        outputFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        //JTextField inputField = new JTextField();
        //bottomPanel.add(inputField, BorderLayout.CENTER);
        JButton closeButton = new JButton("Close");
        bottomPanel.add(closeButton, BorderLayout.EAST);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        outputFrame.add(panel);
        setFontRecursively(outputFrame, currentFont);
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
            final String[] args = (pargs == null || pargs[0].length() == 0) ? new String[0] : pargs;
            new Thread(() -> {
                try {
                    Class<?> cls = Class.forName(className);
                    Method mainMethod = cls.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) args);
                } catch (Exception ex) {
                    System.err.print("Args: ");
                    for (String s : args) {
                        System.err.print("\"" + s + "\"");
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
}
