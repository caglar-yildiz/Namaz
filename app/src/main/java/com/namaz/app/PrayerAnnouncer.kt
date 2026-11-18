package com.namaz.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Exact alarms need the "Alarms & reminders" special access on Android 12+.
 * Without it we fall back to an allow-while-idle alarm, which can be a few minutes late.
 */
internal object ExactAlarms {
    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(context: Context, atMillis: Long, operation: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (allowed(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
                return
            } catch (_: SecurityException) {
                // Access was revoked between the check and the call; use the fallback below.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
    }

    /** Opens the system page where the user can allow exact alarms for this app. */
    fun openSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 31) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

internal object PrayerAnnouncer {
    private const val SPOKEN_CHANNEL = "namaz_prayer_spoken_v1"
    private const val SPEAK_KEY = "speak_prayer"
    private const val PREFS = "prayer_reminder_preferences"

    fun speakEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SPEAK_KEY, true)

    fun setSpeakEnabled(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(SPEAK_KEY, value).apply()

    /**
     * Shows the reminder and, when allowed, reads [speech] aloud twice.
     * Speech is skipped if the phone is silenced, on vibrate or in a call; then the
     * regular notification sound of the chosen channel is used instead.
     */
    fun announce(context: Context, title: String, message: String, speech: String) {
        if (!ReminderNotifications.canNotify(context)) return
        if (!speakEnabled(context) || !canSpeakNow(context)) {
            ReminderNotifications.send(context, title, message)
            return
        }
        val intent = Intent(context, PrayerAnnouncementService::class.java)
            .putExtra("title", title).putExtra("message", message).putExtra("speech", speech)
        try {
            context.startForegroundService(intent)
        } catch (_: Exception) {
            // Android 12+ refuses background service starts unless the alarm was exact.
            ReminderNotifications.send(context, title, message)
        }
    }

    private fun canSpeakNow(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        return audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
            audio.mode != AudioManager.MODE_IN_CALL &&
            audio.mode != AudioManager.MODE_IN_COMMUNICATION
    }

    internal fun spokenChannel(context: Context): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(SPOKEN_CHANNEL) == null) {
            val channel = NotificationChannel(SPOKEN_CHANNEL, "Sesli namaz vakti", NotificationManager.IMPORTANCE_HIGH)
            // The spoken sentence replaces the notification sound.
            channel.setSound(null, null)
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }
        return SPOKEN_CHANNEL
    }
}

/**
 * Short-lived foreground service that reads the prayer reminder aloud on the alarm stream,
 * then leaves the notification in place and stops itself.
 */
class PrayerAnnouncementService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var speech: TextToSpeech? = null
    private var finished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { finish(); return START_NOT_STICKY }
        val title = intent?.getStringExtra("title")
        val message = intent?.getStringExtra("message").orEmpty()
        val text = intent?.getStringExtra("speech")
        if (title == null || text == null) { stopSelf(startId); return START_NOT_STICKY }

        val stop = PendingIntent.getService(this, 1, Intent(this, PrayerAnnouncementService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = ReminderNotifications.build(this, title, message, PrayerAnnouncer.spokenChannel(this), null)
            .addAction(Notification.Action.Builder(null, "Sustur", stop).build())
            .setCategory(Notification.CATEGORY_REMINDER)
            // Prayer name and time are not private; show them on the lock screen.
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        // Never keep the service alive longer than needed, even if the speech engine hangs.
        handler.postDelayed({ finish() }, 45_000L)

        speech?.shutdown()
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(this) { status ->
            handler.post {
                if (finished) return@post
                val language = if (status == TextToSpeech.SUCCESS) engine.setLanguage(turkish) else TextToSpeech.LANG_NOT_SUPPORTED
                if (language < TextToSpeech.LANG_AVAILABLE) { finish(); return@post }
                engine.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                engine.setSpeechRate(0.85f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { if (utteranceId == LAST) handler.post { finish() } }
                    @Deprecated("Android compatibility")
                    override fun onError(utteranceId: String?) { handler.post { finish() } }
                })
                // Said twice with a pause, so it is not missed by someone who is not looking at the phone.
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "first")
                engine.playSilentUtterance(1500L, TextToSpeech.QUEUE_ADD, "pause")
                engine.speak(text, TextToSpeech.QUEUE_ADD, null, LAST)
            }
        }
        speech = engine
        return START_NOT_STICKY
    }

    private fun finish() {
        if (finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        speech?.stop()
        speech?.shutdown()
        speech = null
        // Keep the notification visible after the speech ends.
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        finish()
        super.onDestroy()
    }

    private companion object {
        const val ACTION_STOP = "com.namaz.app.STOP_ANNOUNCEMENT"
        const val LAST = "last"
        const val NOTIFICATION_ID = 4242
    }
}

/** Builds the title, notification text and spoken sentence for a prayer alarm. */
internal fun prayerAlarmTexts(prayer: String, time: String?, leadMinutes: Int, city: String?): Triple<String, String, String> {
    val spoken = spokenPrayer(prayer)
    val place = city?.let { " · $it" }.orEmpty()
    return if (leadMinutes > 0) Triple(
        "$spoken için $leadMinutes dakika kaldı",
        (time?.let { "Vakit saat $it" } ?: "Vakit yaklaşıyor") + place,
        "$spoken vaktine $leadMinutes dakika kaldı." + (time?.let { " Vakit saat $it." }.orEmpty())
    ) else Triple(
        "$spoken vakti girdi",
        (time?.let { "Saat $it" } ?: "Vakit girdi") + place,
        "$spoken vakti girdi." + (time?.let { " Saat $it." }.orEmpty())
    )
}

