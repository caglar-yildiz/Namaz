package com.namaz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Emerald = Color(0xFF14594B)
private val DarkEmerald = Color(0xFF103E36)
private val Gold = Color(0xFFD7B56D)
private val Cream = Color(0xFFF8F6F0)
private val Ink = Color(0xFF203B34)

private val NamazColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = DarkEmerald,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8F0EB),
    onSurfaceVariant = DarkEmerald,
    primaryContainer = Color(0xFFDDEDE5),
    onPrimaryContainer = DarkEmerald,
    outline = Color(0xFFB8C8BE)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = NamazColors) { NamazApp() }
        }
    }
}

private enum class Tab(val label: String, val symbol: String) {
    HOME("Ana Sayfa", "⌂"),
    DAYS("Dini Günler", "▦"),
    REMINDERS("Hatırlatıcı", "◷"),
    WORSHIP("İbadet", "◎"),
    SETTINGS("Ayarlar", "⚙")
}

@Composable
fun NamazApp() {
    var current by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab,
                        onClick = { current = tab },
                        icon = {
                            Text(tab.symbol, fontSize = 23.sp,
                                fontWeight = if (current == tab) FontWeight.Bold else FontWeight.Normal)
                        },
                        label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkEmerald,
                            selectedTextColor = DarkEmerald,
                            indicatorColor = Color(0xFFE2F0E7),
                            unselectedIconColor = Color(0xFF6B7E75),
                            unselectedTextColor = Color(0xFF6B7E75)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (current) {
                Tab.HOME -> NamazHome(
                    onDays = { current = Tab.DAYS },
                    onReminders = { current = Tab.REMINDERS },
                    onWorship = { current = Tab.WORSHIP }
                )
                Tab.DAYS -> ReligiousDaysScreen()
                Tab.REMINDERS -> RemindersScreen()
                Tab.WORSHIP -> WorshipScreen()
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    symbol: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE8F2EB)) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Text(symbol, fontSize = 26.sp, color = Emerald)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = Emerald)
        }
    }
}

@Composable
fun NamazHome(
    onDays: () -> Unit = {},
    onReminders: () -> Unit = {},
    onWorship: () -> Unit = {}
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val dateText = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")))
    val completed = WorshipStore.prayers.count { WorshipStore.completed(context, today, it) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Huzurlu bir gün dileriz", style = MaterialTheme.typography.titleMedium,
                    color = Emerald)
                Text("Bugün", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text(dateText, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkEmerald)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("NAMAZ VAKİTLERİ", color = Gold,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Vakit bilgileri hazırlanıyor", color = Color.White,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Konum ve doğrulanmış namaz vakti verileri bağlandığında burada günün vakitlerini görebileceksiniz.",
                        color = Color(0xFFDDEAE4), style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(color = Color(0xFF48756A))
                    Text("Bildirim tercihlerini şimdi ayarlayabilirsiniz.",
                        color = Color.White, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = onReminders,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold)
                    ) { Text("Hatırlatıcıları aç") }
                }
            }
        }
        item {
            Text("Günlük özet", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onWorship),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Namaz takibim", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("$completed / 5", color = Emerald, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { completed / 5f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Emerald,
                        trackColor = Color(0xFFE7EEE8)
                    )
                    Text("Günlük kayıtlarını görüntüle ve güncelle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("Keşfet", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        item {
            DashboardCard("Dini günler", "Kandiller ve özel günler takvimi", "☾", onDays)
        }
        item {
            DashboardCard("Hatırlatıcılar", "Bildirimleri ve vakit tercihlerini yönet", "◷", onReminders)
        }
        item {
            DashboardCard("İbadet günlüğüm", "Namaz takibi ve zikirmatik", "◎", onWorship)
        }
        item {
            Text("Vakit ve dini gün verileri resmî kaynaklarla doğrulanmadan kesin bilgi olarak sunulmaz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start)
        }
    }
}
