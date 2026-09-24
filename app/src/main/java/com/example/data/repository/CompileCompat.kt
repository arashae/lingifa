package com.example.data.repository

import com.example.data.local.VocabularyDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn as coroutineFlowOn
import kotlin.coroutines.CoroutineContext

/** Compatibility helpers for call sites introduced by the vocabulary performance update. */
fun <T> Flow<T>.flowOn(context: CoroutineContext): Flow<T> = with(this) {
    coroutineFlowOn(context)
}

suspend fun VocabularyDao.getByIdSync(id: Int) = getByIdSync(id.toLong())
