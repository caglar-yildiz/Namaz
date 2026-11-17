package com.namaz.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal val turkish = Locale.forLanguageTag("tr-TR")
internal val istanbul = ZoneId.of("Europe/Istanbul")
internal val shortTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", turkish)

@Composable
internal fun currentIstanbulTime(): ZonedDateTime {
    val owner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(ZonedDateTime.now(istanbul)) }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val tick = ZonedDateTime.now(istanbul)
                now = tick
                delay(60_000L - tick.second * 1000L - tick.nano / 1_000_000L)
            }
        }
    }
    return now
}

@Composable
internal fun preferenceRevision(vararg names: String): Int {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(context, *names) {
        val stores = names.map { context.getSharedPreferences(it, 0) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        stores.forEach { it.registerOnSharedPreferenceChangeListener(listener) }
        onDispose { stores.forEach { it.unregisterOnSharedPreferenceChangeListener(listener) } }
    }
    return revision
}

@Composable
internal fun MarkRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).toggleable(checked, role = Role.Checkbox, onValueChange = onChange)
        .padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(checked, onCheckedChange = null)
    }
}

@Composable
internal fun RoutineMarks(date: LocalDate, showWeek: Boolean = false, onEdit: ((Routine) -> Unit)? = null) {
    val context = LocalContext.current
    val revision = preferenceRevision(RoutineStore.NAME)
    val routines = remember(context, date, revision) { RoutineStore.forDay(context, date) }
    if (routines.isEmpty()) Text("Bu gün için rutin yok.", style = MaterialTheme.typography.bodyMedium)
    routines.forEach { routine ->
        key(routine.id, revision) {
            val detail = if (showWeek) {
                val week = "Bu hafta ${RoutineStore.weekCount(context, routine, date)} gün"
                val streak = RoutineStore.streak(context, routine, date)
                if (streak >= 2) "$week · $streak tekrar üst üste" else week
            } else null
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    MarkRow(routine.title, RoutineStore.completed(context, routine, date), detail) {
                        RoutineStore.mark(context, routine, date, it)
                    }
                }
                if (onEdit != null) TextButton(onClick = { onEdit(routine) }) { Text("Düzenle") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RoutineEditor(routine: Routine, isNew: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable(routine.id) { mutableStateOf(routine.title) }
    var time by rememberSaveable(routine.id) { mutableStateOf(routine.reminder?.toString()) }
    var dayMask by rememberSaveable(routine.id) { mutableIntStateOf(routine.days.sumOf { 1 shl it.ordinal }) }
    var archive by rememberSaveable(routine.id) { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onClose() }
    val selectedTime = time?.let(LocalTime::parse)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (isNew) "Rutin ekle" else "Rutini düzenle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("Rutin adı") }, singleLine = true)
                Text("Tekrar günleri", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val bit = 1 shl day.ordinal
                        FilterChip(dayMask and bit != 0, { dayMask = dayMask xor bit },
                            { Text(day.getDisplayName(TextStyle.SHORT, turkish)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hatırlatma", Modifier.weight(1f))
                    Switch(selectedTime != null, { time = if (it) "21:00" else null })
                }
                if (selectedTime != null) {
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute -> time = LocalTime.of(hour, minute).toString() },
                            selectedTime.hour, selectedTime.minute, true).show()
                    }) { Text("Saat ${selectedTime.format(shortTime)}") }
                    Text("Bildirim seçilen günlerde yaklaşık bu saatte gelir.", style = MaterialTheme.typography.bodySmall)
                }
                if (!isNew) TextButton(onClick = { archive = true }) { Text("Rutini arşivle") }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && dayMask != 0, onClick = {
                val days = DayOfWeek.entries.filter { dayMask and (1 shl it.ordinal) != 0 }.toSet()
                RoutineStore.save(context, routine.copy(title = title.trim(), days = days, reminder = selectedTime))
                if (selectedTime != null && Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.canNotify(context)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else onClose()
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Vazgeç") } }
    )
    if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text("Rutin arşivlensin mi?") },
        text = { Text("Hatırlatmalar durur. Tamamladığın günler geçmişte kalır.") },
        confirmButton = { TextButton(onClick = {
            RoutineStore.save(context, routine.copy(archivedOn = WorshipStore.today(), reminder = null))
            onClose()
        }) { Text("Arşivle") } },
        dismissButton = { TextButton(onClick = { archive = false }) { Text("Vazgeç") } })
}
