package com.example.data.seed

import com.example.data.model.VocabularyItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialDataSeedTest {

    @Test
    fun masterBanksExposeBundledTargets() {
        val packs = InitialDataSeed.getDefaultPacks().associateBy { it.id }

        val ielts = packs.getValue(InitialDataSeed.IELTS_MASTER_PACK_ID)
        val toefl = packs.getValue(InitialDataSeed.TOEFL_MASTER_PACK_ID)
        val gre = packs.getValue(InitialDataSeed.GRE_MASTER_PACK_ID)

        assertEquals(5040, ielts.targetWordCount)
        assertEquals(6974, toefl.targetWordCount)
        assertEquals(7504, gre.targetWordCount)

        assertEquals(ielts.targetWordCount, ielts.wordCount)
        assertEquals(toefl.targetWordCount, toefl.wordCount)
        assertEquals(gre.targetWordCount, gre.wordCount)

        assertTrue(ielts.version >= 3)
        assertTrue(toefl.version >= 3)
        assertTrue(gre.version >= 3)

        assertTrue(ielts.isCorePack)
        assertTrue(toefl.isCorePack)
        assertTrue(gre.isCorePack)
    }

    @Test
    fun oneWordCanBelongToAllExamBanksAndLegacyPack() {
        val word = VocabularyItem(
            word = "ameliorate",
            persianMeaning = "بهبود بخشیدن",
            tags = listOf("IELTS", "TOEFL", "GRE", "Academic"),
            packName = "my_legacy_pack"
        )

        val memberships = InitialDataSeed.getPackIdsFor(word)

        assertTrue("my_legacy_pack" in memberships)
        assertTrue(InitialDataSeed.IELTS_MASTER_PACK_ID in memberships)
        assertTrue(InitialDataSeed.TOEFL_MASTER_PACK_ID in memberships)
        assertTrue(InitialDataSeed.GRE_MASTER_PACK_ID in memberships)
        assertTrue("pack_cefr_b2" in memberships)
        assertEquals(5, memberships.size)
    }

    @Test
    fun bootstrapVocabularyRemainsAvailableOffline() {
        val seed = InitialDataSeed.getSeedVocabulary()
        assertTrue(seed.size >= 300)
        assertTrue(seed.all { it.word.isNotBlank() })
        assertTrue(seed.all { it.persianMeaning.isNotBlank() })
    }
}
