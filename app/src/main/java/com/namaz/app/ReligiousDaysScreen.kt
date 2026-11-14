package com.namaz.app
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ReligiousDay(val date: LocalDate, val name: String, val description: String)
object ReligiousDaysData {
    // Pilot-only entry; verify against official calendar before publishing.
    val days = listOf(ReligiousDay(LocalDate.of(2026,12,10),
        "Üç Ayların Başlangıcı ve Regaib Kandili",
        "Pilot kayıt. Resmî tarih doğrulaması bekleniyor."))
}
private val forest = Color(0xFF103E36)
private val emerald = Color(0xFF14594B)
private val gold = Color(0xFFD7B56D)
private val cream = Color(0xFFF8F6F0)
private val mint = Color(0xFFE8F2EB)

@Composable
fun ReligiousDaysScreen(days: List<ReligiousDay> = ReligiousDaysData.days) {
    val today = LocalDate.now()
    val locale = remember { Locale.forLanguageTag("tr-TR") }
    val fmt = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val monthFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<ReligiousDay?>(null) }
    val upcoming = days.filter { !it.date.isBefore(today) }.sortedBy { it.date }
    val next = upcoming.firstOrNull()
    val monthEvents = days.filter { YearMonth.from(it.date) == month }.sortedBy { it.date }

    LazyColumn(Modifier.fillMaxSize().background(cream),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Dini Günler", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Kandiller, bayramlar ve özel tarihler",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = forest)) {
                Column(Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("☾  YAKLAŞAN ÖZEL GÜN", color = gold,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(next?.name ?: "Yeni tarihler hazırlanıyor",
                        color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(next?.date?.format(fmt) ?: "Takvim verileri yakında eklenecek.",
                        color = Color(0xFFDDEAE4))
                    if (next != null) {
                        Surface(color = gold, shape = RoundedCornerShape(12.dp)) {
                            Text(if (next.date == today) "Bugün" else
                                ChronoUnit.DAYS.between(today, next.date).toString() + " gün kaldı",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                color = forest, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(month.format(monthFmt).replaceFirstChar { it.titlecase(locale) },
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row {
                            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                            TextButton(onClick = { month = YearMonth.from(today) }) { Text("Bugün") }
                            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Pzt","Sal","Çar","Per","Cum","Cmt","Paz").forEach { label ->
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
                                val number = week * 7 + weekday - offset + 1
                                Box(Modifier.weight(1f).height(46.dp), contentAlignment = Alignment.Center) {
                                    if (number in 1..month.lengthOfMonth()) {
                                        val date = month.atDay(number)
                                        val event = days.firstOrNull { it.date == date }
                                        val bg = when {
                                            event != null -> emerald
                                            date == today -> mint
                                            else -> Color.Transparent
                                        }
                                        Box(Modifier.size(38.dp)
                                            .background(bg, RoundedCornerShape(12.dp))
                                            .clickable(enabled = event != null) { selected = event },
                                            contentAlignment = Alignment.Center) {
                                            Text(number.toString() + if (event != null) "•" else "",
                                                color = if (event != null) Color.White else forest,
                                                fontWeight = if (event != null || date == today)
                                                    FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text("Yeşil: özel gün    Açık yeşil: bugün",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Bu aydaki özel günler", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold) }
        if (monthEvents.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text("Bu ay için henüz kayıt bulunmuyor. Takvim tamamlanmadığından bu, özel gün olmadığı anlamına gelmez.",
                        Modifier.fillMaxWidth().padding(18.dp))
                }
            }
        } else {
            items(monthEvents) { event -> ReligiousEventCard(event, fmt) { selected = event } }
        }
        item { Text("Yaklaşan tarihler", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold) }
        if (upcoming.isEmpty()) {
            item { Text("Yeni tarih kayıtları bekleniyor.") }
        } else {
            items(upcoming) { event -> ReligiousEventCard(event, fmt) { selected = event } }
        }
        item {
            Text("Pilot takvim: Resmî tarihler doğrulanmadan kesin bilgi veya bildirim taahhüdü verilmez.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    selected?.let { event ->
        AlertDialog(onDismissRequest = { selected = null },
            title = { Text(event.name) },
            text = { Text(event.date.format(fmt) + "\n\n" + event.description) },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Kapat") } })
    }
}

@Composable
private fun ReligiousEventCard(event: ReligiousDay, fmt: DateTimeFormatter, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = mint, shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text("☾", color = emerald, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                Text(event.date.format(fmt), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", color = emerald, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
