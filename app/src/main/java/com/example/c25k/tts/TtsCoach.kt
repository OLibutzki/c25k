package com.example.c25k.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.example.c25k.domain.AppLanguage
import java.util.Locale

class TtsCoach(private val context: Context) : TextToSpeech.OnInitListener {
    private companion object {
        const val GUIDANCE_STREAM = AudioManager.STREAM_MUSIC
        const val TONE_VOLUME = 100
    }

    private var tts: TextToSpeech? = null
    private var toneGenerator: ToneGenerator? = null
    private var initialized = false
    private var language: AppLanguage = AppLanguage.SYSTEM

    init {
        tts = TextToSpeech(context, this)
        toneGenerator = ToneGenerator(GUIDANCE_STREAM, TONE_VOLUME)
    }

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(GUIDANCE_STREAM)
                .build()
        )
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

    fun playSegmentStartBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        toneGenerator?.release()
        toneGenerator = null
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
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    }
}
