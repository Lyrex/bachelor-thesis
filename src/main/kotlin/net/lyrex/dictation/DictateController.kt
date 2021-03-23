package net.lyrex.dictation

import com.google.cloud.texttospeech.v1.AudioEncoding

import mu.KotlinLogging

import net.lyrex.audio.*
import net.lyrex.nlp.NLPProcessor

import java.io.*
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

import kotlin.math.roundToInt


private val logger = KotlinLogging.logger {}

class DictateController {
    var paused = false

    constructor(inputText: String, dictateOptions: DictateOptions) {
        this.dictateText = inputText
        this.dictateOptions = dictateOptions

        this._audioCache = AudioCache(AudioEncoding.LINEAR16, dictateOptions.voice, dictateOptions.speakingSpeed)
        this._nlpProcessor = NLPProcessor(
            this.dictateOptions.language, dictateOptions.pronouncePunctation,
            dictateOptions.charactersPerSentencePartTarget, dictateOptions.charactersPerSentencePartMax
        )
    }

    // ---[ member methods
    fun dictateFullText() {
        parseTextIntoSentencesIfNecessary()

        if (!paused) {
            _currentSentenceIndex = 0
        } else {
            paused = false
        }

        // actually play audio for dictate now
        if (dictateOptions.readFullDictateOnce) {
            _sentences.forEach { sentence ->
                val sentenceText = sentence.joinToString(separator = " ")
                val sentenceAudio = _audioCache.getAudioForText(sentenceText)
                logger.debug { "plaing back audio for \"$sentenceText\"" }
                _audioPlayer.play(sentenceAudio)
                _audioPlayer.waitUntilPlayingIsOver()

                if (paused) {
                    return
                }
            }
        }

        // dictate sentence by sentence
        _sentences.forEachIndexed { index, _ ->
            if (_sentences.size > _currentSentenceIndex && index < _currentSentenceIndex) {
                return@forEachIndexed
            }

            _currentSentenceIndex = index
            try {
                dictateSentence(index)
            } catch (e: InterruptedException) {
                return
            }

            // wait configured time between sentences
            if (dictateOptions.pauseTimeBetweenSentences > Duration.ZERO) {
                TimeUnit.MILLISECONDS.sleep(dictateOptions.pauseTimeBetweenSentences.toMillis())
            }
        }

        _currentSentenceIndex = 0
    }

    private fun getAudioForSentence(sentenceParts: List<String>): List<ByteArray> {
        val sentenceAudio = mutableListOf<ByteArray>()

        val silenceBytes = this.javaClass.getResourceAsStream("/audio/silence-500ms.wav").use { s ->
            s.readBytes()
        }

        val addFullSentenceAudio = {
            val text = sentenceParts.joinToString(separator = " ")
            val audio = _audioCache.getAudioForText(text)
            sentenceAudio.add(audio)
        }

        if (dictateOptions.readFullSentenceAtStart) {
            addFullSentenceAudio()
        }

        sentenceParts.forEach { part ->
            for (i in dictateOptions.partRepetitions downTo 0) {
                val partAudio = _audioCache.getAudioForText(part)
                sentenceAudio.add(partAudio)

                if (dictateOptions.pauseTimeBetweenRepetitions > Duration.ZERO && partAudio.isNotEmpty()) {
                    val sleepDuration = dictateOptions.pauseTimeBetweenSentences.toMillis()
                    val pauseCount = (sleepDuration / 500.0f).roundToInt()

                    for (n in 0..pauseCount) {
                        sentenceAudio.add(silenceBytes)
                    }
                }
            }
        }

        if (dictateOptions.readFullSentenceAtEnd) {
            addFullSentenceAudio()
        }

        return sentenceAudio
    }

    private fun dictateSentence(index: Int) {
        if (_sentences.lastIndex < index) {
            return
        }

        val sentence = _sentences[index]
        dictateSentence(sentence)
    }

    private fun dictateSentence(sentenceParts: List<String>) {
        val readFullSentence = {
            val sentenceText = sentenceParts.joinToString(separator = " ")
            val sentenceAudio = _audioCache.getAudioForText(sentenceText)
            logger.debug { "plaing back audio for \"$sentenceText\"" }
            _audioPlayer.play(sentenceAudio)
            _audioPlayer.waitUntilPlayingIsOver()

            if (paused) {
                _pausedAtPart = 0
                throw InterruptedException("playback paused")
            }
        }

        if (dictateOptions.readFullSentenceAtStart && _pausedAtPart == 0) {
            readFullSentence()
        }

        sentenceParts.forEachIndexed { idx, part ->
            if (sentenceParts.size > _pausedAtPart && idx < _pausedAtPart) {
                return@forEachIndexed
            }

            for (i in dictateOptions.partRepetitions downTo 0) {
                val partAudio = _audioCache.getAudioForText(part)
                logger.debug { "playing back audio for \"$part\"" }
                _audioPlayer.play(partAudio)
                _audioPlayer.waitUntilPlayingIsOver()

                if (paused) {
                    _pausedAtPart = idx
                    throw InterruptedException("playback paused")
                }

                if (dictateOptions.pauseTimeBetweenRepetitions > Duration.ZERO) {
                    TimeUnit.MILLISECONDS.sleep(dictateOptions.pauseTimeBetweenRepetitions.toMillis())
                }

                _pausedAtPart = 0
            }
        }

        if (dictateOptions.readFullSentenceAtEnd) {
            readFullSentence()
        }
    }

