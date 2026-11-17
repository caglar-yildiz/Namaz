package com.namaz.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

internal object TurkishCities {
    val all = listOf("Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin", "Aydın", "Balıkesir", "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum", "Denizli", "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Isparta", "Mersin", "İstanbul", "İzmir", "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir", "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla", "Muş", "Nevşehir", "Niğde", "Ordu", "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas", "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Şanlıurfa", "Uşak", "Van", "Yozgat", "Zonguldak", "Aksaray", "Bayburt", "Karaman", "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan", "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye", "Düzce")
}

@Composable
internal fun CityPicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedButton(onClick = { query = ""; open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(if (selected.isBlank()) "İl seç" else "İl: $selected  ▾")
    }
    if (open) {
        AlertDialog(onDismissRequest = { open = false }, title = { Text("İl seç") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(query, { query = it }, label = { Text("İl ara") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    val matches = TurkishCities.all.filter {
                        it.lowercase(Locale.forLanguageTag("tr-TR")).contains(query.trim().lowercase(Locale.forLanguageTag("tr-TR")))
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(matches) { city ->
                            TextButton(onClick = { onSelect(city); open = false }, modifier = Modifier.fillMaxWidth()) {
                                Text(city)
                            }
                        }
                        if (matches.isEmpty()) item { Text("Eşleşen il bulunamadı.") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Kapat") } })
    }
}
