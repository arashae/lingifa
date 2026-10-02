package com.example.data.importer

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.VocabularyDao
import com.example.data.local.VocabularyDatasetChunkDao
import com.example.data.local.VocabularyPackItemDao
import com.example.data.local.VocabularySenseDao
import com.example.data.model.VocabularyDatasetChunk
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPackItem
import com.example.data.model.VocabularySense
import com.example.vocab.VocabularyStudyPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Streaming importer for the bundled CEFR / IELTS / TOEFL / GRE banks.
 *
 * Besides importing, this layer performs conservative card-quality cleanup so old bootstrap rows
 * do not permanently override richer bundled definitions/examples. The quality revision is part of
 * the stored chunk version, therefore an app update can re-import all existing chunks once without
 * changing the source dataset files themselves.
 */
object BundledVocabularyImporter {
    private const val TAG = "BundledVocabImporter"
    private const val CATALOG_ASSET = "vocabulary/master_catalog.json"
    private const val IMPORT_QUALITY_VERSION = "q2-english-first"

    data class ImportSummary(
        val importedChunks: Int = 0,
        val skippedChunks: Int = 0,
        val insertedWords: Int = 0,
        val updatedWords: Int = 0,
        val membershipsAdded: Int = 0,
        val errors: List<String> = emptyList()
    )

    private data class ChunkResult(
        val insertedWords: Int,
        val updatedWords: Int,
        val membershipsAdded: Int,
        val processedItems: Int
    )

    suspend fun importBundledCatalog(
        context: Context,
        database: AppDatabase,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao,
        chunkDao: VocabularyDatasetChunkDao,
        vocabularySenseDao: VocabularySenseDao
    ): ImportSummary {
        val catalogText = try {
            context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return ImportSummary()
        }

        val catalog = JSONObject(catalogText)
        val chunks = catalog.optJSONArray("chunks") ?: JSONArray()

        var importedChunks = 0
        var skippedChunks = 0
        var insertedWords = 0
        var updatedWords = 0
        var membershipsAdded = 0
        val errors = mutableListOf<String>()

        val existingChunksMap = try {
            chunkDao.getAll().associateBy { it.chunkId }
        } catch (_: Exception) {
            emptyMap()
        }

        for (index in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(index)
            val chunkId = chunk.getString("id")
            val sourceVersion = chunk.getString("version")
            val effectiveVersion = "$sourceVersion+$IMPORT_QUALITY_VERSION"
            val packId = chunk.getString("packId")
            val assetPath = chunk.getString("asset")
            val expectedItems = chunk.optInt("expectedItems", -1)

            val existingChunk = existingChunksMap[chunkId]
            if (existingChunk?.version == effectiveVersion &&
                (expectedItems < 0 || existingChunk.itemCount == expectedItems)
            ) {
                skippedChunks++
                continue
            }

            try {
                val result = importJsonlChunk(
                    context = context,
                    database = database,
                    assetPath = assetPath,
                    packId = packId,
                    datasetVersion = effectiveVersion,
                    vocabularyDao = vocabularyDao,
                    packItemDao = packItemDao,
                    vocabularySenseDao = vocabularySenseDao
                )

                if (expectedItems >= 0) {
                    require(result.processedItems == expectedItems) {
                        "Chunk $chunkId expected $expectedItems items but processed ${result.processedItems}"
                    }
                }

                chunkDao.upsert(
                    VocabularyDatasetChunk(
                        chunkId = chunkId,
                        packId = packId,
                        version = effectiveVersion,
                        itemCount = result.processedItems
                    )
                )

                importedChunks++
                insertedWords += result.insertedWords
                updatedWords += result.updatedWords
                membershipsAdded += result.membershipsAdded
                kotlinx.coroutines.yield()
            } catch (t: Throwable) {
                val message = "$chunkId: ${t.message ?: t::class.java.simpleName}"
                Log.e(TAG, "Failed to import vocabulary chunk $chunkId", t)
                errors += message
            }
        }

        return ImportSummary(
            importedChunks = importedChunks,
            skippedChunks = skippedChunks,
            insertedWords = insertedWords,
            updatedWords = updatedWords,
            membershipsAdded = membershipsAdded,
            errors = errors
        )
    }

