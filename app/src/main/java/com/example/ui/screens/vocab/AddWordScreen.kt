package com.example.ui.screens.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.components.AppCard
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.HairLine
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var wordInput by remember { mutableStateOf("") }
    var meaningInput by remember { mutableStateOf("") }
    var definitionInput by remember { mutableStateOf("") }
    var exampleInput by remember { mutableStateOf("") }
    var exampleFaInput by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("B2") }
    var levelDropdownExpanded by remember { mutableStateOf(false) }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LinguaTopAppBar(
                    title = "Add Vocabulary Word",
                    subtitle = "Create an entry manually",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space10),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Word Details",
                        subtitle = "Only the English word is required"
                    )

                    FieldLabel("Core information")
                    AppTextField(
                        value = wordInput,
                        onValueChange = { wordInput = it },
                        label = "English Word *",
                        placeholder = "e.g., mitigate",
                        singleLine = true
                    )
                    AppTextField(
                        value = meaningInput,
                        onValueChange = { meaningInput = it },
                        label = "Persian Meaning",
                        placeholder = "Enter the Persian meaning",
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            if (wordInput.isNotBlank()) {
                                viewModel.enrichWordWithAi(wordInput, meaningInput) { enriched ->
                                    if (enriched != null) {
                                        meaningInput = enriched.persianMeaning
                                        definitionInput = enriched.englishDefinition
                                        exampleInput = enriched.example
                                        exampleFaInput = enriched.examplePersian
                                        selectedLevel = enriched.cefrLevel
                                    }
                                }
                            }
                        },
                        enabled = wordInput.isNotBlank() && !state.isAiGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.minTapTarget)
                    ) {
                        if (state.isAiGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.iconMd),
                                strokeWidth = Dimens.hairline * 2,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text(
                                text = "Enriching with AI…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconMd)
                            )
                            Spacer(modifier = Modifier.width(Dimens.space6))
                            Text(
                                text = "Auto-Enrich with AI",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HairLine()
                    FieldLabel("Educational details · optional")
                    AppTextField(
                        value = definitionInput,
                        onValueChange = { definitionInput = it },
                        label = "English Definition",
                        placeholder = "Short, clear definition"
                    )
                    AppTextField(
                        value = exampleInput,
                        onValueChange = { exampleInput = it },
                        label = "English Example",
                        placeholder = "Example sentence"
                    )
                    AppTextField(
                        value = exampleFaInput,
                        onValueChange = { exampleFaInput = it },
                        label = "Example Persian Translation",
                        placeholder = "Persian translation of the example"
                    )

                    ExposedDropdownMenuBox(
                        expanded = levelDropdownExpanded,
                        onExpandedChange = { levelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("CEFR Level", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelDropdownExpanded)
                            },
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = levelDropdownExpanded,
                            onDismissRequest = { levelDropdownExpanded = false }
                        ) {
                            listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = level,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        selectedLevel = level
                                        levelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.space2),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add this entry to your vocabulary library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            viewModel.addWordManually(
                                word = wordInput,
                                meaning = meaningInput.ifEmpty { "Manual addition" },
                                definition = definitionInput,
                                example = exampleInput,
                                exampleFa = exampleFaInput,
                                level = selectedLevel
                            )
                            onBack()
                        },
                        enabled = wordInput.isNotBlank(),
                        modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconMd)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space6))
                        Text(
                            text = "Save Word",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        singleLine = singleLine,
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
