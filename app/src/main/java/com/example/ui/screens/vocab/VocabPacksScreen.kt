package com.example.ui.screens.vocab

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VocabularyPack
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen

@Composable
fun VocabPacksScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit,
    onFilterByPack: (String) -> Unit,
    onNavigateToExamTracks: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val syncStates by viewModel.packSyncStates.collectAsState()

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "بسته‌های واژگان هدفمند",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToExamTracks() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مسیرهای مرحله‌به‌مرحله آزمون‌ها 🎯",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "آموزش گام‌به‌گام و روزانه لغات آیلتس، تافل و GRE با محاسبه درصد تسلط",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onNavigateToExamTracks,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("ورود به مسیرها", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "بانک‌ها و بسته‌های واژگان:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "بانک‌های مادر را یک‌بار دانلود کنید؛ بعد از آن لغات و مرورها کاملاً آفلاین در Room می‌مانند.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
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

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
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
    val icon: ImageVector = when (pack.iconName) {
        "school" -> Icons.Default.School
        "menu_book" -> Icons.Default.MenuBook
        "psychology" -> Icons.Default.Psychology
        "edit_note" -> Icons.Default.EditNote
        "record_voice_over" -> Icons.Default.RecordVoiceOver
        else -> Icons.Default.Translate
    }

    val iconBgColor = when (pack.iconName) {
        "school" -> PrimaryBlue
        "menu_book" -> SuccessGreen
        "psychology" -> Color(0xFF8B5CF6)
        "edit_note" -> Color(0xFFF59E0B)
        "record_voice_over" -> Color(0xFFEC4899)
        else -> SecondaryTeal
    }

    val target = maxOf(pack.targetWordCount, syncState?.target ?: 0).coerceAtLeast(0)
    val installed = maxOf(pack.installedWordCount, syncState?.installed ?: 0).coerceAtLeast(0)
    val progress = if (target > 0) (installed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val isSyncing = syncState?.isRunning == true
    val isComplete = pack.isCorePack && target > 0 && installed >= target

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBgColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconBgColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pack.titleFa,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = pack.titleEn,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                CefrBadge(level = pack.level)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = pack.descriptionFa,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (pack.isCorePack && target > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isComplete) "بانک کامل و آفلاین" else "پوشش فعلی بانک",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) SuccessGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "$installed / $target واژه",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isComplete) SuccessGreen else iconBgColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState?.message?.takeIf { it.isNotBlank() }
                        ?: "${(progress * 100).toInt()}٪ از هدف بانک نصب شده است",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (syncState?.stage == "error") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
                if ((syncState?.warningCount ?: 0) > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "بخشی از منابع در دسترس نبود؛ دانلود قابل ادامه است.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (pack.isCorePack) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isComplete) {
                        Button(
                            onClick = onSync,
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = iconBgColor)
                        ) {
                            Text(
                                text = when {
                                    isSyncing -> "در حال دانلود…"
                                    installed > 0 -> "ادامه دانلود"
                                    else -> "دانلود کامل بانک"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onViewWords,
                        enabled = installed > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("مطالعه $installed واژه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${pack.wordCount} واژه آموزشی",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = onViewWords,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("مشاهده و مطالعه لغات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
