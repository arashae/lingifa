package com.example.ui.screens.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GrammarTopic
import com.example.data.model.ListeningExercise
import com.example.data.model.ReadingPassage
import com.example.ui.components.AppCard
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Dimens

private val learnTabs = listOf("Grammar", "Reading", "Listening")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LearnHomeScreen(
    viewModel: LearnViewModel,
    onNavigateToGrammarDetail: (String) -> Unit,
    onNavigateToReadingDetail: (String) -> Unit,
    onNavigateToListeningDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val selectedIndex = learnTabs.indexOf(state.selectedCategory).coerceAtLeast(0)
    val sectionTitle = when (state.selectedCategory) {
        "Reading" -> "Reading Passages"
        "Listening" -> "Listening Practice"
        else -> "Grammar Modules"
    }
    val sectionDescription = when (state.selectedCategory) {
        "Reading" -> "Academic passages with interactive vocabulary lookup"
        "Listening" -> "Audio comprehension exercises with practice and transcripts"
        else -> "Practical grammar modules with explanations and common mistakes"
    }

    EnglishLtrLayout {
        Scaffold(
            topBar = { LinguaTopAppBar(title = "Learn & Practice") },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SectionHeader(
                    title = "Small Steps Every Day",
                    subtitle = "Bite-sized structured modules for language mastery",
                    modifier = Modifier.padding(
                        start = Dimens.screenGutter,
                        end = Dimens.screenGutter,
                        top = Dimens.blockGap,
                        bottom = Dimens.sectionGap
                    )
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}
                ) {
                    learnTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { viewModel.selectCategory(title) },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
                SectionHeader(
                    title = sectionTitle,
                    subtitle = sectionDescription,
                    modifier = Modifier.padding(
                        start = Dimens.screenGutter,
                        end = Dimens.screenGutter,
                        top = Dimens.space8,
                        bottom = Dimens.space4
                    )
                )
                when (state.selectedCategory) {
                    "Grammar" -> GrammarList(
                        topics = state.grammarTopics,
                        onClick = onNavigateToGrammarDetail
                    )
                    "Reading" -> ReadingList(
                        passages = state.readingPassages,
                        onClick = onNavigateToReadingDetail
                    )
                    else -> ListeningList(
                        exercises = state.listeningExercises,
                        onClick = onNavigateToListeningDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun GrammarList(
    topics: List<GrammarTopic>,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.space4,
            bottom = Dimens.space20
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
    ) {
        items(topics, key = { it.id }) { topic ->
            GrammarTopicCard(topic = topic, onClick = { onClick(topic.id) })
        }
    }
}

@Composable
private fun ReadingList(
    passages: List<ReadingPassage>,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.space4,
            bottom = Dimens.space20
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
    ) {
        items(passages, key = { it.id }) { passage ->
            ReadingPassageCard(passage = passage, onClick = { onClick(passage.id) })
        }
    }
}

@Composable
private fun ListeningList(
    exercises: List<ListeningExercise>,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.space4,
            bottom = Dimens.space20
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
    ) {
        items(exercises, key = { it.id }) { exercise ->
            ListeningExerciseCard(exercise = exercise, onClick = { onClick(exercise.id) })
        }
    }
}

@Composable
private fun GrammarTopicCard(
    topic: GrammarTopic,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, padding = Dimens.cardPaddingTight) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = Icons.Default.School,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Dimens.cardPadding))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.space2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PersianContentRtl {
                        Text(
                            text = topic.titleFa,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.space6))
                    CefrBadge(level = topic.level)
                }
                Text(
                    text = topic.titleEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Dimens.space6))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconMd)
            )
        }
    }
}

@Composable
private fun ReadingPassageCard(
    passage: ReadingPassage,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, padding = Dimens.cardPaddingTight) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Default.Bookmark,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                PersianContentRtl {
                    Text(
                        text = passage.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.space6))
                CefrBadge(level = passage.level)
            }
            Text(
                text = passage.titleEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            PersianContentRtl {
                Text(
                    text = passage.summaryFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ListeningExerciseCard(
    exercise: ListeningExercise,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, padding = Dimens.cardPaddingTight) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Default.Hearing,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                PersianContentRtl {
                    Text(
                        text = exercise.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.space6))
                CefrBadge(level = exercise.level)
            }
            Text(
                text = exercise.titleEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${exercise.exam} · ${exercise.topic}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
