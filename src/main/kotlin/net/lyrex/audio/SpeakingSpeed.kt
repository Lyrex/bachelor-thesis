package net.lyrex.audio

enum class SpeakingSpeed(val speed: Float) {
    VerySlow(0.70f),
    Slow(0.85f),
    Normal(1.0f),
    Fast(1.3f),
    VeryFast(1.5f);

    override fun toString(): String {
        return when (this) {
            VerySlow -> "Sehr langsam"
            Slow -> "Langsam"
            Normal -> "Normal"
            Fast -> "Schnell"
            VeryFast -> "Sehr schnell"
            else -> super.toString()
        }
    }
}