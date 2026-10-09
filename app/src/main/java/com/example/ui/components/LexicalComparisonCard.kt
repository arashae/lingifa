package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.Dimens
import com.example.vocab.LexicalRelation

/** Shared between the study detail page and revealed review feedback. */
@Composable
fun LexicalComparisonCard(relation: LexicalRelation) {
    val uriHandler = LocalUriHandler.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Text("${relation.type.name.replace('_', ' ')} · compare these senses", style = MaterialTheme.typography.labelLarge)
            listOf(relation.first, relation.second).forEach { sense ->
                AppInset {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                        Text("${sense.word} (${sense.partOfSpeech})", fontWeight = FontWeight.Bold)
                        Text(sense.definition)
                        PersianContentRtl { Text(sense.meaningFa) }
                        Text("“${sense.example}”")
                    }
                }
            }
            PersianContentRtl { Text(relation.noteFa, style = MaterialTheme.typography.bodyMedium) }
            relation.sources.forEachIndexed { index, url ->
                TextButton(onClick = { uriHandler.openUri(url) }) {
                    Text("Usage reference: ${if (index == 0) relation.first.word else relation.second.word}")
                }
            }
        }
    }
}
