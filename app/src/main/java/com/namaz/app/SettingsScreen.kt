package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    // Read again when the screen is reopened to reflect changes from Reminders.
    val enabled = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ReminderPreferences.categories.forEach { put(it, ReminderPreferences.enabled(context, it)) }
        }
    }
    var selectedSound by remember { mutableStateOf(ReminderPreferences.sound(context)) }
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
                            enabled[category] = it
                            ReminderPreferences.setEnabled(context, category, it)
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
                        selectedSound = key
                        ReminderPreferences.setSound(context, key)
                    })
                    Text(label, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        item {
            Text("Bu tercihler kaydedilir. Ses ve gerçek bildirim gönderimi, Android bildirim kanalları ve izinleri bağlandığında etkinleşir.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
