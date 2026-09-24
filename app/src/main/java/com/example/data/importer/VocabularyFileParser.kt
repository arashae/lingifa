package com.example.data.importer

import com.example.data.model.VocabularyItem
import org.json.JSONArray
import org.json.JSONObject

enum class DuplicateAction {
    SKIP,
    UPDATE,
    MERGE,
    KEEP_BOTH
}

data class ParsedImportItem(
    val word: String,
    val persianMeaning: String = "",
    val englishDefinition: String = "",
    val example: String = "",
    val cefrLevel: String = "B2",
    val tags: List<String> = emptyList(),
    var isDuplicate: Boolean = false,
    var isSelected: Boolean = true,
    var existingItem: VocabularyItem? = null
)

object VocabularyFileParser {

    /**
     * Parses plain text (e.g. word = meaning, or single word per line)
     */
    fun parsePlainText(text: String): List<ParsedImportItem> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<ParsedImportItem>()

        for (line in lines) {
            when {
                line.contains("=") -> {
                    val parts = line.split("=", limit = 2)
                    val w = parts[0].trim()
                    val m = parts[1].trim()
                    if (w.isNotEmpty()) {
                        result.add(ParsedImportItem(word = w, persianMeaning = m))
                    }
                }
                line.contains("-") && !line.startsWith("-") -> {
                    val parts = line.split("-", limit = 2)
                    val w = parts[0].trim()
                    val m = parts[1].trim()
                    if (w.isNotEmpty()) {
                        result.add(ParsedImportItem(word = w, persianMeaning = m))
                    }
                }
                line.contains(":") -> {
                    val parts = line.split(":", limit = 2)
                    val w = parts[0].trim()
                    val m = parts[1].trim()
                    if (w.isNotEmpty()) {
                        result.add(ParsedImportItem(word = w, persianMeaning = m))
                    }
                }
                else -> {
                    // Single word per line
                    val w = line.trim()
                    if (w.isNotEmpty()) {
                        result.add(ParsedImportItem(word = w))
                    }
                }
            }
        }
        return result
    }

    /**
     * Parses CSV or TSV content with automatic column detection
     */
    fun parseCsv(csvContent: String, delimiter: Char = ','): List<ParsedImportItem> {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val actualDelimiter = when {
            lines.first().contains('\t') -> '\t'
            lines.first().contains(';') -> ';'
            else -> delimiter
        }

        val headerTokens = parseCsvLine(lines.first(), actualDelimiter).map { it.lowercase().trim() }
        val hasHeader = headerTokens.any { it.contains("word") || it.contains("meaning") || it.contains("level") }

        var wordCol = -1
        var meaningCol = -1
        var defCol = -1
        var exampleCol = -1
        var levelCol = -1
        var tagsCol = -1

        if (hasHeader) {
            headerTokens.forEachIndexed { index, token ->
                when {
                    token == "word" || token == "english" || token == "term" -> wordCol = index
                    token.contains("meaning") || token.contains("persian") || token.contains("fa") || token == "translation" -> meaningCol = index
                    token.contains("def") || token.contains("meaning_en") -> defCol = index
                    token.contains("example") || token.contains("sentence") -> exampleCol = index
                    token.contains("level") || token.contains("cefr") -> levelCol = index
                    token.contains("tag") || token.contains("category") -> tagsCol = index
                }
            }
        } else {
            wordCol = 0
            meaningCol = 1
        }

        val rowsToParse = if (hasHeader) lines.drop(1) else lines
        val result = mutableListOf<ParsedImportItem>()

        for (line in rowsToParse) {
            val tokens = parseCsvLine(line, actualDelimiter)
            val word = tokens.getOrNull(if (wordCol != -1) wordCol else 0)?.trim() ?: ""
            if (word.isEmpty()) continue

            val meaning = if (meaningCol != -1 && meaningCol < tokens.size) tokens[meaningCol].trim() else ""
            val def = if (defCol != -1 && defCol < tokens.size) tokens[defCol].trim() else ""
            val example = if (exampleCol != -1 && exampleCol < tokens.size) tokens[exampleCol].trim() else ""
            val level = if (levelCol != -1 && levelCol < tokens.size) tokens[levelCol].trim() else "B2"
            val tags = if (tagsCol != -1 && tagsCol < tokens.size) {
                tokens[tagsCol].split(";").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()

            result.add(
                ParsedImportItem(
                    word = word,
                    persianMeaning = meaning,
                    englishDefinition = def,
                    example = example,
                    cefrLevel = if (level.isNotEmpty()) level else "B2",
                    tags = tags
                )
            )
        }
        return result
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> insideQuotes = !insideQuotes
                ch == delimiter && !insideQuotes -> {
                    tokens.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        tokens.add(current.toString().trim())
        return tokens
    }

    /**
     * Parses JSON array containing objects with word, persianMeaning, level, tags
     */
    fun parseJson(jsonString: String): List<ParsedImportItem> {
        val result = mutableListOf<ParsedImportItem>()
        return try {
            val trimmed = jsonString.trim()
            val array = if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("vocabulary") ?: obj.optJSONArray("words") ?: JSONArray()
            } else {
                JSONArray(trimmed)
            }

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val word = obj.optString("word", "").trim()
                if (word.isEmpty()) continue

                val meaning = obj.optString("persianMeaning", obj.optString("meaning", ""))
                val def = obj.optString("englishDefinition", obj.optString("definition", ""))
                val ex = obj.optString("example", "")
                val level = obj.optString("cefrLevel", obj.optString("level", "B2"))

                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tagsList.add(tagsArr.optString(t))
                    }
                }

                result.add(
                    ParsedImportItem(
                        word = word,
                        persianMeaning = meaning,
                        englishDefinition = def,
                        example = ex,
                        cefrLevel = level,
                        tags = tagsList
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
