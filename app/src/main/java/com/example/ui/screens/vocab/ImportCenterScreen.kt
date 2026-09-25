package com.example.ui.screens.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.importer.DuplicateAction
import com.example.data.importer.ParsedImportItem
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCenterScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit,
    onNavigateToAddManual: () -> Unit,
    onNavigateToAiGenerate: () -> Unit,
    onNavigateToPacks: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedMethod by remember { mutableStateOf("PASTE") }
    var rawInputText by remember { mutableStateOf("") }
    var selectedDuplicateAction by remember { mutableStateOf(DuplicateAction.MERGE) }
    var duplicateDropdownExpanded by remember { mutableStateOf(false) }
    var showFormatHelp by remember { mutableStateOf(false) }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "Vocabulary Import Center",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            if (state.fileImportPreview.isNotEmpty()) {
                ImportPreview(
                    paddingValues = paddingValues,
                    preview = state.fileImportPreview,
                    selectedDuplicateAction = selectedDuplicateAction,
                    duplicateDropdownExpanded = duplicateDropdownExpanded,
                    onDuplicateDropdownExpandedChange = { duplicateDropdownExpanded = it },
                    onDuplicateActionChange = {
                        selectedDuplicateAction = it
                        duplicateDropdownExpanded = false
                    },
                    onSelectAll = { viewModel.selectAllFilePreview(true) },
                    onSelectNone = { viewModel.selectAllFilePreview(false) },
                    onToggleItem = viewModel::toggleFilePreviewItem,
                    onSkip = viewModel::clearFilePreview,
                    onImport = { viewModel.confirmImportFileList(selectedDuplicateAction) }
                )
            } else {
                ImportInput(
                    paddingValues = paddingValues,
                    selectedMethod = selectedMethod,
                    rawInputText = rawInputText,
                    showFormatHelp = showFormatHelp,
                    onToggleFormatHelp = { showFormatHelp = !showFormatHelp },
                    onMethodSelected = { method ->
                        selectedMethod = method
                        rawInputText = when (method) {
                            "PASTE" -> "mitigate = کاهش دادن\nsubstantial = قابل توجه\nallocate = اختصاص دادن\ninevitable = اجتناب‌ناپذیر\nenhance = بهبود بخشیدن"
                            "CSV" -> "word,persianMeaning,level\nmitigate,کاهش دادن,C1\nallocate,اختصاص دادن,B2\nsubstantial,قابل توجه,B2\ninevitable,اجتناب‌ناپذیر,B2\nenhance,بهبود دادن,B2"
                            else -> """[
  {"word": "mitigate", "persianMeaning": "کاهش دادن", "cefrLevel": "C1", "tags": ["IELTS"]},
  {"word": "allocate", "persianMeaning": "اختصاص دادن", "cefrLevel": "B2", "tags": ["Academic"]},
  {"word": "substantial", "persianMeaning": "قابل توجه", "cefrLevel": "B2", "tags": ["Data"]}
]"""
                        }
                    },
                    onInputChanged = { rawInputText = it },
                    onProcess = {
                        if (rawInputText.isNotBlank()) {
                            viewModel.parseImportContent(rawInputText, selectedMethod)
                        }
                    },
                    onNavigateToAddManual = onNavigateToAddManual,
                    onNavigateToAiGenerate = onNavigateToAiGenerate,
                    onNavigateToPacks = onNavigateToPacks
                )
            }
        }
    }
}

