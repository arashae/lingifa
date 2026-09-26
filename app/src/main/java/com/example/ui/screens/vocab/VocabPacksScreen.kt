package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.VocabularyPack
import com.example.ui.components.AppCard
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun VocabPacksScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit,
    onFilterByPack: (String) -> Unit,
    onStartCefrLevel: () -> Unit,
    onNavigateToExamTracks: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val syncStates by viewModel.packSyncStates.collectAsState()
    val wordCounts = state.packs
        .groupBy { it.level.trim().uppercase() }
        .mapValues { (_, packs) -> packs.sumOf { it.wordCount } }
    val examTrackSubtitle = listOf(
        "IELTS" to "pack_ielts_master",
        "TOEFL" to "pack_toefl_master",
        "GRE" to "pack_gre_master"
    ).mapNotNull { (label, packId) ->
        state.packs.firstOrNull { it.id == packId }?.let { pack ->
            val count = pack.targetWordCount.takeIf { it > 0 } ?: pack.wordCount
            "$label $count"
        }
    }.joinToString(" · ").ifBlank { "IELTS · TOEFL · GRE" }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LinguaTopAppBar(
                    title = "Vocabulary Packs",
                    subtitle = "Download and study curated vocabulary",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
            ) {
                item {
                    CefrLearningPathCard(
                        selectedLevel = state.learningLevel,
                        wordCounts = wordCounts,
                        onSelectLevel = viewModel::selectLearningLevel,
                        onStart = onStartCefrLevel
                    )
                }

                item {
                    ExamTracksEntryCard(
                        subtitle = examTrackSubtitle,
                        onClick = onNavigateToExamTracks
                    )
                }

                item {
                    SectionHeader(
                        title = "All Vocabulary Packs",
                        subtitle = "${state.packs.size} available",
                        modifier = Modifier.padding(top = Dimens.space4)
                    )
                }

                items(state.packs, key = { it.id }) { pack ->
                    VocabPackCard(
                        pack = pack,
                        syncState = syncStates[pack.id],
                        onSync = { viewModel.syncMasterPack(pack.id) },
                        onViewWords = {
                            viewModel.onPackFilterChanged(pack.id)
                            onFilterByPack(pack.id)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(Dimens.space24)) }
            }
        }
    }
}

@Composable
private fun CefrLearningPathCard(
    selectedLevel: String,
    wordCounts: Map<String, Int>,
    onSelectLevel: (String) -> Unit,
    onStart: () -> Unit
) {
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                Text(
                    text = "General Vocabulary Track",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Select your CEFR level, then start when ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            levels.chunked(2).forEach { rowLevels ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    rowLevels.forEach { level ->
                        SelectChip(
                            text = "$level · ${wordCounts[level] ?: 0} words",
                            selected = selectedLevel == level,
                            onClick = { onSelectLevel(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowLevels.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.minTapTarget)
            ) {
                Text(
                    text = "Start at $selectedLevel",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ExamTracksEntryCard(
    subtitle: String,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.cardPaddingTight,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.space10)
        ) {
            IconTile(
                icon = Icons.Default.School,
                tint = MaterialTheme.colorScheme.primary,
                size = Dimens.iconTileMd,
                iconSize = Dimens.iconMd
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.space2)
            ) {
                Text(
                    text = "Exam Mastery Tracks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconMd)
            )
        }
    }
}

@Composable
private fun VocabPackCard(
    pack: VocabularyPack,
    syncState: PackSyncUiState?,
    onSync: () -> Unit,
    onViewWords: () -> Unit
) {
    val target = maxOf(pack.targetWordCount, syncState?.target ?: 0).coerceAtLeast(0)
    val installed = maxOf(pack.installedWordCount, syncState?.installed ?: 0).coerceAtLeast(0)
    val progress = if (target > 0) (installed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val isSyncing = syncState?.isRunning == true
    val isComplete = pack.isCorePack && target > 0 && installed >= target
    val statusMessage = syncState?.message?.takeIf { it.isNotBlank() }
    val isC2Advanced = pack.id == "pack_cefr_c2"
    val englishTitle = if (isC2Advanced) "C2 + Advanced Vocabulary" else pack.titleEn
    val displayTitle = englishTitle.ifBlank { pack.titleFa }
    val descriptionEn = if (isC2Advanced) {
        "Extensive advanced and general vocabulary, with specialized and rare words prioritized for later study."
    } else {
        pack.descriptionEn
    }
    val displayDescription = descriptionEn.ifBlank { pack.descriptionFa }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.cardPaddingTight
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Default.MenuBook,
                    tint = MaterialTheme.colorScheme.primary,
                    size = Dimens.iconTileMd,
                    iconSize = Dimens.iconMd
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    if (englishTitle.isNotBlank()) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        PersianContentRtl {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TagChip(text = "${pack.wordCount} words")
                }
                CefrBadge(level = pack.level)
            }

            if (displayDescription.isNotBlank()) {
                if (descriptionEn.isNotBlank()) {
                    Text(
                        text = displayDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    PersianContentRtl {
                        Text(
                            text = displayDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (pack.isCorePack && target > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isComplete) "Ready for offline study" else "Download progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isComplete) Accent.success else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$installed / $target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.progressHeight)
                            .clip(CircleShape),
                        color = if (isComplete) Accent.success else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    statusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (syncState?.stage == "error") {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if ((syncState?.warningCount ?: 0) > 0) {
                        Text(
                            text = "Some resources were unavailable; the download can continue.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent.warning,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                if (pack.isCorePack && !isComplete) {
                    Button(
                        onClick = onSync,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.minTapTarget)
                    ) {
                        Text(
                            text = when {
                                isSyncing -> "Downloading…"
                                installed > 0 -> "Resume"
                                else -> "Download"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onViewWords,
                    enabled = !pack.isCorePack || installed > 0,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    border = BorderStroke(Dimens.hairline, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.minTapTarget)
                ) {
                    Text(
                        text = if (pack.isCorePack) "Study" else "View Words",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
