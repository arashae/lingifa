package com.example.ui.screens.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.ui.components.AppCard
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens
import com.example.vocab.VocabularyDailyPlan
import com.example.vocab.VocabularyMasteryStats
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import com.example.vocab.VocabularyTier

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
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<VocabularyItem?>(null) }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

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
                    text = {
                        Text(
                            text = "Add Word",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                LibraryHeaderBlock(
                    plan = state.dailyPlan,
                    stats = state.masteryStats,
                    totalCount = state.totalCount,
                    searchQuery = state.searchQuery,
                    selectedLevel = state.selectedLevel,
                    selectedStatus = state.selectedStatus,
                    selectedTier = state.selectedTier,
                    showTierFilters = VocabularyStudyPolicy.supportsTiers(state.selectedPackId),
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onLevelSelected = viewModel::onLevelFilterChanged,
                    onStatusSelected = viewModel::onStatusFilterChanged,
                    onTierSelected = viewModel::onTierFilterChanged,
                    onLimitChange = viewModel::setDailyNewWordLimit,
                    onStart = onNavigateToReview,
                    onNavigateToExamTracks = onNavigateToExamTracks,
                    onNavigateToPacks = onNavigateToPacks,
                    onNavigateToImportCenter = onNavigateToImportCenter,
                    onNavigateToAiVocabCard = onNavigateToAiVocabCard
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                ) {
                    if (state.words.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "No words found",
                                message = "Adjust your filters or add a new vocabulary word.",
                                modifier = Modifier.fillMaxWidth(),
                                actionText = "Add Word",
                                onActionClick = onNavigateToAddWord
                            )
                        }
                    } else {
                        items(state.words, key = { it.id }) { item ->
                            WordLibraryCard(
                                item = item,
                                onPlayAudio = { tts.speak(item.word) },
                                onToggleFavorite = { viewModel.toggleFavorite(item) },
                                onDelete = { pendingDelete = item },
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(Dimens.sectionGap * 6)) }
                }
            }
        }

        pendingDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete word?", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    Text(
                        text = "\"${item.word}\" will be removed from your vocabulary library.",
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteWord(item)
                            pendingDelete = null
                        },
                        modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                    ) {
                        Text("Delete", color = Accent.danger)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingDelete = null },
                        modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun LibraryHeaderBlock(
    plan: VocabularyDailyPlan,
    stats: VocabularyMasteryStats,
    totalCount: Int,
    searchQuery: String,
    selectedLevel: String,
    selectedStatus: String,
    selectedTier: VocabularyTier,
    showTierFilters: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onLevelSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit,
    onTierSelected: (VocabularyTier) -> Unit,
    onLimitChange: (Int) -> Unit,
    onStart: () -> Unit,
    onNavigateToExamTracks: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onNavigateToImportCenter: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit
) {
    AppCard(
        modifier = Modifier.padding(horizontal = Dimens.screenGutter),
        padding = Dimens.space10,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                    ) {
                        Text(
                            text = "Vocabulary Bank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TagChip(text = "$totalCount words")
                    }
                    Text(
                        text = "Due reviews first, then weak and new words.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onStart,
                    enabled = plan.total > 0,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                ) {
                    Text(
                        text = if (plan.total > 0) "Review ${plan.total}" else "Plan complete",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                TagChip(
                    text = "Reviews ${plan.dueReviews}",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TagChip(
                    text = "Weak ${plan.weakWords}",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                TagChip(
                    text = "New ${plan.newWords}",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Search English words or meanings",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.minTapTarget)
            )

            FieldLabel("Actions, daily limit and filters")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                VocabularyStudyPolicy.DAILY_NEW_LIMIT_OPTIONS.forEach { limit ->
                    item {
                        SelectChip(
                            text = "$limit new/day",
                            selected = plan.newWordLimit == limit,
                            onClick = { onLimitChange(limit) },
                            leadingIcon = Icons.Default.Timer
                        )
                    }
                }
                item {
                    SelectChip(
                        text = "Exam Tracks",
                        selected = false,
                        onClick = onNavigateToExamTracks,
                        leadingIcon = Icons.Default.School
                    )
                }
                item {
                    SelectChip(
                        text = "Packs",
                        selected = false,
                        onClick = onNavigateToPacks,
                        leadingIcon = Icons.Default.Inventory2
                    )
                }
                item {
                    SelectChip(
                        text = "AI Card",
                        selected = false,
                        onClick = onNavigateToAiVocabCard,
                        leadingIcon = Icons.Default.AutoAwesome
                    )
                }
                item {
                    SelectChip(
                        text = "Import",
                        selected = false,
                        onClick = onNavigateToImportCenter,
                        leadingIcon = Icons.Default.FileUpload
                    )
                }
                listOf("All", "B1", "B2", "C1", "C2").forEach { level ->
                    item {
                        SelectChip(
                            text = level,
                            selected = selectedLevel == level,
                            onClick = { onLevelSelected(level) }
                        )
                    }
                }
                listOf("Due Today", "Mastered", "Learning", "Favorites").forEach { status ->
                    item {
                        SelectChip(
                            text = status,
                            selected = selectedStatus == status,
                            onClick = {
                                onStatusSelected(if (selectedStatus == status) "All" else status)
                            }
                        )
                    }
                }
                if (showTierFilters) {
                    listOf(
                        VocabularyTier.ALL to "All tiers",
                        VocabularyTier.CORE to "Core · Essential",
                        VocabularyTier.EXTENDED to "Extended · Additional"
                    ).forEach { (tier, label) ->
                        item {
                            SelectChip(
                                text = label,
                                selected = selectedTier == tier,
                                onClick = { onTierSelected(tier) }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Mastery: ${stats.unseen} new · ${stats.learning} learning · ${stats.review} review · ${stats.mastered} mastered",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    val spellingMastery = VocabularyStudyPolicy.skillMastery(item, VocabularySkillAxis.SPELLING)
    val contextMastery = VocabularyStudyPolicy.skillMastery(item, VocabularySkillAxis.CONTEXT)

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.cardPaddingTight,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.ipa.isNotBlank()) {
                            Text(
                                text = item.ipa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    PersianContentRtl {
                        Text(
                            text = item.persianMeaning,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    CefrBadge(level = item.cefrLevel)
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(Dimens.minTapTarget)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (item.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (item.isFavorite) Accent.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconMd)
                        )
                    }
                }
            }

            if (item.example.isNotBlank()) {
                Text(
                    text = item.example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Meaning ${item.mastery}% · Context $contextMastery% · Spelling $spellingMastery%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                AudioSpeakerButton(onClick = onPlayAudio)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space4)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.mastery}% mastery",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.mastery >= 70) Accent.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.intervalDays > 0) {
                            Text(
                                text = "Next: ${item.intervalDays}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (item.mastery / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.progressHeight)
                            .clip(CircleShape),
                        color = if (item.mastery >= 70) Accent.success else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(Dimens.minTapTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${item.word}",
                        tint = Accent.danger,
                        modifier = Modifier.size(Dimens.iconMd)
                    )
                }
            }
        }
    }
}
