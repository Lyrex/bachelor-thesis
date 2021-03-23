package net.lyrex.nlp


import edu.stanford.nlp.pipeline.CoreSentence
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.StringUtils
import net.lyrex.audio.Language
import java.lang.Exception
import java.util.*

fun Tree.isSentence(): Boolean = this.label().value().length == 2 && this.label().value().endsWith("P")

class NLPProcessor(
    private val lang: Language, private val pronouncePunctation: Boolean,
    var targetPartLength: Int, var maxPartLength: Int
) {
    private var pipeline: StanfordCoreNLP

    init {
        val props: Properties =
            StringUtils.argsToProperties("-props", "StanfordCoreNLP-${lang.toCoreNlpString()}.properties")
        props.setProperty("language", lang.toCoreNlpString())
        props.setProperty("annotators", "tokenize,ssplit,pos,parse")
        props.setProperty("coref.algorithm", "neural")
        pipeline = StanfordCoreNLP(props)
    }

    private fun parseSubTree(tree: Tree, openn: Boolean = false): String {
        var result = ""

        when {
            tree.isPrePreTerminal -> {
                result = "[${tree.spanString()}]"
            }
            tree.isPreTerminal    -> {
                result += "${tree.spanString()} "
            }
            tree.isPhrasal        -> {
                var s = ""
                var open = openn
                for (c in tree.children()) {
                    if (!open && c.isPreTerminal) {
                        s = s.removePrefix(" ") + "["
                        open = true
                    }
                    if (open && !c.isPreTerminal &&
                        !(tree.label().value().length == 3 &&
                                tree.label().value().startsWith("C") &&
                                tree.label().value().endsWith("P"))
                    ) {
                        s = s.removeSuffix(" ") + "]"
                        open = false
                    }
                    s += parseSubTree(c, open)
                }

                if (open) s = s.removeSuffix(" ") + "]"
                result += s
            }
            else                  -> {
                throw Exception("this should never happen")
            }
        }

        // post-process results
        result = result
            .replace("[[", "[")
            .replace("]]", "]")
        result = result
            .replace("][,]", ", ]")
            .replace("][.]", ".]")
        result = result
            .replace(" ,", ", ")

        return result
    }

    private fun dissectSentence(sentence: CoreSentence): List<String> {
        val constituencyTree: Tree = sentence.constituencyParse()

        val isSentence = { node: Tree -> node.label().value().length == 2 && node.label().value().endsWith("P") }
        val isNoSentence = { node: Tree -> !isSentence(node) }

        val parsed = parseSubTree(constituencyTree)
        val parts: List<String> = parsed.split("[", "]")

        val preProcessedParts = flattenOncePass(parts).toMutableList()
        fixPunctationPass(preProcessedParts)

        val processedList = parsePartsPass(preProcessedParts)
        val correctedWhitespace = fixWhitespacePass(processedList)

        val resultList: List<String> = if (pronouncePunctation) {
            makePunctationPronouncedPass(correctedWhitespace)
        } else {
            correctedWhitespace
        }
/*
        val dependencyGraph: SemanticGraph = sentence.dependencyParse()

        println(dependencyGraph.firstRoot.value())
        println(dependencyGraph.edgeCount())
        println(dependencyGraph.edgeListSorted())

        for (node in constituencyTree) {
            if (node.isPrePreTerminal || node.nodeString().matches(Regex("AUX|VERB|PUNCT"))) {
                parts.add(node.spanString())
            } else if (node.label().value() == "NP") {
                var s = ""
                for (cs in node.children()) {
                    if (!isSentence(cs) && cs.isPreTerminal) {
                        s += cs.spanString() + " "
                    }
                }
                parts.add(s.removeSuffix(" "))
            }
        }
*/


        return resultList
    }

    private fun parsePartsPass(parts: List<String>): List<String> {
        var skipN = 0

        val processedList = mutableListOf<String>()
        for ((i, part) in parts.withIndex()) {
            if (skipN > 0) {
                skipN -= 1
                continue
            }

            val partLen = part.length
            if (partLen == 0) {
                continue
            }

            val getPartLength = { n: Int ->
                when {
                    parts.lastIndex > n -> parts[n].length
                    else                -> 99999
                }
            }

            if (partLen > maxPartLength || containsStopPunctation(part)) {
                processedList.add(part)
                continue
            }

            var currentPart: String = part
            for (n in 0..(parts.lastIndex - i)) {
                val currentPartNumber = i + n
                val partsRemaining = (parts.lastIndex - i - n) + 1
                val isLastPart = currentPartNumber >= (parts.lastIndex - 2)

                val nextPart = parts[i + n + 1]
                if (currentPart.length < targetPartLength
                    && (partsRemaining > 2 || isLastPart)
                    && currentPart.length + getPartLength(i + n) < targetPartLength
                ) {
                    currentPart += " $nextPart"
                    skipN += 1

                    if (containsStopPunctation(currentPart)) {
                        break
                    }
                } else {
                    break
                }
            }

            processedList.add(currentPart)
        }

        return processedList
            .filter { s -> s.isNotEmpty() }
            .map { s -> s.replace("  ", " ") }
            .map { s -> s.replace("  ", " ") }
            .map { s -> s.trim() }
    }

    private fun flattenOncePass(parts: List<String>): List<String> {
        val preProcessedParts = mutableListOf<String>()

        var skip = 0
        parts.forEachIndexed bp@{ index, s ->
            if (skip > 0) {
                skip -= 1
                return@bp
            }

            if (s.isEmpty()) {
                return@bp
            }

            var current = s
            for (i in 0..(parts.lastIndex - index)) {
                if (parts.lastIndex > index + i + 1) {
                    if (parts[index + i + 1].isNotEmpty()) {
                        current += " " + parts[index + i + 1]
                        skip += 1
                    } else {
                        break
                    }
                }
            }
            preProcessedParts.add(current)
        }

        return preProcessedParts
            .filter { s -> s.isNotEmpty() }
            .map { s -> s.replace("  ", " ") }
            .map { s -> s.replace("  ", " ") }
            .map { s -> s.trim() }
    }

    private fun fixPunctationPass(parts: MutableList<String>) {
        parts.forEachIndexed { index, s ->
            if (s.isNotEmpty()) {
                if (parts.lastIndex > index + 1) {
                    val next = parts[index + 1]

                    if (next.isNotEmpty()) {
                        when {
                            next.startsWith('.') -> {
                                parts[index] += "."
                                parts[index + 1] = next.removePrefix(".").trimStart()
                            }
                            next.startsWith('!') -> {
                                parts[index] += "!"
                                parts[index + 1] = next.removePrefix("!").trimStart()
                            }
                            next.startsWith('?') -> {
                                parts[index] += "?"
                                parts[index + 1] = next.removePrefix("?").trimStart()
                            }
                            next.startsWith(',') -> {
                                parts[index] += ","
                                parts[index + 1] = next.removePrefix(",").trimStart()
                            }
                            next.contains(':')   -> {
                                val ps = next.split(":")

                                parts[index] += " " + ps[0].trim() + ":"
                                parts[index + 1] = next.removeRange(0..next.indexOf(":")).trimStart()
                            }
                            next.startsWith(';') -> {
                                parts[index] += ";"
                                parts[index + 1] = next.removePrefix(";").trimStart()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fixWhitespacePass(parts: List<String>): List<String> {
        val result = mutableListOf<String>()

        parts.forEach { s ->
            var current = s

            if (current.isNotEmpty()) {
                current = current.replace("  ", " ")
                current = current.replace(" .", ".")
                current = current.replace(" !", "!")
                current = current.replace(" ?", "?")
                current = current.replace(" ,", ",")
                current = current.replace(" :", ":")
                current = current.replace("\" ", "\"")
                current = current.replace(" \"", "\"")
            }

            result.add(current)
        }

        return result
    }

    private fun makePunctationPronouncedPass(parts: List<String>): List<String> {
        val result = mutableListOf<String>()

        parts.forEach { s ->
            val current = if (s.isNotEmpty()) {
                replacePunctation(s)
            } else {
                s
            }

            result.add(current)
        }

        return result
    }

    private fun replacePunctation(input: String): String {
        return when (lang) {
            Language.German                        ->
                input.replace(".", " Punkt.")
                    .replace(",", " Komma,")
                    .replace(":", " Doppelpunkt:")
                    .replace("!", " Ausrufezeichen!")
                    .replace("?", " Fragezeichen?")
                    .replace(";", " Semikolon;")
                    .replace("\"", " Anführungszeichen ")
                    .replace("[", " Paranthese auf")
                    .replace("]", " Paranthese zu")
                    .replace("-", " Bindestrich")
                    .replace("»", "Anführungszeichen ")
                    .replace("«", " Anführungszeichen")
                    .replace("/", " Schrägstrich")
                    .replace("(", " Klammer auf")
                    .replace(")", " Klammer zu")
                    .replace("'", " Anführungszeichen")
                    .replace("-RRB-", " Klammer zu")
                    .replace("-LRB-", " Klammer auf")
            Language.EnglishUS, Language.EnglishUK ->
                input.replace(".", " dot")
                    .replace(",", " comma")
                    .replace(":", " colon")
                    .replace("!", " exclamation mark")
                    .replace("?", " question mark")
                    .replace(";", " semicolon")
                    .replace("\"", " quotation marks")
                    .replace("[", " parenthesis on")
                    .replace("]", " parenthesis closed")
                    .replace("-", " hyphen")
                    .replace("»", " quotation marks")
                    .replace("«", " quotation marks")
                    .replace("'", " quotation marks")
                    .replace("/", " slash")
                    .replace("(", " brackets on")
                    .replace(")", " brackets closed")
                    .replace("-RRB-", " brackets on")
                    .replace("-LRB-", " brackets closed")
            Language.Spanish                       ->
                input.replace(".", " punto")
                    .replace(",", " coma")
                    .replace(":", " los dos puntos")
                    .replace("!", " signos de exclamación")
                    .replace("¡", " signos de exclamación")
                    .replace("?", " signos de interrogación")
                    .replace("¿", " signos de interrogación")
                    .replace(";", " punto y coma")
                    .replace("\"", " comillas")
                    .replace("[", " corchetes")
                    .replace("]", " corchetes")
                    .replace("-", " guión")
                    .replace("»", " comillas")
                    .replace("«", " comillas")
                    .replace("'", " comillas")
                    .replace("/", " barra oblicua")
                    .replace("(", " paréntesis")
                    .replace(")", " paréntesis")
                    .replace("-RRB-", " paréntesis")
                    .replace("-LRB-", " paréntesis")
            Language.French                        ->
                input.replace(".", " point")
                    .replace(",", " virgule")
                    .replace(":", " double point")
                    .replace("!", " point d'exclamation")
                    .replace("?", " point d'interrogation")
                    .replace(";", " point d'interrogation")
                    .replace("\"", " guillemets")
                    .replace("[", " parenthèses")
                    .replace("]", " parenthèses")
                    .replace("-", " piret")
                    .replace("»", " guillemets")
                    .replace("«", " guillemets")
                    .replace("'", " guillemets")
                    .replace("/", " sabrer")
                    .replace("(", " parenthèses")
                    .replace(")", " parenthèses")
                    .replace("-RRB-", " parenthèses")
                    .replace("-LRB-", " parenthèses")
        }
    }

    fun dissectText(input: String): List<List<String>> {
        val doc = pipeline.processToCoreDocument(input)

        val sentences = doc.sentences()

        val parsedSentences = mutableListOf<List<String>>()
        for (sentence in sentences) {
            parsedSentences.add(dissectSentence(sentence))
        }

/*
        val sentence = sentences[4]

        // list of the part-of-speech tags for the second sentence
        val posTags = sentence.posTags()
        println("Example: pos tags")
        println(posTags)
        println()

        // constituency parse for the second sentence
        val constituencyParse = sentence.constituencyParse()
        println(constituencyParse.childrenAsList)
        println(constituencyParse.depth())
        println(constituencyParse.flatten())
        println(constituencyParse.pennPrint())
        // println(constituencyParse.subTrees())

        println("Example: constituency parse")
        println(constituencyParse)
        println()
*/


        // dependency parse for the second sentence
//        val dependencyParse = sentences[1].dependencyParse()
//        println("Example: dependency parse")
//        println(dependencyParse)
//        println()

        return parsedSentences
    }

    private fun containsStopPunctation(s: String) =
        s.contains('.') || s.contains('!') || s.contains('?') || s.contains(';') || s.contains(':')

    private fun containsPunctation(s: String) = containsStopPunctation(s) || s.contains(',')
}