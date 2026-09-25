package com.example.ui.screens.mistakes

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.MistakeRecord
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.EmptyState
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun MistakeNotebookScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedSkillFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    val skillFilters = listOf(
        "VOCABULARY" to "Vocabulary",
        "GRAMMAR" to "Grammar",
        "READING" to "Reading",
        "LISTENING" to "Listening",
        "SPEAKING" to "Speaking",
        "WRITING" to "Writing"
    )
    val statusFilters = listOf("Needs Review", "Resolved")

    val filteredMistakes = state.mistakes.filter { mistake ->
        val matchesSkill = selectedSkillFilter == "All" ||
            mistake.skillType.equals(selectedSkillFilter, ignoreCase = true)
        val matchesStatus = when (selectedStatusFilter) {
            "Needs Review" -> !mistake.isReviewed
            "Resolved" -> mistake.isReviewed
            else -> true
        }
        matchesSkill && matchesStatus
    }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "Mistake Notebook",
                    subtitle = "Review, filter, and resolve recorded errors",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(
                        horizontal = Dimens.screenGutter,
                        vertical = Dimens.space12
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                AppInset(
                    color = Accent.dangerSoft,
                    contentColor = Accent.dangerOnSoft
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Accent.danger,
                            modifier = Modifier.size(Dimens.iconLg)
                        )
                        Spacer(modifier = Modifier.width(Dimens.blockGap))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                        ) {
                            Text(
                                text = "Error Pattern Analysis",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Mistakes from reviews and exercises feed targeted spaced repetition.",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    SelectChip(
                        text = "All",
                        selected = selectedSkillFilter == "All" && selectedStatusFilter == "All",
                        onClick = {
                            selectedSkillFilter = "All"
                            selectedStatusFilter = "All"
                        }
                    )
                    statusFilters.forEach { status ->
                        SelectChip(
                            text = status,
                            selected = selectedStatusFilter == status,
                            onClick = { selectedStatusFilter = status }
                        )
                    }
                    skillFilters.forEach { (value, label) ->
                        SelectChip(
                            text = label,
                            selected = selectedSkillFilter == value,
                            onClick = { selectedSkillFilter = value }
                        )
                    }
                }

                if (filteredMistakes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Default.CheckCircle,
                            title = "No errors in this view",
                            message = "Choose different filters or continue learning to build your notebook."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                    ) {
                        items(filteredMistakes, key = { it.id }) { mistake ->
                            MistakeCard(
                                mistake = mistake,
                                onDelete = { viewModel.deleteMistake(mistake.id) },
                                onToggleReviewed = {
                                    viewModel.markMistakeReviewed(
                                        mistake.id,
                                        !mistake.isReviewed
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeCard(
    mistake: MistakeRecord,
    onDelete: () -> Unit,
    onToggleReviewed: () -> Unit
) {
    AppCard(
        containerColor = if (mistake.isReviewed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagChip(text = mistake.skillType)
                TagChip(
                    text = if (mistake.isReviewed) "Resolved" else "Needs Review",
                    containerColor = if (mistake.isReviewed) {
                        Accent.successSoft
                    } else {
                        Accent.warningSoft
                    },
                    contentColor = if (mistake.isReviewed) {
                        Accent.successOnSoft
                    } else {
                        Accent.warningOnSoft
                    }
                )
            }
            Row {
                IconButton(onClick = onToggleReviewed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (mistake.isReviewed) {
                            "Mark as needs review"
                        } else {
                            "Mark as resolved"
                        },
                        tint = if (mistake.isReviewed) {
                            Accent.success
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(Dimens.iconMd)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete error",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.iconMd)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.blockGap))
        Text(
            text = mistake.question,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(Dimens.blockGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppInset(
                    color = Accent.dangerSoft,
                    contentColor = Accent.dangerOnSoft
                ) {
                    Text(
                        text = "MY ANSWER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent.dangerOnSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mistake.myAnswer,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Accent.dangerOnSoft,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                AppInset(
                    color = Accent.successSoft,
                    contentColor = Accent.successOnSoft
                ) {
                    Text(
                        text = "CORRECT ANSWER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent.successOnSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mistake.correctAnswer,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Accent.successOnSoft,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (mistake.explanationFa.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.blockGap))
            AppInset {
                FieldLabel(text = "Explanation")
                PersianContentRtl {
                    Text(
                        text = mistake.explanationFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
