package com.example.ui.screens.vocab

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun VocabLibraryScreen(
    viewModel: VocabViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddWord: () -> Unit,
    onNavigateToAiGenerate: () -> Unit,
    onNavigateToImportCenter: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearStatusMessage()
        }
    }

    PersianRtlLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddWord,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن لغت")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Search Box
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("جستجوی لغت انگلیسی یا معنی فارسی...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "جستجو")
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Action Bar with Prominent Entry Points
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip(
                        icon = Icons.Default.AutoAwesome,
                        label = "کارت واژه هوشمند AI (صوتی)",
                        color = Color(0xFF8B5CF6),
                        onClick = onNavigateToAiVocabCard
                    )
                    QuickActionChip(
                        icon = Icons.Default.FileUpload,
                        label = "ورود لغات (Import)",
                        color = PrimaryBlue,
                        onClick = onNavigateToImportCenter
                    )
                    QuickActionChip(
                        icon = Icons.Default.AutoAwesome,
                        label = "ساخت بسته با هوش مصنوعی",
                        color = Color(0xFF6366F1),
                        onClick = onNavigateToAiGenerate
                    )
                    QuickActionChip(
                        icon = Icons.Default.Inventory2,
                        label = "بسته‌های لغت",
                        color = Color(0xFF0D9488),
                        onClick = onNavigateToPacks
                    )
                }

                // Level Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val levels = listOf("همه", "B1", "B2", "C1", "C2")
                    levels.forEach { lvl ->
                        FilterChip(
                            selected = state.selectedLevel == lvl,
                            onClick = { viewModel.onLevelFilterChanged(lvl) },
                            label = { Text(lvl, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    // Status Filters
                    val statuses = listOf("مرور امروز", "یاد گرفته شده", "در حال یادگیری", "نشان‌شده‌ها")
                    statuses.forEach { st ->
                        FilterChip(
                            selected = state.selectedStatus == st,
                            onClick = {
                                viewModel.onStatusFilterChanged(if (state.selectedStatus == st) "همه" else st)
                            },
                            label = { Text(st, fontSize = 12.sp) }
                        )
                    }
                }

                // List Header with Total Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نمایش ${state.words.size} از ${state.totalCount} لغت",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Word List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
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

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.ipa.isNotEmpty()) {
                        Text(
                            text = item.ipa,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    AudioSpeakerButton(onClick = onPlayAudio, size = 30)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CefrBadge(level = item.cefrLevel)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "نشان‌کردن",
                            tint = if (item.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Persian meaning
            Text(
                text = item.persianMeaning,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Example snippet if present
            if (item.example.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.example,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mastery bar & Spaced repetition interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { (item.mastery / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (item.mastery >= 70) SuccessGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسلط: ${item.mastery}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (item.intervalDays > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "دوره مرور: ${item.intervalDays} روز",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
