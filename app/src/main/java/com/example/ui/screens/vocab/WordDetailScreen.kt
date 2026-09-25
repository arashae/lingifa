package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.theme.AccentGold

@Composable
fun WordDetailScreen(
    vocabId: Long,
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val wordFlow = remember(vocabId) { viewModel.getWordById(vocabId) }
    val item by wordFlow.collectAsState(initial = null)
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }

    var userSelectedTab by remember(vocabId) { mutableStateOf<Int?>(null) }
    var feedbackMessage by remember(vocabId) { mutableStateOf<String?>(null) }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LinguaTopAppBar(
                    title = "Word Details",
                    onBack = onBack,
                    actions = {
                        item?.let { current ->
                            IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                                Icon(
                                    imageVector = if (current.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (current.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            val currentWord = item
            if (currentWord == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading word…")
                }
                return@Scaffold
            }

            val defaultTab = if (currentWord.cefrLevel.uppercase() in setOf("A1", "A2")) 0 else 1
            val selectedTab = userSelectedTab ?: defaultTab

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WordHeroCard(
                    item = currentWord,
                    onPronounce = { tts.speak(currentWord.word) }
                )

                LanguageTabs(
                    selectedTab = selectedTab,
                    onSelected = { userSelectedTab = it }
                )

                DefinitionCard(
                    item = currentWord,
                    englishFirst = selectedTab == 1
                )

                if (currentWord.example.isNotBlank()) {
                    ExampleCard(
                        example = currentWord.example,
                        translation = currentWord.examplePersian,
                        onPronounce = { tts.speak(currentWord.example) }
                    )
                }

                if (currentWord.collocations.isNotEmpty()) {
                    DetailListCard(
                        title = "Collocations",
                        subtitle = "High-frequency natural combinations",
                        items = currentWord.collocations
                    )
                }

                if (currentWord.synonyms.isNotEmpty()) {
                    DetailListCard(
                        title = "Synonyms",
                        subtitle = "Related meaning words",
                        items = currentWord.synonyms
                    )
                }

                if (currentWord.wordFamily.isNotEmpty()) {
                    DetailListCard(
                        title = "Word Family",
                        subtitle = "Related forms and derivatives",
                        items = currentWord.wordFamily
                    )
                }

                if (currentWord.commonMistakes.isNotBlank()) {
                    CommonMistakeCard(currentWord.commonMistakes)
                }

                ExamRelevanceCard(
                    ielts = currentWord.ieltsRelevance,
                    toefl = currentWord.toeflRelevance,
                    gre = currentWord.greRelevance
                )

                ReviewActionCard(
                    feedbackMessage = feedbackMessage,
                    onKnown = {
                        viewModel.recordLearningJudgement(currentWord, ReviewRating.GOOD)
                        feedbackMessage = "Saved: scheduled for optimal spaced review interval."
                    },
                    onHard = {
                        viewModel.recordLearningJudgement(currentWord, ReviewRating.HARD)
                        feedbackMessage = "Saved: scheduled for early review."
                    },
                    onPractice = {
                        viewModel.recordLearningJudgement(currentWord, ReviewRating.AGAIN)
                        feedbackMessage = "Saved: added to upcoming review queue."
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun WordHeroCard(item: VocabularyItem, onPronounce: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (item.ipa.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.ipa,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                            )
                        }
                        if (item.partOfSpeech.isNotBlank() && item.partOfSpeech != "word") {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = item.partOfSpeech,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            )
                        }
                    }
                    CefrBadge(level = item.cefrLevel)
                }
            }

            Surface(
                onClick = onPronounce,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("Pronounce", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun LanguageTabs(selectedTab: Int, onSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onSelected(0) },
            text = { Text("Persian", style = MaterialTheme.typography.labelLarge) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onSelected(1) },
            text = { Text("English", style = MaterialTheme.typography.labelLarge) }
        )
    }
}

@Composable
private fun DefinitionCard(item: VocabularyItem, englishFirst: Boolean) {
    SectionCard(title = if (englishFirst) "English Definition" else "Meaning & Definition") {
        if (englishFirst) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = item.englishDefinition.ifBlank { item.persianMeaning },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            if (item.englishDefinition.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.persianMeaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = item.persianMeaning,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (item.englishDefinition.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = item.englishDefinition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleCard(example: String, translation: String, onPronounce: () -> Unit) {
    SectionCard(
        title = "Example in Context",
        trailing = {
            IconButton(onClick = onPronounce, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Play example",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = example,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
        if (translation.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailListCard(title: String, subtitle: String, items: List<String>) {
    SectionCard(title = title, subtitle = subtitle) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                items.take(8).forEach { value ->
                    Text(
                        text = "• $value",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun CommonMistakeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text("Common Mistake", style = MaterialTheme.typography.titleSmall)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExamRelevanceCard(ielts: String, toefl: String, gre: String) {
    SectionCard(title = "Exam Usage & Relevance") {
        ExamRow("IELTS", ielts)
        Spacer(modifier = Modifier.height(7.dp))
        ExamRow("TOEFL", toefl)
        Spacer(modifier = Modifier.height(7.dp))
        ExamRow("GRE", gre)
    }
}

@Composable
private fun ExamRow(exam: String, relevance: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(11.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(exam, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                relevance.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewActionCard(
    feedbackMessage: String?,
    onKnown: () -> Unit,
    onHard: () -> Unit,
    onPractice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("SRS Learning Status", style = MaterialTheme.typography.titleMedium)
            Text(
                "This assessment calibrates future spaced review intervals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onKnown, modifier = Modifier.fillMaxWidth()) {
                Text("Mastered (Schedule Next Review)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onHard, modifier = Modifier.weight(1f)) {
                    Text("Hard")
                }
                OutlinedButton(onClick = onPractice, modifier = Modifier.weight(1f)) {
                    Text("Needs Practice")
                }
            }
            feedbackMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}
