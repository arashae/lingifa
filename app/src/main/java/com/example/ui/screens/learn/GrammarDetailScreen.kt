package com.example.ui.screens.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.seed.GrammarSeed
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun GrammarDetailScreen(
    topicId: String,
    onBack: () -> Unit
) {
    val topic = remember(topicId) {
        GrammarSeed.getTopics().find { it.id == topicId } ?: GrammarSeed.getTopics().first()
    }
    var selectedOptionIndices by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var isAnswerChecked by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = topic.titleEn,
                    subtitle = "Grammar lesson",
                    onBack = onBack,
                    actions = {
                        CefrBadge(
                            level = topic.level,
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
                    AppCard(padding = Dimens.cardPaddingTight) {
                        PersianContentRtl {
                            Text(
                                text = topic.descriptionFa,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                item {
                    AppCard {
                        SectionHeader(title = "Grammar Rules & Structure")
                        Spacer(modifier = Modifier.height(Dimens.blockGap))
                        PersianContentRtl {
                            Text(
                                text = topic.rulesFa,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                item {
                    AppCard {
                        SectionHeader(title = "Academic & Practical Examples")
                        Column(
                            modifier = Modifier.padding(top = Dimens.blockGap),
                            verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                        ) {
                            topic.examplesEn.forEachIndexed { index, exampleEn ->
                                val exampleFa = topic.examplesFa.getOrNull(index).orEmpty()
                                AppInset {
                                    Text(
                                        text = exampleEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (exampleFa.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Dimens.space6))
                                        FieldLabel(text = "Translation")
                                        PersianContentRtl {
                                            Text(
                                                text = exampleFa,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (topic.iranianCommonMistakesFa.isNotEmpty()) {
                    item {
                        AppCard(
                            containerColor = Accent.dangerSoft,
                            borderColor = Accent.danger
                        ) {
                            SectionHeader(title = "Common Pitfalls for Persian Speakers")
                            Spacer(modifier = Modifier.height(Dimens.blockGap))
                            PersianContentRtl {
                                Text(
                                    text = topic.iranianCommonMistakesFa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Accent.dangerOnSoft
                                )
                            }
                        }
                    }
                }
                if (topic.quizQuestions.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Grammar Practice Quiz")
                    }
                    itemsIndexed(
                        items = topic.quizQuestions,
                        key = { index, _ -> "${topic.id}-$index" }
                    ) { index, question ->
                        QuizQuestionCard(
                            questionIndex = index,
                            question = question.questionEn,
                            options = question.options,
                            correctIndex = question.correctIndex,
                            explanationFa = question.explanationFa,
                            selectedOption = selectedOptionIndices[index],
                            checked = isAnswerChecked[index] == true,
                            onOptionSelected = { optionIndex ->
                                selectedOptionIndices = selectedOptionIndices.toMutableMap().apply {
                                    this[index] = optionIndex
                                }
                            },
                            onCheck = {
                                isAnswerChecked = isAnswerChecked.toMutableMap().apply {
                                    this[index] = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuizQuestionCard(
    questionIndex: Int,
    question: String,
    options: List<String>,
    correctIndex: Int,
    explanationFa: String,
    selectedOption: Int?,
    checked: Boolean,
    onOptionSelected: (Int) -> Unit,
    onCheck: () -> Unit
) {
    AppCard(padding = Dimens.cardPaddingTight) {
        Text(
            text = "Question ${questionIndex + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimens.space4))
        Text(
            text = question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Column(
            modifier = Modifier.padding(top = Dimens.blockGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.space8)
        ) {
            options.forEachIndexed { optionIndex, optionText ->
                val isSelected = selectedOption == optionIndex
                val isCorrect = optionIndex == correctIndex
                val containerColor = when {
                    !checked && isSelected -> MaterialTheme.colorScheme.primaryContainer
                    checked && isCorrect -> Accent.successSoft
                    checked && isSelected -> Accent.dangerSoft
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when {
                    !checked && isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    checked && isCorrect -> Accent.successOnSoft
                    checked && isSelected -> Accent.dangerOnSoft
                    else -> MaterialTheme.colorScheme.onSurface
                }

                AppInset(
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.rowHeight),
                    color = containerColor,
                    contentColor = contentColor,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    onClick = if (checked) null else {
                        { onOptionSelected(optionIndex) }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(Dimens.iconSm)
                            )
                        }
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        if (!checked && selectedOption != null) {
            Button(
                onClick = onCheck,
                modifier = Modifier
                    .padding(top = Dimens.blockGap)
                    .heightIn(min = Dimens.minTapTarget),
                shape = RoundedCornerShape(Dimens.radiusSm)
            ) {
                Text(
                    text = "Check Answer",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (checked) {
            Spacer(modifier = Modifier.height(Dimens.blockGap))
            FieldLabel(text = "Explanation")
            PersianContentRtl {
                Text(
                    text = explanationFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
