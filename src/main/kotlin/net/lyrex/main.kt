package net.lyrex

import net.lyrex.dictation.DiktatGui

import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JOptionPane.NO_OPTION
import javax.swing.JOptionPane.YES_OPTION
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

const val configuration_file_name = "gcp-credentials.dat"

fun selectFile(): File? {
    JOptionPane.showMessageDialog(
        null,
        "Bitte wählen Sie die Konfigurationsdatei aus.",
        "Konfiguration",
        JOptionPane.INFORMATION_MESSAGE
    )

    val chooser = JFileChooser()
    chooser.fileFilter = FileNameExtensionFilter("GCP Konfigurationsdatei (*.json)", "json")

    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        var file = chooser.selectedFile
        if (!file.exists()) {
            JOptionPane.showMessageDialog(
                null,
                "Die Konfiguierte Datei konnte nicht gefunden werden.",
                "Konfiguration - Fehler",
                JOptionPane.ERROR_MESSAGE
            )

            return null
        }

        return file
    }

    return null
}

fun validateFile(file: File): Boolean {
    if (!file.exists() || file.isDirectory) {
        return false
    }

    val content = file.readText()
    if (!content.contains("private_key") ||
        !content.contains("private_key_id") ||
        !content.contains("auth_provider_x509_cert_url")
    ) {
        return false
    }

    return true
}

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val credentialsFile = File(configuration_file_name)
    if (!credentialsFile.exists()) {
        // env: GOOGLE_APPLICATION_CREDENTIALS
        val googleCredentialsPath: String? = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if (googleCredentialsPath != null && googleCredentialsPath.isNotBlank()) {
            val selection = JOptionPane.showConfirmDialog(
                null,
                "Es wurde ein Pfad zu einer GCP Konfigurationsdatei in den Umgebungsvariablen gefunden.\n" +
                        "Möchten Sie die die konfiguierte Datei benutzen?",
                "GCP Konfiguration",
                JOptionPane.YES_NO_CANCEL_OPTION
            )

            if (selection == YES_OPTION) {
                // copy file to current directory and proceed
                val environmentFile = File(googleCredentialsPath)
                if (environmentFile.exists() && validateFile(environmentFile)) {
                    environmentFile.copyTo(credentialsFile)
                } else {
                    JOptionPane.showMessageDialog(
                        null,
                        "Die konfiguierte Datei konnte entweder nicht gefunden werden ist keine gültige Konfigurationsdatei.",
                        "Konfiguration - Fehler",
                        JOptionPane.WARNING_MESSAGE
                    )

                    val selectedFile = selectFile()
                    if (selectedFile == null || !validateFile(selectedFile)) {
                        JOptionPane.showMessageDialog(
                            null,
                            "Sie haben keine Datei ausgewählt oder die ausgewählte Datei ist keine gültige Konfigurationsdatei.",
                            "Konfiguration - Fehler",
                            JOptionPane.ERROR_MESSAGE
                        )

                        return  // exit program
                    }

                    selectedFile.copyTo(credentialsFile)
                }
            } else if (selection == NO_OPTION) {
                val selectedFile = selectFile()
                if (selectedFile == null || !validateFile(selectedFile)) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Sie haben keine Datei ausgewählt oder die ausgewählte Datei ist keine gültige Konfigurationsdatei.",
                        "Konfiguration - Fehler",
                        JOptionPane.ERROR_MESSAGE
                    )

                    return  // exit program
                }

                selectedFile.copyTo(credentialsFile)
            } else {
                JOptionPane.showMessageDialog(
                    null,
                    "Es muss eine gültige Konfigurationsdatei ausgewählt werden, um fortzufahren.",
                    "Konfiguration - Fehler",
                    JOptionPane.ERROR_MESSAGE
                )

                return // exit program
            }
        }
    }


    val diktatGui = DiktatGui()
    diktatGui.pack()

    // make main window visible
    diktatGui.isVisible = true
}