package com.example.ui.screens.review

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.ui.theme.LinguaTheme
import com.example.vocab.LexicalBank
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 30)
class ReviewScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun matchProductionWindow() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
    @Composable private fun ReviewHost(content: @Composable () -> Unit) {
        LinguaTheme(darkTheme = false) {
            Scaffold { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() }
            }
        }
    }
    private val item = VocabularyItem(id = 1, word = "buy", persianMeaning = "خریدن", correctCount = 2)
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LinguaFaReview")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } finally { bitmap.recycle() }
    }

    @Test fun typingWithKeyboardHintAndNextCard() {
        val first = ReviewTask(item, ReviewMode.WORD_RECALL, "خریدن", "buy")
        val second = ReviewTask(item.copy(id = 2, word = "retain"), ReviewMode.WORD_RECALL, "حفظ کردن", "retain")
        val state = mutableStateOf(ReviewSessionUiState(queue = listOf(first, second), sessionTotal = 2))
        var submitted: ReviewRating? = null
        lateinit var rootView: android.view.View
        compose.setContent {
            val view = LocalView.current
            SideEffect { rootView = view }
            ReviewHost {
                ReviewSessionScreen(state.value, ReviewSessionActions(
                    onAnswer = { state.value = state.value.copy(answerText = it) },
                    onHint = { state.value = state.value.copy(hintUsed = true) },
                    onCheck = { state.value = state.value.copy(answerChecked = true, isAnswerRevealed = true, typedAnswerCorrect = true) },
                    onRate = { submitted = it; state.value = state.value.copy(currentIndex = 1, answerText = "", answerChecked = false, isAnswerRevealed = false, hintUsed = false) }
                ))
            }
        }
        compose.onNodeWithText("Target:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Check Answer").assertIsNotEnabled()
        compose.onNodeWithText("Show a hint").performClick()
        compose.onNodeWithText("Target: b", substring = true).assertExists()
        // Enable the real device IME after the test runner's device setup, then send a touch.
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put secure show_ime_with_hard_keyboard 1").close()
        compose.onNodeWithText("Your answer").performTouchInput { click() }
        compose.runOnIdle {
            rootView.context.getSystemService(InputMethodManager::class.java).showSoftInput(rootView, InputMethodManager.SHOW_IMPLICIT)
            rootView.windowInsetsController?.show(WindowInsets.Type.ime())
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            rootView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
        }
        compose.onNodeWithText("Your answer").performTextInput("buy")
        compose.onNodeWithText("Check Answer").assertIsDisplayed()
        compose.onNodeWithText("I don’t know").assertIsDisplayed()
        compose.onNodeWithText("خریدن").assertIsDisplayed()
        compose.onNodeWithText("Your answer").assertIsDisplayed()
        screenshot("01-keyboard")
        compose.onNodeWithText("Check Answer").performClick()
        compose.onNodeWithText("Good").assertIsDisplayed().performClick()
        assertEquals(ReviewRating.HARD, submitted)
        compose.onNodeWithText("Word 2 of 2").assertIsDisplayed()
        // Android's IME hides asynchronously; wait for the next cue to settle on screen.
        compose.waitUntil(timeoutMillis = 5_000) {
            rootView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == false &&
                runCatching { compose.onNodeWithText("حفظ کردن").assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithText("حفظ کردن").assertIsDisplayed()
        compose.onNodeWithText("Target:", substring = true).assertDoesNotExist()
        screenshot("02-next-card")
    }

    @Test fun synonymFeedbackAllowsValidAlternativeAndExplainsBothSenses() {
        val pair = LexicalBank.forWord("buy")!!
        val task = ReviewTask(item, ReviewMode.SYNONYM, "buy or purchase?\n${pair.prompt("buy")}", "buy",
            pair.noteFa, pair.answers("buy"), lexicalRelation = pair)
        val state = ReviewSessionUiState(queue = listOf(task), sessionTotal = 1, answerText = "purchase",
            answerChecked = true, typedAnswerCorrect = true, isAnswerRevealed = true)
        compose.setContent { ReviewHost { ReviewSessionScreen(state, ReviewSessionActions()) } }
        compose.onNodeWithText("Valid alternative. The target word stays ungraded.").assertExists()
        compose.onNodeWithText(pair.second.definition).performScrollTo().assertIsDisplayed()
        screenshot("03-synonym-senses")
        compose.onNodeWithText("Continue").assertIsDisplayed()
        compose.onNodeWithText("Good").assertIsNotEnabled()
    }

    @Test fun wrongAnswerRetryAndRetentionSettingsRemainAccessible() {
        val task = ReviewTask(item, ReviewMode.WORD_RECALL, "خریدن", "buy")
        val state = mutableStateOf(ReviewSessionUiState(queue = listOf(task), sessionTotal = 1))
        var retention = 0.0
        var retry = false
        compose.setContent {
            ReviewHost { ReviewSessionScreen(state.value, ReviewSessionActions(
                onDontKnow = { state.value = state.value.copy(answerChecked = true, typedAnswerCorrect = false, isAnswerRevealed = true) },
                onStart = { retry = true }, onRetention = { retention = it }
            )) }
        }
        compose.onNodeWithText("FSRS 90%", substring = true).performClick()
        compose.onNodeWithText("95% —", substring = true).performClick()
        assertEquals(.95, retention, 0.0)
        compose.onNodeWithText("I don’t know").performClick()
        compose.onNodeWithText("Good").assertIsNotEnabled()
        compose.onNodeWithText("Again").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(errorMessage = "Save failed; retry the same card") }
        compose.onNodeWithText("Restart review").assertIsDisplayed().performClick()
        assertEquals(true, retry)
        screenshot("04-save-error")
    }
}
