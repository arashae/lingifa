package com.example.data.seed

data class ExamPrompt(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val examType: String, // "IELTS_WRITING_TASK2", "IELTS_WRITING_TASK1", "IELTS_SPEAKING", "TOEFL_2026_DISCUSSION", "TOEFL_2026_COMPLETE_WORDS"
    val promptTextEn: String,
    val guideFa: String,
    val sampleHighBandResponse: String = "",
    val timeLimitMinutes: Int = 40
)

data class DiagnosticQuestion(
    val id: Int,
    val questionEn: String,
    val options: List<String>,
    val correctIndex: Int,
    val testedLevel: String, // "A1", "A2", "B1", "B2", "C1"
    val explanationFa: String
)

object ExamSeed {
    fun getExamPrompts(): List<ExamPrompt> = listOf(
        ExamPrompt(
            id = "ielts_w2_environment",
            titleEn = "IELTS Writing Task 2: Individual vs Government Responsibility",
            titleFa = "رایتینگ تسک ۲ آیلتس: مسئولیت فردی در برابر دولتی برای محیط زیست",
            examType = "IELTS_WRITING_TASK2",
            promptTextEn = "Some people argue that environmental degradation can only be solved by government regulation, while others believe that individual lifestyle choices have a greater impact.\n\nDiscuss both views and give your own opinion.\n\nWrite at least 250 words.",
            guideFa = "در این مقاله باید به هر دو دیدگاه بپردازید: چرا عده‌ای معتقدند قوانین دولتی و جریمه‌ها موثرترند و چرا عده‌ای تغییر رفتار شهروندان را کلیدی می‌دانند. ساختار پیشنهادی: مقدمه همراه با بازنویسی صورت سوال + پاراگراف بدنه اول (نقش دولت) + پاراگراف بدنه دوم (نقش فردی) + نتیجه‌گیری شفاف همراه با بیان موضع خودتان.",
            timeLimitMinutes = 40
        ),
        ExamPrompt(
            id = "toefl_academic_discussion_ai",
            titleEn = "TOEFL iBT 2026: Academic Discussion on Remote Work",
            titleFa = "تافل ۲۰۲۶: مباحثه دانشگاهی پیرامون دورکاری",
            examType = "TOEFL_2026_DISCUSSION",
            promptTextEn = "Professor Diaz: In recent years, remote work has expanded dramatically. Some economists argue that continuing remote work indefinitely improves worker well-being and reduces carbon footprints. Others contend that remote work undermines innovation and damages mentorship for younger staff. In your post, express and support your opinion on whether companies should mandate returning to physical offices.",
            guideFa = "در قالب جدید تافل ۲۰۲۶، استاد یک سوال آکادمیک مطرح می‌کند و شما ۱۰ دقیقه فرصت دارید متنی با حداقل ۱۰۰ کلمه بنویسید که دیدگاه شما را با دلایل منسجم و مثال‌های عینی مطرح کند.",
            timeLimitMinutes = 10
        ),
        ExamPrompt(
            id = "ielts_sp_part2_technology",
            titleEn = "IELTS Speaking Part 2: A Useful Mobile Application",
            titleFa = "اسپیکینگ تسک ۲ آیلتس: توصیف یک اپلیکیشن کاربردی",
            examType = "IELTS_SPEAKING",
            promptTextEn = "Describe a mobile application or digital tool that you find indispensable in your daily life.\n\nYou should say:\n- What the application is and what it does\n- When and how you first learned about it\n- How frequently you utilize it\nAnd explain why you consider this tool so valuable to you.",
            guideFa = "یک دقیقه فرصت نت‌برداری دارید و سپس باید بین ۱ تا ۲ دقیقه پیوسته صحبت کنید. از کلمات سطح بالا مثل indispensable, intuitive, streamline your workflow, seamlessly بهره ببرید.",
            timeLimitMinutes = 2
        )
    )

    fun getDiagnosticQuestions(): List<DiagnosticQuestion> = listOf(
        DiagnosticQuestion(
            id = 1,
            questionEn = "Every morning, my colleague _______ the train at 7:30 AM.",
            options = listOf("catches", "catch", "is catch", "catching"),
            correctIndex = 0,
            testedLevel = "A1",
            explanationFa = "فاعل my colleague سوم شخص مفرد است و در زمان حال ساده فعل باید پسوند -s بگیرد."
        ),
        DiagnosticQuestion(
            id = 2,
            questionEn = "We didn't _______ the museum yesterday because of the public holiday.",
            options = listOf("visit", "visited", "visiting", "to visit"),
            correctIndex = 0,
            testedLevel = "A2",
            explanationFa = "پس از فعل کمکی didn't در زمان گذشته، فعل اصلی به صورت ساده (visit) می‌آید."
        ),
        DiagnosticQuestion(
            id = 3,
            questionEn = "Dr. Evans _______ at the university for over fifteen years before retiring.",
            options = listOf("had taught", "has taught", "teaches", "is teaching"),
            correctIndex = 0,
            testedLevel = "B1",
            explanationFa = "عملی که پیش از عمل دیگری در گذشته (retiring) انجام شده و استمرار داشته، در زمان گذشته کامل (had taught) بیان می‌شود."
        ),
        DiagnosticQuestion(
            id = 4,
            questionEn = "If governments _______ in public transit earlier, urban pollution would not be this severe.",
            options = listOf("had invested", "invested", "would invest", "invest"),
            correctIndex = 0,
            testedLevel = "B2",
            explanationFa = "این جمله یک شرطی ترکیبی (Mixed Conditional) است: علت در گذشته (شرطی سوم had invested) و نتیجه در زمان حال (would not be)."
        ),
        DiagnosticQuestion(
            id = 5,
            questionEn = "Rarely _______ such decisive consensus among international climate researchers.",
            options = listOf("has there been", "there has been", "is there", "there was"),
            correctIndex = 0,
            testedLevel = "C1",
            explanationFa = "هنگامی که جمله با قید منفی Rarely آغاز می‌گردد، وارونگی (Inversion) دستوری الزامی است و فعل کمکی قبل از فاعل قرار می‌گیرد."
        )
    )
}
