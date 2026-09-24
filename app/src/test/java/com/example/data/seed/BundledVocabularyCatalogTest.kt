package com.example.data.seed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundledVocabularyCatalogTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun catalogContainsAllThreeExamMasterBanks() {
        val catalog = readCatalog()
        val targets = catalog.getJSONObject("targets")
        val chunks = catalog.getJSONArray("chunks")

        val expectedPackIds = setOf(
            InitialDataSeed.IELTS_MASTER_PACK_ID,
            InitialDataSeed.TOEFL_MASTER_PACK_ID,
            InitialDataSeed.GRE_MASTER_PACK_ID
        )

        val generatedCounts = mutableMapOf<String, Int>()
        for (i in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(i)
            val packId = chunk.getString("packId")
            generatedCounts[packId] = generatedCounts.getOrDefault(packId, 0) +
                chunk.getInt("expectedItems")
        }

        expectedPackIds.forEach { packId ->
            assertTrue("Missing target for $packId", targets.has(packId))
            assertTrue("Missing generated chunks for $packId", generatedCounts.containsKey(packId))
            assertTrue("Vocabulary bank $packId is unexpectedly small", targets.getInt(packId) >= 1000)
            assertEquals(
                "Catalog target must equal the number of generated offline entries for $packId",
                targets.getInt(packId),
                generatedCounts.getValue(packId)
            )
        }
    }

    @Test
    fun everyChunkMatchesManifestAndContainsValidPersianEntries() {
        val chunks = readCatalog().getJSONArray("chunks")
        val normalizedWordsByPack = mutableMapOf<String, MutableSet<String>>()
        val placeholder = "معنی فارسی در منابع آزاد فعلی پیدا نشد"

        for (i in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(i)
            val packId = chunk.getString("packId")
            val assetPath = chunk.getString("asset")
            val expectedItems = chunk.getInt("expectedItems")
            val rows = context.assets.open(assetPath).bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }

            assertEquals("Unexpected item count for $assetPath", expectedItems, rows.size)

            val normalizedWords = normalizedWordsByPack.getOrPut(packId) { mutableSetOf() }
            rows.forEach { line ->
                val item = JSONObject(line)
                val word = item.getString("word").trim()
                val persianMeaning = item.getString("persianMeaning").trim()

                assertFalse("Blank word in $assetPath", word.isBlank())
                assertFalse("Blank Persian meaning for $word", persianMeaning.isBlank())
                assertFalse("Placeholder Persian meaning for $word", persianMeaning == placeholder)
                assertTrue(
                    "Duplicate word $word inside offline pack $packId",
                    normalizedWords.add(word.lowercase())
                )
            }
        }
    }

    private fun readCatalog(): JSONObject {
        val text = context.assets.open("vocabulary/master_catalog.json")
            .bufferedReader()
            .use { it.readText() }
        val catalog = JSONObject(text)
        assertTrue(catalog.getInt("schemaVersion") >= 1)
        assertTrue(catalog.has("chunks"))
        assertTrue(catalog.getJSONArray("chunks").length() > 0)
        return catalog
    }
}
