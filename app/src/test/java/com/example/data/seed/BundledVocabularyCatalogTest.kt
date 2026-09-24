package com.example.data.seed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
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

        assertEquals(9000, targets.getInt(InitialDataSeed.IELTS_MASTER_PACK_ID))
        assertEquals(7000, targets.getInt(InitialDataSeed.TOEFL_MASTER_PACK_ID))
        assertEquals(5000, targets.getInt(InitialDataSeed.GRE_MASTER_PACK_ID))

        val chunkPackIds = buildSet {
            val chunks = catalog.getJSONArray("chunks")
            for (i in 0 until chunks.length()) {
                add(chunks.getJSONObject(i).getString("packId"))
            }
        }

        assertTrue(InitialDataSeed.IELTS_MASTER_PACK_ID in chunkPackIds)
        assertTrue(InitialDataSeed.TOEFL_MASTER_PACK_ID in chunkPackIds)
        assertTrue(InitialDataSeed.GRE_MASTER_PACK_ID in chunkPackIds)
    }

    @Test
    fun everyChunkMatchesManifestAndContainsValidPersianEntries() {
        val chunks = readCatalog().getJSONArray("chunks")

        for (i in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(i)
            val assetPath = chunk.getString("asset")
            val expectedItems = chunk.getInt("expectedItems")
            val rows = context.assets.open(assetPath).bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }

            assertEquals("Unexpected item count for $assetPath", expectedItems, rows.size)

            val normalizedWords = mutableSetOf<String>()
            rows.forEach { line ->
                val item = JSONObject(line)
                val word = item.getString("word").trim()
                val persianMeaning = item.getString("persianMeaning").trim()

                assertFalse("Blank word in $assetPath", word.isBlank())
                assertFalse("Blank Persian meaning for $word", persianMeaning.isBlank())
                assertTrue("Duplicate word $word inside $assetPath", normalizedWords.add(word.lowercase()))
            }
        }
    }

    private fun readCatalog(): JSONObject {
        val text = context.assets.open("vocabulary/master_catalog.json")
            .bufferedReader()
            .use { it.readText() }
        val catalog = JSONObject(text)
        assertTrue(catalog.getInt("schemaVersion") >= 1)
        assertTrue(catalog.optJSONArray("chunks") ?: JSONArray() is JSONArray)
        return catalog
    }
}
