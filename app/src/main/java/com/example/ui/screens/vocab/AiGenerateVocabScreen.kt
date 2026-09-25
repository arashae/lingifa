package com.example.ui.screens.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.importer.ParsedImportItem
import com.example.ui.components.AppCard
import com.example.ui.components.CefrBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SelectChip
import com.example.ui.theme.Dimens

@Composable
fun AiGenerateVocabScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var promptInput by remember { mutableStateOf("50 high-yield IELTS words for Environment and Economy") }

    val samplePrompts = listOf(
        "50 high-yield IELTS words for Environment",
        "Essential vocabulary for IELTS Writing Task 2",
        "TOEFL 2026 Academic Discussion key words",
        "Advanced C1 Collocations for high band scores"
    )

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "AI Vocabulary Generator",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
                        FieldLabel("Prompt")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                        ) {
                            samplePrompts.forEach { prompt ->
                                SelectChip(
                                    text = prompt,
                                    selected = promptInput == prompt,
                                    onClick = { promptInput = prompt }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("e.g., 30 academic words for IELTS Speaking Part 3") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(Dimens.radiusSm),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (promptInput.isNotBlank()) {
                                    viewModel.generateVocabWithAi(promptInput)
                                }
                            },
                            enabled = promptInput.isNotBlank() && !state.isAiGenerating,
                            shape = RoundedCornerShape(Dimens.radiusSm),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = Dimens.minTapTarget)
                        ) {
                            if (state.isAiGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.iconMd),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = Dimens.hairline
                                )
                                Spacer(modifier = Modifier.width(Dimens.space8))
                                Text("Generating vocabulary")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.iconSm)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space6))
                                Text("Generate smart vocabulary pack")
                            }
                        }
                    }
                }

                if (state.aiGeneratedPreview.isNotEmpty()) {
                    AiPreview(
                        items = state.aiGeneratedPreview,
                        onSelectAll = { viewModel.selectAllAiPreview(true) },
                        onSelectNone = { viewModel.selectAllAiPreview(false) },
                        onToggleItem = viewModel::toggleAiPreviewItem,
                        onAddSelected = {
                            viewModel.confirmImportAiList()
                            onBack()
                        }
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.AutoAwesome,
                        title = "Preview your vocabulary",
                        message = "Enter a prompt and generate a pack to review the words here.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPreview(
    items: List<ParsedImportItem>,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onToggleItem: (Int) -> Unit,
    onAddSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4)
        ) {
            Text(
                text = "Preview · ${items.count { it.isSelected }} of ${items.size} selected",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SelectChip(
                text = "All",
                selected = items.all { it.isSelected },
                onClick = onSelectAll
            )
            SelectChip(
                text = "None",
                selected = items.none { it.isSelected },
                onClick = onSelectNone
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            itemsIndexed(items) { index, item ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = Dimens.cardPaddingTight,
                    onClick = { onToggleItem(index) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = { onToggleItem(index) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                            ) {
                                Text(
                                    text = item.word,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CefrBadge(level = item.cefrLevel)
                            }
                            PersianContentRtl {
                                Text(
                                    text = item.persianMeaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (item.example.isNotEmpty()) {
                                Text(
                                    text = item.example,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onAddSelected,
            enabled = items.any { it.isSelected },
            shape = RoundedCornerShape(Dimens.radiusSm),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.minTapTarget)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSm)
            )
            Spacer(modifier = Modifier.width(Dimens.space6))
            Text("Add ${items.count { it.isSelected }} words to my vocabulary")
        }
    }
}
