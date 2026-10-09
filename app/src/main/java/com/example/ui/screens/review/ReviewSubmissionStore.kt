package com.example.ui.screens.review

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.MistakeRepository
import com.example.data.repository.VocabularyRepository
import com.example.srs.ReviewRating

/** One submitted card is durable, even if the learner leaves before finishing a session. */
internal class ReviewSubmissionStore(private val db: AppDatabase) {
    private val vocabRepo = VocabularyRepository(db.vocabularyDao(), db.vocabularyPackDao(), db.vocabularyPackItemDao())
    private val mistakeRepo = MistakeRepository(db.mistakeDao())
    private val streakRepo = DailyStreakRepository(db.dailyStreakDao(), db.userProfileDao())

    suspend fun save(task: ReviewTask, answerText: String, rating: ReviewRating?, now: Long, minutesSpent: Int, xp: Int, hintUsed: Boolean = false) {
        db.withTransaction {
            val currentItem = vocabRepo.getByIdSync(task.item.id)
                ?: error("Word deleted")
            require(currentItem.word == task.item.word &&
                currentItem.persianMeaning == task.item.persianMeaning &&
                currentItem.example == task.item.example &&
                currentItem.englishDefinition == task.item.englishDefinition) { "Card changed; restart review" }
            ReviewLearningStore(db).record(task, answerText, rating, now, hintUsed)
            if (rating == ReviewRating.AGAIN) {
                mistakeRepo.addMistake(
                    question = task.prompt,
                    myAnswer = answerText.ifBlank { "Could not recall the target word" },
                    correctAnswer = if (task.mode == ReviewMode.MEANING) task.expectedAnswer
                        else "${task.expectedAnswer} — ${currentItem.persianMeaning}",
                    explanationFa = currentItem.examplePersian.ifEmpty { currentItem.example },
                    whyWrongFa = "واژهٔ هدف یادآوری نشد؛ مرور بعدی حداکثر ۳۰ دقیقه دیگر است.",
                    concept = currentItem.word,
                    skillType = "VOCABULARY"
                )
            }
            streakRepo.recordPracticeActivity(
                itemsCount = 1,
                minutesSpent = minutesSpent,
                xpEarned = xp,
                activityType = "VOCABULARY_SRS_REVIEW"
            )
        }
    }
}
