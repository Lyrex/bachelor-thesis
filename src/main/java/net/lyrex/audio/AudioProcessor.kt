package net.lyrex.audio

import com.google.cloud.texttospeech.v1.*;


class AudioProcessor {
    companion object {
        // TODO(tobias): Implement audio file caching (using the hash of a text snippet as cache index)

        @JvmStatic
        fun textToMp3(inputText: String): ByteArray {
            return textToAudio(inputText, AudioEncoding.MP3)
        }


        @JvmStatic
        fun textToWav(inputText: String): ByteArray {
            return textToAudio(inputText, AudioEncoding.LINEAR16)
        }

        @JvmStatic
        private fun textToAudio(inputText: String, audioEncoding: AudioEncoding): ByteArray {
            TextToSpeechClient.create().use { ttsClient ->
                val input = SynthesisInput.newBuilder().setText(inputText).build()

                val voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode("de-DE")
                        .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                        .build()

                val audioConfig = AudioConfig.newBuilder().setAudioEncoding(audioEncoding).build()
                if (audioConfig == null) {
                    // TODO(tobias): handle null exception here, should not ever happen
                    assert(false);
                }

                // Perform the text-to-speech request on the text input with the selected voice parameters and
                // audio file type
                val response: SynthesizeSpeechResponse = ttsClient.synthesizeSpeech(input, voice, audioConfig)

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