package com.example.ui.navigation

sealed class Screen(val route: String) {
    // Bottom Nav Tabs
    object Home : Screen("home")
    object Vocab : Screen("vocab")
    object Learn : Screen("learn")
    object Practice : Screen("practice")
    object Profile : Screen("profile")

    // Sub-screens
    object WordDetail : Screen("word_detail/{vocabId}") {
        fun createRoute(vocabId: Long) = "word_detail/$vocabId"
    }
    object AddWord : Screen("add_word")
    object AiGenerateVocab : Screen("ai_generate_vocab")
    object AiVocabCard : Screen("ai_vocab_card")
    object ImportCenter : Screen("import_center")
    object VocabPacks : Screen("vocab_packs")
    object ExamTracks : Screen("exam_tracks")
    object SrsReview : Screen("srs_review")
    object Flashcards : Screen("flashcards")
    object AiTutor : Screen("ai_tutor")
    object GrammarDetail : Screen("grammar_detail/{topicId}") {
        fun createRoute(topicId: String) = "grammar_detail/$topicId"
    }
    object ReadingDetail : Screen("reading_detail/{passageId}") {
        fun createRoute(passageId: String) = "reading_detail/$passageId"
    }
    object ListeningDetail : Screen("listening_detail/{exerciseId}") {
        fun createRoute(exerciseId: String) = "listening_detail/$exerciseId"
    }
    object WritingGrader : Screen("writing_grader")
    object SpeakingPractice : Screen("speaking_practice")
    object MistakeNotebook : Screen("mistake_notebook")
    object DiagnosticTest : Screen("diagnostic_test")
    object Onboarding : Screen("onboarding")
}
