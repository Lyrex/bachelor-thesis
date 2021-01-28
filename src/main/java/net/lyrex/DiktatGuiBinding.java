package net.lyrex;

import java.util.List;

public class DiktatGuiBinding {
    private List<List<String>> sentences;
    private String inputText;

    public DiktatGuiBinding() {
    }

    public List<List<String>> getSentences() {
        return sentences;
    }

    public void setSentences(final List<List<String>> sentences) {
        this.sentences = sentences;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(final String inputText) {
        this.inputText = inputText;
    }
}