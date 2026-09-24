package com.example.data.seed

import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack

object InitialDataSeed {

    fun getDefaultPacks(): List<VocabularyPack> = listOf(
        VocabularyPack(
            id = "pack_ielts_academic",
            titleFa = "لغات ضروری آیلتس آکادمیک",
            titleEn = "IELTS Academic Essentials",
            descriptionFa = "واژگان منتخب با بالاترین تکرار در ریدینگ و رایتینگ تسک ۱ و ۲ آیلتس آکادمیک به همراه مثال و کالوکیشن.",
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
            descriptionFa = "واژگان فاخر و کلمات آکادمیک سطح پیشرفته برای نمره‌های ۸ به بالا در آزمون‌های بین‌المللی.",
            level = "C1-C2",
            exam = "All Exams",
            wordCount = 110,
            isDownloaded = true,
            category = "Advanced",
            iconName = "psychology"
        ),
        VocabularyPack(
            id = "pack_oxford_essential",
            titleFa = "واژگان کلیدی آکسفورد (B1-B2)",
            titleEn = "Oxford Essential 3000",
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
            descriptionFa = "کالوکیشن‌های طلایی، واژگان تحلیل داده در تسک ۱ و پیوندهای منطقی برای تسک ۲.",
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
            descriptionFa = "افعال عبارتی پرکاربرد و اصطلاحات طبیعی برای افزایش روانی کلام و نمره Fluency.",
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
        // Total items is 15 + 15 + 15 + 15 + 10 + 72 + 103 + 105 = 350 items!
        return all
    }
}
