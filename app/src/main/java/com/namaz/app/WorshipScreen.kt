package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun WorshipScreen(open: (String) -> Unit, onNewRoutine: () -> Unit) {
    val context = LocalContext.current
    val today = currentIstanbulTime().toLocalDate()
    val revision = preferenceRevision(WorshipStore.STORE)
    val completed = remember(today, revision) {
        WorshipStore.prayers.filter { WorshipStore.completed(context, today, it) }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("İbadet", style = MaterialTheme.typography.headlineLarge) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bugünkü kayıtlar", style = MaterialTheme.typography.titleLarge)
                Text("${completed.size} / 5", color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator({ completed.size / 5f }, Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    WorshipStore.prayers.forEach { prayer ->
                        MarkRow("$prayer namazı", prayer in completed) { WorshipStore.setCompleted(context, today, prayer, it) }
                    }
                }
            }
            TextButton(onClick = { open("history") }) { Text("Haftalık ve aylık geçmiş") }
        }
        item {
            Text("Kişisel rutinler", style = MaterialTheme.typography.titleLarge)
            RoutineMarks(today, showWeek = true)
            TextButton(onClick = onNewRoutine) { Text("Rutin ekle") }
        }
        item { Text("Öğren ve uygula", style = MaterialTheme.typography.titleLarge) }
        item { DashboardCard("Zikirmatik", "Zikrini ve günlük hedefini seç", "◎") { open("dhikr") } }
        item { DashboardCard("Kur’an Ezberi", "Ayetleri dinle, gizle ve tekrar et", "☾") { open("memorize") } }
        item { DashboardCard("Dualar", "Günlük dualar, özel günler ve favoriler", "◇") { open("duas") } }
        item { DashboardCard("Namazı Öğren", "Beş vakit için adım adım sesli prova", "▷") { open("guide") } }
    }
}
