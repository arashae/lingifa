package com.example.data.seed

import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack

object InitialDataSeed {

    const val IELTS_MASTER_PACK_ID = "pack_ielts_master"
    const val TOEFL_MASTER_PACK_ID = "pack_toefl_master"
    const val GRE_MASTER_PACK_ID = "pack_gre_master"

    fun getDefaultPacks(): List<VocabularyPack> = listOf(
        VocabularyPack(
            id = IELTS_MASTER_PACK_ID,
            titleFa = "بانک جامع واژگان IELTS",
            titleEn = "IELTS Master Vocabulary Bank",
            descriptionFa = "بانک اصلی واژگان آیلتس برای Reading، Listening، Writing و Speaking. محتوا به‌صورت نسخه‌بندی‌شده و مرحله‌ای نصب می‌شود و یک لغت می‌تواند هم‌زمان عضو چند بسته باشد.",
            level = "A2-C2",
            exam = "IELTS",
            wordCount = 9000,
            isDownloaded = false,
            category = "Master Bank",
            iconName = "school",
            version = 1,
            source = "LinguaFa curated/original and legally redistributable sources",
            targetWordCount = 9000,
            installedWordCount = 0,
            isCorePack = true
        ),
        VocabularyPack(
            id = TOEFL_MASTER_PACK_ID,
            titleFa = "بانک جامع واژگان TOEFL",
            titleEn = "TOEFL Academic Master Vocabulary Bank",
            descriptionFa = "بانک اصلی واژگان آکادمیک و دانشگاهی برای TOEFL iBT با پوشش متون علمی، سخنرانی‌ها، Writing و Speaking.",
            level = "B1-C2",
            exam = "TOEFL",
            wordCount = 7000,
            isDownloaded = false,
            category = "Master Bank",
            iconName = "menu_book",
            version = 1,
            source = "LinguaFa curated/original and legally redistributable sources",
            targetWordCount = 7000,
            installedWordCount = 0,
            isCorePack = true
        ),
        VocabularyPack(
            id = GRE_MASTER_PACK_ID,
            titleFa = "بانک جامع واژگان GRE",
            titleEn = "GRE Verbal Master Vocabulary Bank",
            descriptionFa = "بانک واژگان پیشرفته GRE Verbal برای Text Completion، Sentence Equivalence و درک دقیق واژگان در متن.",
            level = "B2-C2",
            exam = "GRE",
            wordCount = 5000,
            isDownloaded = false,
            category = "Master Bank",
            iconName = "psychology",
            version = 1,
            source = "LinguaFa curated/original and legally redistributable sources",
            targetWordCount = 5000,
            installedWordCount = 0,
            isCorePack = true
        ),
        VocabularyPack(
            id = "pack_ielts_academic",
            titleFa = "لغات ضروری آیلتس آکادمیک",
            titleEn = "IELTS Academic Essentials",
            descriptionFa = "واژگان منتخب با بالاترین کاربرد در ریدینگ و رایتینگ تسک ۱ و ۲ آیلتس آکادمیک به همراه مثال و کالوکیشن.",
            level = "B2-C1",
            exam = "IELTS",
            wordCount = 120,
            isDownloaded = true,
            category = "Exam Preparation",
            iconName = "school"
        ),
        VocabularyPack(
            id = "pack_toefl_academic",
            titleFa = "لغات تافل ۲۰۲۶ و متون دانشگاهی",
            titleEn = "TOEFL iBT 2026 Academic",
            descriptionFa = "واژگان سخنرانی‌های دانشگاهی، مقاله‌نویسی آکادمیک و متون علمی در ساختار جدید تافل ۲۰۲۶.",
            level = "B2-C1",
            exam = "TOEFL",
            wordCount = 95,
            isDownloaded = true,
            category = "Exam Preparation",
            iconName = "menu_book"
        ),
        VocabularyPack(
            id = "pack_c1_advanced",
            titleFa = "لغات پیشرفته سطح C1 و C2",
            titleEn = "C1-C2 Master Vocabulary",
            descriptionFa = "واژگان پیشرفته و آکادمیک سطح C1 و C2 برای آزمون‌های بین‌المللی و متون دانشگاهی.",
            level = "C1-C2",
            exam = "All Exams",
            wordCount = 110,
            isDownloaded = true,
            category = "Advanced",
            iconName = "psychology"
        ),
        VocabularyPack(
            id = "pack_oxford_essential",
            titleFa = "واژگان کلیدی عمومی (B1-B2)",
            titleEn = "Core Essential Vocabulary",
            descriptionFa = "لغات پرکاربرد مکالمه روزمره، مطالعه دانشگاهی و درک فیلم و اخبار به زبان انگلیسی.",
            level = "B1-B2",
            exam = "General",
            wordCount = 150,
            isDownloaded = true,
            category = "Core English",
            iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_ielts_writing",
            titleFa = "کالوکیشن‌ها و واژگان رایتینگ آیلتس",
            titleEn = "IELTS Writing Collocations",
            descriptionFa = "کالوکیشن‌های کاربردی، واژگان تحلیل داده در تسک ۱ و پیوندهای منطقی برای تسک ۲.",
            level = "B2-C1",
            exam = "IELTS",
            wordCount = 85,
            isDownloaded = true,
            category = "Writing",
            iconName = "edit_note"
        ),
        VocabularyPack(
            id = "pack_ielts_speaking",
            titleFa = "اصطلاحات و افعال عبارتی اسپیکینگ",
            titleEn = "IELTS Speaking & Phrasal Verbs",
            descriptionFa = "افعال عبارتی و اصطلاحات طبیعی برای افزایش دقت و روانی کلام در اسپیکینگ.",
            level = "B1-B2",
            exam = "IELTS",
            wordCount = 75,
            isDownloaded = true,
            category = "Speaking",
            iconName = "record_voice_over"
        )
    )

    fun getSeedVocabulary(): List<VocabularyItem> {
        val all = mutableListOf<VocabularyItem>()
        all.addAll(VocabSeedPart1.getItems())
        all.addAll(VocabSeedPart2.getItems())
        all.addAll(VocabSeedPart3.getItems())
        all.addAll(VocabSeedPart4.getItems())
        all.addAll(VocabSeedPart5.getItems())
        all.addAll(VocabSeedExtended.getItems())
        all.addAll(VocabSeedExtended2.getItems())
        all.addAll(VocabSeedExtended3.getItems())
        // Existing offline bootstrap: 350 items. Master banks are populated in versioned chunks.
        return all
    }

    /**
     * Derives all built-in pack memberships for a vocabulary row.
     * This intentionally keeps the legacy packName membership while also attaching
     * words tagged for IELTS/TOEFL/GRE to their master banks.
     */
    fun getPackIdsFor(item: VocabularyItem): Set<String> {
        val result = linkedSetOf<String>()
        item.packName.takeIf { it.isNotBlank() }?.let(result::add)

        val normalizedTags = item.tags.map { it.trim().uppercase() }.toSet()
        if ("IELTS" in normalizedTags) result.add(IELTS_MASTER_PACK_ID)
        if ("TOEFL" in normalizedTags) result.add(TOEFL_MASTER_PACK_ID)
        if ("GRE" in normalizedTags) result.add(GRE_MASTER_PACK_ID)

        return result
    }
}
