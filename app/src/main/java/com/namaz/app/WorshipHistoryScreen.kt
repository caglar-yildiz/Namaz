package com.namaz.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.ZoneId

/** History and corrections for past days; no future-date editing. */
@Composable
fun WorshipHistoryScreen() {
    val context = LocalContext.current
    var today by remember { mutableStateOf(WorshipStore.today()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) today = WorshipStore.today()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(today) {
        val now = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"))
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1000L) + 250L)
        today = WorshipStore.today()
    }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("worship_journal_v1", android.content.Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(today) {
        if (selected.isAfter(today)) selected = today
        if (month.isAfter(YearMonth.from(today))) month = YearMonth.from(today)
    }
    val format = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR")) }
    val startOfWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val week = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val monthDays = (1..month.lengthOfMonth()).map { month.atDay(it) }
    // Read revision during composition to refresh persisted values after edits.
    val refreshMarker = revision
    val cells: List<LocalDate?> = List(month.atDay(1).dayOfWeek.value - 1) { null } + monthDays
    val weekDates = week.filter { !it.isAfter(today) }
    val monthDates = monthDays.filter { !it.isAfter(today) }
    val weeklyTotal = weekDates.sumOf { day -> WorshipStore.prayers.count { WorshipStore.completed(context, day, it) } }
    val monthlyTotal = monthDates.sumOf { day -> WorshipStore.prayers.count { WorshipStore.completed(context, day, it) } }

    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Namaz geçmişi", style = MaterialTheme.typography.titleLarge) }
        item { Text("Bu hafta", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val completed = WorshipStore.prayers.count {
                        WorshipStore.completed(context, date, it)
                    }
                    FilterChip(
                        selected = selected == date,
                        enabled = !date.isAfter(today),
                        onClick = { selected = date },
                        label = { Text("${date.dayOfMonth}\n$completed/5") },
                        modifier = Modifier.width(58.dp)
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { month = month.minusMonths(1) }) { Text("Önceki") }
                Text(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR"))))
                TextButton(enabled = month.isBefore(YearMonth.from(today)),
                    onClick = { month = month.plusMonths(1) }) { Text("Sonraki") }
            }
        }
        item { Text("Aylık kayıtlar (gün / tamamlanan namaz)", style = MaterialTheme.typography.titleMedium) }
        item {
            // Header and all calendar rows share one scroll position and identical cell widths.
            Column(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Pzt","Sal","Çar","Per","Cum","Cmt","Paz").forEach { label ->
                        Text(label, modifier = Modifier.width(58.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                cells.chunked(7).forEach { days ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (days + List(7 - days.size) { null }).forEach { date ->
                            if (date == null) {
                                Spacer(Modifier.width(58.dp))
                            } else {
                                val completed = WorshipStore.prayers.count {
                                    WorshipStore.completed(context, date, it)
                                }
                                OutlinedButton(
                                    onClick = { selected = date },
                                    enabled = !date.isAfter(today),
                                    contentPadding = PaddingValues(2.dp),
                                    modifier = Modifier.width(58.dp)
                                ) { Text("${date.dayOfMonth}\\n$completed/5") }
                            }
                        }
                    }
                }
            }
        }
        item { Text("Bu hafta: $weeklyTotal / ${weekDates.size * 5} işaret")
            Text("Seçilen ay: $monthlyTotal / ${monthDates.size * 5} işaret")
            Text("İşaretlenmeyen kayıtlar kılınmadı anlamına gelmez.", style = MaterialTheme.typography.bodySmall)
        }
        item { HorizontalDivider() }
        item { Text("Seçilen gün: ${selected.format(format)}", style = MaterialTheme.typography.titleMedium) }
        WorshipStore.prayers.forEach { prayer ->
            item {
                val done = WorshipStore.completed(context, selected, prayer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = done, onCheckedChange = { value ->
                        WorshipStore.setCompleted(context, selected, prayer, value)
                        revision++
                    })
                    Text(prayer)
                }
            }
        }
        item {
            val days = (0..29).map { today.minusDays(it.toLong()) }
            val total = days.sumOf { day ->
                WorshipStore.prayers.count { WorshipStore.completed(context, day, it) }
            }
            Text("Son 30 gün: $total / 150 işaretlenmiş namaz",
                style = MaterialTheme.typography.titleMedium)
            Text("İşaretlenmeyen kayıtlar, kılınmadığı anlamına gelmez.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
