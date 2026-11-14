package com.namaz.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** History and corrections for past days; no future-date editing. */
@Composable
fun WorshipHistoryScreen() {
    val context = LocalContext.current
    val today = WorshipStore.today()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var revision by remember { mutableIntStateOf(0) }
    val format = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR")) }
    val startOfWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val week = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val monthDays = (1..month.lengthOfMonth()).map { month.atDay(it) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val completed = WorshipStore.prayers.count {
                        WorshipStore.completed(context, date, it)
                    }
                    FilterChip(
                        selected = selected == date,
                        enabled = !date.isAfter(today),
                        onClick = { selected = date },
                        label = { Text("${date.dayOfMonth}\n$completed/5") },
                        modifier = Modifier.weight(1f)
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
        items(cells.chunked(7)) { days ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                days.forEach { date ->
                    if (date == null) { Spacer(Modifier.weight(1f)) } else {
                    val completed = WorshipStore.prayers.count {
                        WorshipStore.completed(context, date, it)
                    }
                    OutlinedButton(
                        onClick = { selected = date },
                        enabled = !date.isAfter(today),
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("${date.dayOfMonth}\n$completed/5") }
                    }
                }
                repeat(7 - days.size) { Spacer(Modifier.weight(1f)) }
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
