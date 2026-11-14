package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.SharedPreferences
import android.content.Context

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var preferencesRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefsListenerStore = context.getSharedPreferences("prayer_reminder_preferences", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> preferencesRevision++ }
        prefsListenerStore.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefsListenerStore.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Read again when the screen is reopened to reflect changes from Reminders.
    val enabled = remember(context, preferencesRevision) {
        mutableStateMapOf<String, Boolean>().apply {
            ReminderPreferences.categories.forEach { put(it, ReminderPreferences.enabled(context, it)) }
        }
    }
    val selectedSound = remember(context, preferencesRevision) { ReminderPreferences.sound(context) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Ayarlar", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Bildirim kategorileri", style = MaterialTheme.typography.titleLarge) }
        ReminderPreferences.categories.forEach { category ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled[category] == true,
                        onCheckedChange = {
                            ReminderPreferences.setEnabled(context, category, it)
                            if (category == "İbadet takibi") WorshipReminder.reconcile(context)
                            if (category == "Namaz vakitleri") {
                                if (it) PrayerRefreshWork.refreshNow(context)
                                else PrayerTimesService.cancelAll(context)
                            }
                        }
                    )
                }
            }
        }
        item { HorizontalDivider() }
        item { Text("Bildirim sesi tercihi", style = MaterialTheme.typography.titleLarge) }
        listOf("silent" to "Sessiz", "soft" to "Hafif kısa ton", "default" to "Standart ses", "vibrate" to "Titreşim").forEach { (key, label) ->
            item {
                Row(Modifier.fillMaxWidth()) {
                    RadioButton(selected = selectedSound == key, onClick = {
                        ReminderPreferences.setSound(context, key)
                    })
                    Text(label, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        item {
            Text("İbadet takibi açıksa her akşam yaklaşık 21.00’de günlüğünüzü kontrol etmeniz hatırlatılır. Android bildirim izni gerekir; pil tasarrufu bildirimi geciktirebilir.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
