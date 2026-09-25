package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar

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
            topBar = { LinguaTopAppBar(title = "Add Vocabulary Word", onBack = onBack) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "New Word",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Only the word is required; fill additional details manually or enrich with AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FormCard(title = "Core Information") {
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
                        placeholder = "e.g., کاهش دادن، تعدیل کردن",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        if (state.isAiGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enriching with AI…")
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("Auto-Enrich with AI")
                        }
                    }
                }

                FormCard(title = "Educational Details", subtitle = "Optional") {
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
                        placeholder = "Persian sentence translation"
                    )

                    ExposedDropdownMenuBox(
                        expanded = levelDropdownExpanded,
                        onExpandedChange = { levelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("CEFR Level") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelDropdownExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
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
                                DropMenuItem(level) {
                                    selectedLevel = level
                                    levelDropdownExpanded = false
                                }
                            }
                        }
                    }
                }

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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Word")
                }

                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun DropMenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick
    )
}

@Composable
private fun FormCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
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
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