    fun generateAudioFromDicate(): ByteArray {
        val audioPartsList = mutableListOf<ByteArray>()

        parseTextIntoSentencesIfNecessary()

        val silenceBytes = this.javaClass.getResourceAsStream("/audio/silence-500ms.wav").use { s ->
            s.readBytes()
        }

        if (dictateOptions.readFullDictateOnce) {
            _sentences.forEach { sentence ->
                val sentenceText = sentence.joinToString(separator = " ")
                val sentenceAudio = _audioCache.getAudioForText(sentenceText)

                audioPartsList.add(sentenceAudio)
            }
        }

        _sentences.forEach { sentence ->
            val sentenceAudio = getAudioForSentence(sentence)
            audioPartsList.addAll(sentenceAudio)

            // wait configured time between sentences
            if (dictateOptions.pauseTimeBetweenSentences > Duration.ZERO && sentenceAudio.isNotEmpty()) {
                var pauseDuration =
                    dictateOptions.pauseTimeBetweenSentences - dictateOptions.pauseTimeBetweenRepetitions
                if (pauseDuration < Duration.ZERO) {
                    pauseDuration = Duration.ZERO
                }

                val pauseDurationMillis = pauseDuration.toMillis()
                val pauseCount = (pauseDurationMillis / 500.0f).roundToInt()

                for (n in 0..pauseCount) {
                    audioPartsList.add(silenceBytes)
                }
            }
        }

        return mergeAudioStreams(audioPartsList, AudioFileFormat.Type.WAVE)
    }

    fun dictatePreviousSentence() {
        parseTextIntoSentencesIfNecessary()

        if (_currentSentenceIndex > 0) {
            _pausedAtPart = 0
            paused = false

            _currentSentenceIndex -= 1
            dictateSentence(_currentSentenceIndex)
        }
    }

    fun dictateCurrentSentence() {
        parseTextIntoSentencesIfNecessary()

        if (_sentences.lastIndex <= _currentSentenceIndex) {
            _pausedAtPart = 0
            paused = false

            dictateSentence(_currentSentenceIndex)
        }
    }

    fun dictateNextSentence() {
        parseTextIntoSentencesIfNecessary()

        if ((_currentSentenceIndex + 1) < _sentences.lastIndex) {
            _pausedAtPart = 0
            paused = false

            _currentSentenceIndex += 1
            dictateSentence(_currentSentenceIndex)
        }
    }

    fun stopDictate() {
        paused = true
        _audioPlayer.stop()
        _currentSentenceIndex = 0
        _pausedAtPart = 0
    }

    fun pauseDictate() {
        paused = true
        _audioPlayer.stop() // _audioPlayer.pause()
    }

    fun resumeDictate() {
        paused = false
        _audioPlayer.resume()
    }

    private fun parseTextIntoSentencesIfNecessary() {
        if (_textChanged) {
            _sentences = if (!dictateText.isNullOrBlank()) {
                _nlpProcessor.dissectText(dictateText)
            } else {
                listOf()
            }

            _textChanged = false
        }
    }

    private fun mergeAudioStreams(audioInputByteArrays: List<ByteArray>, audioType: AudioFileFormat.Type): ByteArray {
        if (audioInputByteArrays.isEmpty()) {
            return ByteArray(0)
        }
        if (audioInputByteArrays.size == 1) {
            return audioInputByteArrays.first()
        }

        val clipA = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioInputByteArrays[0]))
        val clipB = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioInputByteArrays[1]))
        var appendedFiles = AudioInputStream(
            SequenceInputStream(clipA, clipB),
            clipA.format, clipA.frameLength + clipB.frameLength
        )


        for (i in 1 until audioInputByteArrays.size - 1) {
            val clip = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioInputByteArrays[i + 1]))

            appendedFiles = AudioInputStream(
                SequenceInputStream(appendedFiles, clip),
                appendedFiles.format,
                appendedFiles.frameLength + clip.frameLength
            )
        }

        val output = ByteArrayOutputStream()
        try {
            AudioSystem.write(appendedFiles, audioType, output)
        } catch (ex: Exception) {
            logger.error { "exception Occurred: $ex" }
            return ByteArray(0)
        }

        return output.toByteArray()
    }

    // ---[ member variables
    var dictateText: String = ""
        set(value) {
            field = value
            _currentSentenceIndex = 0
            _textChanged = true
        }

    var dictateOptions: DictateOptions
        set(value) {
            // don't to anything if it's the same input
            if (field == value) {
                return
            }

            // recreate the NLP pipeline if the language changed
            if (field == null || field.voice.language != value.voice.language) {
                _nlpProcessor = NLPProcessor(
                    value.voice.language, value.pronouncePunctation,
                    value.charactersPerSentencePartTarget, value.charactersPerSentencePartMax
                )
                parseTextIntoSentencesIfNecessary()
            }

            // update the NLP pipeline if the part parameters changes
            if (field != null) {
                if (field.charactersPerSentencePartTarget != value.charactersPerSentencePartTarget ||
                    field.charactersPerSentencePartMax != value.charactersPerSentencePartMax
                ) {
                    _nlpProcessor.targetPartLength = value.charactersPerSentencePartTarget
                    _nlpProcessor.maxPartLength = value.charactersPerSentencePartMax

                    parseTextIntoSentencesIfNecessary()
                }
            }

            // invalidate the audio cache if the voice changed
            if (field == null || field.voice != value.voice || field.speakingSpeed != value.speakingSpeed) {
                _audioCache = AudioCache(AudioEncoding.LINEAR16, value.voice, value.speakingSpeed)
            }

            field = value
        }


    // ---[ private members
    private val _audioPlayer: AudioPlayer = AudioPlayer()
    private var _audioCache: AudioCache
    private var _nlpProcessor: NLPProcessor
    private var _sentences: List<List<String>> = listOf()
    private var _currentSentenceIndex = 0
    private var _textChanged = true
    private var _pausedAtPart = 0
}