@Composable
private fun ImportPreview(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    preview: List<ParsedImportItem>,
    selectedDuplicateAction: DuplicateAction,
    duplicateDropdownExpanded: Boolean,
    onDuplicateDropdownExpandedChange: (Boolean) -> Unit,
    onDuplicateActionChange: (DuplicateAction) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onToggleItem: (Int) -> Unit,
    onSkip: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.space4)
        ) {
            Text(
                text = "Import preview · ${preview.count { it.isSelected }} of ${preview.size}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTapTarget)
            ) {
                Text("Select all", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(
                onClick = onSelectNone,
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTapTarget)
            ) {
                Text("None", style = MaterialTheme.typography.labelMedium)
            }
        }

        DuplicateActionSelector(
            selectedAction = selectedDuplicateAction,
            expanded = duplicateDropdownExpanded,
            onExpandedChange = onDuplicateDropdownExpandedChange,
            onActionChange = onDuplicateActionChange
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            itemsIndexed(preview) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimens.minTapTarget)
                        .background(
                            if (item.isDuplicate) Accent.warningSoft.copy(alpha = 0.45f)
                            else Color.Transparent
                        )
                        .clickable { onToggleItem(index) }
                        .padding(horizontal = Dimens.space4, vertical = Dimens.space2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    Checkbox(
                        checked = item.isSelected,
                        onCheckedChange = { onToggleItem(index) }
                    )
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CefrBadge(level = item.cefrLevel)
                    if (item.isDuplicate) {
                        TagChip(
                            text = "Duplicate",
                            containerColor = Accent.warningSoft,
                            contentColor = Accent.warningOnSoft
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap)
        ) {
            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(Dimens.radiusSm),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = Dimens.minTapTarget)
            ) {
                Text("Skip")
            }
            Button(
                onClick = onImport,
                shape = RoundedCornerShape(Dimens.radiusSm),
                modifier = Modifier
                    .weight(2f)
                    .defaultMinSize(minHeight = Dimens.minTapTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSm)
                )
                Spacer(modifier = Modifier.width(Dimens.space6))
                Text("Import ${preview.count { it.isSelected }} words")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicateActionSelector(
    selectedAction: DuplicateAction,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onActionChange: (DuplicateAction) -> Unit
) {
    val options = listOf(
        DuplicateAction.MERGE to "Merge with existing words (recommended)",
        DuplicateAction.SKIP to "Skip duplicates",
        DuplicateAction.UPDATE to "Update with new data",
        DuplicateAction.KEEP_BOTH to "Keep both copies"
    )
    val selectedLabel = options.first { it.first == selectedAction }.second

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
        FieldLabel("Duplicate handling")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("When a word already exists") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { (action, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onActionChange(action) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportInput(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    selectedMethod: String,
    rawInputText: String,
    showFormatHelp: Boolean,
    onToggleFormatHelp: () -> Unit,
    onMethodSelected: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onProcess: () -> Unit,
    onNavigateToAddManual: () -> Unit,
    onNavigateToAiGenerate: () -> Unit,
    onNavigateToPacks: () -> Unit
) {
    val hintText = when (selectedMethod) {
        "PASTE" -> "Paste words line by line, or use the word = meaning format."
        "CSV" -> "Use word,persianMeaning,level. Columns are detected automatically."
        else -> "Use the exported JSON structure for reliable parsing."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space8),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
    ) {
        SectionHeader(
            title = "Add vocabulary",
            subtitle = "Start with a quick action or import a file."
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
                FieldLabel("Quick add")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    QuickMethodButton(
                        icon = Icons.Default.Add,
                        title = "Manual add",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToAddManual,
                        modifier = Modifier.weight(1f)
                    )
                    QuickMethodButton(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI generate",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToAiGenerate,
                        modifier = Modifier.weight(1f)
                    )
                    QuickMethodButton(
                        icon = Icons.Default.Inventory2,
                        title = "Word packs",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToPacks,
                        modifier = Modifier.weight(1f)
                    )
                }

                FieldLabel("Import source")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    SelectChip(
                        text = "Paste text",
                        selected = selectedMethod == "PASTE",
                        onClick = { onMethodSelected("PASTE") },
                        leadingIcon = Icons.Default.ContentPaste
                    )
                    SelectChip(
                        text = "CSV file",
                        selected = selectedMethod == "CSV",
                        onClick = { onMethodSelected("CSV") },
                        leadingIcon = Icons.Default.Description
                    )
                    SelectChip(
                        text = "JSON file",
                        selected = selectedMethod == "JSON",
                        onClick = { onMethodSelected("JSON") },
                        leadingIcon = Icons.Default.DataObject
                    )
                }

                AppInset(
                    onClick = onToggleFormatHelp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        IconTile(
                            icon = Icons.Default.HelpOutline,
                            tint = MaterialTheme.colorScheme.primary,
                            size = Dimens.iconTileSm,
                            iconSize = Dimens.iconSm
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Format help",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (showFormatHelp) hintText else "Show accepted formats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (showFormatHelp) 3 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = onInputChanged,
                    placeholder = { Text("Paste vocabulary text, CSV, or JSON here") },
                    minLines = 7,
                    maxLines = 14,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onProcess,
                    enabled = rawInputText.isNotBlank(),
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimens.minTapTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space6))
                    Text("Process and preview")
                }
            }
        }
    }
}

@Composable
private fun QuickMethodButton(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppInset(
        modifier = modifier,
        padding = Dimens.space8,
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        onClick = onClick
    ) {
        Column(
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
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
