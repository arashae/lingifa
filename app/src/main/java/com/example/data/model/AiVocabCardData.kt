package com.example.data.model

data class AiVocabCardData(
    val word: String,
    val phonetic: String = "",
    val partOfSpeech: String = "noun",
    val persianTranslation: String = "",
    val englishDefinition: String = "",
    val exampleSentenceEn: String = "",
    val exampleSentenceFa: String = "",
    val collocations: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val cefrLevel: String = "B2",
    val ieltsTipFa: String = "",
    val persianCommonMistake: String = "",
    val isGeneratedByAi: Boolean = true
)