    private suspend fun importJsonlChunk(
        context: Context,
        database: AppDatabase,
        assetPath: String,
        packId: String,
        datasetVersion: String,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao,
        vocabularySenseDao: VocabularySenseDao
    ): ChunkResult {
        val lines = context.assets.open(assetPath).bufferedReader().use { reader ->
            reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }

        if (lines.isEmpty()) {
            return ChunkResult(0, 0, 0, 0)
        }

        val parsedItems = lines.map { line ->
            val json = JSONObject(line)
            val word = json.getString("word").trim()
            val persianMeaning = json.getString("persianMeaning").trim()
            require(word.isNotEmpty()) { "Vocabulary word cannot be empty" }
            require(persianMeaning.isNotEmpty()) { "Persian meaning is required for $word" }

            val normalized = word.lowercase(Locale.US).trim()
            Triple(
                normalized,
                json.toVocabularyItem(
                    normalizedWord = normalized,
                    datasetVersion = datasetVersion
                ),
                json.toVocabularySenses(normalized, persianMeaning)
            )
        }

        val normalizedWords = parsedItems.map { it.first }
        val existingMap = vocabularyDao.getByNormalizedWords(normalizedWords).associateBy { it.normalizedWord }

        val toInsert = mutableListOf<VocabularyItem>()
        val toUpdate = mutableListOf<VocabularyItem>()
        val incomingList = mutableListOf<Pair<VocabularyItem, Boolean>>()

        for ((normalized, incoming, _) in parsedItems) {
            val existing = existingMap[normalized]
            if (existing == null) {
                toInsert.add(incoming)
                incomingList.add(incoming to true)
            } else {
                val merged = merge(existing, incoming)
                toUpdate.add(merged)
                incomingList.add(merged to false)
            }
        }

        database.withTransaction {
            val insertedIds = if (toInsert.isNotEmpty()) {
                vocabularyDao.insertAll(toInsert)
            } else {
                emptyList()
            }
            if (toUpdate.isNotEmpty()) {
                vocabularyDao.updateAll(toUpdate)
            }

            // A word can appear in more than one bank, so the same sense list may
            // arrive twice for one row. Senses are replaced wholesale per word
            // rather than appended, which keeps a re-import idempotent.
            val sensesByWord = parsedItems.associate { it.first to it.third }
            var senseInsertIdx = 0
            val senseRows = mutableListOf<VocabularySense>()
            for ((item, isInsert) in incomingList) {
                // The counter must advance for every inserted row, not only for
                // rows that carry senses: insertedIds is a single flat list in
                // insertion order, and pack-membership insertion below walks it
                // with its own index. Skipping rows here would desynchronise the
                // two and attach senses to the wrong word.
                //
                // No `continue`/`return` here: withTransaction takes a
                // non-inline suspend lambda, so a jump out of this loop is
                // illegal. The work is nested in `if` blocks instead.
                val rowId: Long? = if (isInsert) {
                    insertedIds.getOrNull(senseInsertIdx++)
                } else {
                    item.id
                }
                if (rowId != null) {
                    val senses = sensesByWord[item.normalizedWord]
                    if (!senses.isNullOrEmpty()) {
                        vocabularySenseDao.deleteSensesForWord(rowId)
                        senseRows += senses.map { it.copy(vocabularyId = rowId) }
                    }
                }
            }
            if (senseRows.isNotEmpty()) {
                vocabularySenseDao.insertAll(senseRows)
            }

            var insertIdx = 0
            val packItems = ArrayList<VocabularyPackItem>(incomingList.size)
            for ((item, isInsert) in incomingList) {
                val vocabId = if (isInsert) {
                    insertedIds[insertIdx++]
                } else {
                    item.id
                }
                packItems.add(VocabularyPackItem(packId = packId, vocabularyId = vocabId))
                // Core-tier membership follows the pedagogical learning order.
                // The earlier `examPriority >= 70` clause never matched anything:
                // the dataset uses a 0..4 priority scale, so it silently left
                // both core packs empty. VocabularyStudyPolicy.IELTS_CORE_LIMIT
                // and TOEFL_CORE_LIMIT define the cut-offs used here.
                if (packId == "pack_ielts_master" && item.learningOrder in 1..VocabularyStudyPolicy.IELTS_CORE_LIMIT) {
                    packItems.add(VocabularyPackItem(packId = "pack_ielts_core", vocabularyId = vocabId))
                }
                if (packId == "pack_toefl_master" && item.learningOrder in 1..VocabularyStudyPolicy.TOEFL_CORE_LIMIT) {
                    packItems.add(VocabularyPackItem(packId = "pack_toefl_core", vocabularyId = vocabId))
                }
            }
            packItemDao.insertAll(packItems)
        }

        return ChunkResult(
            insertedWords = toInsert.size,
            updatedWords = toUpdate.size,
            membershipsAdded = parsedItems.size,
            processedItems = parsedItems.size
        )
    }

