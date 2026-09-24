package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.IeltsFlashcardDao
import com.example.data.model.IeltsFlashcard
import com.example.data.model.IeltsVocabularyDeck
import com.example.data.repository.FlashcardRating
import com.example.data.repository.IeltsFlashcardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IeltsFlashcardRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: IeltsFlashcardDao
    private lateinit var repository: IeltsFlashcardRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.ieltsFlashcardDao()
        repository = IeltsFlashcardRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertDeckAndCards_retrievesDeckWithFlashcardsAndPersianTranslations() = runBlocking {
        // 1. Create an IELTS vocabulary deck
        val deck = IeltsVocabularyDeck(
            id = 101,
            titleEn = "IELTS Band 7.5 Academic Environment",
            titleFa = "واژگان آکادمیک محیط زیست آیلتس",
            descriptionEn = "High-yield vocabulary for environmental essays",
            descriptionFa = "لغات کلیدی رایتینگ تسک ۲ برای موضوعات محیط زیست",
            targetBand = "7.5",
            topic = "Environment",
            examModule = "Academic"
        )
        val deckId = dao.insertDeck(deck)
        assertEquals(101L, deckId)

        // 2. Create IELTS flashcard with Persian translation & example sentence
        val card = IeltsFlashcard(
            deckId = deckId,
            word = "Mitigate",
            normalizedWord = "mitigate",
            phonetic = "/ˈmɪt.ɪ.ɡeɪt/",
            partOfSpeech = "verb",
            persianTranslation = "کاهش دادن، تعدیل کردن",
            englishDefinition = "To make something less severe or painful",
            exampleSentenceEn = "Governments must mitigate the adverse effects of climate change.",
            exampleSentenceFa = "دولت‌ها باید اثرات نامطلوب تغییرات اقلیمی را کاهش دهند.",
            ieltsTopic = "Environment",
            targetBand = "7.5",
            collocations = listOf("mitigate the effects", "mitigate risks"),
            synonyms = listOf("alleviate", "lessen"),
            antonyms = listOf("exacerbate")
        )
        val cardId = dao.insertCard(card)
        assertTrue(cardId > 0)

        // 3. Query deck with flashcards
        val deckWithCards = dao.getDeckWithFlashcards(deckId).first()
        assertNotNull(deckWithCards)
        assertEquals("IELTS Band 7.5 Academic Environment", deckWithCards!!.deck.titleEn)
        assertEquals("واژگان آکادمیک محیط زیست آیلتس", deckWithCards.deck.titleFa)
        assertEquals(1, deckWithCards.flashcards.size)

        val retrievedCard = deckWithCards.flashcards[0]
        assertEquals("Mitigate", retrievedCard.word)
        assertEquals("کاهش دادن، تعدیل کردن", retrievedCard.persianTranslation)
        assertEquals("Governments must mitigate the adverse effects of climate change.", retrievedCard.exampleSentenceEn)
        assertEquals("دولت‌ها باید اثرات نامطلوب تغییرات اقلیمی را کاهش دهند.", retrievedCard.exampleSentenceFa)
        assertEquals(listOf("mitigate the effects", "mitigate risks"), retrievedCard.collocations)
    }

    @Test
    fun bilingualSearch_findsCardByPersianOrEnglish() = runBlocking {
        val deck = IeltsVocabularyDeck(
            id = 202,
            titleEn = "IELTS Tech Deck",
            titleFa = "دک فناوری آیلتس",
            targetBand = "8.0"
        )
        dao.insertDeck(deck)

        val card = IeltsFlashcard(
            deckId = 202,
            word = "Ubiquitous",
            phonetic = "/juːˈbɪk.wɪ.təs/",
            persianTranslation = "همه‌جا حاضر، فراگیر",
            exampleSentenceEn = "Smartphones have become ubiquitous in daily life.",
            exampleSentenceFa = "گوشی‌های هوشمند در زندگی روزمره فراگیر شده‌اند."
        )
        dao.insertCard(card)

        // Search by English word
        val searchByWord = dao.searchAllCards("ubiq").first()
        assertEquals(1, searchByWord.size)
        assertEquals("Ubiquitous", searchByWord[0].word)

        // Search by Persian translation
        val searchByPersian = dao.searchAllCards("فراگیر").first()
        assertEquals(1, searchByPersian.size)
        assertEquals("Ubiquitous", searchByPersian[0].word)

        // Search within Persian example sentence
        val searchByPersianExample = dao.searchAllCards("روزمره").first()
        assertEquals(1, searchByPersianExample.size)
        assertEquals("Ubiquitous", searchByPersianExample[0].word)
    }

    @Test
    fun spacedRepetition_progressesLeitnerBoxesOnReview() = runBlocking {
        val deckId = dao.insertDeck(IeltsVocabularyDeck(titleEn = "SRS Deck", titleFa = "دک مرور"))
        val cardId = dao.insertCard(
            IeltsFlashcard(
                deckId = deckId,
                word = "Exacerbate",
                persianTranslation = "تشدید کردن، وخیم‌تر ساختن",
                exampleSentenceEn = "Traffic congestion exacerbates stress.",
                exampleSentenceFa = "ازدحام ترافیک استرس را تشدید می‌کند.",
                leitnerBox = 1
            )
        )

        // Process review rating GOOD
        val reviewedCard = repository.processFlashcardReview(cardId, FlashcardRating.GOOD)
        assertNotNull(reviewedCard)
        assertEquals(2, reviewedCard!!.leitnerBox)
        assertEquals(1, reviewedCard.consecutiveCorrectStreak)
        assertTrue(reviewedCard.masteryPercentage > 0)
        assertTrue(reviewedCard.nextReviewDueAt > System.currentTimeMillis())

        // Toggle bookmark
        repository.toggleBookmark(cardId)
        val bookmarked = dao.getCardById(cardId).first()
        assertTrue(bookmarked!!.isBookmarked)
    }
}
