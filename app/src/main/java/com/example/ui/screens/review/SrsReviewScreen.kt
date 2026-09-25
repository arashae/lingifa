package com.example.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun SrsReviewScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "مرور هوشمند روزانه",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            if (state.isSessionFinished) {
                FinishedSessionCard(
                    total = state.sessionTotal,
                    xp = state.xpEarned,
                    onBack = onBack,
                    modifier = Modifier.padding(paddingValues)
                )
                return@Scaffold
            }

            val currentWord = state.queue.getOrNull(state.currentIndex)
            if (currentWord == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("در حال آماده‌سازی کارت‌های مرور…")
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مرور امروز • ${state.currentIndex + 1} از ${state.queue.size}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${currentWord.mastery}% تسلط",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ((state.currentIndex + 1).toFloat() / state.queue.size).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (state.currentExerciseType) {
                    ReviewExerciseType.FLASHCARD -> FlashcardExercise(
                        item = currentWord,
                        isRevealed = state.isAnswerRevealed,
                        onReveal = viewModel::revealAnswer,
                        onPlayAudio = { tts.speak(currentWord.word) },
                        onPlayExample = { if (currentWord.example.isNotBlank()) tts.speak(currentWord.example) },
                        onRate = viewModel::submitRating,
                        modifier = Modifier.weight(1f)
                    )

                    ReviewExerciseType.MULTIPLE_CHOICE_EN_FA,
                    ReviewExerciseType.MULTIPLE_CHOICE_FA_EN,
                    ReviewExerciseType.LISTENING_CHOOSE -> MultipleChoiceExercise(
                        item = currentWord,
                        exerciseType = state.currentExerciseType,
                        options = state.multipleChoiceOptions,
                        selectedIndex = state.selectedOptionIndex,
                        isChecked = state.isOptionAnswerChecked,
                        onSelectOption = viewModel::selectMultipleChoiceOption,
                        onPlayAudio = { tts.speak(currentWord.word) },
                        onNext = { viewModel.advanceQueue() },
                        modifier = Modifier.weight(1f)
                    )

                    ReviewExerciseType.TYPE_WORD -> TypeWordExercise(
                        item = currentWord,
                        typedInput = state.typedInput,
                        isCorrect = state.isTypedCorrect,
                        onInputChanged = viewModel::onTypedInputChanged,
                        onCheck = viewModel::checkTypedAnswer,
                        onRate = viewModel::submitRating,
                        onPlayAudio = { tts.speak(currentWord.word) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishedSessionCard(
    total: Int,
    xp: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (total == 0) "هنوز واژه‌ای برای مرور نداری" else "مرور امروز تکمیل شد 🎉",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (total == 0) {
                        "از مسیر CEFR یا بانک واژگان چند کلمه را وارد یادگیری کن؛ مرور بعدی خودکار برنامه‌ریزی می‌شود."
                    } else {
                        "$total کارت مرور شد و زمان مرور بعدی هر واژه بر اساس عملکردت تنظیم شد."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+$xp XP", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بازگشت به خانه", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FlashcardExercise(
    item: VocabularyItem,
    isRevealed: Boolean,
    onReveal: () -> Unit,
    onPlayAudio: () -> Unit,
    onPlayExample: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRevealed) "پاسخ را با چیزی که در ذهنت بود مقایسه کن" else "قبل از نمایش پاسخ، معنی یا کاربرد را در ذهنت بازیابی کن",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                CefrBadge(level = item.cefrLevel)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                if (item.ipa.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.ipa,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                AudioSpeakerButton(onClick = onPlayAudio, size = 44)

                if (isRevealed) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    if (item.englishDefinition.isNotBlank()) {
                        Text(
                            text = "Definition",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = item.englishDefinition,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (item.example.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            onClick = onPlayExample,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = "“${item.example}”",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (item.collocations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = item.collocations.take(3).joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    if (item.examplePersian.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.examplePersian,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        if (!isRevealed) {
            Button(
                onClick = onReveal,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("نمایش پاسخ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            RatingRow(item = item, onRate = onRate)
        }
    }
}

@Composable
private fun RatingRow(item: VocabularyItem, onRate: (ReviewRating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ResponseRatingButton(
            label = "یادم نبود",
            sub = SpacedRepetitionSystem.getIntervalLabel(item, ReviewRating.AGAIN),
            color = ErrorRed,
            onClick = { onRate(ReviewRating.AGAIN) },
            modifier = Modifier.weight(1f)
        )
        ResponseRatingButton(
            label = "سخت",
            sub = SpacedRepetitionSystem.getIntervalLabel(item, ReviewRating.HARD),
            color = Color(0xFFD97706),
            onClick = { onRate(ReviewRating.HARD) },
            modifier = Modifier.weight(1f)
        )
        ResponseRatingButton(
            label = "بلد بودم",
            sub = SpacedRepetitionSystem.getIntervalLabel(item, ReviewRating.GOOD),
            color = PrimaryBlue,
            onClick = { onRate(ReviewRating.GOOD) },
            modifier = Modifier.weight(1f)
        )
        ResponseRatingButton(
            label = "خیلی راحت",
            sub = SpacedRepetitionSystem.getIntervalLabel(item, ReviewRating.EASY),
            color = SuccessGreen,
            onClick = { onRate(ReviewRating.EASY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ResponseRatingButton(
    label: String,
    sub: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.10f),
            contentColor = color
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.32f)),
        modifier = modifier.height(62.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 5.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
            Text(text = sub, fontSize = 8.sp, color = color.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun MultipleChoiceExercise(
    item: VocabularyItem,
    exerciseType: ReviewExerciseType,
    options: List<String>,
    selectedIndex: Int?,
    isChecked: Boolean,
    onSelectOption: (Int) -> Unit,
    onPlayAudio: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (exerciseType) {
                    ReviewExerciseType.LISTENING_CHOOSE -> {
                        Text("به تلفظ گوش کن و معنی درست را انتخاب کن:")
                        Spacer(modifier = Modifier.height(14.dp))
                        AudioSpeakerButton(onClick = onPlayAudio, size = 56)
                    }
                    ReviewExerciseType.MULTIPLE_CHOICE_EN_FA -> {
                        Text("معنی صحیح واژه زیر چیست؟")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.word,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AudioSpeakerButton(onClick = onPlayAudio, size = 32)
                        }
                    }
                    else -> {
                        Text("کدام واژه انگلیسی با این معنی هماهنگ است؟")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            item.persianMeaning,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEachIndexed { idx, optionText ->
                val isSelected = selectedIndex == idx
                val isCorrect = when (exerciseType) {
                    ReviewExerciseType.MULTIPLE_CHOICE_FA_EN -> optionText == item.word
                    else -> optionText == item.persianMeaning
                }
                val background = when {
                    !isChecked -> MaterialTheme.colorScheme.surface
                    isCorrect -> Color(0xFFDCFCE7)
                    isSelected -> Color(0xFFFEE2E2)
                    else -> MaterialTheme.colorScheme.surface
                }
                val border = when {
                    !isChecked && isSelected -> androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue)
                    isChecked && isCorrect -> androidx.compose.foundation.BorderStroke(2.dp, SuccessGreen)
                    isChecked && isSelected -> androidx.compose.foundation.BorderStroke(2.dp, ErrorRed)
                    else -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }

                Surface(
                    color = background,
                    shape = RoundedCornerShape(14.dp),
                    border = border,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isChecked) { onSelectOption(idx) }
                ) {
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected || (isChecked && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChecked && isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (isChecked) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("واژه بعدی", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TypeWordExercise(
    item: VocabularyItem,
    typedInput: String,
    isCorrect: Boolean?,
    onInputChanged: (String) -> Unit,
    onCheck: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تمرین تکمیلی املا و تولید واژه",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.persianMeaning,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
                if (isCorrect != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isCorrect) "املای درست ✓" else "پاسخ صحیح: ${item.word}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AudioSpeakerButton(onClick = onPlayAudio, size = 30)
                    if (item.englishDefinition.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = item.englishDefinition,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "این تمرین به‌تنهایی وضعیت حافظه را تعیین نمی‌کند؛ حالا خودت میزان یادآوری را ثبت کن.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = typedInput,
            onValueChange = onInputChanged,
            enabled = isCorrect == null,
            placeholder = { Text("Enter English word…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isCorrect == null) {
            Button(
                onClick = onCheck,
                enabled = typedInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("بررسی املا", fontWeight = FontWeight.Bold)
            }
        } else {
            RatingRow(item = item, onRate = onRate)
        }
    }
}
