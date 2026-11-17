package com.namaz.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Small wrapper around Android's Turkish text-to-speech engine.
 * Text requested before the engine is ready is spoken as soon as it becomes ready.
 */
internal class Speaker {
    internal var engine: TextToSpeech? = null
    var ready by mutableStateOf(false)
        internal set
    var unavailable by mutableStateOf(false)
        internal set
    var speaking by mutableStateOf(false)
        internal set
    private var pending: String? = null
    private var sequence = 0

    fun speak(text: String) {
        val current = engine
        if (ready && current != null) {
            speaking = true
            current.speak(text, TextToSpeech.QUEUE_FLUSH, null, "namaz-${sequence++}")
        } else if (!unavailable) {
            pending = text
        }
    }

    fun stop() {
        pending = null
        engine?.stop()
        speaking = false
    }

    internal fun onReady() {
        pending?.let { pending = null; speak(it) }
    }
}

@Composable
internal fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val speaker = remember { Speaker() }
    DisposableEffect(context, lifecycle) {
        val handler = Handler(Looper.getMainLooper())
        var disposed = false
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            handler.post {
                if (disposed) return@post
                val result = if (status == TextToSpeech.SUCCESS) engine.setLanguage(turkish) else null
                speaker.ready = result != null && result >= TextToSpeech.LANG_AVAILABLE
                speaker.unavailable = !speaker.ready
                if (speaker.ready) speaker.onReady()
            }
        }
        engine.setSpeechRate(0.85f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { handler.post { if (!disposed) speaker.speaking = false } }
            @Deprecated("Android compatibility")
            override fun onError(utteranceId: String?) { handler.post { if (!disposed) speaker.speaking = false } }
        })
        speaker.engine = engine
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) speaker.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            disposed = true
            lifecycle.removeObserver(observer)
            engine.stop()
            engine.shutdown()
            speaker.engine = null
            speaker.ready = false
        }
    }
    return speaker
}

/** User-level preferences for how the app is presented. Stored only on the device. */
internal object AppMode {
    const val STORE = "namaz_app_mode"
    private fun prefs(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun onboarded(context: Context) = prefs(context).getBoolean("onboarded", false)
    fun setOnboarded(context: Context, value: Boolean) = prefs(context).edit().putBoolean("onboarded", value).apply()

    fun easy(context: Context) = prefs(context).getBoolean("easy", false)
    fun setEasy(context: Context, value: Boolean) = prefs(context).edit().putBoolean("easy", value).apply()

    /** Speak the next prayer automatically when the simple home screen opens. */
    fun autoSpeak(context: Context) = prefs(context).getBoolean("auto_speak", true)
    fun setAutoSpeak(context: Context, value: Boolean) = prefs(context).edit().putBoolean("auto_speak", value).apply()
}

/** Prayer names as they should be read aloud. */
internal fun spokenPrayer(name: String) = when (name) {
    "İmsak" -> "Sabah namazı"
    "Öğle" -> "Öğle namazı"
    "İkindi" -> "İkindi namazı"
    "Akşam" -> "Akşam namazı"
    "Yatsı" -> "Yatsı namazı"
    else -> name
}

/** Maps a prayer-time key to the journal key used by [WorshipStore]. */
internal fun journalPrayer(timeKey: String) = if (timeKey == "İmsak") "Sabah" else timeKey

internal fun remainingText(from: LocalDateTime, to: LocalDateTime): String {
    val minutes = (Duration.between(from, to).seconds / 60).coerceAtLeast(0)
    return when {
        minutes == 0L -> "bir dakikadan az"
        minutes < 60L -> "$minutes dakika"
        minutes % 60 == 0L -> "${minutes / 60} saat"
        else -> "${minutes / 60} saat ${minutes % 60} dakika"
    }
}

internal fun nextPrayerSentence(times: Map<String, LocalDateTime>, now: LocalDateTime): String {
    val next = times.entries.firstOrNull { it.value > now }
        ?: return "Bugünün son vakti olan yatsı girdi. Bir sonraki vakit yarın sabah."
    return "${spokenPrayer(next.key)} saat ${next.value.format(shortTime)}. ${remainingText(now, next.value).replaceFirstChar { it.uppercase() }} kaldı."
}

internal fun todaySentence(date: LocalDate): String =
    "Bugün " + date.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", turkish)) + "."
