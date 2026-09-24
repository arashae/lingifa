package com.example.data.exporter

import com.example.data.model.VocabularyItem
import org.json.JSONArray
import org.json.JSONObject

object VocabularyExporter {

    fun exportToCsv(items: List<VocabularyItem>): String {
        val sb = StringBuilder()
        sb.append("word,persianMeaning,englishDefinition,example,examplePersian,cefrLevel,tags\n")
        for (item in items) {
            val cleanWord = escapeCsv(item.word)
            val cleanMeaning = escapeCsv(item.persianMeaning)
            val cleanDef = escapeCsv(item.englishDefinition)
            val cleanEx = escapeCsv(item.example)
            val cleanExFa = escapeCsv(item.examplePersian)
            val cleanLevel = escapeCsv(item.cefrLevel)
            val cleanTags = escapeCsv(item.tags.joinToString(";"))
            sb.append("$cleanWord,$cleanMeaning,$cleanDef,$cleanEx,$cleanExFa,$cleanLevel,$cleanTags\n")
        }
        return sb.toString()
    }

    fun exportToJson(items: List<VocabularyItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("word", item.word)
            obj.put("ipa", item.ipa)
            obj.put("persianMeaning", item.persianMeaning)
            obj.put("englishDefinition", item.englishDefinition)
            obj.put("example", item.example)
            obj.put("examplePersian", item.examplePersian)
            obj.put("cefrLevel", item.cefrLevel)
            obj.put("partOfSpeech", item.partOfSpeech)

            val tagsArr = JSONArray()
            item.tags.forEach { tagsArr.put(it) }
            obj.put("tags", tagsArr)

            val synArr = JSONArray()
            item.synonyms.forEach { synArr.put(it) }
            obj.put("synonyms", synArr)

            array.put(obj)
        }
        return array.toString(2)
    }

    private fun escapeCsv(text: String): String {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }
}
