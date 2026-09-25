package com.example.ui.screens.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.ai.AiVocabCardGenerator
import com.example.data.model.AiVocabCardData
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.HairLine
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun AiVocabCardScreen(onBack: () -> Unit, viewModel: AiVocabCardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { LinguaTopAppBar(title = "AI Word Card", onBack = onBack) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space10),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                WordSearchBar(
                    query = state.query,
                    loading = state.isLoading,
                    onQueryChange = viewModel::onQueryChange,
                    onGenerate = { viewModel.generateCard() }
                )
                SuggestedWords(
                    selectedWord = state.query,
                    onChoose = { word ->
                        viewModel.onQueryChange(word)
                        viewModel.generateCard(word)
                    }
                )
                state.currentCard?.let { card ->
                    if (!card.isValid) {
                        InvalidCardNotice(
                            message = card.errorMessage ?: "Word not found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        VocabularyFocusCard(
                            card = card,
                            isPlaying = state.isPlayingAudio,
                            isUkAccent = state.isUkAccent,
                            onSpeakWord = { viewModel.speakWord(tts) },
                            onSpeakExample = { viewModel.speakExample(tts) },
                            onToggleAccent = viewModel::toggleAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        SaveCardButton(
                            saved = state.isSaved,
                            onSave = viewModel::saveCardToLibrary
                        )
                    }
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WordSearchBar(
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search a word",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onGenerate() }),
            shape = RoundedCornerShape(Dimens.radiusMd),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onGenerate,
            enabled = !loading && query.isNotBlank(),
            shape = RoundedCornerShape(Dimens.radiusSm),
            modifier = Modifier.size(Dimens.minTapTarget)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.iconMd),
                    strokeWidth = Dimens.hairline * 2,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Generate card",
                    modifier = Modifier.size(Dimens.iconMd)
                )
            }
        }
    }
}

@Composable
private fun SuggestedWords(
    selectedWord: String,
    onChoose: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
        FieldLabel("Quick suggestions")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            items(AiVocabCardGenerator.getQuickInspirationWords().take(4)) { word ->
                SelectChip(
                    text = word,
                    selected = selectedWord.equals(word, ignoreCase = true),
                    onClick = { onChoose(word) }
                )
            }
        }
    }
}

@Composable
private fun VocabularyFocusCard(
    card: AiVocabCardData,
    isPlaying: Boolean,
    isUkAccent: Boolean,
    onSpeakWord: () -> Unit,
    onSpeakExample: () -> Unit,
    onToggleAccent: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        padding = Dimens.cardPadding
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Text(
                        text = card.word,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${card.phonetic} · ${card.partOfSpeech}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CefrBadge(card.cefrLevel)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioSpeakerButton(onClick = onSpeakWord)
                SelectChip(
                    text = if (isUkAccent) "British English" else "American English",
                    selected = isUkAccent,
                    onClick = onToggleAccent
                )
                if (isPlaying) {
                    TagChip(
                        text = "Playing",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HairLine()
            AppInset(padding = Dimens.space10) {
                FieldLabel("Meaning")
                PersianContentRtl {
                    Text(
                        text = card.persianTranslation,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (card.englishDefinition.isNotBlank()) {
                    Text(
                        text = card.englishDefinition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (card.exampleSentenceEn.isNotBlank()) {
                AppInset(padding = Dimens.space10) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FieldLabel(
                            text = "Example",
                            modifier = Modifier.weight(1f)
                        )
                        AudioSpeakerButton(
                            onClick = onSpeakExample,
                            contentDescription = "Play example"
                        )
                    }
                    Text(
                        text = card.exampleSentenceEn,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (card.exampleSentenceFa.isNotBlank()) {
                        PersianContentRtl {
                            Text(
                                text = card.exampleSentenceFa,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (card.collocations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                    FieldLabel("Collocations")
                    Text(
                        text = card.collocations.take(4).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveCardButton(saved: Boolean, onSave: () -> Unit) {
    Button(
        onClick = onSave,
        enabled = !saved,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (saved) Accent.success else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTapTarget)
    ) {
        Icon(
            imageVector = if (saved) Icons.Default.Check else Icons.Default.Bookmark,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconMd)
        )
        Spacer(modifier = Modifier.width(Dimens.space6))
        Text(
            text = if (saved) "Saved to Vocabulary" else "Save to Vocabulary",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InvalidCardNotice(message: String, modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.space24),
            verticalArrangement = Arrangement.spacedBy(Dimens.space8),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.iconLg)
            )
            Text(
                text = "Unverified Word",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
