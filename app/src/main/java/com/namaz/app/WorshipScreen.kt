package com.namaz.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
        item { ScreenTitle("İbadet", "Günlük kayıtların, rutinlerin ve öğrenme araçların") }
        item {
            SectionTitle("Bugünkü kayıtlar") {
                Text("${completed.size} / 5", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator({ completed.size / 5f },
                Modifier.fillMaxWidth().padding(vertical = 8.dp).height(6.dp),
                trackColor = MaterialTheme.colorScheme.primaryContainer, drawStopIndicator = {})
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
            SectionTitle("Kişisel rutinler")
            RoutineMarks(today, showWeek = true)
            TextButton(onClick = onNewRoutine) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Rutin ekle")
            }
        }
        item { SectionTitle("Öğren ve uygula") }
        item { DashboardCard("Zikirmatik", "Zikrini ve günlük hedefini seç", "◎") { open("dhikr") } }
        item { DashboardCard("Kur’an Ezberi", "Ayetleri dinle, gizle ve tekrar et", "☾") { open("memorize") } }
        item { DashboardCard("Dualar", "Günlük dualar, özel günler ve favoriler", "◇") { open("duas") } }
        item { DashboardCard("Namazı Öğren", "Beş vakit için adım adım sesli prova", "▷") { open("guide") } }
        item { DashboardCard("Abdest Almayı Öğren", "Abdestin farzları ve adım adım uygulanışı", "◉") { open("ablution") } }
    }
}
