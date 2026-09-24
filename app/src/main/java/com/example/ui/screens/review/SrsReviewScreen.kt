package com.example.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
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

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "مرور هوشمند روزانه (SRS)",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            if (state.isSessionFinished) {
                // Session Finished Celebration Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
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
                                text = "آفرین! مرور امروز تکمیل شد 🎉",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${state.sessionTotal} لغت با موفقیت مرور و در فواصل زمانی آینده برنامه‌ریزی شدند.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+${state.xpEarned} امتیاز تجربه (XP) کسب شد",
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
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
            } else {
                val currentWord = state.queue.getOrNull(state.currentIndex)

                if (currentWord == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("در حال آماده‌سازی کارت‌های مرور...")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Progress Bar Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مرور امروز  •  ${state.currentIndex + 1} از ${state.queue.size}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${currentWord.mastery}% تسلط",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { ((state.currentIndex + 1).toFloat() / state.queue.size).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Exercise Mode Dispatcher
                        when (state.currentExerciseType) {
                            ReviewExerciseType.FLASHCARD -> {
                                FlashcardExercise(
                                    item = currentWord,
                                    isRevealed = state.isAnswerRevealed,
                                    onReveal = { viewModel.revealAnswer() },
                                    onPlayAudio = { tts.speak(currentWord.word) },
                                    onRate = { rating -> viewModel.submitRating(rating) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ReviewExerciseType.MULTIPLE_CHOICE_EN_FA,
                            ReviewExerciseType.MULTIPLE_CHOICE_FA_EN,
                            ReviewExerciseType.LISTENING_CHOOSE -> {
                                MultipleChoiceExercise(
                                    item = currentWord,
                                    exerciseType = state.currentExerciseType,
                                    options = state.multipleChoiceOptions,
                                    selectedIndex = state.selectedOptionIndex,
                                    isChecked = state.isOptionAnswerChecked,
                                    onSelectOption = { viewModel.selectMultipleChoiceOption(it) },
                                    onPlayAudio = { tts.speak(currentWord.word) },
                                    onNext = { viewModel.advanceQueue() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ReviewExerciseType.TYPE_WORD -> {
                                TypeWordExercise(
                                    item = currentWord,
                                    typedInput = state.typedInput,
                                    isCorrect = state.isTypedCorrect,
                                    onInputChanged = { viewModel.onTypedInputChanged(it) },
                                    onCheck = { viewModel.checkTypedAnswer() },
                                    onNext = { viewModel.advanceQueue() },
                                    onPlayAudio = { tts.speak(currentWord.word) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
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
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRevealed) "پاسخ را مرور کن" else "معنی این واژه را به خاطر بیاور",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                CefrBadge(level = item.cefrLevel)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.word,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                if (item.ipa.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.ipa,
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                AudioSpeakerButton(onClick = onPlayAudio, size = 44)

                Spacer(modifier = Modifier.height(28.dp))

                if (item.example.isNotEmpty()) {
                    val blankedSentence = item.example.replace(Regex("(?i)\\b${item.word}\\b"), "_______")
                    Text(
                        text = "\"$blankedSentence\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                if (isRevealed) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (item.examplePersian.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.examplePersian,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isRevealed) {
            Button(
                onClick = onReveal,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("نمایش پاسخ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            // 4 SRS Response Buttons (دوباره، سخت بود، خوب بود، آسان بود)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResponseRatingButton(
                    label = "دوباره",
                    sub = "< ۱ ساعت",
                    color = ErrorRed,
                    onClick = { onRate(ReviewRating.AGAIN) },
                    modifier = Modifier.weight(1f)
                )
                ResponseRatingButton(
                    label = "سخت",
                    sub = "۱ روز",
                    color = Color(0xFFD97706),
                    onClick = { onRate(ReviewRating.HARD) },
                    modifier = Modifier.weight(1f)
                )
                ResponseRatingButton(
                    label = "خوب",
                    sub = "۳ روز",
                    color = PrimaryBlue,
                    onClick = { onRate(ReviewRating.GOOD) },
                    modifier = Modifier.weight(1f)
                )
                ResponseRatingButton(
                    label = "آسان",
                    sub = "۶ روز",
                    color = SuccessGreen,
                    onClick = { onRate(ReviewRating.EASY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
        modifier = modifier.height(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = sub, fontSize = 9.sp, color = color.copy(alpha = 0.78f))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (exerciseType == ReviewExerciseType.LISTENING_CHOOSE) {
                    Text(
                        text = "به صوت گوش دهید و معنی درست را انتخاب کنید:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    AudioSpeakerButton(onClick = onPlayAudio, size = 56)
                } else if (exerciseType == ReviewExerciseType.MULTIPLE_CHOICE_EN_FA) {
                    Text(
                        text = "معنی صحیح واژه زیر چیست؟",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AudioSpeakerButton(onClick = onPlayAudio, size = 32)
                    }
                } else {
                    Text(
                        text = "کدام واژه انگلیسی معادل عبارت زیر است؟",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEachIndexed { idx, optionText ->
                val isSelected = selectedIndex == idx
                val isCorrect = when (exerciseType) {
                    ReviewExerciseType.MULTIPLE_CHOICE_FA_EN -> optionText == item.word
                    else -> optionText == item.persianMeaning
                }

                val btnBgColor = when {
                    !isChecked -> MaterialTheme.colorScheme.surface
                    isCorrect -> Color(0xFFDCFCE7)
                    isSelected -> Color(0xFFFEE2E2)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderStroke = when {
                    !isChecked && isSelected -> androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue)
                    isChecked && isCorrect -> androidx.compose.foundation.BorderStroke(2.dp, SuccessGreen)
                    isChecked && isSelected -> androidx.compose.foundation.BorderStroke(2.dp, ErrorRed)
                    else -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }

                Surface(
                    color = btnBgColor,
                    shape = RoundedCornerShape(14.dp),
                    border = borderStroke,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isChecked) { onSelectOption(idx) }
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

        Spacer(modifier = Modifier.height(16.dp))

        if (isChecked) {
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
    onNext: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "واژه انگلیسی را تایپ کنید (Active Recall):",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.persianMeaning,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                if (isCorrect != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "پاسخ صحیح: ${item.word}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AudioSpeakerButton(onClick = onPlayAudio, size = 30)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = typedInput,
            onValueChange = onInputChanged,
            placeholder = { Text("Enter English word...") },
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
                Text("بررسی پاسخ", fontWeight = FontWeight.Bold)
            }
        } else {
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
