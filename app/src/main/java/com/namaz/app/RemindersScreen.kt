package com.namaz.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val prayerNames = listOf("İmsak", "Öğle", "İkindi", "Akşam", "Yatsı")
private const val PREFS_NAME = "prayer_reminder_preferences"

@Composable
fun RemindersScreen() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val enabled = remember {
        mutableStateMapOf<String, Boolean>().apply {
            prayerNames.forEach { name -> put(name, prefs.getBoolean(name, false)) }
        }
    }
    var leadMinutes by remember {
        mutableIntStateOf(prefs.getInt("lead_minutes", 0))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Hatırlatıcılar", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(
                "Namaz vakitleri için bildirim tercihlerini belirleyin. " +
                    "Gerçek bildirimler, güvenilir vakit verisi ve Android izinleri " +
                    "entegre edildiğinde çalışacaktır."
            )
        }
        item { Text("Namaz vakitleri", style = MaterialTheme.typography.titleLarge) }
        prayerNames.forEach { name ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled[name] == true,
                        onCheckedChange = { value ->
                            enabled[name] = value
                            prefs.edit().putBoolean(name, value).apply()
                        }
                    )
                }
            }
        }
        item { HorizontalDivider() }
        item { Text("Ne kadar önce hatırlatılsın?", style = MaterialTheme.typography.titleMedium) }
        item {
            listOf(0, 5, 10, 15, 30).forEach { minutes ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = leadMinutes == minutes,
                        onClick = {
                            leadMinutes = minutes
                            prefs.edit().putInt("lead_minutes", minutes).apply()
                        }
                    )
                    Text(
                        if (minutes == 0) "Vakit geldiğinde" else "$minutes dakika önce",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        item {
            Text(
                "Not: Bu ekranda yalnızca tercihler kaydedilir; henüz alarm veya " +
                    "bildirim planlanmaz.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
