package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.ai.AiVocabCardGenerator
import com.example.data.model.AiVocabCardData
import com.example.ui.components.CefrBadge
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVocabCardScreen(onBack: () -> Unit, viewModel: AiVocabCardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val tts = remember { TtsManager(LocalContext.current) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.statusMessage) { state.statusMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearStatusMessage() } }

    PersianRtlLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("کارت واژه", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)) {
                WordSearchBar(state.query, state.isLoading, viewModel::onQueryChange) { viewModel.generateCard() }
                SuggestedWords(viewModel::generateCard)
                state.currentCard?.let { card ->
                    VocabularyFocusCard(card, state.isPlayingAudio, state.isUkAccent, { viewModel.speakWord(tts) }, { viewModel.speakExample(tts) }, viewModel::toggleAccent, Modifier.weight(1f).padding(top = 20.dp))
                    SaveCardButton(state.isSaved, viewModel::saveCardToLibrary)
                } ?: Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
            }
        }
    }
}

@Composable
private fun WordSearchBar(query: String, loading: Boolean, onQueryChange: (String) -> Unit, onGenerate: () -> Unit) {
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, placeholder = { Text("Search a word") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onGenerate() }), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Surface(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(enabled = !loading && query.isNotBlank(), onClick = onGenerate),
            color = PrimaryBlue,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Icon(Icons.Default.AutoAwesome, "تولید کارت")
            }
        }
    }
}

@Composable
private fun SuggestedWords(onChoose: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("پیشنهاد:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AiVocabCardGenerator.getQuickInspirationWords().take(4).forEach { word ->
            Text(word, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { onChoose(word) })
        }
    }
}

@Composable
private fun VocabularyFocusCard(card: AiVocabCardData, isPlaying: Boolean, isUkAccent: Boolean, onSpeakWord: () -> Unit, onSpeakExample: () -> Unit, onToggleAccent: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(card.word, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp), color = MaterialTheme.colorScheme.onSurface)
                        Text("${card.phonetic}  •  ${card.partOfSpeech}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CefrBadge(card.cefrLevel)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onSpeakWord),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = PrimaryBlue
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp, "پخش تلفظ") }
                }
                Text(if (isUkAccent) "British English" else "American English", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onToggleAccent))
            }
            Spacer(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("معنی", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(card.persianTranslation, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                if (card.englishDefinition.isNotBlank()) Text(card.englishDefinition, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (card.exampleSentenceEn.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("مثال", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        IconButton(onClick = onSpeakExample, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "پخش مثال", tint = PrimaryBlue, modifier = Modifier.size(19.dp)) }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(card.exampleSentenceEn, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start) }
                    if (card.exampleSentenceFa.isNotBlank()) Text(card.exampleSentenceFa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (card.collocations.isNotEmpty()) Text(card.collocations.take(3).joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SaveCardButton(saved: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave, enabled = !saved, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (saved) SuccessGreen else PrimaryBlue)) {
        Icon(if (saved) Icons.Default.Check else Icons.Default.Bookmark, null)
        Spacer(Modifier.width(8.dp))
        Text(if (saved) "در لغات ذخیره شد" else "ذخیره در لغات", fontWeight = FontWeight.Bold)
    }
}
