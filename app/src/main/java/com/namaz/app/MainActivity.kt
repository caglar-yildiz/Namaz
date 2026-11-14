package com.namaz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { NamazApp() } }
    }
}

private enum class Tab(val label: String, val icon: String) {
    HOME("Ana Sayfa", "⌂"),
    DAYS("Dini Günler", "▦"),
    REMINDERS("Hatırlatıcılar", "◷"),
    WORSHIP("İbadet", "◎"),
    SETTINGS("Ayarlar", "⚙")
}

@Composable
fun NamazApp() {
    var current by remember { mutableStateOf(Tab.HOME) }
    Scaffold(bottomBar = {
        NavigationBar {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = current == tab,
                    onClick = { current = tab },
                    icon = { Text(tab.icon) },
                    label = { Text(tab.label) }
                )
            }
        }
    }) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (current) {
                Tab.HOME -> NamazHome()
                Tab.DAYS -> ReligiousDaysScreen()
                Tab.REMINDERS -> RemindersScreen()
                Tab.WORSHIP -> Placeholder("İbadet")
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Column(Modifier.padding(20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("Bu bölüm sonraki aşamada geliştirilecek.")
    }
}

// Existing home screen content is retained.
@Composable
fun NamazHome() {
    Scaffold(topBar = {
        Surface(tonalElevation = 3.dp) {
            Text("Namaz", modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineMedium)
        }
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Namaz vakitleri", style = MaterialTheme.typography.titleLarge)
            Text("Konum ve güvenilir vakit verisi bağlantısı yakında eklenecek.")
            HorizontalDivider()
            Text("Hatırlatmalar", style = MaterialTheme.typography.titleMedium)
            Text("Bildirim tercihleri yakında burada olacak.")
            HorizontalDivider()
            Text("Dini günler", style = MaterialTheme.typography.titleMedium)
            Text("Kandil ve bayram takvimi yakında eklenecek.")
        }
    }
}
