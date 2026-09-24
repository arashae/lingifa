package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "Language not supported, fallback to default")
            } else {
                isInitialized = true
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    fun speak(text: String, isUk: Boolean = false, speechRate: Float = 0.9f) {
        if (!isInitialized || tts == null) return
        try {
            tts?.language = if (isUk) Locale.UK else Locale.US
            tts?.setSpeechRate(speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LFS_UTTERANCE_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("TtsManager", "Error speaking text", e)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
