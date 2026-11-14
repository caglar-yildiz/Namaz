package com.namaz.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ReligiousDay(val date: LocalDate, val name: String, val description: String)

// Source: https://diyanet.gov.tr/dini-gun-geceler (2026 calendar)
// Only individually verified dates are included. 2027 entries are pending verification.
object ReligiousDaysData {
    val days: List<ReligiousDay> = listOf(
        ReligiousDay(
            LocalDate.of(2026, 12, 10),
            "Üç Ayların Başlangıcı ve Regaib Kandili",
            "Diyanet İşleri Başkanlığı 2026 dini günler takvimine göre."
        )
    )
}

@Composable
fun ReligiousDaysScreen(days: List<ReligiousDay> = ReligiousDaysData.days) {
    val today = remember { LocalDate.now() }
    val locale = remember { Locale.forLanguageTag("tr-TR") }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<ReligiousDay?>(null) }
    val upcoming = remember(days, today) { days.filter { !it.date.isBefore(today) }.sortedBy { it.date } }
    val next = upcoming.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Dini Günler", style = MaterialTheme.typography.headlineMedium) }
        item {
            ElevatedCard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("YAKLAŞAN DİNİ GÜN", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    if (next == null) {
                        Text("Doğrulanmış dini gün takvimi henüz yüklenmedi.")
                    } else {
                        Text(next.name, style = MaterialTheme.typography.titleLarge)
                        Text(next.date.format(formatter))
                        Text("Kalan gün: ${ChronoUnit.DAYS.between(today, next.date)}")
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                    style = MaterialTheme.typography.titleLarge)
                Row {
                    TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                    TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
                }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz").forEach { label ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                val offset = month.atDay(1).dayOfWeek.value - 1
                val cellCount = ((offset + month.lengthOfMonth() + 6) / 7) * 7
                repeat(cellCount / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { weekday ->
                            val day = week * 7 + weekday - offset + 1
                            Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                                if (day in 1..month.lengthOfMonth()) {
                                    val date = month.atDay(day)
                                    val event = days.firstOrNull { it.date == date }
                                    Text(
                                        text = if (event == null) "$day" else "$day •",
                                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.clickable(enabled = event != null) { selected = event }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("Yaklaşan dini günler", style = MaterialTheme.typography.titleLarge) }
        if (upcoming.isEmpty()) {
            item { Text("Dini günler, resmî tarihler doğrulandıktan sonra listelenecek.") }
        } else {
            items(upcoming) { event ->
                ListItem(
                    headlineContent = { Text(event.name) },
                    supportingContent = { Text(event.date.format(formatter)) },
                    modifier = Modifier.clickable { selected = event }
                )
                HorizontalDivider()
            }
        }
    }
    selected?.let { event ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(event.name) },
            text = { Text("${event.date.format(formatter)}\n\n${event.description}") },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Kapat") } }
        )
    }
}