    private fun JSONObject.toVocabularyItem(
        normalizedWord: String,
        datasetVersion: String
    ): VocabularyItem {
        val word = getString("word").trim()
        val partOfSpeech = optString("partOfSpeech", "word").trim().ifBlank { "word" }
        val definition = cleanDefinition(optString("englishDefinition"))
        val example = cleanExample(optString("example"))
        val rawTags = stringList("tags")
        val qualityTags = buildList {
            if (definition.isBlank()) add("needs-definition")
            if (example.isBlank()) add("needs-example")
        }
        val collocations = sanitizeCollocations(word, partOfSpeech, stringList("collocations"))
        val finalQualityTags = if (collocations.isEmpty()) qualityTags + "needs-collocation" else qualityTags

        return VocabularyItem(
            word = word,
            normalizedWord = normalizedWord,
            ipa = cleanInlineText(optString("ipa")),
            persianMeaning = cleanInlineText(getString("persianMeaning")),
            englishDefinition = definition,
            partOfSpeech = partOfSpeech,
            example = example,
            examplePersian = cleanExample(optString("examplePersian")),
            cefrLevel = optString("cefrLevel", "B2").trim().uppercase(Locale.US),
            synonyms = sanitizeLexicalList(word, stringList("synonyms")),
            antonyms = sanitizeLexicalList(word, stringList("antonyms")),
            collocations = collocations,
            wordFamily = sanitizeLexicalList(word, stringList("wordFamily"), removeHeadword = false),
            commonMistakes = cleanInlineText(optString("commonMistakes")),
            ieltsRelevance = optString("ieltsRelevance", "Medium"),
            toeflRelevance = optString("toeflRelevance", "Medium"),
            greRelevance = optString("greRelevance", "Medium"),
            tags = (rawTags + finalQualityTags).distinct(),
            source = optString("source", "LinguaFa Bundled"),
            sourceLicense = optString("sourceLicense", "Original"),
            datasetVersion = datasetVersion,
            frequencyRank = optInt("frequencyRank", 0),
            examPriority = optInt("examPriority", 0),
            learningOrder = optInt("learningOrder", 0),
            examTopics = stringList("examTopics").distinct(),
            targetBand = optString("targetBand", "7.0").trim().ifEmpty { "7.0" },
            skillFocus = stringList("skillFocus").distinct()
        )
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun merge(existing: VocabularyItem, incoming: VocabularyItem): VocabularyItem {
        val bestDefinition = chooseBetterEnglish(existing.englishDefinition, incoming.englishDefinition, isExample = false)
        val bestExample = chooseBetterEnglish(existing.example, incoming.example, isExample = true)
        val mergedCollocations = sanitizeCollocations(
            incoming.word,
            incoming.partOfSpeech,
            existing.collocations + incoming.collocations
        )
        val mergedSynonyms = sanitizeLexicalList(incoming.word, existing.synonyms + incoming.synonyms)
        val mergedAntonyms = sanitizeLexicalList(incoming.word, existing.antonyms + incoming.antonyms)
        val mergedWordFamily = sanitizeLexicalList(
            incoming.word,
            existing.wordFamily + incoming.wordFamily,
            removeHeadword = false
        )

        val qualityTags = buildList {
            if (bestDefinition.isBlank()) add("needs-definition")
            if (bestExample.isBlank()) add("needs-example")
            if (mergedCollocations.isEmpty()) add("needs-collocation")
        }
        val oldTagsWithoutQualityFlags = existing.tags.filterNot { it.startsWith("needs-") }
        val incomingTagsWithoutQualityFlags = incoming.tags.filterNot { it.startsWith("needs-") }

        return existing.copy(
            word = incoming.word.ifBlank { existing.word },
            normalizedWord = incoming.normalizedWord,
            ipa = chooseBetterInline(existing.ipa, incoming.ipa),
            persianMeaning = mergeMeaning(existing.persianMeaning, incoming.persianMeaning),
            englishDefinition = bestDefinition,
            partOfSpeech = choosePartOfSpeech(existing.partOfSpeech, incoming.partOfSpeech),
            example = bestExample,
            examplePersian = chooseBetterPersian(existing.examplePersian, incoming.examplePersian),
            cefrLevel = chooseCefr(existing.cefrLevel, incoming.cefrLevel),
            synonyms = mergedSynonyms,
            antonyms = mergedAntonyms,
            collocations = mergedCollocations,
            wordFamily = mergedWordFamily,
            commonMistakes = chooseBetterInline(existing.commonMistakes, incoming.commonMistakes),
            ieltsRelevance = strongerRelevance(existing.ieltsRelevance, incoming.ieltsRelevance),
            toeflRelevance = strongerRelevance(existing.toeflRelevance, incoming.toeflRelevance),
            greRelevance = strongerRelevance(existing.greRelevance, incoming.greRelevance),
            tags = (oldTagsWithoutQualityFlags + incomingTagsWithoutQualityFlags + qualityTags).distinct(),
            source = mergeSource(existing.source, incoming.source),
            sourceLicense = mergeSource(existing.sourceLicense, incoming.sourceLicense),
            datasetVersion = incoming.datasetVersion,
            frequencyRank = chooseFrequencyRank(existing.frequencyRank, incoming.frequencyRank),
            examPriority = maxOf(existing.examPriority, incoming.examPriority),
            learningOrder = chooseLearningOrder(existing.learningOrder, incoming.learningOrder),
            // Prefer the newer dataset's taxonomy; union only when the existing
            // row has metadata and the incoming row does not.
            examTopics = mergeTaxonomy(existing.examTopics, incoming.examTopics),
            targetBand = incoming.targetBand.ifBlank { existing.targetBand },
            skillFocus = mergeTaxonomy(existing.skillFocus, incoming.skillFocus),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Parses the optional `senses` array written by scripts/normalize_definitions.py.
     *
     * Rows that were raw multi-sense dictionary dumps gain one row per sense, with
     * the sense matching the card's part of speech marked primary. The Persian
     * meaning and example are only known for the primary sense, so non-primary
     * senses leave them blank rather than inheriting text that describes a
     * different meaning.
     */
    private fun JSONObject.toVocabularySenses(
        normalizedWord: String,
        persianMeaning: String
    ): List<VocabularySense> {
        val cefr = optString("cefrLevel", "").trim().uppercase(Locale.US)
        val array = optJSONArray("senses")
        if (array == null) {
            // Every card needs at least its own primary sense, otherwise a word
            // with only one meaning has no sense row at all and the app shows an
            // empty sense list. This mirrors the single-sense backfill that
            // AppDatabase.ensureVocabularySenses used to do on an empty table.
            val definition = cleanDefinition(optString("englishDefinition"))
            if (definition.isBlank()) return emptyList()
            return listOf(
                VocabularySense(
                    vocabularyId = 0L,
                    senseIndex = 1,
                    partOfSpeech = optString("partOfSpeech", "").trim(),
                    cefrLevel = cefr,
                    englishDefinition = definition,
                    persianMeaning = persianMeaning,
                    exampleSentence = cleanExample(optString("example")),
                    exampleTranslation = cleanExample(optString("examplePersian")),
                    collocations = sanitizeCollocations(
                        normalizedWord,
                        optString("partOfSpeech", ""),
                        stringList("collocations")
                    ),
                    isPrimary = true
                )
            )
        }
        val senses = mutableListOf<VocabularySense>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val definition = entry.optString("englishDefinition").trim()
            if (definition.isEmpty()) continue
            val isPrimary = entry.optBoolean("isPrimary", senses.isEmpty())
            senses.add(
                VocabularySense(
                    vocabularyId = 0L,
                    senseIndex = entry.optInt("senseIndex", senses.size + 1),
                    partOfSpeech = entry.optString("partOfSpeech", "").trim(),
                    cefrLevel = optString("cefrLevel", "").trim().uppercase(Locale.US),
                    englishDefinition = definition,
                    persianMeaning = if (isPrimary) persianMeaning else "",
                    exampleSentence = if (isPrimary) cleanExample(optString("example")) else "",
                    exampleTranslation = if (isPrimary) cleanExample(optString("examplePersian")) else "",
                    collocations = if (isPrimary) sanitizeCollocations(
                        normalizedWord,
                        optString("partOfSpeech", ""),
                        stringList("collocations")
                    ) else emptyList(),
                    isPrimary = isPrimary
                )
            )
        }
        return senses
    }

    private fun mergeTaxonomy(existing: List<String>, incoming: List<String>): List<String> {
        return if (incoming.isNotEmpty()) incoming.distinct() else existing.distinct()
    }

    private fun cleanDefinition(value: String): String {
        var cleaned = cleanInlineText(value)
        cleaned = cleaned.replace(Regex("\\s*;\\s*(?:;\\s*)+.*$"), "")
        cleaned = cleaned.replace(Regex("(?:\\s*;\\s*){3,}"), "; ")
        cleaned = cleaned.trim(' ', ';', ',')
        if (cleaned.isBlank()) return ""
        return ensureSentencePunctuation(cleaned)
    }

    private fun cleanExample(value: String): String {
        val cleaned = cleanInlineText(value).trim(' ', '"', '“', '”')
        if (cleaned.isBlank()) return ""
        return ensureSentencePunctuation(cleaned)
    }

    private fun cleanInlineText(value: String): String = value
        .replace(Regex("\\s+"), " ")
        .replace("�", "")
        .trim()

    private fun ensureSentencePunctuation(value: String): String {
        if (value.lastOrNull() in listOf('.', '!', '?', ':', ';')) return value
        return "$value."
    }

    private fun englishQuality(value: String, isExample: Boolean): Int {
        val text = cleanInlineText(value)
        if (text.isBlank()) return Int.MIN_VALUE
        val words = text.split(' ').count { it.isNotBlank() }
        var score = 0
        score += minOf(words, 30) * 2
        if (text.length in 25..220) score += 18
        if (text.firstOrNull()?.isUpperCase() == true) score += 4
        if (text.lastOrNull() in listOf('.', '!', '?')) score += 4
        if (text.contains(Regex("[A-Za-z]{3,}"))) score += 8
        if (text.contains("; ;") || text.contains(";;")) score -= 60
        if (text.contains("�")) score -= 80
        if (isExample && words < 4) score -= 25
        if (!isExample && words < 3) score -= 20
        return score
    }

    private fun chooseBetterEnglish(existing: String, incoming: String, isExample: Boolean): String {
        val cleanExisting = if (isExample) cleanExample(existing) else cleanDefinition(existing)
        val cleanIncoming = if (isExample) cleanExample(incoming) else cleanDefinition(incoming)
        return if (englishQuality(cleanIncoming, isExample) > englishQuality(cleanExisting, isExample)) {
            cleanIncoming
        } else {
            cleanExisting
        }
    }

    private fun chooseBetterInline(existing: String, incoming: String): String {
        val old = cleanInlineText(existing)
        val new = cleanInlineText(incoming)
        return when {
            old.isBlank() -> new
            new.isBlank() -> old
            new.length > old.length && new.length <= 240 -> new
            else -> old
        }
    }

    private fun chooseBetterPersian(existing: String, incoming: String): String {
        val old = cleanInlineText(existing)
        val new = cleanInlineText(incoming)
        return when {
            old.isBlank() -> new
            new.isBlank() -> old
            new.length in 8..240 && new.length > old.length -> new
            else -> old
        }
    }

    private fun sanitizeLexicalList(
        word: String,
        values: List<String>,
        removeHeadword: Boolean = true
    ): List<String> {
        val normalizedWord = word.lowercase(Locale.US).trim()
        return values
            .map(::cleanInlineText)
            .filter { it.isNotBlank() }
            .filterNot { removeHeadword && it.lowercase(Locale.US) == normalizedWord }
            .distinctBy { it.lowercase(Locale.US) }
            .take(8)
    }

    /**
     * Earlier asset builders used POS templates when no sourced collocation existed. If most of a
     * row matches that exact template family, we discard the synthetic entries instead of showing
     * confident-looking but unnatural phrases. Sourced/curated collocations are preserved.
     */
    private fun sanitizeCollocations(word: String, partOfSpeech: String, values: List<String>): List<String> {
        val w = word.lowercase(Locale.US).trim()
        val cleaned = values
            .map(::cleanInlineText)
            .filter { it.length in 3..100 }
            .distinctBy { it.lowercase(Locale.US) }

        if (cleaned.isEmpty()) return emptyList()

        val generatedTemplates = when {
            partOfSpeech.contains("verb", ignoreCase = true) -> setOf(
                "$w the process", "$w effectively", "$w a solution", "attempt to $w", "seek to $w", "$w rapidly",
                "effectively $w", "fail to $w", "ability to $w"
            )
            partOfSpeech.contains("adj", ignoreCase = true) -> setOf(
                "highly $w", "increasingly $w", "$w factor", "$w importance", "$w impact", "particularly $w"
            )
            partOfSpeech.contains("adv", ignoreCase = true) -> setOf(
                "$w important", "$w significant", "$w different", "$w associated", "$w evident",
                "$w observed", "$w apparent"
            )
            else -> setOf(
                "crucial $w", "significant $w", "play a role in $w", "development of $w", "high level of $w",
                "fundamental $w", "key $w", "underlying $w", "role of $w"
            )
        }

        val generatedCount = cleaned.count { it.lowercase(Locale.US) in generatedTemplates }
        val mostlyGenerated = generatedCount >= 3 && generatedCount * 2 >= cleaned.size
        val selected = if (mostlyGenerated) {
            cleaned.filterNot { it.lowercase(Locale.US) in generatedTemplates }
        } else {
            cleaned
        }
        return selected.take(6)
    }

    private fun choosePartOfSpeech(existing: String, incoming: String): String {
        val old = existing.trim()
        val new = incoming.trim()
        return when {
            old.isBlank() || old.equals("word", ignoreCase = true) -> new.ifBlank { "word" }
            new.isBlank() || new.equals("word", ignoreCase = true) -> old
            new.length > old.length -> new
            else -> old
        }
    }

    private fun chooseCefr(existing: String, incoming: String): String {
        val levels = setOf("A1", "A2", "B1", "B2", "C1", "C2")
        val old = existing.trim().uppercase(Locale.US)
        val new = incoming.trim().uppercase(Locale.US)
        return when {
            new in levels && old !in levels -> new
            old in levels -> old
            new in levels -> new
            else -> "B2"
        }
    }

    private fun mergeMeaning(existing: String, incoming: String): String {
        val old = cleanInlineText(existing)
        val new = cleanInlineText(incoming)
        if (old.isBlank()) return new
        if (new.isBlank() || old.contains(new, ignoreCase = true)) return old
        if (new.contains(old, ignoreCase = true)) return new
        return "$old / $new"
    }

    private fun mergeSource(existing: String, incoming: String): String {
        val old = cleanInlineText(existing)
        val new = cleanInlineText(incoming)
        if (old.isBlank()) return new
        if (new.isBlank() || old.contains(new, ignoreCase = true)) return old
        if (new.contains(old, ignoreCase = true)) return new
        return "$old + $new"
    }

    private fun chooseFrequencyRank(existing: Int, incoming: Int): Int = when {
        existing <= 0 -> incoming
        incoming <= 0 -> existing
        else -> minOf(existing, incoming)
    }

    private fun chooseLearningOrder(existing: Int, incoming: Int): Int = when {
        existing <= 0 -> incoming
        incoming <= 0 -> existing
        else -> minOf(existing, incoming)
    }

    private fun strongerRelevance(a: String, b: String): String {
        fun rank(value: String): Int = when (value.trim().lowercase(Locale.US)) {
            "very high", "essential" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 0
        }
        return if (rank(b) > rank(a)) b else a
    }
}
