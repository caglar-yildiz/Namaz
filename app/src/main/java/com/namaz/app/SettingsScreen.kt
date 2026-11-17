package com.namaz.app

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsScreen(onReminders: () -> Unit) {
    val context = LocalContext.current
    val revision = preferenceRevision("prayer_reminder_preferences")
    val selectedSound = remember(revision) { ReminderPreferences.sound(context) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ayarlar", style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
        item { Text("Bildirim sesi", style = MaterialTheme.typography.titleLarge) }
        listOf("silent" to "Sessiz", "soft" to "Hafif kısa ton", "default" to "Standart ses", "vibrate" to "Titreşim").forEach { (key, label) ->
            item {
                val selected = selectedSound == key
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton,
                    onClick = { ReminderPreferences.setSound(context, key) }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
                    Text(label)
                }
            }
        }
        item { HorizontalDivider() }
        item { DashboardCard("Hatırlatıcıları yönet", "Kategoriler, saatler ve kişisel rutinler", "◷", onReminders) }
        item { OutlinedButton(onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
        }) { Text("Android bildirim ayarları") } }
        item { Text("Namaz kayıtların, zikir sayın ve rutinlerin bu cihazda saklanır.", style = MaterialTheme.typography.bodySmall) }
    }
}
