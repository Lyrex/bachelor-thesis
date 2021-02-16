package net.lyrex.audio

import com.google.cloud.texttospeech.v1.*;


internal class AudioProcessor {
    internal companion object {
        private val defaultVoice = Voice(Language.German, Gender.Male, "de-DE-Wavenet-B")

        @JvmStatic
        fun textToMp3(inputText: String): ByteArray {
            return textToAudio(inputText, AudioEncoding.MP3, defaultVoice)
        }

        @JvmStatic
        fun textToWav(inputText: String): ByteArray {
            return textToAudio(inputText, AudioEncoding.LINEAR16, defaultVoice)
        }

        @JvmStatic
        fun textToAudio(inputText: String, audioEncoding: AudioEncoding, voice: Voice, speakingRate: Double = 1.0): ByteArray {
            TextToSpeechClient.create().use { ttsClient ->
                val input = SynthesisInput.newBuilder().setText(inputText).build()

                val voiceParams = VoiceSelectionParams.newBuilder()
                        .setLanguageCode(voice.language.languageString)
                        .setSsmlGender(voice.gender.ssmlGender)
                        .setName(voice.name)
                        .build()

                val audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(audioEncoding)
                        .setSpeakingRate(speakingRate)
                        .build()

                // Perform the text-to-speech request on the text input with the selected voice parameters and
                // audio file type
                val response: SynthesizeSpeechResponse = ttsClient.synthesizeSpeech(input, voiceParams, audioConfig)

                // Get the audio contents from the response
                val audioContents = response.audioContent

                val audioBytes = audioContents.toByteArray()
                if (audioBytes == null || audioBytes.isEmpty()) {
                    // TODO(tobias): improve error handling
                    System.err.format("could not parse audio response.\n")
                    return ByteArray(0)
                }

                return audioBytes
            }
        }
    }
}