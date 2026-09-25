package com.example.ui.screens.learn

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import com.example.data.seed.ReadingListeningSeed
import com.example.ui.components.AppCard
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Dimens

private const val listeningUtteranceId = "lingua_listening_exercise"

@Composable
fun ListeningDetailScreen(
    exerciseId: String,
    viewModel: LearnViewModel,
    onBack: () -> Unit
) {
    val exercise = remember(exerciseId) {
        ReadingListeningSeed.getListeningExercises().find { it.id == exerciseId }
            ?: ReadingListeningSeed.getListeningExercises().first()
    }
    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var showTranscript by remember { mutableStateOf(false) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var checkedAnswers by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }
    val tts = remember {
        TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                isPlaying = false
            }

            @Deprecated("Use the errorCode overload")
            override fun onError(utteranceId: String?) {
                isPlaying = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isPlaying = false
            }
        })
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = exercise.titleEn,
                    subtitle = exercise.exam,
                    onBack = {
                        tts.stop()
                        isPlaying = false
                        onBack()
                    },
                    actions = {
                        CefrBadge(
                            level = exercise.level,
                            modifier = Modifier.padding(end = Dimens.screenGutter)
                        )
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = Dimens.screenGutter,
                    end = Dimens.screenGutter,
                    top = Dimens.sectionGap,
                    bottom = Dimens.space20
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
            ) {
                item {
                    AppCard(
                        padding = Dimens.cardPaddingLoose,
                        shape = RoundedCornerShape(Dimens.radiusLg)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        tts.stop()
                                        isPlaying = false
                                    } else if (ttsReady) {
                                        tts.language = Locale.US
                                        tts.setSpeechRate(0.9f)
                                        isPlaying = tts.speak(
                                            exercise.audioScriptEn,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            listeningUtteranceId
                                        ) == TextToSpeech.SUCCESS
                                    }
                                },
                                enabled = ttsReady,
                                modifier = Modifier
                                    .size(Dimens.minTapTarget + Dimens.space20)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Dimens.iconLg + Dimens.space12)
                                )
                            }
                            Text(
                                text = when {
                                    !ttsReady -> "Preparing audio..."
                                    isPlaying -> "Audio is playing..."
                                    else -> "Tap to listen to the audio passage"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { showTranscript = !showTranscript },
                                modifier = Modifier.heightIn(min = Dimens.minTapTarget),
                                shape = RoundedCornerShape(Dimens.radiusSm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.iconMd)
                                )
                                Text(
                                    text = if (showTranscript) "Hide Transcript" else "Show Transcript",
                                    modifier = Modifier.padding(start = Dimens.space6)
                                )
                            }
                        }
                    }
                }
                if (showTranscript) {
                    item {
                        AppCard {
                            SectionHeader(title = "English Transcript")
                            Text(
                                text = exercise.audioScriptEn,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = Dimens.blockGap)
                            )
                            SectionHeader(
                                title = "Persian Translation",
                                modifier = Modifier.padding(top = Dimens.sectionGap)
                            )
                            PersianContentRtl {
                                Text(
                                    text = exercise.transcriptFa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Dimens.blockGap)
                                )
                            }
                        }
                    }
                }
                if (exercise.questions.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Listening Comprehension Questions")
                    }
                    itemsIndexed(
                        items = exercise.questions,
                        key = { _, question -> question.id }
                    ) { index, question ->
                        QuizQuestionCard(
                            questionIndex = index,
                            question = question.questionEn,
                            options = question.options,
                            correctIndex = question.correctIndex,
                            explanationFa = question.explanationFa,
                            selectedOption = userAnswers[question.id],
                            checked = checkedAnswers[question.id] == true,
                            onOptionSelected = { optionIndex ->
                                userAnswers = userAnswers.toMutableMap().apply {
                                    this[question.id] = optionIndex
                                }
                            },
                            onCheck = {
                                checkedAnswers = checkedAnswers.toMutableMap().apply {
                                    this[question.id] = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
