package com.example.ui.screens.vocab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning as materialWarning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp as composeSp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn as coroutineFlowOn
import kotlin.coroutines.CoroutineContext

/** Compatibility helpers for the vocabulary UI changes merged into main. */
fun <T> Flow<T>.flowOn(context: CoroutineContext): Flow<T> = with(this) {
    coroutineFlowOn(context)
}

val Int.sp: TextUnit
    get() = with(this) { composeSp }

val Icons.Filled.Warning: ImageVector
    get() = with(this) { materialWarning }
