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
    // Official Diyanet 2026/2027 religious days lists.
    // https://vakithesaplama.diyanet.gov.tr/dinigunler.php?yil=2026
    // https://vakithesaplama.diyanet.gov.tr/icerik.php?icerik=154
    val days = listOf(
        ReligiousDay(LocalDate.of(2026, 1, 15), "Miraç Kandili", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 2, 2), "Berat Kandili", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 2, 19), "Ramazan Başlangıcı", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 3, 16), "Kadir Gecesi", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 3, 19), "Ramazan Bayramı Arefesi", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 3, 20), "Ramazan Bayramı 1. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 3, 21), "Ramazan Bayramı 2. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 3, 22), "Ramazan Bayramı 3. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 5, 26), "Kurban Bayramı Arefesi", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 5, 27), "Kurban Bayramı 1. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 5, 28), "Kurban Bayramı 2. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 5, 29), "Kurban Bayramı 3. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 5, 30), "Kurban Bayramı 4. Gün", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 6, 16), "Hicri Yılbaşı", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 6, 25), "Aşure Günü", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 8, 24), "Mevlid Kandili", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 12, 10), "Üç Ayların Başlangıcı", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2026, 12, 10), "Regaib Kandili", "Diyanet İşleri Başkanlığı 2026 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 1, 4), "Miraç Kandili", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 1, 22), "Berat Kandili", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 2, 8), "Ramazan Başlangıcı", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 3, 5), "Kadir Gecesi", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 3, 8), "Ramazan Bayramı Arefesi", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 3, 9), "Ramazan Bayramı 1. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 3, 10), "Ramazan Bayramı 2. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 3, 11), "Ramazan Bayramı 3. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 5, 15), "Kurban Bayramı Arefesi", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 5, 16), "Kurban Bayramı 1. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 5, 17), "Kurban Bayramı 2. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 5, 18), "Kurban Bayramı 3. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 5, 19), "Kurban Bayramı 4. Gün", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 6, 6), "Hicri Yılbaşı", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 6, 15), "Aşure Günü", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 8, 13), "Mevlid Kandili", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 11, 29), "Üç Ayların Başlangıcı", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 12, 2), "Regaib Kandili", "Diyanet İşleri Başkanlığı 2027 dini günler listesi."),
        ReligiousDay(LocalDate.of(2027, 12, 24), "Miraç Kandili", "Diyanet İşleri Başkanlığı 2027 dini günler listesi.")
    )
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
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
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
                                        val hasEvent = days.any { it.date == date }
                                        val bg = when {
                                            hasEvent -> emerald
                                            date == today -> mint
                                            else -> Color.Transparent
                                        }
                                        Box(Modifier.size(38.dp)
                                            .background(bg, RoundedCornerShape(12.dp))
                                            .clickable(enabled = hasEvent) { selectedDate = date },
                                            contentAlignment = Alignment.Center) {
                                            Text(number.toString() + if (hasEvent) "•" else "",
                                                color = if (hasEvent) Color.White else forest,
                                                fontWeight = if (hasEvent || date == today)
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
                    Text("Bu ay için listelenen özel gün bulunmuyor.",
                        Modifier.fillMaxWidth().padding(18.dp))
                }
            }
        } else {
            items(monthEvents) { event -> ReligiousEventCard(event, fmt) { selectedDate = event.date } }
        }
        item { Text("Yaklaşan tarihler", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold) }
        if (upcoming.isEmpty()) {
            item { Text("Yeni tarih kayıtları bekleniyor.") }
        } else {
            items(upcoming) { event -> ReligiousEventCard(event, fmt) { selectedDate = event.date } }
        }
        item {
            Text("Kaynak: Diyanet İşleri Başkanlığı 2026–2027 dini günler listeleri. Tarihler güncellenebilir; bildirim entegrasyonu ayrıca test edilmelidir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    selectedDate?.let { date ->
        val eventsOnDate = days.filter { it.date == date }
        AlertDialog(onDismissRequest = { selectedDate = null },
            title = { Text(date.format(fmt)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                eventsOnDate.forEach { event ->
                    Text(event.name, fontWeight = FontWeight.Bold)
                    Text(event.description, style = MaterialTheme.typography.bodySmall)
                }
            } },
            confirmButton = { TextButton(onClick = { selectedDate = null }) { Text("Kapat") } })
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
