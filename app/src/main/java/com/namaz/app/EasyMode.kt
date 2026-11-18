package com.namaz.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/*
 * "Sade kullanım" (simple mode): one home screen with a few very large buttons and
 * spoken feedback, meant for older users. It reuses the same stores as the full app,
 * so switching modes never loses data.
 */

private const val EASY_HELP =
    "En üstte sıradaki namaz vakti yazar. Üzerine dokunursanız vakti sesli söylerim. " +
        "Konuşarak sor düğmesine basıp, örneğin öğle ne zaman, diyebilirsiniz. " +
        "Tespih çek ile zikir sayabilir, Dua dinle ile duaları dinleyebilirsiniz. " +
        "Namazınızı kıldığınızda, Namazımı kıldım düğmesine basın."

@Composable
internal fun EasyApp(opening: String?, consumeOpening: () -> Unit) {
    val speaker = rememberSpeaker()
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    // Notification links always land on the simple home screen.
    LaunchedEffect(opening) { if (opening != null) { route = null; consumeOpening() } }
    BackHandler(route != null) { speaker.stop(); route = null }
    fun back() { speaker.stop(); route = null }
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (route) {
                "times" -> EasyTimes(speaker, ::back)
                "reliability" -> EasyReliability(speaker, ::back)
                "dhikr" -> EasyDhikr(speaker, ::back)
                "duas" -> EasyDuas(speaker, ::back)
                else -> EasyHome(speaker) { route = it }
            }
        }
    }
}

internal data class PrayerDay(val city: String?, val times: Map<String, LocalDateTime>?, val failed: Boolean)

