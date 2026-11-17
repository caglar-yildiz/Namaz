package com.namaz.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
internal fun NamazHome(
    onEvent: (String) -> Unit, onVakit: () -> Unit, onJournal: () -> Unit,
    onTool: (String) -> Unit, onWheel: () -> Unit, onNewRoutine: () -> Unit,
    onEditRoutine: (Routine) -> Unit, onReminders: () -> Unit
) {
    val context = LocalContext.current
    val now = currentIstanbulTime()
    val today = now.toLocalDate()
    val journalRevision = preferenceRevision(WorshipStore.STORE)
    val prayerRevision = preferenceRevision("prayer_reminder_preferences")
    val city = remember(prayerRevision) { ReminderRefresh.city(context) }
    val marked = remember(today, journalRevision) {
        WorshipStore.prayers.count { WorshipStore.completed(context, today, it) }
    }
    val due = pendingMemorization(today)
    val event = remember(today) {
        ReligiousDaysData.days.filter { ChronoUnit.DAYS.between(today, it.date) in 0..14 }.minByOrNull { it.date }
    }
    val format = remember { DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", turkish) }
    var retry by remember { mutableIntStateOf(0) }
    val times by key(city, today, retry) {
        produceState<Result<Map<String, LocalDateTime>>?>(null) {
            if (city != null) value = withContext(Dispatchers.IO) {
                try { Result.success(PrayerTimesService.forDisplay(context, today, city)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { Result.failure(failure) }
            }
        }
    }
    val dailyTimes = times?.getOrNull()
    val nextToday = dailyTimes?.entries?.firstOrNull { it.value > now.toLocalDateTime() }
    val tomorrow = today.plusDays(1)
    val firstTomorrow by produceState<LocalDateTime?>(null, city, tomorrow, dailyTimes != null && nextToday == null, retry) {
        value = null
        if (city != null && dailyTimes != null && nextToday == null) value = withContext(Dispatchers.IO) {
            try { PrayerTimesService.forDisplay(context, tomorrow, city)["İmsak"] }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }
    }
    val nextTime = nextToday?.value ?: firstTomorrow
    val nextLabel = nextToday?.key ?: "Yarın İmsak"

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenTitle("Bugün", today.format(format).replaceFirstChar { it.titlecase(turkish) })
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = DarkEmerald, contentColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NAMAZ VAKİTLERİ", color = Gold, style = MaterialTheme.typography.labelLarge)
                    when {
                        city == null -> Text("Şehrini seç", style = MaterialTheme.typography.headlineSmall)
                        times == null -> Text("$city için vakitler yükleniyor…")
                        dailyTimes == null -> Text("$city için vakitlere ulaşılamadı.")
                        else -> {
                            if (nextTime != null) {
                                Text("$nextLabel · ${nextTime.format(shortTime)}", style = MaterialTheme.typography.headlineSmall)
                                val minutes = (Duration.between(now.toLocalDateTime(), nextTime).seconds / 60).coerceAtLeast(0)
                                Text(when {
                                    minutes == 0L -> "Bir dakikadan az kaldı"
                                    minutes < 60L -> "$minutes dakika kaldı"
                                    else -> "${minutes / 60} saat ${minutes % 60} dakika kaldı"
                                })
                            } else Text("Bugünün son vakti geçti.", style = MaterialTheme.typography.titleLarge)
                            HorizontalDivider(color = Color(0xFF48756A))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                dailyTimes.forEach { (name, time) ->
                                    val isNext = name == nextToday?.key
                                    val passed = !isNext && time <= now.toLocalDateTime()
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .background(if (isNext) Gold.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val color = when { isNext -> Gold; passed -> OnDarkMuted.copy(alpha = 0.6f); else -> Color.White }
                                        val weight = if (isNext) FontWeight.Bold else FontWeight.Normal
                                        Text(name, color = color, fontWeight = weight)
                                        Text(time.format(shortTime), color = color, fontWeight = weight)
                                    }
                                }
                            }
                            Text("$city · Pilot vakit verisi (AlAdhan)", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                        }
                    }
                    Row {
                        TextButton(onClick = onVakit, colors = ButtonDefaults.textButtonColors(contentColor = Gold)) {
                            Text(if (city == null) "Şehir seç" else "Şehir ve vakit ayarları")
                        }
                        if (times?.isFailure == true) TextButton(onClick = { retry++ },
                            colors = ButtonDefaults.textButtonColors(contentColor = Gold)) { Text("Tekrar dene") }
                    }
                }
            }
        }
        item { DashboardCard("Hatırlatıcılar", "Namaz, dini günler ve rutin bildirimleri", "◷", onReminders) }
        item { SectionTitle("Günlük özet") }
        item { DashboardCard("Namaz takibim", "$marked / 5 namaz işaretlendi", "✓", onJournal) }
        if (due > 0) item { DashboardCard("Kur’an Ezberi", "$due ayet tekrar bekliyor", "☾") { onTool("memorize") } }
        item {
            SectionTitle("Rutinlerim")
            RoutineMarks(today, showWeek = true, onEdit = onEditRoutine)
            TextButton(onClick = onNewRoutine) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Rutin ekle")
            }
        }
        event?.let { next -> item {
            DashboardCard(next.name, next.date.format(format), "☾") { onEvent(next.date.toString()) }
        } }
        item { SectionTitle("Keşfet") }
        item { DashboardCard("Dualar", "Günlük hayat ve özel günler için", "◇") { onTool("duas") } }
        item { DashboardCard("İyilik Çarkı", "Bugün yapabileceğin bir iyilik seç", "✦", onWheel) }
    }
}
