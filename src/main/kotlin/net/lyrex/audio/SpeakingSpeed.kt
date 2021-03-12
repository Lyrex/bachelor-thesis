package net.lyrex.audio

enum class SpeakingSpeed(val speed: Double) {
    VerySlow(0.70),
    Slow(0.85),
    Normal(1.0),
    Fast(1.15),
    VeryFast(1.3);

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