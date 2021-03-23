package net.lyrex.audio

import com.google.cloud.texttospeech.v1.AudioEncoding
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

//todo(tobias): Add additional file level cache based on some hash function to avoid repeated calls to google for
//               the same text

class AudioCache(
    private val encoding: AudioEncoding,
    private val voice: Voice,
    private val speakingSpeed: SpeakingSpeed
) {
    private val _cache: MutableMap<String, ByteArray> = hashMapOf()

    init {
        require(
            encoding == AudioEncoding.LINEAR16
                    || encoding == AudioEncoding.MP3
                    || encoding == AudioEncoding.OGG_OPUS
        ) { "encoding must be a valid audio encofing" }

        logger.info { "rebuilding audio cache with with encoding: $" }
    }

    fun getAudioForText(input: String): ByteArray {
        synchronized(_cache) {
            logger.debug { "Finding audio for input: \"$input\"" }

            // check if the cache already contains the audio for the requested string
            if (_cache.containsKey(input)) {
                logger.debug { "audio for input was found in cache" }
                return _cache[input]!!
            }

            val audio = AudioProcessor.textToAudio(input, encoding, voice, speakingSpeed)
            _cache[input] = audio

            return audio
        }
    }
}