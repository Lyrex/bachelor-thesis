package net.lyrex.audio

import com.google.cloud.texttospeech.v1.SsmlVoiceGender

enum class Gender(val ssmlGender: SsmlVoiceGender) {
    Male(SsmlVoiceGender.MALE),
    Female(SsmlVoiceGender.FEMALE),
    Neutral(SsmlVoiceGender.NEUTRAL);

    override fun toString(): String {
        return when (this) {
            Male -> "männlich"
            Female -> "weiblich"
            Neutral -> "neutral"
            else -> super.toString()
        }
    }
}