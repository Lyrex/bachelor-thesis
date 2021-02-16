package net.lyrex

import net.lyrex.gui.DiktatGui
import javax.swing.JFrame

fun main() {
    val diktatGui = DiktatGui()

    val frame = JFrame("Diktat UI")
    frame.contentPane = diktatGui.mainPanel
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.pack()
    frame.isVisible = true


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