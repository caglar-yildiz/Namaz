package com.namaz.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.TextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemindersScreen(group: String, onGroup: (String) -> Unit, onEdit: (Routine) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prayer_reminder_preferences", 0) }
    val revision = preferenceRevision("prayer_reminder_preferences")
    val routineRevision = preferenceRevision(RoutineStore.NAME)
    val routines = remember(routineRevision) { RoutineStore.all(context).filter { it.archivedOn == null } }
    val scope = rememberCoroutineScope()
    var city by rememberSaveable { mutableStateOf(ReminderRefresh.city(context).orEmpty()) }
    var syncStatus by rememberSaveable { mutableStateOf("") }
    var savingCity by remember { mutableStateOf(false) }
    var testStatus by rememberSaveable { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(ReminderNotifications.canNotify(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }
    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.canNotify(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val prayerEnabled = remember(revision) { ReminderPreferences.enabled(context, "Namaz vakitleri") }
    val daysEnabled = remember(revision) { ReminderPreferences.enabled(context, "Kandil ve bayramlar") }
    val journalEnabled = remember(revision) { ReminderPreferences.enabled(context, "İbadet takibi") }
    val enabled = remember(revision) { ReminderPreferences.prayers.associateWith { prefs.getBoolean(it, false) } }
    val lead = remember(revision) { prefs.getInt("lead_minutes", 0) }
    val dayLead = remember(revision) { ReligiousDayReminders.leadDays(context) }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Hatırlatıcılar", "Namaz, dini günler ve rutin bildirimleri") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Namaz vakitleri", "Dini günler", "Kişisel rutinler").forEach { title ->
                    FilterChip(group == title, { onGroup(title) }, { Text(title) })
                }
            }
        }
        if (!permissionGranted && Build.VERSION.SDK_INT >= 33) item {
            TextButton(onClick = { requestPermission() }) { Text("Bildirim iznini aç") }
        }
        when (group) {
            "Namaz vakitleri" -> {
                item {
                    CityPicker(city, !savingCity) { city = it; syncStatus = "" }
                    Button(enabled = city in TurkishCities.all && !savingCity, onClick = {
                        val chosen = city.trim()
                        savingCity = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { PrayerTimesService.forDisplay(context, WorshipStore.today(), chosen) }
                                ReminderRefresh.saveCity(context, chosen)
                                PrayerRefreshWork.refreshNow(context)
                                syncStatus = "$chosen kaydedildi. Vakitler Ana Sayfa’da görünür."
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { syncStatus = "Vakitler alınamadı. Şehir adını ve bağlantını kontrol et." }
                            finally { savingCity = false }
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) { Text(if (savingCity) "Vakitler alınıyor…" else "Şehri kaydet") }
                    if (syncStatus.isNotEmpty()) Text(syncStatus)
                    Text("Kaynak: AlAdhan pilot verisi. Resmî Diyanet vakitleriyle doğrulanmadı.",
                        style = MaterialTheme.typography.bodySmall)
                }
                item { HorizontalDivider() }
                item { ReminderToggle("Namaz vakti bildirimleri", prayerEnabled) { value ->
                    ReminderPreferences.setEnabled(context, "Namaz vakitleri", value)
                    if (value) { PrayerRefreshWork.refreshNow(context); requestPermission() }
                    else PrayerTimesService.cancelAll(context)
                } }
                enabled.forEach { (name, set) -> item {
                    ReminderToggle(name, set, prayerEnabled) {
                        prefs.edit().putBoolean(name, it).apply()
                        PrayerRefreshWork.refreshNow(context)
                    }
                } }
                item {
                    Text("Ne kadar önce?", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 5, 10, 15, 30).forEach { min ->
                            FilterChip(lead == min, {
                                prefs.edit().putInt("lead_minutes", min).apply()
                                PrayerRefreshWork.refreshNow(context)
                            }, { Text(if (min == 0) "Vaktinde" else "$min dk önce") }, enabled = prayerEnabled)
                        }
                    }
                }
            }
            "Dini günler" -> item {
                ReminderToggle("Kandil ve bayramları hatırlat", daysEnabled) {
                    ReminderPreferences.setEnabled(context, "Kandil ve bayramlar", it)
                    ReligiousDayReminders.reconcile(context)
                    if (it) requestPermission()
                }
                Text("Bildirim yaklaşık 09.00’da gelir.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Bir gün önce", 0 to "Aynı gün").forEach { (days, label) ->
                        FilterChip(dayLead == days, { ReligiousDayReminders.setLeadDays(context, days) },
                            { Text(label) }, enabled = daysEnabled)
                    }
                }
            }
            else -> {
                item {
                    ReminderToggle("İbadet günlüğünü hatırlat", journalEnabled) {
                        ReminderPreferences.setEnabled(context, "İbadet takibi", it)
                        WorshipReminder.reconcile(context)
                        if (it) requestPermission()
                    }
                    Text("Namaz kayıtların eksikse yaklaşık 21.00’da bir kontrol hatırlatması gelir.",
                        style = MaterialTheme.typography.bodySmall)
                }
                routines.forEach { routine -> item(key = routine.id) {
                    val days = if (routine.days.size == 7) "Her gün" else routine.days.sorted().joinToString(", ") {
                        it.getDisplayName(TextStyle.SHORT, turkish)
                    }
                    val time = routine.reminder?.format(shortTime)?.let { "$days · $it" } ?: "$days · Hatırlatma kapalı"
                    DashboardCard(routine.title, time, "◷") { onEdit(routine) }
                } }
                item { Button(onClick = { onEdit(Routine(title = "")) }) { Text("Rutin ekle") } }
            }
        }
        item { HorizontalDivider() }
        item {
            OutlinedButton(onClick = {
                if (ReminderNotifications.canNotify(context)) {
                    ReminderNotifications.scheduleTest(context)
                    testStatus = "Test bildirimi yaklaşık bir dakika içinde gelir."
                } else requestPermission()
            }) { Text("Test bildirimi planla") }
            if (testStatus.isNotBlank()) Text(testStatus, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReminderToggle(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange, enabled = enabled)
    }
}
