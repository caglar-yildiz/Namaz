package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class AblutionStep(val title: String, val detail: String)

private val ablutionSteps = listOf(
    AblutionStep("Niyet ve besmele", "Abdest almaya niyet et ve besmele çek. Niyet ve besmele Hanefî mezhebinde sünnettir."),
    AblutionStep("Elleri yıka", "Ellerini bileklere kadar üçer kez yıka. Parmak aralarını temizle."),
    AblutionStep("Ağzına su ver", "Sağ elinle ağzına üç kez su alıp çalkala."),
    AblutionStep("Burnuna su ver", "Burnuna üç kez su çekip sol elinle temizle."),
    AblutionStep("Yüzünü yıka", "Alın saç bitiminden çene altına, bir kulak yumuşağından diğerine kadar yüzünü üç kez yıka. Yüzü bir kez yıkamak farzdır."),
    AblutionStep("Sağ kolunu yıka", "Sağ kolunu dirsek dâhil üç kez yıka. Kuru yer kalmamasına dikkat et."),
    AblutionStep("Sol kolunu yıka", "Sol kolunu dirsek dâhil üç kez yıka."),
    AblutionStep("Başını mesh et", "Islak elinle başının en az dörtte birini mesh et. Başın tamamını mesh etmek sünnettir."),
    AblutionStep("Kulaklarını ve enseni mesh et", "Kulaklarını ıslak parmaklarınla mesh et. Enseni mesh etmek de Hanefî uygulamasında müstehaptır."),
    AblutionStep("Sağ ayağını yıka", "Sağ ayağını topuklarla birlikte üç kez yıka; parmak aralarına su ulaştır."),
    AblutionStep("Sol ayağını yıka", "Sol ayağını topuklarla birlikte üç kez yıka; parmak aralarını temizle.")
)

@Composable
internal fun AblutionGuideScreen(onBack: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = ablutionSteps[index]
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { InnerHeader("Abdest Almayı Öğren", onBack) }
        item {
            Text("Hanefî uygulamasına göre adım adım abdest rehberi.",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Abdestin dört farzı", style = MaterialTheme.typography.titleMedium)
                    Text("1. Yüzü yıkamak\n2. Kolları dirseklerle birlikte yıkamak\n3. Başın en az dörtte birini mesh etmek\n4. Ayakları topuklarla birlikte yıkamak")
                }
            }
        }
        item {
            Text("Adım ${index + 1} / ${ablutionSteps.size}", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator({ (index + 1f) / ablutionSteps.size }, Modifier.fillMaxWidth())
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(step.title, style = MaterialTheme.typography.headlineSmall)
                    Text(step.detail, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { index-- }, enabled = index > 0) { Text("Önceki") }
                Button(onClick = { if (index == ablutionSteps.lastIndex) onBack() else index++ }) {
                    Text(if (index == ablutionSteps.lastIndex) "Bitir" else "Sonraki")
                }
            }
        }
        item {
            Text("Not: Bu rehber Hanefî uygulamasını esas alır; diğer mezheplerde bazı ayrıntılar farklıdır.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
