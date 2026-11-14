package com.namaz.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var testStatus by remember { mutableStateOf("") }
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var prayerCategoryEnabled by remember { mutableStateOf(ReminderPreferences.enabled(context, "Namaz vakitleri")) }
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
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.canNotify(context)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    testStatus = "Bildirim izni istendi. İzin verdikten sonra test düğmesine tekrar basın."
                } else {
                    ReminderNotifications.scheduleTest(context)
                    testStatus = "Yaklaşık bir dakika içinde test bildirimi planlandı."
                }
            }) { Text("Test bildirimi planla") }
            if (testStatus.isNotEmpty()) Text(testStatus)
        }
        item {
            Text(
                "Namaz vakitleri için bildirim tercihlerini belirleyin. " +
                    "Gerçek bildirimler, güvenilir vakit verisi ve Android izinleri " +
                    "entegre edildiğinde çalışacaktır."
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Namaz vakti bildirimleri", modifier = Modifier.weight(1f))
                Switch(checked = prayerCategoryEnabled, onCheckedChange = {
                    prayerCategoryEnabled = it
                    ReminderPreferences.setEnabled(context, "Namaz vakitleri", it)
                })
            }
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
                        enabled = prayerCategoryEnabled,
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
                "Not: Namaz vakti verisi henüz bağlı değil; gerçek vakitler için alarm veya " +
                    "bildirim planlanmaz. Test bildirimi gerçek namaz vakti değildir.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
