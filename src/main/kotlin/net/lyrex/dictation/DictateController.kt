package net.lyrex.dictation

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.google.cloud.texttospeech.v1.AudioEncoding
import mu.KotlinLogging

import net.lyrex.audio.*
import net.lyrex.nlp.NLPProcessor


private val logger = KotlinLogging.logger {}

class DictateController {
    constructor(inputText: String, dictateOptions: DictateOptions) {
        this.dictateText = inputText
        this.dictateOptions = dictateOptions

        this._audioCache = AudioCache(AudioEncoding.LINEAR16, dictateOptions.voice)
        this._nlpProcessor = NLPProcessor(this.dictateOptions.language, dictateOptions.pronouncePunctation)
    }

    // ---[ member methods
    fun dictateFullText() {
        if (_textChanged) {
            parseTextIntoSentences()
        }

        // actually play audio for dictate now
        if (dictateOptions.readFullDictateOnce) {
            _sentences.forEach { sentence ->
                val sentenceText = sentence.joinToString(separator = " ")
                val sentenceAudio = _audioCache.getAudioForText(sentenceText)
                logger.debug { "plaing back audio for \"$sentenceText\"" }
                _audioPlayer.play(sentenceAudio)
                _audioPlayer.waitUntilPlayingIsOver()
            }
        }

        // dictate sentence by sentence
        _sentences.forEachIndexed { index, sentence ->
            _currentSentenceIndex = index
            dictateSentence(sentence)

            // wait configured time between sentences
            if (dictateOptions.pauseTimeBetweenSentences > Duration.ZERO) {
                TimeUnit.MILLISECONDS.sleep(dictateOptions.pauseTimeBetweenSentences.toMillis())
            }
        }
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
        }

        if (dictateOptions.readFullSentenceAtStart) {
            readFullSentence()
        }

        sentenceParts.forEach { part ->
            for (i in dictateOptions.partRepetitions downTo 0) {
                val partAudio = _audioCache.getAudioForText(part)
                logger.debug { "playing back audio for \"$part\"" }
                _audioPlayer.play(partAudio)
                _audioPlayer.waitUntilPlayingIsOver()

                if (dictateOptions.pauseTimeBetweenRepetitions > Duration.ZERO) {
                    TimeUnit.MILLISECONDS.sleep(dictateOptions.pauseTimeBetweenRepetitions.toMillis())
                }
            }
        }

        if (dictateOptions.readFullSentenceAtEnd) {
            readFullSentence()
        }
    }

    fun dictatePreviousSentence() {
        if (_textChanged) {
            parseTextIntoSentences()
        }

        if (_currentSentenceIndex > 0) {
            _currentSentenceIndex -= 1
            dictateSentence(_currentSentenceIndex)
        }
    }

    fun dictateCurrentSentence() {
        if (_textChanged) {
            parseTextIntoSentences()
        }

        if (_sentences.lastIndex <= _currentSentenceIndex) {
            dictateSentence(_currentSentenceIndex)
        }
    }

    fun dictateNextSentence() {
        if (_textChanged) {
            parseTextIntoSentences()
        }

        if ((_currentSentenceIndex + 1) < _sentences.lastIndex) {
            _currentSentenceIndex += 1
            dictateSentence(_currentSentenceIndex)
        }
    }

    fun pauseDictate() {
        _audioPlayer.pause()
    }

    fun resumeDictate() {
        _audioPlayer.resume()
    }

    private fun parseTextIntoSentences() {
        if (_textChanged) {
            _sentences = if (!dictateText.isNullOrBlank()) {
                _nlpProcessor.dissectText(dictateText)
            } else {
                listOf()
            }

            _textChanged = false
        }
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
                _nlpProcessor = NLPProcessor(value.voice.language, value.pronouncePunctation)
                parseTextIntoSentences()
            }

            // invalidate the audio cache if the voice changed
            if (field == null || field.voice != value.voice) {
                _audioCache = AudioCache(AudioEncoding.LINEAR16, value.voice)
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
}