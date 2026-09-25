package com.example.data.seed

import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack

object InitialDataSeed {

    const val IELTS_MASTER_PACK_ID = "pack_ielts_master"
    const val TOEFL_MASTER_PACK_ID = "pack_toefl_master"
    const val GRE_MASTER_PACK_ID = "pack_gre_master"
    const val IELTS_CORE_PACK_ID = "pack_ielts_core"
    const val TOEFL_CORE_PACK_ID = "pack_toefl_core"

    private val CEFR_PACK_IDS = mapOf(
        "A1" to "pack_cefr_a1",
        "A2" to "pack_cefr_a2",
        "B1" to "pack_cefr_b1",
        "B2" to "pack_cefr_b2",
        "C1" to "pack_cefr_c1",
        "C2" to "pack_cefr_c2"
    )

    fun getDefaultPacks(): List<VocabularyPack> = listOf(
        VocabularyPack(
            id = "pack_cefr_a1", titleFa = "مسیر عمومی A1", titleEn = "CEFR A1 Foundation",
            descriptionFa = "پایه‌ترین واژه‌های پرکاربرد برای شروع مکالمه و زندگی روزمره.",
            level = "A1", exam = "General", wordCount = 292, category = "CEFR Curriculum", iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_cefr_a2", titleFa = "مسیر عمومی A2", titleEn = "CEFR A2 Everyday English",
            descriptionFa = "واژگان روزمره برای خرید، سفر، کارهای شخصی و مکالمه‌های کوتاه.",
            level = "A2", exam = "General", wordCount = 707, category = "CEFR Curriculum", iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_cefr_b1", titleFa = "مسیر عمومی B1", titleEn = "CEFR B1 Independent",
            descriptionFa = "واژه‌های ضروری برای بیان نظر، تجربه و ارتباط مستقل.",
            level = "B1", exam = "General", wordCount = 968, category = "CEFR Curriculum", iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_cefr_b2", titleFa = "مسیر عمومی B2", titleEn = "CEFR B2 Upper Intermediate",
            descriptionFa = "واژگان پرکاربردِ مطالعه، کار و بحث‌های پیچیده‌تر.",
            level = "B2", exam = "General", wordCount = 1209, category = "CEFR Curriculum", iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_cefr_c1", titleFa = "مسیر عمومی C1", titleEn = "CEFR C1 Advanced",
            descriptionFa = "واژگان پیشرفته و آکادمیک برای بیان دقیق و متون جدی.",
            level = "C1", exam = "General", wordCount = 1105, category = "CEFR Curriculum", iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_cefr_c2", titleFa = "مسیر عمومی C2 + Advanced", titleEn = "CEFR C2 + Advanced",
            descriptionFa = "واژگان پیشرفته عمومی و متون تخصصی برای بالاترین سطح تسلط زبانی.",
            level = "C2", exam = "General", wordCount = 16321, category = "CEFR Curriculum", iconName = "translate"
        ),
        VocabularyPack(
            id = "pack_ielts_core",
            titleFa = "هسته واژگان آیلتس (IELTS Core)",
            titleEn = "IELTS Core Academic (High Yield)",
            descriptionFa = "واژگان بنیادین و بسیار پرتکرار رایتینگ و اسپیکینگ آیلتس برای برنامه فشرده ۱ تا ۲ ماهه.",
            level = "B2-C1",
            exam = "IELTS",
            wordCount = 2000,
            isDownloaded = false,
            category = "Exam Sprints",
            iconName = "school",
            version = 1,
            source = "AWL + Cambridge Lexical Focus",
            targetWordCount = 2000,
            installedWordCount = 0,
            isCorePack = true
        ),
        VocabularyPack(
            id = "pack_toefl_core",
            titleFa = "هسته واژگان تافل (TOEFL Core)",
            titleEn = "TOEFL Core Academic (High Yield)",
            descriptionFa = "واژگان با اولویت بالای لکچرهای علمی، متون دانشگاهی و تسک‌های تلفیقی تافل برای مطالعه هدفمند.",
            level = "B1-C1",
            exam = "TOEFL",
            wordCount = 2200,
            isDownloaded = false,
            category = "Exam Sprints",
            iconName = "menu_book",
            version = 1,
            source = "NAWL + ETS Academic Corpus",
            targetWordCount = 2200,
            installedWordCount = 0,
            isCorePack = true
        ),
        VocabularyPack(
            id = IELTS_MASTER_PACK_ID,
            titleFa = "بانک جامع واژگان IELTS",
            titleEn = "IELTS Master Vocabulary Bank",
            descriptionFa = "بانک اصلی واژگان آیلتس برای Reading، Listening، Writing و Speaking. محتوا به‌صورت نسخه‌بندی‌شده و مرحله‌ای نصب می‌شود و یک لغت می‌تواند هم‌زمان عضو چند بسته باشد.",
            level = "A2-C2",
            exam = "IELTS",
            wordCount = 5040,
            isDownloaded = false,
            category = "Master Bank",
            iconName = "school",
            version = 3,
            source = "ECDICT + Openjam + EnglishToPersianDictionaries",
            targetWordCount = 5040,
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
            wordCount = 6974,
            isDownloaded = false,
            category = "Master Bank",
            iconName = "menu_book",
            version = 3,
            source = "ECDICT + Openjam + EnglishToPersianDictionaries",
            targetWordCount = 6974,
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
            wordCount = 7504,
            isDownloaded = false,
            category = "Master Bank",
            iconName = "psychology",
            version = 3,
            source = "ECDICT + Openjam + EnglishToPersianDictionaries",
            targetWordCount = 7504,
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
        CEFR_PACK_IDS[item.cefrLevel.trim().uppercase()]?.let(result::add)

        return result
    }
}
