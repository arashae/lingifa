package com.example.data.importer

import android.content.Context
import android.util.Log
import com.example.data.local.VocabularyDao
import com.example.data.local.VocabularyDatasetChunkDao
import com.example.data.local.VocabularyPackItemDao
import com.example.data.model.VocabularyDatasetChunk
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPackItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Streaming importer for the large bundled IELTS/TOEFL/GRE vocabulary banks.
 *
 * Dataset files are JSONL (one JSON object per line) so a 5k-9k word bank does not
 * have to be materialized in memory. The manifest is versioned and each chunk is
 * recorded after a successful import, making imports resumable and updateable.
 */
object BundledVocabularyImporter {
    private const val TAG = "BundledVocabImporter"
    private const val CATALOG_ASSET = "vocabulary/master_catalog.json"

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
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao,
        chunkDao: VocabularyDatasetChunkDao
    ): ImportSummary {
        val catalogText = try {
            context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            // The app can still run with Kotlin bootstrap data when no bundled catalog exists.
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

        for (index in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(index)
            val chunkId = chunk.getString("id")
            val version = chunk.getString("version")
            val packId = chunk.getString("packId")
            val assetPath = chunk.getString("asset")

            val existingChunk = chunkDao.get(chunkId)
            if (existingChunk?.version == version) {
                skippedChunks++
                continue
            }

            try {
                val result = importJsonlChunk(
                    context = context,
                    assetPath = assetPath,
                    packId = packId,
                    datasetVersion = version,
                    vocabularyDao = vocabularyDao,
                    packItemDao = packItemDao
                )

                chunkDao.upsert(
                    VocabularyDatasetChunk(
                        chunkId = chunkId,
                        packId = packId,
                        version = version,
                        itemCount = result.processedItems
                    )
                )

                importedChunks++
                insertedWords += result.insertedWords
                updatedWords += result.updatedWords
                membershipsAdded += result.membershipsAdded
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
        assetPath: String,
        packId: String,
        datasetVersion: String,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao
    ): ChunkResult {
        var inserted = 0
        var updated = 0
        var memberships = 0
        var processed = 0

        context.assets.open(assetPath).bufferedReader().useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach

                val json = JSONObject(line)
                val word = json.getString("word").trim()
                val persianMeaning = json.getString("persianMeaning").trim()
                require(word.isNotEmpty()) { "Vocabulary word cannot be empty" }
                require(persianMeaning.isNotEmpty()) { "Persian meaning is required for $word" }

                val normalized = word.lowercase(Locale.US).trim()
                val incoming = json.toVocabularyItem(
                    normalizedWord = normalized,
                    datasetVersion = datasetVersion
                )

                val existing = vocabularyDao.getByNormalizedWord(normalized)
                val vocabularyId = if (existing == null) {
                    inserted++
                    vocabularyDao.insert(incoming)
                } else {
                    updated++
                    vocabularyDao.update(merge(existing, incoming))
                    existing.id
                }

                packItemDao.insert(
                    VocabularyPackItem(
                        packId = packId,
                        vocabularyId = vocabularyId
                    )
                )
                memberships++
                processed++
            }
        }

        return ChunkResult(
            insertedWords = inserted,
            updatedWords = updated,
            membershipsAdded = memberships,
            processedItems = processed
        )
    }

    private fun JSONObject.toVocabularyItem(
        normalizedWord: String,
        datasetVersion: String
    ): VocabularyItem {
        return VocabularyItem(
            word = getString("word").trim(),
            normalizedWord = normalizedWord,
            ipa = optString("ipa"),
            persianMeaning = getString("persianMeaning").trim(),
            englishDefinition = optString("englishDefinition"),
            partOfSpeech = optString("partOfSpeech", "word"),
            example = optString("example"),
            examplePersian = optString("examplePersian"),
            cefrLevel = optString("cefrLevel", "B2"),
            synonyms = stringList("synonyms"),
            antonyms = stringList("antonyms"),
            collocations = stringList("collocations"),
            wordFamily = stringList("wordFamily"),
            commonMistakes = optString("commonMistakes"),
            ieltsRelevance = optString("ieltsRelevance", "Medium"),
            toeflRelevance = optString("toeflRelevance", "Medium"),
            greRelevance = optString("greRelevance", "Medium"),
            tags = stringList("tags"),
            source = optString("source", "LinguaFa Bundled"),
            sourceLicense = optString("sourceLicense", "Original"),
            datasetVersion = datasetVersion,
            frequencyRank = optInt("frequencyRank", 0),
            examPriority = optInt("examPriority", 0)
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
        return existing.copy(
            ipa = existing.ipa.ifBlank { incoming.ipa },
            persianMeaning = mergeMeaning(existing.persianMeaning, incoming.persianMeaning),
            englishDefinition = existing.englishDefinition.ifBlank { incoming.englishDefinition },
            partOfSpeech = existing.partOfSpeech.takeUnless { it == "word" || it.isBlank() }
                ?: incoming.partOfSpeech,
            example = existing.example.ifBlank { incoming.example },
            examplePersian = existing.examplePersian.ifBlank { incoming.examplePersian },
            cefrLevel = existing.cefrLevel.ifBlank { incoming.cefrLevel },
            synonyms = (existing.synonyms + incoming.synonyms).distinct(),
            antonyms = (existing.antonyms + incoming.antonyms).distinct(),
            collocations = (existing.collocations + incoming.collocations).distinct(),
            wordFamily = (existing.wordFamily + incoming.wordFamily).distinct(),
            commonMistakes = existing.commonMistakes.ifBlank { incoming.commonMistakes },
            ieltsRelevance = strongerRelevance(existing.ieltsRelevance, incoming.ieltsRelevance),
            toeflRelevance = strongerRelevance(existing.toeflRelevance, incoming.toeflRelevance),
            greRelevance = strongerRelevance(existing.greRelevance, incoming.greRelevance),
            tags = (existing.tags + incoming.tags).distinct(),
            sourceLicense = existing.sourceLicense.ifBlank { incoming.sourceLicense },
            datasetVersion = incoming.datasetVersion,
            frequencyRank = chooseFrequencyRank(existing.frequencyRank, incoming.frequencyRank),
            examPriority = maxOf(existing.examPriority, incoming.examPriority),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun mergeMeaning(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank() || existing.contains(incoming, ignoreCase = true)) return existing
        return "$existing / $incoming"
    }

    private fun chooseFrequencyRank(existing: Int, incoming: Int): Int = when {
        existing <= 0 -> incoming
        incoming <= 0 -> existing
        else -> minOf(existing, incoming)
    }

    private fun strongerRelevance(a: String, b: String): String {
        fun rank(value: String): Int = when (value.trim().lowercase()) {
            "very high", "essential" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 0
        }
        return if (rank(b) > rank(a)) b else a
    }
}
