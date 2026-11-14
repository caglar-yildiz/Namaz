package com.namaz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { NamazHome() } }
    }
}

@Composable
fun NamazHome() {
    Scaffold(topBar = { Surface(tonalElevation = 3.dp) { Text("Namaz", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.headlineMedium) } }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
