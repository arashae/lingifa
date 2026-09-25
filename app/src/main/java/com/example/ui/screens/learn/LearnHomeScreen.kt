package com.example.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrammarTopic
import com.example.data.model.ListeningExercise
import com.example.data.model.ReadingPassage
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal

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
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Small Steps Every Day",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Bite-sized structured modules for language mastery",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
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
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Practical grammar modules with explanations and common mistakes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(topics, key = { it.id }) { topic ->
            GrammarTopicCard(topic = topic, onClick = { onClick(topic.id) })
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ReadingList(
    passages: List<ReadingPassage>,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Academic reading passages with interactive vocabulary lookup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(passages, key = { it.id }) { passage ->
            ReadingPassageCard(passage = passage, onClick = { onClick(passage.id) })
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ListeningList(
    exercises: List<ListeningExercise>,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Listening comprehension exercises with audio player, tests and transcripts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(exercises, key = { it.id }) { exercise ->
            ListeningExerciseCard(exercise = exercise, onClick = { onClick(exercise.id) })
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun GrammarTopicCard(
    topic: GrammarTopic,
    onClick: () -> Unit
) {
    LearningCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Default.School, tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = topic.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(7.dp))
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
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReadingPassageCard(
    passage: ReadingPassage,
    onClick: () -> Unit
) {
    LearningCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = passage.titleFa, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CefrBadge(level = passage.level)
            }
            Text(text = passage.titleEn, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = passage.summaryFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ListeningExerciseCard(
    exercise: ListeningExercise,
    onClick: () -> Unit
) {
    LearningCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = exercise.titleFa, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CefrBadge(level = exercise.level)
            }
            Text(text = exercise.titleEn, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "${exercise.exam} · ${exercise.topic}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LearningCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(15.dp)) {
            content()
        }
    }
}

@Composable
private fun IconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}
