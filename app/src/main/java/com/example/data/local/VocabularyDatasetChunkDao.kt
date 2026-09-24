package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VocabularyDatasetChunk

@Dao
interface VocabularyDatasetChunkDao {
    @Query("SELECT * FROM vocabulary_dataset_chunks WHERE chunkId = :chunkId LIMIT 1")
    suspend fun get(chunkId: String): VocabularyDatasetChunk?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chunk: VocabularyDatasetChunk)

    @Query("SELECT * FROM vocabulary_dataset_chunks ORDER BY importedAt DESC")
    suspend fun getAll(): List<VocabularyDatasetChunk>
}
