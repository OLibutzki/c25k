package com.example.c25k.tts

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.example.c25k.domain.AppLanguage
import java.util.Locale

class TtsCoach(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var language: AppLanguage = AppLanguage.SYSTEM

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        applyLanguage(language)
    }

    fun setLanguage(language: AppLanguage) {
        this.language = language
        applyLanguage(language)
    }

    fun speak(text: String) {
        val engine = tts
        if (!initialized || engine == null) {
            playFallbackTone()
            return
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "c25k-cue")
        if (result == TextToSpeech.ERROR) playFallbackTone()
    }

    fun playPhaseStartBeep() {
        ToneGenerator(AudioManager.STREAM_ALARM, 100)
            .startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun applyLanguage(language: AppLanguage) {
        val engine = tts ?: return
        val locale = when (language) {
            AppLanguage.SYSTEM -> Locale.getDefault()
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.DE -> Locale.GERMAN
        }
        val availability = engine.setLanguage(locale)
        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            playFallbackTone()
        }
    }

    private fun playFallbackTone() {
        ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    }
}
