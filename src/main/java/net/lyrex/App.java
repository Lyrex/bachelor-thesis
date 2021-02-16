package net.lyrex;

import net.lyrex.gui.DiktatGui;

import javax.swing.*;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {

        var diktatGui = new DiktatGui();

        var frame = new JFrame("Diktat UI");
        frame.setContentPane(diktatGui.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

//         var res = diktatGui.getClass().getResource("/diktat-der-verraeter-1.png");
//         var text = ImageProcessor.imageToText(new ImageContainer(res.getFile()));
//
//        var p = new NLPProcessor();
//        p.dissectText(p.getInputString());
//
//        var binding = new DiktatGuiBinding();
//        binding.setInputText(p.getInputString().replace(".", ".\n"));
//        diktatGui.setData(binding);

        // process audio
    }
}
