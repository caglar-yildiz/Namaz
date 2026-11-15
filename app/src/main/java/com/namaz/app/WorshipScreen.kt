package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.SharedPreferences
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.ZoneId

@Composable
fun WorshipScreen() {
    val context = LocalContext.current
    var currentDay by remember { mutableStateOf(WorshipStore.today()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentDay = WorshipStore.today()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(currentDay) {
        val now = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"))
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1000L) + 250L)
        currentDay = WorshipStore.today()
    }
    var showHistory by remember { mutableStateOf(false) }
    var showMemorization by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("worship_journal_v1", android.content.Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    if (showMemorization) {
        QuranMemorizationScreen(onBack = { showMemorization = false })
        return
    }
    if (showHistory) {
        Column {
            TextButton(onClick = { showHistory = false }) { Text("← İbadet ekranına dön") }
            WorshipHistoryScreen()
        }
        return
    }
    val date = currentDay
    val checked = remember(date, revision) {
        mutableStateMapOf<String, Boolean>().apply {
            WorshipStore.prayers.forEach { put(it, WorshipStore.completed(context, date, it)) }
        }
    }
    var selectedPhrase by remember { mutableStateOf(WorshipStore.phrases.first()) }
    var count by remember(date, selectedPhrase, revision) {
        mutableIntStateOf(WorshipStore.count(context, selectedPhrase))
    }
    var target by remember { mutableIntStateOf(WorshipStore.target(context)) }
    var confirmReset by remember { mutableStateOf(false) }
    val completedCount = WorshipStore.prayers.count { checked[it] == true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("İbadet", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR"))),
                style = MaterialTheme.typography.titleMedium)
        }
        item {
            Card(
                onClick = { showMemorization = true },
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF103E36)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("☾  KUR'AN EZBERİ", color = androidx.compose.ui.graphics.Color(0xFFD7B56D),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Ezber Asistanım", color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineSmall)
                    Text("10 kısa sure • Ayet bazlı dinleme • Akıllı tekrar",
                        color = androidx.compose.ui.graphics.Color(0xFFE5EFE9))
                    Button(onClick = { showMemorization = true }) { Text("Ezbere başla") }
                }
            }
        }
        item { Text("Günlük namaz takibi", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedButton(onClick = { showHistory = true }) { Text("Haftalık ve aylık geçmiş") } }
        item {
            LinearProgressIndicator(
                progress = { completedCount / 5f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("$completedCount / 5 namaz işaretlendi")
        }
        WorshipStore.prayers.forEach { prayer ->
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked[prayer] == true, onCheckedChange = { value ->
                        checked[prayer] = value
                        WorshipStore.setCompleted(context, date, prayer, value)
                    })
                    Text(prayer)
                }
            }
        }
        item { HorizontalDivider() }
        item { Text("Zikirmatik", style = MaterialTheme.typography.titleLarge) }
        item {
            Text("Zikir seçimi")
            WorshipStore.phrases.forEach { phrase ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedPhrase == phrase, onClick = {
                        selectedPhrase = phrase
                    })
                    Text(phrase)
                }
            }
        }
        item {
            Text("Günlük hedef")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { value ->
                    FilterChip(
                        selected = target == value,
                        onClick = {
                            target = value
                            WorshipStore.setTarget(context, value)
                        },
                        label = { Text("$value") }
                    )
                }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Text("$count", style = MaterialTheme.typography.displayLarge)
                Text("Hedef: $target")
                LinearProgressIndicator(
                    progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    count = WorshipStore.increment(context, selectedPhrase)
                }, modifier = Modifier.fillMaxWidth()) { Text("+1 Zikir") }
                TextButton(onClick = { confirmReset = true }) { Text("Bugünkü sayacı sıfırla") }
            }
        }
        item {
            Text("Namaz işaretleri ve zikir sayıları yalnızca bu cihazda saklanır. " +
                "Zikir sayacı her yeni gün için ayrı tutulur; önceki kayıtlar silinmez.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Sayacı sıfırla?") },
            text = { Text("Bugünkü $selectedPhrase sayısı sıfırlanacak.") },
            confirmButton = {
                TextButton(onClick = {
                    WorshipStore.reset(context, selectedPhrase)
                    count = 0
                    confirmReset = false
                }) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") }
            }
        )
    }
}
