package net.lyrex

import net.lyrex.dictation.DiktatGui
import javax.swing.*


fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

    val diktatGui = DiktatGui()
    diktatGui.pack()

    // make main window visible
    diktatGui.isVisible = true
}