package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SuccessGreen

@Composable
fun VocabLibraryScreen(
    viewModel: VocabViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddWord: () -> Unit,
    onNavigateToAiGenerate: () -> Unit,
    onNavigateToImportCenter: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit = {},
    onNavigateToExamTracks: () -> Unit = {},
    onNavigateToReview: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearStatusMessage()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddWord,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Word", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LibraryHeader(totalCount = state.totalCount)

                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("Search English words or Persian meanings...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    QuickActions(
                        onNavigateToExamTracks = onNavigateToExamTracks,
                        onNavigateToPacks = onNavigateToPacks,
                        onNavigateToImportCenter = onNavigateToImportCenter,
                        onNavigateToAiVocabCard = onNavigateToAiVocabCard,
                        onNavigateToReview = onNavigateToReview
                    )

                    FilterStrip(
                        selectedLevel = state.selectedLevel,
                        selectedStatus = state.selectedStatus,
                        onLevelSelected = viewModel::onLevelFilterChanged,
                        onStatusSelected = { status ->
                            viewModel.onStatusFilterChanged(if (state.selectedStatus == status) "All" else status)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vocabulary Bank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.words.size} of ${state.totalCount} words",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.words, key = { it.id }) { item ->
                        WordLibraryCard(
                            item = item,
                            onPlayAudio = { tts.speak(item.word) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onDelete = { viewModel.deleteWord(item) },
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(92.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(totalCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Vocabulary Bank",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Browse, search and manage your master lexicon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "$totalCount Words",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun QuickActions(
    onNavigateToExamTracks: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onNavigateToImportCenter: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionChip(
            icon = Icons.Default.School,
            label = "Exam Tracks",
            badge = "IELTS • TOEFL • GRE",
            onClick = onNavigateToExamTracks,
            highlight = true
        )
        QuickActionChip(
            icon = Icons.Default.Timer,
            label = "Smart Review",
            badge = "SRS",
            onClick = onNavigateToReview,
            highlight = false
        )
        QuickActionChip(
            icon = Icons.Default.Inventory2,
            label = "Curriculum Packs",
            badge = null,
            onClick = onNavigateToPacks,
            highlight = false
        )
        QuickActionChip(
            icon = Icons.Default.AutoAwesome,
            label = "AI Flashcard",
            badge = null,
            onClick = onNavigateToAiVocabCard,
            highlight = false
        )
        QuickActionChip(
            icon = Icons.Default.FileUpload,
            label = "Import",
            badge = null,
            onClick = onNavigateToImportCenter,
            highlight = false
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    badge: String?,
    onClick: () -> Unit,
    highlight: Boolean
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterStrip(
    selectedLevel: String,
    selectedStatus: String,
    onLevelSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        listOf("All", "B1", "B2", "C1", "C2").forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onLevelSelected(level) },
                label = { Text(level) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        listOf("Due Today", "Mastered", "Learning", "Favorites").forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(status) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

@Composable
fun WordLibraryCard(
    item: VocabularyItem,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.ipa.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.ipa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CefrBadge(level = item.cefrLevel)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (item.example.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioSpeakerButton(onClick = onPlayAudio, size = 34)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.mastery}% Mastery",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (item.mastery >= 70) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.intervalDays > 0) {
                            Text(
                                text = "Next: ${item.intervalDays}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (item.mastery / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (item.mastery >= 70) SuccessGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete word",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
