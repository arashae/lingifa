package com.example.ui.screens.learn

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrammarTopic
import com.example.data.model.ListeningExercise
import com.example.data.model.ReadingPassage
import com.example.ui.components.CefrBadge
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LearnHomeScreen(
    viewModel: LearnViewModel,
    onNavigateToGrammarDetail: (String) -> Unit,
    onNavigateToReadingDetail: (String) -> Unit,
    onNavigateToListeningDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val tabs = listOf("گرامر", "Reading", "Listening")
    val selectedIndex = tabs.indexOf(state.selectedCategory).coerceAtLeast(0)

    PersianRtlLayout {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Category Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { viewModel.selectCategory(tabs[index]) },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content List
                when (state.selectedCategory) {
                    "گرامر" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "سرفصل‌های گرامر کاربردی با توضیح فارسی و اشتباهات رایج ایرانی‌ها:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            items(state.grammarTopics) { topic ->
                                GrammarTopicCard(
                                    topic = topic,
                                    onClick = { onNavigateToGrammarDetail(topic.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                    "Reading" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "متون آکادمیک آیلتس و تافل با امکان ضربه روی هر کلمه برای مشاهده معنی و افزودن به لغات من:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            items(state.readingPassages) { passage ->
                                ReadingPassageCard(
                                    passage = passage,
                                    onClick = { onNavigateToReadingDetail(passage.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                    "Listening" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "تمرین‌های شنیداری با پخش صوت هوشمند، سوالات تستی و رونوشت کلمه‌به‌کلمه:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            items(state.listeningExercises) { exercise ->
                                ListeningExerciseCard(
                                    exercise = exercise,
                                    onClick = { onNavigateToListeningDetail(exercise.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarTopicCard(
    topic: GrammarTopic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.titleFa,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CefrBadge(level = topic.level)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = topic.titleEn,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ReadingPassageCard(
    passage: ReadingPassage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = passage.titleFa,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                CefrBadge(level = passage.level)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = passage.titleEn,
                style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = passage.summaryFa,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ListeningExerciseCard(
    exercise: ListeningExercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Hearing, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exercise.titleFa,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                CefrBadge(level = exercise.level)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.titleEn,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8B5CF6), fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "قالب: ${exercise.exam} | موضوع: ${exercise.topic}",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
