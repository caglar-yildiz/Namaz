package com.namaz.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val deep = Color(0xFF103E36)
private val emerald = Color(0xFF14594B)
private val gold = Color(0xFFD7B56D)
private val cream = Color(0xFFF8F6F0)

private data class MemorizeSurah(val number: Int, val title: String, val ayahs: Int)
private data class MemorizeAyah(val text: String, val audio: String)
private val shortSurahs = listOf(
    MemorizeSurah(1, "Fâtiha", 7),
    MemorizeSurah(112, "İhlâs", 4),
    MemorizeSurah(113, "Felak", 5),
    MemorizeSurah(114, "Nâs", 6),
    MemorizeSurah(108, "Kevser", 3),
    MemorizeSurah(103, "Asr", 3),
    MemorizeSurah(110, "Nasr", 3),
    MemorizeSurah(109, "Kâfirûn", 6),
    MemorizeSurah(107, "Mâûn", 7),
    MemorizeSurah(105, "Fîl", 5)
)

/** API returns Arabic Quran text; never substitute invented or incomplete verses. */
private suspend fun loadAyahs(surah: MemorizeSurah): List<MemorizeAyah> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = (java.net.URL("https://api.alquran.cloud/v1/surah/${surah.number}/ar.alafasy")
            .openConnection() as java.net.HttpURLConnection)
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
            val json = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val data = json.getJSONObject("data")
            require(data.getInt("number") == surah.number)
            val ayahs = data.getJSONArray("ayahs")
            require(ayahs.length() == surah.ayahs)
            (0 until ayahs.length()).map { i ->
                val ayah = ayahs.getJSONObject(i)
                require(ayah.getInt("numberInSurah") == i + 1)
                val audio = ayah.getString("audio")
                require(audio.startsWith("https://"))
                MemorizeAyah(ayah.getString("text"), audio)
            }
        } finally { connection.disconnect() }
    }

private object MemorizeStore {
    internal const val NAME = "quran_memorization_v1"
    fun mastered(context: Context, surah: Int, verse: Int): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean("mastered_${surah}_$verse", false)
    fun setMastered(context: Context, surah: Int, verse: Int, value: Boolean) {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("mastered_${surah}_$verse", value)
            .putString("review_${surah}_$verse",
                if (value) WorshipStore.today().plusDays(1).toString() else "")
            .remove("streak_${surah}_$verse").apply()
    }
    fun reviewDate(context: Context, surah: Int, verse: Int): LocalDate? {
        val raw = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString("review_${surah}_$verse", null)
        return try { raw?.takeIf { it.isNotBlank() }?.let(LocalDate::parse) }
        catch (_: Exception) { null }
    }
    fun review(context: Context, surah: Int, verse: Int, remembered: Boolean) {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val streakKey = "streak_${surah}_$verse"
        val streak = if (remembered) (prefs.getInt(streakKey, 0) + 1).coerceAtMost(4) else 0
        val days = listOf(1L, 3L, 7L, 14L)[streak.coerceAtMost(3)]
        prefs.edit().putInt(streakKey, streak)
            .putBoolean("mastered_${surah}_$verse", true)
            .putString("review_${surah}_$verse", WorshipStore.today().plusDays(days).toString()).apply()
    }
}

@Composable
internal fun pendingMemorization(today: LocalDate): Int {
    val context = LocalContext.current
    val revision = preferenceRevision(MemorizeStore.NAME)
    return remember(today, revision) {
        shortSurahs.sumOf { surah -> (1..surah.ayahs).count { verse ->
            MemorizeStore.reviewDate(context, surah.number, verse)?.let { it <= today } == true
        } }
    }
}