@Composable
internal fun rememberPrayerDay(today: LocalDate, retry: Int): PrayerDay {
    val context = LocalContext.current
    val revision = preferenceRevision("prayer_reminder_preferences")
    val city = remember(revision) { ReminderRefresh.city(context) }
    var result by remember { mutableStateOf<Result<Map<String, LocalDateTime>>?>(null) }
    LaunchedEffect(city, today, retry) {
        result = null
        if (city != null) result = withContext(Dispatchers.IO) {
            try { Result.success(PrayerTimesService.forDisplay(context, today, city)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Result.failure(failure) }
        }
    }
    return PrayerDay(city, result?.getOrNull(), result?.isFailure == true)
}

@Composable
internal fun BigButton(
    emoji: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    container: Color = Emerald, content: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 80.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(Modifier.width(16.dp))
        Text(text, Modifier.weight(1f), fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EasyHeader(title: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 60.dp),
            shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
            BackIcon()
            Spacer(Modifier.width(10.dp))
            Text("Ana ekrana dön", fontSize = 22.sp)
        }
        Text(title, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EasyHome(speaker: Speaker, open: (String) -> Unit) {
    val context = LocalContext.current
    val now = currentIstanbulTime()
    val today = now.toLocalDate()
    val nowLocal = now.toLocalDateTime()
    var retry by remember { mutableIntStateOf(0) }
    val day = rememberPrayerDay(today, retry)
    val times = day.times
    val next = times?.entries?.firstOrNull { it.value > nowLocal }
    val journalRevision = preferenceRevision(WorshipStore.STORE)
    val prayedCount = remember(today, journalRevision) {
        WorshipStore.prayers.count { WorshipStore.completed(context, today, it) }
    }
    var spokenOnOpen by rememberSaveable { mutableStateOf(false) }
    var lastMarked by rememberSaveable { mutableStateOf<String?>(null) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var confirmFull by rememberSaveable { mutableStateOf(false) }
    var savingCity by remember { mutableStateOf(false) }
    val reminderRevision = preferenceRevision("prayer_reminder_preferences")
    val remindersOn = remember(reminderRevision) { ReminderPreferences.enabled(context, "Namaz vakitleri") }
    val reliability = rememberReliability()

    LaunchedEffect(times) {
        if (times != null && !spokenOnOpen && AppMode.autoSpeak(context)) {
            spokenOnOpen = true
            speaker.speak(nextPrayerSentence(times, nowLocal))
        }
    }

    val noTimes = if (day.city == null) "Önce şehir seçilmesi gerekiyor."
        else "Vakitlere şu an ulaşılamadı. İnternet bağlantısını kontrol edin."

    fun markCurrent(): String {
        val loaded = times ?: return noTimes
        val current = loaded.entries.lastOrNull { it.value <= nowLocal }
            ?: return "Bugünün ilk vakti henüz girmedi."
        val key = journalPrayer(current.key)
        WorshipStore.setCompleted(context, today, key, true)
        lastMarked = key
        return "${spokenPrayer(current.key)} kaydedildi. Allah kabul etsin."
    }

    fun handle(command: VoiceCommand) {
        when (command) {
            VoiceCommand.NextPrayer -> speaker.speak(times?.let { nextPrayerSentence(it, nowLocal) } ?: noTimes)
            is VoiceCommand.PrayerTime -> speaker.speak(times?.get(command.timeKey)?.let {
                "${spokenPrayer(command.timeKey)} bugün saat ${it.format(shortTime)}."
            } ?: noTimes)
            VoiceCommand.MarkPrayed -> speaker.speak(markCurrent())
            VoiceCommand.OpenDhikr -> open("dhikr")
            VoiceCommand.OpenDuas -> open("duas")
            VoiceCommand.OpenTimes -> open("times")
            VoiceCommand.Today -> speaker.speak(todaySentence(today))
            VoiceCommand.Help -> speaker.speak(VoiceCommands.HELP)
            VoiceCommand.Unknown -> speaker.speak("Anlayamadım. " + VoiceCommands.HELP)
        }
    }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (result.resultCode == Activity.RESULT_OK && heard.isNotEmpty()) handle(VoiceCommands.parse(heard))
        else speaker.speak("Sizi duyamadım. Düğmeye tekrar basıp konuşabilirsiniz.")
    }
    fun listen() {
        speaker.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Sorunuzu söyleyin")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        try { recognizer.launch(intent) }
        catch (_: ActivityNotFoundException) { speaker.speak("Bu telefonda sesli komut kullanılamıyor.") }
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Namaz Vakti", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(todaySentence(today).removePrefix("Bugün ").removeSuffix(".").replaceFirstChar { it.uppercase() },
                fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
                onClick = { speaker.speak(times?.let { nextPrayerSentence(it, nowLocal) } ?: noTimes) },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkEmerald, contentColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        day.city == null -> {
                            Text("Şehrinizi seçin", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("Vakitleri gösterebilmem için gerekli.", fontSize = 20.sp, color = OnDarkMuted,
                                textAlign = TextAlign.Center)
                        }
                        times == null && !day.failed -> Text("Vakitler yükleniyor…", fontSize = 26.sp)
                        times == null -> {
                            Text("Vakitlere ulaşılamadı", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("İnternet bağlantısını kontrol edin.", fontSize = 20.sp, color = OnDarkMuted)
                        }
                        next == null -> {
                            Text("Bugünün son vakti girdi", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center)
                            Text("Sıradaki vakit yarın sabah", fontSize = 20.sp, color = OnDarkMuted)
                        }
                        else -> {
                            Text("SIRADAKİ VAKİT", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(spokenPrayer(next.key), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text(next.value.format(shortTime), fontSize = 64.sp, lineHeight = 70.sp,
                                fontWeight = FontWeight.Bold, color = Gold)
                            Text("${remainingText(nowLocal, next.value)} kaldı", fontSize = 24.sp)
                        }
                    }
                    if (times != null) Text("🔊 Dokunun, sesli söyleyeyim", fontSize = 18.sp, color = OnDarkMuted,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        if (day.city == null) item {
            CityPicker("", !savingCity) { chosen ->
                ReminderRefresh.saveCity(context, chosen)
                PrayerRefreshWork.refreshNow(context)
                speaker.speak("$chosen seçildi.")
            }
        }
        if (day.failed) item { BigButton("↻", "Tekrar dene", { retry++ }) }
        if (remindersOn && reliability.issues > 0) item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4D6))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val missed = reliability.report.missed
                    Text(
                        if (missed.isNotEmpty()) "⚠ ${missed.joinToString(", ")} bildirimi gelmedi"
                        else "⚠ Vakit bildirimleri gecikebilir",
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text("Telefonun birkaç ayarını açmak gerekiyor. Bir yakınınızdan yardım isteyebilirsiniz.",
                        fontSize = 19.sp, lineHeight = 26.sp)
                    BigButton("🔧", "Bildirimleri düzelt", { open("reliability") }, container = DarkEmerald)
                }
            }
        }
        item { BigButton("🎤", "Konuşarak sor", ::listen, container = Gold, content = DarkEmerald) }
        item { BigButton("🕌", "Bugünün vakitleri", { open("times") }) }
        item { BigButton("📿", "Tespih çek", { open("dhikr") }) }
        item { BigButton("🤲", "Dua dinle", { open("duas") }) }
        item { BigButton("✅", "Namazımı kıldım", { speaker.speak(markCurrent()) }) }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bugün kıldığınız namaz: $prayedCount / 5", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    lastMarked?.let { marked ->
                        Text("$marked namazı kaydedildi.", fontSize = 20.sp, color = Emerald)
                        OutlinedButton(onClick = {
                            WorshipStore.setCompleted(context, today, marked, false)
                            lastMarked = null
                            speaker.speak("$marked namazı kaydı geri alındı.")
                        }, modifier = Modifier.heightIn(min = 56.dp)) { Text("Yanlışlıkla bastım, geri al", fontSize = 20.sp) }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { showHelp = true; speaker.speak(EASY_HELP) }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Nasıl kullanılır?", fontSize = 20.sp)
                }
                TextButton(onClick = { confirmFull = true }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Tüm özellikler", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showHelp) {
        var autoSpeak by remember { mutableStateOf(AppMode.autoSpeak(context)) }
        AlertDialog(
            onDismissRequest = { showHelp = false; speaker.stop() },
            title = { Text("Nasıl kullanılır?", fontSize = 26.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(EASY_HELP, fontSize = 20.sp, lineHeight = 28.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Açılışta vakti sesli söyle", Modifier.weight(1f), fontSize = 19.sp)
                        Switch(autoSpeak, { autoSpeak = it; AppMode.setAutoSpeak(context, it) })
                    }
                    TextButton(onClick = { showHelp = false; speaker.stop(); AppMode.setOnboarded(context, false) }) {
                        Text("Tanıtımı baştan izle", fontSize = 19.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHelp = false; speaker.stop() }, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text("Anladım", fontSize = 20.sp)
                }
            }
        )
    }
    if (confirmFull) AlertDialog(
        onDismissRequest = { confirmFull = false },
        title = { Text("Tüm özellikler açılsın mı?", fontSize = 24.sp) },
        text = { Text("Daha fazla menü ve ayar görünür. İstediğiniz zaman Ayarlar’dan sade kullanıma dönebilirsiniz.",
            fontSize = 19.sp, lineHeight = 26.sp) },
        confirmButton = { TextButton(onClick = { confirmFull = false; AppMode.setEasy(context, false) }) {
            Text("Evet, aç", fontSize = 19.sp)
        } },
        dismissButton = { TextButton(onClick = { confirmFull = false }) { Text("Vazgeç", fontSize = 19.sp) } }
    )
}

@Composable
private fun EasyTimes(speaker: Speaker, onBack: () -> Unit) {
    val now = currentIstanbulTime()
    val nowLocal = now.toLocalDateTime()
    var retry by remember { mutableIntStateOf(0) }
    val day = rememberPrayerDay(now.toLocalDate(), retry)
    val times = day.times
    val nextKey = times?.entries?.firstOrNull { it.value > nowLocal }?.key
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EasyHeader("Bugünün vakitleri", onBack) }
        when {
            times != null -> {
                item {
                    BigButton("🔊", "Hepsini sesli oku", {
                        speaker.speak("${day.city} için bugünün vakitleri. " + times.entries.joinToString(" ") {
                            "${spokenPrayer(it.key)}, ${it.value.format(shortTime)}."
                        })
                    }, container = Gold, content = DarkEmerald)
                }
                items(times.entries.toList()) { (name, time) ->
                    val isNext = name == nextKey
                    val passed = !isNext && time <= nowLocal
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (isNext) Gold else Color.White, RoundedCornerShape(20.dp))
                            .clickable { speaker.speak("${spokenPrayer(name)}, saat ${time.format(shortTime)}.") }
                            .padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = if (passed) MaterialTheme.colorScheme.onSurfaceVariant else DarkEmerald
                        Column(Modifier.weight(1f)) {
                            Text(spokenPrayer(name), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = color)
                            if (isNext) Text("Sıradaki vakit", fontSize = 18.sp, color = DarkEmerald)
                            else if (passed) Text("Geçti", fontSize = 18.sp, color = color)
                        }
                        Text(time.format(shortTime), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
                item {
                    Text("${day.city} · Pilot vakit verisi", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            day.failed -> item {
                Text("Vakitlere ulaşılamadı. İnternet bağlantısını kontrol edin.", fontSize = 22.sp)
                Spacer(Modifier.height(12.dp))
                BigButton("↻", "Tekrar dene", { retry++ })
            }
            day.city == null -> item { Text("Önce ana ekrandan şehrinizi seçin.", fontSize = 22.sp) }
            else -> item { Text("Vakitler yükleniyor…", fontSize = 22.sp) }
        }
    }
}

private val numberWords = mapOf(33 to "Otuz üç", 66 to "Altmış altı", 99 to "Doksan dokuz", 100 to "Yüz")

@Composable
private fun EasyDhikr(speaker: Speaker, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selected by rememberSaveable { mutableStateOf(WorshipStore.phrases.first()) }
    val today = currentIstanbulTime().toLocalDate()
    val revision = preferenceRevision(WorshipStore.STORE)
    val count = remember(today, selected, revision) { WorshipStore.count(context, selected) }
    val target = remember(revision) { WorshipStore.target(context) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val beads = remember { Animatable(count.toFloat()) }
    LaunchedEffect(count) {
        if (abs(count - beads.value) > 3f) beads.snapTo(count.toFloat())
        else beads.animateTo(count.toFloat(), spring(dampingRatio = 0.72f, stiffness = 320f))
    }
    fun advance() {
        val next = WorshipStore.increment(context, selected)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when {
            next == target -> speaker.speak("${numberWords[next] ?: next}. Hedefe ulaştınız. Allah kabul etsin.")
            next % 33 == 0 -> speaker.speak(numberWords[next] ?: "$next")
        }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { EasyHeader("Tespih", onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WorshipStore.phrases.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { phrase ->
                            val isSelected = phrase == selected
                            val choose = { selected = phrase; speaker.speak(phrase) }
                            val buttonModifier = Modifier.weight(1f).heightIn(min = 64.dp)
                            if (isSelected) Button(onClick = choose, modifier = buttonModifier, shape = RoundedCornerShape(18.dp)) {
                                Text(phrase, fontSize = 19.sp, textAlign = TextAlign.Center)
                            } else OutlinedButton(onClick = choose, modifier = buttonModifier, shape = RoundedCornerShape(18.dp)) {
                                Text(phrase, fontSize = 19.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold, color = Emerald)
                Text("Hedef: $target", fontSize = 22.sp)
                if (count >= target) Text("Hedef tamamlandı", fontSize = 22.sp, color = Emerald, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(360.dp)
                    .semantics { contentDescription = "Tespih. Saymak için dokunun."; role = Role.Button }
                    .pointerInput(selected) { detectTapGestures { advance() } },
                contentAlignment = Alignment.Center
            ) {
                TespihCanvas(beads.value, 0f, Modifier.size(width = 280.dp, height = 360.dp))
            }
        }
        item { BigButton("📿", "Bir tane çek", ::advance) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { speaker.speak("$selected, $count") },
                    modifier = Modifier.weight(1f).heightIn(min = 60.dp)) { Text("🔊 Sayıyı söyle", fontSize = 19.sp) }
                OutlinedButton(onClick = { confirmReset = true }, enabled = count > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 60.dp)) { Text("Sıfırla", fontSize = 19.sp) }
            }
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Sayaç sıfırlansın mı?", fontSize = 24.sp) },
        text = { Text("Bugünkü $selected sayısı sıfırlanır.", fontSize = 20.sp) },
        confirmButton = { TextButton(onClick = {
            WorshipStore.reset(context, selected); confirmReset = false; speaker.speak("Sayaç sıfırlandı.")
        }) { Text("Sıfırla", fontSize = 19.sp) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç", fontSize = 19.sp) } }
    )
}

@Composable
private fun EasyDuas(speaker: Speaker, onBack: () -> Unit) {
    var playing by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(speaker.speaking) { if (!speaker.speaking) playing = null }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { EasyHeader("Dualar", onBack) }
        if (speaker.unavailable) item {
            Text("Bu telefonda Türkçe sesli okuma açık değil. Telefon ayarlarından Türkçe ses paketini yükleyebilirsiniz.",
                fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
        }
        items(Duas.all, key = { it.id }) { dua ->
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(dua.title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = DarkEmerald)
                    dua.arabic?.let {
                        Text(it, Modifier.fillMaxWidth(), fontSize = 26.sp, lineHeight = 40.sp, textAlign = TextAlign.End)
                    }
                    Text(dua.meaning, fontSize = 21.sp, lineHeight = 30.sp)
                    val isPlaying = playing == dua.id
                    BigButton(if (isPlaying) "■" else "🔊", if (isPlaying) "Durdur" else "Dinle", {
                        if (isPlaying) { speaker.stop(); playing = null }
                        else { speaker.speak("${dua.title}. ${dua.meaning}"); playing = dua.id }
                    }, container = if (isPlaying) DarkEmerald else Emerald)
                }
            }
        }
    }
}

@Composable
private fun EasyReliability(speaker: Speaker, onBack: () -> Unit) {
    val context = LocalContext.current
    val state = rememberReliability()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var testScheduled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        speaker.speak(if (state.issues == 0) "Bildirim ayarlarınız hazır."
            else "Bildirimlerin tam vaktinde gelmesi için sarı satırlardaki düğmelere sırayla basın.")
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { EasyHeader("Bildirim ayarları", onBack) }
        item {
            ReliabilityChecklist(state, large = true) {
                if (android.os.Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        item {
            BigButton("🔔", "Sesli deneme yap", {
                ReminderNotifications.scheduleTest(context)
                testScheduled = true
                speaker.speak("Bir dakika sonra deneme bildirimi gelecek. Ekranı kapatıp bekleyebilirsiniz.")
            })
            if (testScheduled) Text("Bir dakika içinde “Bu bir denemedir” diye konuşmalı. Konuşmazsa ayarları tekrar kontrol edin.",
                fontSize = 18.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
