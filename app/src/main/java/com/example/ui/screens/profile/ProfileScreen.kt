package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToMistakes: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(state.profile.userName) }
    var editGoal by remember { mutableStateOf(state.profile.targetGoal) }
    var editTargetScore by remember { mutableStateOf(state.profile.targetBandOrScore) }

    val retentionRate = if (state.totalWordsCount > 0) {
        ((state.learnedWordsCount.toFloat() / state.totalWordsCount) * 100).toInt()
    } else {
        0
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearStatusMessage()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LinguaTopAppBar(
                    title = "Profile & Academic Progress",
                    subtitle = "${state.profile.currentLevel} level · ${state.totalWordsCount} saved words",
                    actions = {
                        IconButton(onClick = {
                            editName = state.profile.userName
                            editGoal = state.profile.targetGoal
                            editTargetScore = state.profile.targetBandOrScore
                            showEditProfileDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit learner profile"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = Dimens.screenGutter,
                        vertical = Dimens.space12
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(
                            icon = Icons.Default.Person,
                            tint = MaterialTheme.colorScheme.primary,
                            size = Dimens.cardPaddingLoose * 4,
                            iconSize = Dimens.iconLg
                        )
                        Spacer(modifier = Modifier.width(Dimens.space12))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                        ) {
                            Text(
                                text = state.profile.userName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Goal: ${state.profile.targetGoal} ${state.profile.targetBandOrScore}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        CefrBadge(level = state.profile.currentLevel)
                    }
                }

                AppCard(padding = Dimens.cardPaddingTight) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStat(
                            icon = Icons.Default.Bolt,
                            value = "${state.profile.xp} XP",
                            label = "Points",
                            color = Accent.warning,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileDivider()
                        ProfileStat(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${state.profile.streakDays} days",
                            label = "Streak",
                            color = Accent.streak,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileDivider()
                        ProfileStat(
                            icon = Icons.Default.AutoStories,
                            value = state.totalWordsCount.toString(),
                            label = "Words",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AppCard {
                    SectionHeader(
                        title = "Vocabulary Mastery",
                        subtitle = "Long-term retention and learning progress"
                    )
                    Spacer(modifier = Modifier.height(Dimens.blockGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Retention rate",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$retentionRate%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Accent.success,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (retentionRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.progressHeight)
                            .clip(RoundedCornerShape(Dimens.radiusPill)),
                        color = Accent.success,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mastered: ${state.learnedWordsCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "In learning: ${state.totalWordsCount - state.learnedWordsCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AiConfigurationCard(
                    currentKey = state.deepSeekApiKey,
                    currentModel = state.deepSeekModel,
                    isTesting = state.isTestingAi,
                    connectionStatus = state.aiConnectionStatus,
                    onSaveKey = viewModel::setDeepSeekApiKey,
                    onClearKey = viewModel::clearDeepSeekApiKey,
                    onTestConnection = viewModel::testDeepSeekConnection,
                    onSelectModel = viewModel::setDeepSeekModel
                )

                AppCard(onClick = onNavigateToMistakes) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(
                            icon = Icons.Default.Warning,
                            tint = Accent.danger
                        )
                        Spacer(modifier = Modifier.width(Dimens.space12))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                        ) {
                            Text(
                                text = "Mistake Notebook",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.mistakes.size} recorded errors to review",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SectionHeader(
                    title = "Backup & Export",
                    subtitle = "Download your vocabulary for backups or AI training workflows"
                )

                AppCard {
                    FieldLabel(text = "Export format")
                    Spacer(modifier = Modifier.height(Dimens.blockGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportVocabulary("CSV") },
                            modifier = Modifier
                                .weight(1f)
                                .height(Dimens.minTapTarget),
                            shape = RoundedCornerShape(Dimens.radiusSm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconSm)
                            )
                            Spacer(modifier = Modifier.width(Dimens.space6))
                            Text(
                                text = "CSV",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportVocabulary("JSON") },
                            modifier = Modifier
                                .weight(1f)
                                .height(Dimens.minTapTarget),
                            shape = RoundedCornerShape(Dimens.radiusSm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataObject,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconSm)
                            )
                            Spacer(modifier = Modifier.width(Dimens.space6))
                            Text(
                                text = "JSON",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }

                    state.exportedContent?.let { content ->
                        Spacer(modifier = Modifier.height(Dimens.blockGap))
                        AppInset {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.exportFormat} preview",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(
                                            Context.CLIPBOARD_SERVICE
                                        ) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(
                                                "LinguaFa Export",
                                                content
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy export content"
                                    )
                                }
                            }
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (showEditProfileDialog) {
                AlertDialog(
                    onDismissRequest = { showEditProfileDialog = false },
                    title = { Text("Edit Learner Profile") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editGoal,
                                onValueChange = { editGoal = it },
                                label = { Text("Target Goal") },
                                placeholder = { Text("IELTS, TOEFL, Academic") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editTargetScore,
                                onValueChange = { editTargetScore = it },
                                label = { Text("Target Score") },
                                placeholder = { Text("7.5 or 105") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    name = editName,
                                    goal = editGoal,
                                    level = state.profile.currentLevel,
                                    dailyMinutes = state.profile.dailyMinutes,
                                    targetScore = editTargetScore
                                )
                                showEditProfileDialog = false
                            },
                            modifier = Modifier.height(Dimens.minTapTarget)
                        ) {
                            Text("Save Changes")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showEditProfileDialog = false },
                            modifier = Modifier.height(Dimens.minTapTarget)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.space4)
    ) {
        IconTile(
            icon = icon,
            tint = color,
            size = Dimens.iconTileSm,
            iconSize = Dimens.iconSm
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .width(Dimens.hairline)
            .height(Dimens.iconTileMd)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun AiConfigurationCard(
    currentKey: String,
    currentModel: String,
    isTesting: Boolean,
    connectionStatus: String?,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onTestConnection: (String) -> Unit,
    onSelectModel: (String) -> Unit
) {
    var inputKey by remember(currentKey) { mutableStateOf(currentKey) }
    var keyVisible by remember { mutableStateOf(false) }

    AppCard {
        SectionHeader(
            title = "DeepSeek AI Configuration",
            subtitle = "Powers the tutor, writing grader, speaking coach, and flashcard generator"
        )
        Spacer(modifier = Modifier.height(Dimens.blockGap))
        FieldLabel(text = "API key")
        OutlinedTextField(
            value = inputKey,
            onValueChange = { inputKey = it },
            label = { Text("DeepSeek API Key") },
            placeholder = { Text("sk-...") },
            singleLine = true,
            visualTransformation = if (keyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (keyVisible) "Hide API key" else "Show API key"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.radiusSm)
        )

        Spacer(modifier = Modifier.height(Dimens.blockGap))
        FieldLabel(text = "Model")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
        ) {
            SelectChip(
                text = "Chat",
                selected = currentModel == "deepseek-chat",
                onClick = { onSelectModel("deepseek-chat") },
                modifier = Modifier.weight(1f)
            )
            SelectChip(
                text = "Reasoner",
                selected = currentModel == "deepseek-reasoner",
                onClick = { onSelectModel("deepseek-reasoner") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.blockGap))
        if (!connectionStatus.isNullOrBlank()) {
            val isSuccess = connectionStatus.contains("Connected", ignoreCase = true)
            AppInset(
                color = if (isSuccess) Accent.successSoft else Accent.dangerSoft,
                contentColor = if (isSuccess) Accent.successOnSoft else Accent.dangerOnSoft
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = if (isSuccess) Accent.success else Accent.danger,
                        modifier = Modifier.size(Dimens.iconMd)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space8))
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.blockGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
        ) {
            Button(
                onClick = { onSaveKey(inputKey) },
                enabled = inputKey.isNotBlank() && inputKey != currentKey,
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.minTapTarget),
                shape = RoundedCornerShape(Dimens.radiusSm)
            ) {
                Text("Save Key", maxLines = 1)
            }
            OutlinedButton(
                onClick = { onTestConnection(inputKey) },
                enabled = !isTesting && inputKey.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.minTapTarget),
                shape = RoundedCornerShape(Dimens.radiusSm)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.iconSm),
                        strokeWidth = Dimens.hairline,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Dimens.space6))
                }
                Text(
                    text = if (isTesting) "Testing" else "Test Connection",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
