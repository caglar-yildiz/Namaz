package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun WorshipHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val today = currentIstanbulTime().toLocalDate()
    val thisMonth = YearMonth.from(today)
    var monthKey by rememberSaveable { mutableStateOf(thisMonth.toString()) }
    var selectedKey by rememberSaveable { mutableStateOf(today.toString()) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    val month = minOf(YearMonth.parse(monthKey), thisMonth)
    val selected = minOf(LocalDate.parse(selectedKey), today)
    val journalRevision = preferenceRevision(WorshipStore.STORE)
    val routineRevision = preferenceRevision(RoutineStore.NAME)
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val week = (0L..6L).map(weekStart::plusDays)
    val monthDates = (1..month.lengthOfMonth()).map(month::atDay)
    val visible = if (monthly) List(month.atDay(1).dayOfWeek.value - 1) { null } + monthDates else week
    val dayLabel = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", turkish) }
    val monthLabel = remember { DateTimeFormatter.ofPattern("MMMM yyyy", turkish) }
    val counts = remember(journalRevision, routineRevision, today, monthly, month) {
        val routines = RoutineStore.all(context)
        visible.filterNotNull().filter { it <= today }.associateWith { date ->
            val prayerCount = WorshipStore.prayers.count { WorshipStore.completed(context, date, it) }
            val routineCount = routines.count { RoutineStore.completed(context, it, date) }
            prayerCount to routineCount
        }
    }
    val marked = remember(journalRevision, selected) {
        WorshipStore.prayers.filter { WorshipStore.completed(context, selected, it) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { InnerHeader("İbadet geçmişi", onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!monthly, { monthly = false; selectedKey = today.toString() }, { Text("Bu hafta") })
                FilterChip(monthly, { monthly = true; monthKey = YearMonth.from(selected).toString() }, { Text("Aylık") })
            }
        }
        if (monthly) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    val previous = month.minusMonths(1)
                    monthKey = previous.toString(); selectedKey = previous.atDay(1).toString()
                }) { Text("Önceki") }
                Text(month.atDay(1).format(monthLabel))
                TextButton(enabled = month < thisMonth, onClick = {
                    val next = month.plusMonths(1)
                    monthKey = next.toString(); selectedKey = next.atDay(1).toString()
                }) { Text("Sonraki") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { label ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                visible.chunked(7).forEach { days ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        (days + List(7 - days.size) { null }).forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f)) else {
                                val count = counts[date]
                                val label = buildString {
                                    append(date.format(dayLabel))
                                    if (count != null) append(", ${count.first} namaz, ${count.second} rutin")
                                }
                                Surface(Modifier.weight(1f).heightIn(min = 52.dp)
                                    .semantics { contentDescription = label }
                                    .selectable(selected == date, enabled = count != null, role = Role.Button,
                                        onClick = { selectedKey = date.toString() }),
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        date == selected -> MaterialTheme.colorScheme.primary
                                        count != null && count.first + count.second > 0 -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = if (date == selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant) {
                                    Column(Modifier.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center) {
                                        Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelMedium)
                                        if (count != null) Text("${count.first}/5", style = MaterialTheme.typography.labelSmall)
                                        if (count != null && count.second > 0) {
                                            Text("• ${count.second}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text("5 üzerinden namaz kaydı · Nokta tamamlanan rutinleri gösterir.", style = MaterialTheme.typography.bodySmall)
        }
        item {
            HorizontalDivider()
            Text(selected.format(dayLabel), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
            Text("Namaz kayıtları", style = MaterialTheme.typography.titleMedium)
        }
        item {
            WorshipStore.prayers.forEach { prayer ->
                MarkRow("$prayer namazı", prayer in marked) { WorshipStore.setCompleted(context, selected, prayer, it) }
            }
        }
        item {
            Text("Kişisel rutinler", style = MaterialTheme.typography.titleMedium)
            RoutineMarks(selected)
        }
    }
}
