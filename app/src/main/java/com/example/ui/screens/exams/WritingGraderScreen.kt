package com.example.ui.screens.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.network.WritingEvaluationResult
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.HairLine
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun WritingGraderScreen(
    viewModel: ExamsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val activePrompt = state.prompts.getOrNull(state.selectedPromptIndex)

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "IELTS Writing Evaluator",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                if (activePrompt != null) {
                    AppCard(
                        shape = RoundedCornerShape(Dimens.radiusLg),
                        borderColor = MaterialTheme.colorScheme.outlineVariant
                    ) {
                        FieldLabel("Choose a task")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                        ) {
                            state.prompts.forEachIndexed { index, prompt ->
                                SelectChip(
                                    text = prompt.titleEn,
                                    selected = state.selectedPromptIndex == index,
                                    onClick = { viewModel.selectPrompt(index) }
                                )
                            }
                        }

                        HairLine()
                        SectionHeader(
                            title = activePrompt.titleEn,
                            subtitle = "Write at least 250 words",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = activePrompt.promptTextEn,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (activePrompt.guideFa.isNotBlank()) {
                            AppInset(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                FieldLabel("Writing guide")
                                PersianContentRtl {
                                    Text(
                                        text = activePrompt.guideFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        FieldLabel("Your essay submission")
                        OutlinedTextField(
                            value = state.essayInput,
                            onValueChange = { viewModel.onEssayInputChanged(it) },
                            placeholder = { Text("Write your essay in English here…") },
                            minLines = 8,
                            maxLines = 16,
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Word count: ${state.wordCount} (minimum 250)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.wordCount >= 250) Accent.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "English",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.submitEssayForGrading() },
                            enabled = state.essayInput.isNotBlank() && !state.isEvaluatingWriting,
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.minTapTarget)
                        ) {
                            if (state.isEvaluatingWriting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.iconMd),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = Dimens.space2
                                )
                                Spacer(modifier = Modifier.width(Dimens.space8))
                                Text("Evaluating with 4 IELTS criteria…", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(Dimens.space8))
                                Text("Evaluate and grade with AI", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    state.writingResult?.let { result ->
                        WritingResultCard(result = result)
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingResultCard(result: WritingEvaluationResult) {
    AppCard(
        shape = RoundedCornerShape(Dimens.radiusXl),
        padding = Dimens.cardPaddingLoose,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        SectionHeader(
            title = "Estimated IELTS score: Band ${result.estimatedBand}",
            subtitle = "AI assessment, not an official IELTS score",
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4)
        ) {
            ScoreBadge("Task Response", result.taskAchievementScore, Modifier.weight(1f))
            ScoreBadge("Coherence", result.coherenceScore, Modifier.weight(1f))
            ScoreBadge("Lexical", result.lexicalScore, Modifier.weight(1f))
            ScoreBadge("Grammar", result.grammarScore, Modifier.weight(1f))
        }

        AppInset(
            padding = Dimens.cardPaddingTight,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            FieldLabel("Overall evaluation")
            PersianContentRtl {
                Text(
                    text = result.overallFeedbackFa,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (result.strengthsFa.isNotEmpty()) {
                FieldLabel("Essay strengths", color = Accent.success)
                PersianFeedbackBullets(result.strengthsFa)
            }
            if (result.mainIssuesFa.isNotEmpty()) {
                FieldLabel("Areas for improvement", color = Accent.danger)
                PersianFeedbackBullets(result.mainIssuesFa)
            }
        }

        if (result.sentenceCorrections.isNotEmpty()) {
            SectionHeader(
                title = "Sentence corrections and better alternatives",
                subtitle = "Review each suggestion before resubmitting"
            )
            result.sentenceCorrections.forEach { correction ->
                AppInset(
                    padding = Dimens.cardPaddingTight,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "Original: ${correction.original}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Accent.danger,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Improved: ${correction.corrected}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Accent.success,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (correction.explanationFa.isNotEmpty()) {
                        FieldLabel("Explanation")
                        PersianContentRtl {
                            Text(
                                text = correction.explanationFa,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersianFeedbackBullets(items: List<String>) {
    PersianContentRtl {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScoreBadge(
    title: String,
    score: String,
    modifier: Modifier = Modifier
) {
    AppInset(
        modifier = modifier,
        padding = Dimens.space6,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(Dimens.radiusSm)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
