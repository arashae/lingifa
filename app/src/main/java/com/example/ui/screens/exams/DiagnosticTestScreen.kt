package com.example.ui.screens.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.IconTile
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun DiagnosticTestScreen(
    viewModel: ExamsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "Smart Level Placement Test",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            if (state.diagnosticFinished) {
                DiagnosticResult(
                    estimatedLevel = state.estimatedCefrLevel?.take(2) ?: "B2",
                    score = state.diagnosticScore,
                    total = state.diagnosticQuestions.size,
                    onBack = onBack
                )
            } else {
                val currentQuestion = state.diagnosticQuestions.getOrNull(state.diagnosticCurrentIndex)
                if (currentQuestion != null) {
                    DiagnosticQuestionContent(
                        questionIndex = state.diagnosticCurrentIndex,
                        questionCount = state.diagnosticQuestions.size,
                        level = currentQuestion.testedLevel,
                        question = currentQuestion.questionEn,
                        options = currentQuestion.options,
                        onAnswer = viewModel::answerDiagnosticQuestion
                    )
                }
            }

            @Suppress("UNUSED_EXPRESSION")
            Column(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun DiagnosticResult(
    estimatedLevel: String,
    score: Int,
    total: Int,
    onBack: () -> Unit
) {
    EnglishLtrLayout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space16)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
        ) {
            AppCard(
                shape = RoundedCornerShape(Dimens.radiusXl),
                padding = Dimens.cardPaddingLoose,
                borderColor = MaterialTheme.colorScheme.outlineVariant
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                ) {
                    IconTile(
                        icon = Icons.Default.EmojiEvents,
                        tint = Accent.success,
                        size = Dimens.space24 * 2,
                        iconSize = Dimens.iconLg,
                        shape = CircleShape
                    )
                    Text(
                        text = "Level placement completed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CefrBadge(level = estimatedLevel)
                    Text(
                        text = "Score: $score of $total correct",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your profile is updated and future lessons will be personalized to this starting level.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.minTapTarget)
                    ) {
                        Text("Continue to learning", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticQuestionContent(
    questionIndex: Int,
    questionCount: Int,
    level: String,
    question: String,
    options: List<String>,
    onAnswer: (Int) -> Unit
) {
    val progress = ((questionIndex + 1).toFloat() / questionCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()
    var selectedOption by remember(questionIndex) { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space16)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Question ${questionIndex + 1} of $questionCount",
                subtitle = "Choose the best answer",
                modifier = Modifier.weight(1f)
            )
            CefrBadge(level = level)
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.progressHeight)
                .clip(RoundedCornerShape(Dimens.radiusPill))
                .semantics {
                    contentDescription = "Question progress"
                    stateDescription = "$percent% complete"
                },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "$percent% complete",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AppCard(
            shape = RoundedCornerShape(Dimens.radiusLg),
            padding = Dimens.cardPaddingLoose,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }

        FieldLabel("Your answer")
        options.forEachIndexed { optionIndex, option ->
            val isSelected = selectedOption == optionIndex
            AppInset(
                modifier = Modifier
                    .heightIn(min = Dimens.rowHeight)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            selectedOption = optionIndex
                            onAnswer(optionIndex)
                        }
                    ),
                padding = Dimens.cardPaddingTight,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(Dimens.radiusMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.iconTileSm)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${optionIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
