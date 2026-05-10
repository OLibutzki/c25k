package de.libutzki.c25k.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import de.libutzki.c25k.domain.AppLanguage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class TtsCoach(private val context: Context) : TextToSpeech.OnInitListener {
    private companion object {
        const val GUIDANCE_STREAM = AudioManager.STREAM_MUSIC
        const val TONE_VOLUME = 100
    }

    private var tts: TextToSpeech? = null
    private var toneGenerator: ToneGenerator? = null
    private var initialized = false
    private var language: AppLanguage = AppLanguage.SYSTEM
    @Volatile
    private var completionCallback: (() -> Unit)? = null
    @Volatile
    private var completionUtteranceId: String? = null

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
        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    completeUtterance(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    completeUtterance(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    completeUtterance(utteranceId)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    completeUtterance(utteranceId)
                }
            }
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

    suspend fun speakAndWait(text: String) {
        val engine = tts
        if (!initialized || engine == null) {
            playFallbackTone()
            return
        }

        suspendCancellableCoroutine { continuation ->
            val utteranceId = "c25k-cue-${System.nanoTime()}"
            completionUtteranceId = utteranceId
            completionCallback = { continuation.resume(Unit) }

            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                clearCompletionCallback(utteranceId)
                playFallbackTone()
                if (continuation.isActive) continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                clearCompletionCallback(utteranceId)
            }
        }
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

    private fun completeUtterance(utteranceId: String?) {
        val callback = synchronized(this) {
            if (utteranceId == null || utteranceId != completionUtteranceId) {
                null
            } else {
                completionUtteranceId = null
                completionCallback.also { completionCallback = null }
            }
        }
        callback?.invoke()
    }

    private fun clearCompletionCallback(utteranceId: String) {
        synchronized(this) {
            if (completionUtteranceId == utteranceId) {
                completionUtteranceId = null
                completionCallback = null
            }
        }
    }
}
