package com.example.ui.screens.vocab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.ai.AiVocabCardGenerator
import com.example.ui.components.CefrBadge
import com.example.ui.components.MinimalStreakCard
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiVocabCardScreen(
    onBack: () -> Unit,
    viewModel: AiVocabCardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val streakInfo by viewModel.streakInfo.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    PersianRtlLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "کارت واژه هوشمند (AI Vocab)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Streak summary banner
                MinimalStreakCard(
                    streakInfo = streakInfo,
                    modifier = Modifier.fillMaxWidth()
                )

                // Search / Word Input Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "واژه انگلیسی مورد نظر را وارد کنید:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = { viewModel.onQueryChange(it) },
                                placeholder = { Text("مثال: Pragmatic, Ubiquitous...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    viewModel.generateCard()
                                }),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            )

                            Button(
                                onClick = { viewModel.generateCard() },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !state.isLoading && state.query.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تولید با AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Quick inspiration pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AiVocabCardGenerator.getQuickInspirationWords().forEach { word ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable {
                                        viewModel.generateCard(word)
                                    }
                                ) {
                                    Text(
                                        text = word,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Vocabulary Flashcard Result Card
                state.currentCard?.let { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Card Header: English word, IPA, CEFR, Part of speech
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = card.word,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 24.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (card.phonetic.isNotBlank()) {
                                        Text(
                                            text = card.phonetic,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryBlue.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = card.partOfSpeech,
                                            color = PrimaryBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    CefrBadge(level = card.cefrLevel)
                                }
                            }

                            // Audio Pronunciation Bar
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.speakWord(tts) },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryBlue)
                                        ) {
                                            Icon(
                                                imageVector = if (state.isPlayingAudio) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "پخش تلفظ صوتی",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "تلفظ صوتی واژه",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (state.isUkAccent) "لهجه بریتانیایی (UK)" else "لهجه آمریکایی (US)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // US / UK Accent Chip
                                        FilterChip(
                                            selected = state.isUkAccent,
                                            onClick = { viewModel.toggleAccent() },
                                            label = { Text(if (state.isUkAccent) "UK" else "US", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryBlue,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        // Speed button
                                        IconButton(
                                            onClick = {
                                                val nextSpeed = if (state.speechRate <= 0.8f) 1.0f else 0.8f
                                                viewModel.setSpeechRate(nextSpeed)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = "سرعت تلفظ",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Persian Meaning Section
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "معنی و ترجمه فارسی:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = card.persianTranslation,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            lineHeight = 26.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            // English Learner Definition
                            if (card.englishDefinition.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "تعریف انگلیسی (Definition):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = card.englishDefinition,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Example Sentence with Audio Player
                            if (card.exampleSentenceEn.isNotBlank()) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = PrimaryBlue.copy(alpha = 0.04f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        PrimaryBlue.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "جمله نمونه در بافت آکادمیک:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PrimaryBlue
                                            )
                                            IconButton(
                                                onClick = { viewModel.speakExample(tts) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "پخش صوتی جمله نمونه",
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = card.exampleSentenceEn,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (card.exampleSentenceFa.isNotBlank()) {
                                            Text(
                                                text = card.exampleSentenceFa,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Collocations Chips
                            if (card.collocations.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "کالوکیشن‌های مهم (Collocations):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        card.collocations.forEach { col ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SuccessGreen.copy(alpha = 0.1f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f))
                                            ) {
                                                Text(
                                                    text = col,
                                                    color = SuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Synonyms & Antonyms
                            if (card.synonyms.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مترادف‌ها:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = card.synonyms.joinToString(", "),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Persian Learner Pitfall / Tip
                            if (card.persianCommonMistake.isNotBlank() || card.ieltsTipFa.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentGold.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "نکته آموزشی آیلتس / اشتباه رایج ایرانیان:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = AccentGold
                                            )
                                            Text(
                                                text = card.ieltsTipFa.ifEmpty { card.persianCommonMistake },
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Primary Save & Record Streak Button
                            Button(
                                onClick = { viewModel.saveCardToLibrary() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isSaved) SuccessGreen else PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isSaved) Icons.Default.Check else Icons.Default.Bookmark,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isSaved) "در لغات ذخیره شد و زنجیره ثبت گردید ✓" else "ذخیره کارت در لغات و ثبت در زنجیره روزانه (+۲۰ XP)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
