package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a successfully imported bundled vocabulary chunk.
 * This makes large master banks resumable and versionable: a JSONL chunk is only
 * processed again when its version changes.
 */
@Entity(tableName = "vocabulary_dataset_chunks")
data class VocabularyDatasetChunk(
    @PrimaryKey
    val chunkId: String,
    val packId: String,
    val version: String,
    val itemCount: Int,
    val importedAt: Long = System.currentTimeMillis()
)
