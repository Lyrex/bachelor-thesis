package net.lyrex.dictation;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import net.lyrex.image.ImageContainer;
import net.lyrex.image.ImageProcessor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;

import java.nio.file.Paths;

public class DiktatGui extends JFrame {
    private JButton selectSourceButton;
    private JButton dictateButton;
    private JButton previousSentenceButton;
    private JButton repeatSentenceButton;
    private JButton nextSentenceButton;
    private JButton pauseDictationButton;
    private JTextArea diktatText;
    public JPanel mainPanel;
    private JScrollPane textPane;
    private JButton stopDictationButton;

    private final OptionDialogue optionWindow = new OptionDialogue();

    private final DictateController dictateController =
            new DictateController("",
                    optionWindow.getDictateOptions()
            );

    public DiktatGui() {
        // set-up window
        this.setTitle("Diktat Buddy");
        this.setContentPane(this.mainPanel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set-up menu bar
        final JMenuBar menuBar = new JMenuBar();
        var fileMenu = new JMenu("Datei");

        var settingsMenuItem = new JMenuItem("Einstellungen");
        settingsMenuItem.addActionListener(e -> optionWindow.setVisible(true));
        fileMenu.add(settingsMenuItem);

        var exportMenuItem = new JMenuItem("Export als WAV");
        exportMenuItem.addActionListener(e -> {
            loadingDialogue.setVisible(true);

            final var dictateAudio = dictateController.generateAudioFromDicate();
            loadingDialogue.setVisible(false);

            if (dictateAudio.length == 0) {
                JOptionPane.showMessageDialog(this, "Keine Daten gefunden.", "Warnung", JOptionPane.WARNING_MESSAGE);
                return;
            }

            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Audio-Datei (.wav)", "wav"));

            int returnVal = fc.showOpenDialog(mainPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();

                if (!file.getName().toLowerCase().endsWith(".wav")) {
                    file = new File(file.toString() + ".wav");
                }

                try (var outputStream = new FileOutputStream(file, false)) {
                    outputStream.write(dictateAudio);
                } catch (IOException ioException) {
                    // todo(tobias): improve error handling
                    ioException.printStackTrace();
                }
            }
        });
        fileMenu.add(exportMenuItem);

        menuBar.add(fileMenu);
        this.setJMenuBar(menuBar);

        // set-up option window action listeners
        optionWindow.cancelButton.addActionListener(e -> optionWindow.setVisible(false));
        optionWindow.confirmButton.addActionListener(e -> {
            dictateController.setDictateOptions(optionWindow.getDictateOptions());
            optionWindow.setVisible(false);
        });

        // set-up action listeners
        dictateButton.addActionListener(e -> {
            var worker = new DictateButtonWorker();
            worker.execute();
        });

        previousSentenceButton.addActionListener(e -> {
            var w = new DictatePreviousSentenceWorker();
            w.execute();
        });
        repeatSentenceButton.addActionListener(e -> {
            var w = new DictateCurrentSentenceWorker();
            w.execute();
        });
        nextSentenceButton.addActionListener(e -> {
            var w = new DictateNextSentenceWorker();
            w.execute();
        });
        pauseDictationButton.addActionListener(e -> {
            dictateController.pauseDictate();
            showTextArea();
            dictateButton.setText("Diktat Fortsetzen");
        });

        stopDictationButton.addActionListener(e -> {
            dictateController.stopDictate();
            showTextArea();
            dictateButton.setText("Diktieren");
        });

        selectSourceButton.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser(Paths.get("./src/main/resources").toAbsolutePath().normalize().toString());
            fc.setFileFilter(new FileNameExtensionFilter("Eingabedateien", "*.txt", "txt", "jpg", "png", "gif", "bmp"));

            if (e.getSource() == selectSourceButton) {
                int returnVal = fc.showOpenDialog(mainPanel);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();

                    diktatText.setText("Lade..");

                    // todo(tobias): maybe improve loading times by threading
                    try {
                        String text;
                        if (file.exists() && file.getName().endsWith(".txt")) {
                            try (var fis = new FileInputStream(file)) {
                                text = new String(fis.readAllBytes());
                            }
                        } else {
                            text = ImageProcessor.imageToText(new ImageContainer(file));
                        }

                        diktatText.setText(text);
                        dictateController.setDictateText(text);
                    } catch (IOException ioException) {
                        // todo(tobias): improve error handling
                        ioException.printStackTrace();
                    }
                }
            }
        });

        diktatText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                dictateController.setDictateText(diktatText.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                dictateController.setDictateText(diktatText.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                dictateController.setDictateText(diktatText.getText());
            }
        });

        this.pack();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        selectSourceButton = new JButton();
        selectSourceButton.setText("Quell-Datei auswählen");
        mainPanel.add(selectSourceButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        previousSentenceButton = new JButton();
        previousSentenceButton.setText("Vorheriger Satz");
        panel1.add(previousSentenceButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        repeatSentenceButton = new JButton();
        repeatSentenceButton.setText("Satz wiederholen");
        panel1.add(repeatSentenceButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nextSentenceButton = new JButton();
        nextSentenceButton.setText("Nächster Satz");
        panel1.add(nextSentenceButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pauseDictationButton = new JButton();
        pauseDictationButton.setText("Ausgabe Pausieren");
        panel1.add(pauseDictationButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stopDictationButton = new JButton();
        stopDictationButton.setText("Diktat Beenden");
        panel1.add(stopDictationButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dictateButton = new JButton();
        dictateButton.setText("Diktieren");
        mainPanel.add(dictateButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textPane = new JScrollPane();
        textPane.setEnabled(true);
        mainPanel.add(textPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(480, 280), null, 0, false));
        diktatText = new JTextArea();
        diktatText.setEnabled(true);
        textPane.setViewportView(diktatText);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


    private void hideTextArea() {
        diktatText.setVisible(false);
        textPane.setVisible(false);

        textPane.invalidate();
        textPane.revalidate();
    }

    private void showTextArea() {
        diktatText.setVisible(true);
        textPane.setVisible(false);

        textPane.invalidate();
        textPane.revalidate();
    }

    private class DictatePreviousSentenceWorker extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() {
            if (optionWindow.getDictateOptions().getHideTextWhileDictating()) {
                hideTextArea();
            }

            dictateController.dictatePreviousSentence();

            return null;
        }

        @Override
        protected void done() {
            showTextArea();
        }
    }

    private class DictateCurrentSentenceWorker extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() {
            if (optionWindow.getDictateOptions().getHideTextWhileDictating()) {
                hideTextArea();
            }

            dictateController.dictateCurrentSentence();

            return null;
        }

        @Override
        protected void done() {
            showTextArea();
        }
    }

    private class DictateNextSentenceWorker extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() {
            if (optionWindow.getDictateOptions().getHideTextWhileDictating()) {
                hideTextArea();
            }

            dictateController.dictateNextSentence();

            return null;
        }

        @Override
        protected void done() {
            showTextArea();
        }
    }

    private class DictateButtonWorker extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() {
            if (optionWindow.getDictateOptions().getHideTextWhileDictating()) {
                hideTextArea();
            }

            dictateController.dictateFullText();

            return null;
        }

        @Override
        protected void done() {
            showTextArea();

            if (!dictateController.getPaused()) {
                dictateButton.setText("Diktieren");
            }
        }
    }
}
