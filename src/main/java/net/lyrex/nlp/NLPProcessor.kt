package net.lyrex.nlp


import edu.stanford.nlp.pipeline.CoreSentence
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.util.StringUtils
import java.lang.Exception
import java.util.*

fun Tree.isSentence(): Boolean = this.label().value().length == 2 && this.label().value().endsWith("P")

const val TARGET_TEXT_LENGTH = 20
const val MAX_TEXT_LENGTH = 40

class NLPProcessor {
    val inputString = """Ein Fuchs und ein Esel waren schon seit Jahren ziemlich gute Freunde. Gemeinsam gingen sie sogar auf Nahrungssuche. Als der Fuchs sich zeitweilig von dem Esel trennte, weil er einige Brombeeren erschnüffelt hatte, erblickte er plötzlich einen gewaltigen Löwen vor sich. Da der Fuchs wusste, dass ein Fliehen offenkundig unmöglich war, überspielte er sein Entsetzen und meinte unbekümmert: "Großer, barmherziger König, ich fürchte dich nicht. Doch sollte dich der Hunger plagen, kann ich dir meinen dummen Gefährten als Mahlzeit bringen." Der Löwe versprach, den Fuchs zu verschonen, wenn er im Gegenzug den Esel zu ihm führen würde. Darauf konnte der Fuchs den Esel tatsächlich mit einer List in eine Grube locken und schon kam der Löwe mit dröhnendem Gebrüll angesprungen. Allerdings stürzte er sich nun direkt auf den Fuchs. Die letzten Worte, die der verräterische Fuchs vernahm, lauteten: "Der Esel ist mir sicher, aber dich fresse ich wegen deiner Falschheit zuerst."Die Moral: Den Verrat mag man ausnutzen, aber den Verräter mag man deshalb noch lange nicht."""

    private var pipeline: StanfordCoreNLP

    init {
        val props: Properties = StringUtils.argsToProperties("-props", "StanfordCoreNLP-german.properties")
        props.setProperty("language", "german")
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
            tree.isPreTerminal -> {
                result += "${tree.spanString()} "
            }
            tree.isPhrasal -> {
                var s: String = ""
                var open = openn
                for (c in tree.children()) {
                    if (!open && c.isPreTerminal) {
                        s = s.removePrefix(" ") + "["
                        open = true
                    }
                    if (open && !c.isPreTerminal &&
                            !(tree.label().value().length == 3 && tree.label().value().startsWith("C") && tree.label().value().endsWith("P"))) {
                        s = s.removeSuffix(" ") + "]"
                        open = false
                    }
                    s += parseSubTree(c, open)
                }

                if (open) s = s.removeSuffix(" ") + "]"
                result += s
            }
            else -> {
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

        //println(1)
        val parsed = parseSubTree(constituencyTree)
        //println(parsed)

        //println(2)
        val parts: List<String> = parsed.split("[", "]");

        val preProcessedParts = flattenOnce(parts).toMutableList()
        fixPunctation(preProcessedParts)
        val processedList = parseParts(preProcessedParts)
        val finalList = finalizePunctation(fixWhitespace(processedList))

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


        return finalList
    }

    private fun parseParts(parts: List<String>): List<String> {
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
                    else -> 99999
                }
            }

            if (partLen > MAX_TEXT_LENGTH || containsStopPunctation(part)) {
                processedList.add(part)
                continue
            }

            var currentPart: String = part
            for (n in 0..(parts.lastIndex - i)) {
                val currentPartNumber = i + n
                val partsRemaining = (parts.lastIndex - i - n) + 1
                val isLastPart = currentPartNumber >= (parts.lastIndex - 2)

                val nextPart = parts[i + n + 1]
                if (currentPart.length < TARGET_TEXT_LENGTH
                        && (partsRemaining > 2 || isLastPart)
                        && currentPart.length + getPartLength(i + n) < MAX_TEXT_LENGTH) {
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

    private fun flattenOnce(parts: List<String>): List<String> {
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

    private fun fixPunctation(parts: MutableList<String>) {
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
                            next.startsWith(',') -> {
                                parts[index] += ","
                                parts[index + 1] = next.removePrefix(",").trimStart()
                            }
                            next.contains(':') -> {
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

    private fun fixWhitespace(parts: List<String>): List<String> {
        val result = mutableListOf<String>()

        parts.forEach { s ->
            var current = s

            if (current.isNotEmpty()) {
                current = current.replace("  ", " ")
                current = current.replace(" .", ".")
                current = current.replace(" ,", ",")
                current = current.replace(" :", ":")
                current = current.replace("\" ", "\"")
                current = current.replace(" \"", "\"")
            }

            result.add(current)
        }

        return result
    }

    private fun finalizePunctation(parts: List<String>): List<String> {
        val result = mutableListOf<String>()

        parts.forEach { s ->
            var current = s

            if (current.isNotEmpty()) {
                current = current.replace(".", " Punkt")
                current = current.replace(",", " Komma")
                current = current.replace(":", " Doppelpunkt")
                current = current.replace("\" ", " Anführungszeichen ")
            }

            result.add(current)
        }

        return result
    }

    fun dissectText(input: String): List<List<String>> {
        val doc = pipeline.processToCoreDocument(input)

        val sentences = doc.sentences()

//        println("aauupp")
//        println(dissectSentence(sentences[0]))

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

    private fun containsStopPunctation(s: String) = s.contains('.') || s.contains(';') || s.contains(':')
    private fun containsPunctation(s: String) = containsStopPunctation(s) || s.contains(',')
}