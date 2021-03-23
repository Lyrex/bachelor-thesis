package net.lyrex.audio

import java.lang.IllegalArgumentException
import java.util.EnumMap

import com.google.cloud.texttospeech.v1.SsmlVoiceGender
import com.google.cloud.texttospeech.v1.TextToSpeechClient


class Voice(val language: Language, val gender: Gender, val name: String) {
    companion object {
        @JvmStatic
        private val _cache: MutableMap<Language, List<Voice>> = EnumMap(Language::class.java)

        @JvmStatic
        fun getAvailableVoices(lang: Language): List<Voice> {
            synchronized(_cache) {
                if (_cache.containsKey(lang)) {
                    return _cache[lang]!!
                }

                TextToSpeechClient.create().use { ttsClient ->
                    val response = ttsClient.listVoices(lang.languageString)
                    if (response.voicesCount == 0) {
                        return listOf()
                    }

                    val voices = response.voicesList.map { v ->
                        val gender = when (v.ssmlGender) {
                            SsmlVoiceGender.MALE    -> Gender.Male
                            SsmlVoiceGender.FEMALE  -> Gender.Female
                            SsmlVoiceGender.NEUTRAL -> Gender.Neutral
                            else                    -> {
                                throw IllegalArgumentException("unknown gender")
                            }
                        }

                        Voice(lang, gender, v.name)
                    }

                    _cache[lang] = voices
                    return voices
                }
            }
        }
    }

    override fun toString(): String {
        return "$name ($gender, ${language.languageString})"
    }
}
