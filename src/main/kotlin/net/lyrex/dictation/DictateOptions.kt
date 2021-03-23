package net.lyrex.dictation

import net.lyrex.audio.Language
import net.lyrex.audio.SpeakingSpeed
import net.lyrex.audio.Voice

import java.time.Duration


data class DictateOptions(
    val language: Language,
    val readFullDictateOnce: Boolean,
    val readFullSentenceAtStart: Boolean,
    val readFullSentenceAtEnd: Boolean,
    val partRepetitions: Int,
    val pauseTimeBetweenRepetitions: Duration,
    val pauseTimeBetweenSentences: Duration,
    val pronouncePunctation: Boolean,
    val speakingSpeed: SpeakingSpeed,
    val voice: Voice,
    val hideTextWhileDictating: Boolean,

    val charactersPerSentencePartTarget: Int,
    val charactersPerSentencePartMax: Int,
)