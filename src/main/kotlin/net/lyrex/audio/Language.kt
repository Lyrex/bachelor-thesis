package net.lyrex.audio

enum class Language(val languageString: String) {
    German("de-DE"),
    EnglishUS("en-US"),
    EnglishUK("en-GB"),
    Spanish("es-ES"),
    French("fr-FR");

    override fun toString(): String {
        return when (this) {
            German    -> "Deutsch"
            EnglishUS -> "Englisch (US)"
            EnglishUK -> "Englisch (UK)"
            Spanish   -> "Spanisch"
            French    -> "Französisch"
            else      -> super.toString()
        }
    }

    fun toCoreNlpString(): String {
        return when (this) {
            German               -> "german"
            EnglishUS, EnglishUK -> "english"
            Spanish              -> "spanish"
            French               -> "french"
            else                 -> super.toString().toLowerCase()
        }
    }
}