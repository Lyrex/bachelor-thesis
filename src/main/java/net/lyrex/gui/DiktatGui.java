package net.lyrex.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import net.lyrex.audio.AudioProcessor;
import net.lyrex.image.ImageContainer;
import net.lyrex.image.ImageProcessor;
import net.lyrex.nlp.NLPProcessor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

import java.nio.file.Paths;

public class DiktatGui {
    final private NLPProcessor nlpProcessor = new NLPProcessor();
    private List<List<String>> sentences = new ArrayList(new ArrayList());

    private JButton selectImageButton;
    private JButton diktatReadButton;
    private JButton previousSentence;
    private JButton repeatSentence;
    private JButton nextSentence;
    private JButton pauseSentence;
    private JTextArea diktatText;
    public JPanel mainPanel;

    private Thread audioThread;

    private static final int REPETITIONS = 3;

    public class AudioOutputRunner implements Runnable {
        private final List<List<String>> sentences;

        public AudioOutputRunner(List<List<String>> sentences) {
            this.sentences = sentences;
        }

        public void run() {
            for (var s : this.sentences) {
                // read full sentence once
                {
                    var fullSentence = String.join(" ", s);
                    var audioBytes = AudioProcessor.textToWav(fullSentence);

                    InputStream stream = new ByteArrayInputStream(audioBytes);
                    try (AudioInputStream ais = AudioSystem.getAudioInputStream(stream)) {
                        final Clip clip = AudioSystem.getClip();
                        // Listener which allow method return once sound is completed
                        clip.addLineListener(e -> {
                            synchronized (clip) {
                                clip.notifyAll();
                            }
                        });

                        clip.open(ais);
                        clip.start();

                        do {
                            synchronized (clip) {
                                clip.wait();
                            }
                        } while (clip.isRunning());
                        clip.drain();
                        clip.stop();
                        clip.close();

                        stream.close();
                    } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(1500);
                } catch (InterruptedException e) {
                    return;
                }

                for (var ss : s) {
                    if (ss.isBlank()) {
                        continue;
                    }

                    var audioBytes = AudioProcessor.textToWav(ss);
                    for (int i = 0; i < REPETITIONS; i++) {
                        InputStream stream = new ByteArrayInputStream(audioBytes);
                        try (AudioInputStream ais = AudioSystem.getAudioInputStream(stream)) {
                            final Clip clip = AudioSystem.getClip();
                            // Listener which allow method return once sound is completed
                            clip.addLineListener(e -> {
                                synchronized (clip) {
                                    clip.notifyAll();
                                }
                            });

                            clip.open(ais);
                            clip.start();

                            do {
                                synchronized (clip) {
                                    clip.wait();
                                }
                            } while (clip.isRunning());
                            clip.drain();
                            clip.stop();
                            clip.close();

                            stream.close();

                            try {
                                TimeUnit.MILLISECONDS.sleep(2000);
                            } catch (InterruptedException e) {
                                return;
                            }
                        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                }
            }

        }
    }

    public DiktatGui() {
        diktatReadButton.addActionListener(e -> {
            // todo(tobias): this needs to have some improved logic to be able to read proper sentences
            if (audioThread != null) {
                audioThread.interrupt();
                try {
                    audioThread.wait();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }

            audioThread = new Thread(new AudioOutputRunner(this.sentences));
            audioThread.start();


            /*
            for (var s : sentences) {
                for (var ss : s) {
                    if (ss.isBlank()) {
                        return;
                    }

                    // todo(tobias): improve sound output logic
                    InputStream stream = new ByteArrayInputStream(AudioProcessor.textToWav(ss));
                    AudioInputStream ais =
                            null;
                    try {
                        ais = AudioSystem.getAudioInputStream(stream);
                    } catch (UnsupportedAudioFileException | IOException unsupportedAudioFileException) {
                        unsupportedAudioFileException.printStackTrace();
                    }

                    Clip clip = null;
                    try {
                        clip = AudioSystem.getClip();
                        clip.open(ais);
                    } catch (LineUnavailableException | IOException lineUnavailableException) {
                        lineUnavailableException.printStackTrace();
                    }

                    assert clip != null;
                    clip.start();
                }
            }
            */

//            String s = diktatText.getText();
//            if (s.isBlank()) {
//                return;
//            }
//
//            // todo(tobias): improve sound output logic
//            InputStream stream = new ByteArrayInputStream(AudioProcessor.textToWav(s));
//            AudioInputStream ais =
//                    null;
//            try {
//                ais = AudioSystem.getAudioInputStream(stream);
//            } catch (UnsupportedAudioFileException | IOException unsupportedAudioFileException) {
//                unsupportedAudioFileException.printStackTrace();
//            }
//
//            Clip clip = null;
//            try {
//                clip = AudioSystem.getClip();
//                clip.open(ais);
//            } catch (LineUnavailableException | IOException lineUnavailableException) {
//                lineUnavailableException.printStackTrace();
//            }
//
//            assert clip != null;
//            clip.start();
        });

        selectImageButton.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser(Paths.get("./src/main/resources").toAbsolutePath().normalize().toString());

            if (e.getSource() == selectImageButton) {
                int returnVal = fc.showOpenDialog(mainPanel);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();

                    diktatText.setText("Lade..");

                    // todo(tobias): improve loading times by threading
                    try {
                        var s = ImageProcessor.imageToText(new ImageContainer(file));
                        System.out.println(s);
                        diktatText.setText(s);
                        sentences = nlpProcessor.dissectText(s);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });
        pauseSentence.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (audioThread != null && !audioThread.isInterrupted()) {
                    audioThread.interrupt();
                }
            }
        });
    }

    public void setData(DiktatGuiBinding data) {
        diktatText.setText(data.getInputText());
    }

    public void getData(DiktatGuiBinding data) {
        data.setInputText(diktatText.getText());
    }

    public boolean isModified(DiktatGuiBinding data) {
        if (diktatText.getText() != null ? !diktatText.getText().equals(data.getInputText()) : data.getInputText() != null)
            return true;
        return false;
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
        selectImageButton = new JButton();
        selectImageButton.setText("Bild-Datei auswählen");
        mainPanel.add(selectImageButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        previousSentence = new JButton();
        previousSentence.setText("Vorheriger Satz");
        panel1.add(previousSentence, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        repeatSentence = new JButton();
        repeatSentence.setText("Satz wiederholen");
        panel1.add(repeatSentence, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nextSentence = new JButton();
        nextSentence.setText("Nächster Satz");
        panel1.add(nextSentence, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pauseSentence = new JButton();
        pauseSentence.setText("Ausgabe Pausieren");
        panel1.add(pauseSentence, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        diktatReadButton = new JButton();
        diktatReadButton.setText("Diktieren");
        mainPanel.add(diktatReadButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 270), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(480, 280), null, 0, false));
        diktatText = new JTextArea();
        scrollPane1.setViewportView(diktatText);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
