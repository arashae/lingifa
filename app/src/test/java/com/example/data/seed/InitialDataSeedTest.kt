package com.example.data.seed

import com.example.data.model.VocabularyItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialDataSeedTest {

    @Test
    fun masterBanksExposeRequestedTargets() {
        val packs = InitialDataSeed.getDefaultPacks().associateBy { it.id }

        assertEquals(9000, packs.getValue(InitialDataSeed.IELTS_MASTER_PACK_ID).targetWordCount)
        assertEquals(7000, packs.getValue(InitialDataSeed.TOEFL_MASTER_PACK_ID).targetWordCount)
        assertEquals(5000, packs.getValue(InitialDataSeed.GRE_MASTER_PACK_ID).targetWordCount)

        assertTrue(packs.getValue(InitialDataSeed.IELTS_MASTER_PACK_ID).isCorePack)
        assertTrue(packs.getValue(InitialDataSeed.TOEFL_MASTER_PACK_ID).isCorePack)
        assertTrue(packs.getValue(InitialDataSeed.GRE_MASTER_PACK_ID).isCorePack)
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
        assertEquals(4, memberships.size)
    }

    @Test
    fun bootstrapVocabularyRemainsAvailableOffline() {
        val seed = InitialDataSeed.getSeedVocabulary()
        assertTrue(seed.size >= 300)
        assertTrue(seed.all { it.word.isNotBlank() })
        assertTrue(seed.all { it.persianMeaning.isNotBlank() })
    }
}