@Composable
fun QuranMemorizationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var surah by remember { mutableStateOf<MemorizeSurah?>(null) }
    var verseIndex by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(false) }
    var repeatCount by remember { mutableIntStateOf(3) }
    val revision = preferenceRevision(MemorizeStore.NAME)
    val today = currentIstanbulTime().toLocalDate()
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playError by remember { mutableStateOf("") }
    var remaining by remember { mutableIntStateOf(0) }

    val verses by key(surah) {
        produceState<List<MemorizeAyah>?>(null) {
            value = null
            val selected = surah ?: return@produceState
            value = try { loadAyahs(selected) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { emptyList() }
        }
    }
    DisposableEffect(surah) {
        onDispose { player?.release(); player = null; playing = false }
    }
    fun stopAudio() {
        player?.release()
        player = null
        playing = false
        remaining = 0
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) stopAudio()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun goBack() {
        stopAudio()
        if (surah == null) onBack() else { surah = null; verseIndex = 0 }
    }
    BackHandler(surah != null, onBack = ::goBack)
    fun playRepeats() {
        val audio = verses?.getOrNull(verseIndex)?.audio ?: return
        stopAudio()
        playError = ""
        remaining = repeatCount
        fun startOne() {
            val mp = MediaPlayer()
            player = mp
            try {
                mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                mp.setDataSource(audio)
                mp.setOnPreparedListener { if (player === it) { it.start(); playing = true } }
                mp.setOnCompletionListener {
                    if (player !== it) return@setOnCompletionListener
                    it.release()
                    player = null
                    remaining--
                    if (remaining > 0) startOne() else playing = false
                }
                mp.setOnErrorListener { _, _, _ ->
                    stopAudio()
                    playError = "Ses oynatılamadı. İnternet bağlantınızı kontrol edin."
                    true
                }
                mp.prepareAsync()
            } catch (_: Exception) {
                mp.release()
                player = null
                playing = false
                playError = "Ses oynatılamadı. İnternet bağlantınızı kontrol edin."
            }
        }
        startOne()
    }

    LazyColumn(Modifier.fillMaxSize().background(cream),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = ::goBack) { Text("← " + if (surah == null) "Geri" else "Sureler") }
        }
        if (surah == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = deep),
                    shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("☾  EZBER ASİSTANIM", color = gold, fontWeight = FontWeight.Bold)
                        Text("Kur'an Ezberi", color = Color.White,
                            style = MaterialTheme.typography.headlineLarge)
                        Text("Dinle, gizle, hatırla ve düzenli tekrar et.",
                            color = Color(0xFFE4EFE9))
                    }
                }
            }
            item { Text("Kısa sureler", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold) }
            items(shortSurahs) { item ->
                val done = remember(item, revision) {
                    (1..item.ayahs).count { MemorizeStore.mastered(context, item.number, it) }
                }
                val due = remember(item, revision, today) {
                    (1..item.ayahs).count { verse ->
                        MemorizeStore.reviewDate(context, item.number, verse)?.let { it <= today } == true
                    }
                }
                Card(Modifier.fillMaxWidth().clickable {
                    surah = item; verseIndex = 0; hidden = false
                }, colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.title, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium)
                            Text("$done / ${item.ayahs}", color = emerald)
                        }
                        LinearProgressIndicator(progress = { done.toFloat() / item.ayahs },
                            modifier = Modifier.fillMaxWidth(), color = emerald)
                        Text(if (due > 0) "$due ayet tekrar bekliyor" else "${item.ayahs} ayet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text("Ayet metni ve ses için internet gerekir. Ezber ilerlemesi cihazınızda saklanır.",
                style = MaterialTheme.typography.bodySmall) }
        } else {
            val current = surah!!
            val text = verses
            item {
                Text(current.title + " Suresi", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text("Ayet ${verseIndex + 1} / ${current.ayahs}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (text == null) {
                item { CircularProgressIndicator() }
            } else if (text.isEmpty() || verseIndex >= text.size) {
                item { Text("Ayetler yüklenemedi. İnternet bağlantınızı kontrol edip sureyi yeniden açın.") }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("AYET ${verseIndex + 1}", color = emerald,
                                style = MaterialTheme.typography.labelLarge)
                            Text(if (hidden) "•••" else text[verseIndex].text,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center, color = deep)
                            OutlinedButton(onClick = { hidden = !hidden }) {
                                Text(if (hidden) "Ayeti göster" else "Ayeti gizle")
                            }
                        }
                    }
                }
                item {
                    Text("Sesli tekrar", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { count ->
                            FilterChip(selected = repeatCount == count,
                                onClick = { repeatCount = count }, label = { Text("$count kez") })
                        }
                    }
                    Button(onClick = { if (playing || player != null) stopAudio() else playRepeats() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (playing || player != null) "Durdur" else "▶ Ayeti dinle")
                    }
                    if (playError.isNotBlank()) Text(playError, color = MaterialTheme.colorScheme.error)
                }
                item {
                    val mastered = remember(current, verseIndex, revision) { MemorizeStore.mastered(context, current.number, verseIndex + 1) }
                    val due = remember(current, verseIndex, revision) { MemorizeStore.reviewDate(context, current.number, verseIndex + 1) }
                    Text("Ezber durumu", style = MaterialTheme.typography.titleMedium)
                    Text(if (mastered) "Ezberlendi" else "Henüz tamamlanmadı")
                    if (due != null) Text("Sonraki tekrar: $due")
                    Button(onClick = {
                        MemorizeStore.setMastered(context, current.number, verseIndex + 1, !mastered)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (mastered) "Ezberi kaldır" else "Bu ayeti ezberledim")
                    }
                    if (mastered) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                MemorizeStore.review(context, current.number, verseIndex + 1, false)
                            }) { Text("Zorlandım") }
                            OutlinedButton(onClick = {
                                MemorizeStore.review(context, current.number, verseIndex + 1, true)
                            }) { Text("Hatırladım") }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(enabled = verseIndex > 0, onClick = {
                            stopAudio(); verseIndex--; hidden = false
                        }) { Text("← Önceki") }
                        Button(enabled = verseIndex < current.ayahs - 1, onClick = {
                            stopAudio(); verseIndex++; hidden = false
                        }) { Text("Sonraki →") }
                    }
                }
                item { Text("Ses ve metin çevrimiçi kaynaktan gelir. Telaffuz ve tecvid değerlendirmesi yapılmaz.",
                    style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
