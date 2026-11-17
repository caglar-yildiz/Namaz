package com.namaz.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val Emerald = Color(0xFF14594B)
internal val DarkEmerald = Color(0xFF103E36)
internal val Gold = Color(0xFFD7B56D)
private val Cream = Color(0xFFF8F6F0)
private val Ink = Color(0xFF203B34)

private val NamazColors = lightColorScheme(
    primary = Emerald, onPrimary = Color.White, secondary = Gold, onSecondary = DarkEmerald,
    background = Cream, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE8F0EB), onSurfaceVariant = DarkEmerald,
    primaryContainer = Color(0xFFDDEDE5), onPrimaryContainer = DarkEmerald, outline = Color(0xFFB8C8BE)
)

class MainActivity : ComponentActivity() {
    private var opening by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoutineAlarm.refresh(this, replace = false)
        if (savedInstanceState == null) readDestination(intent)
        setContent { MaterialTheme(colorScheme = NamazColors) { NamazApp(opening) { opening = null } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDestination(intent)
    }

    private fun readDestination(intent: Intent) {
        val uri = intent.data?.takeIf { it.scheme == "namaz" && it.host == "open" } ?: return
        opening = uri.lastPathSegment
    }
}

private enum class Tab(val label: String, val symbol: String) {
    HOME("Ana Sayfa", "⌂"), DAYS("Dini Günler", "▦"), REMINDERS("Hatırlatıcı", "◷"),
    WORSHIP("İbadet", "◎"), VIDEOS("Videolar", "▶"), SETTINGS("Ayarlar", "⚙")
}

@Composable
private fun NamazApp(opening: String?, consumeOpening: () -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var routes by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var reminderGroup by rememberSaveable { mutableStateOf("Namaz vakitleri") }
    var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editPrayer by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationRoutine by rememberSaveable { mutableStateOf<String?>(null) }
    val holder = rememberSaveableStateHolder()

    fun select(next: Tab) { routes.forEach(holder::removeState); routes = emptyList(); tab = next }
    fun open(route: String) { routes = routes + route }
    fun back() {
        val last = routes.lastOrNull() ?: return
        holder.removeState(last)
        routes = routes.dropLast(1)
    }
    fun edit(routine: Routine = Routine(title = "")) {
        editId = routine.id
        editTitle = routine.title
        editPrayer = routine.prayerId
    }
    LaunchedEffect(opening) {
        when {
            opening == null -> Unit
            opening.startsWith("routine:") -> { select(Tab.HOME); notificationRoutine = opening.substringAfter(':') }
            opening == "worship" -> select(Tab.WORSHIP)
            opening.startsWith("day:") -> { selectedDay = opening.substringAfter(':'); select(Tab.DAYS) }
        }
        if (opening != null) consumeOpening()
    }
    BackHandler(routes.isNotEmpty() || tab != Tab.HOME) {
        if (routes.isNotEmpty()) back() else select(Tab.HOME)
    }

    Scaffold(containerColor = Cream, bottomBar = {
        NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
            Tab.entries.forEach { item ->
                NavigationBarItem(selected = tab == item, onClick = { select(item) },
                    icon = { Text(item.symbol, fontSize = 22.sp) },
                    label = { Text(item.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
        }
    }) { padding ->
        val route = routes.lastOrNull()
        Box(Modifier.fillMaxSize().padding(padding)) {
            holder.SaveableStateProvider(route ?: tab.name) {
                when {
                    route == "history" -> WorshipHistoryScreen(::back)
                    route == "memorize" -> QuranMemorizationScreen(::back)
                    route == "dhikr" -> DhikrScreen(::back)
                    route == "guide" -> PrayerGuideScreen(::back)
                    route == "wheel" -> KindnessWheel(::back)
                    route?.startsWith("dua:") == true -> DuaDetail(route.substringAfter(':'), ::back, ::edit)
                    route?.startsWith("duas") == true -> DuaLibrary(route.substringAfter(':', "").ifEmpty { null }, ::back) { open("dua:$it") }
                    else -> when (tab) {
                        Tab.HOME -> NamazHome(
                            onEvent = { selectedDay = it; select(Tab.DAYS) },
                            onVakit = { reminderGroup = "Namaz vakitleri"; select(Tab.REMINDERS) },
                            onJournal = { select(Tab.WORSHIP) },
                            onTool = ::open,
                            onWheel = { open("wheel") },
                            onNewRoutine = { edit() },
                            onEditRoutine = { edit(it) },
                            onReminders = { reminderGroup = "Namaz vakitleri"; select(Tab.REMINDERS) }
                        )
                        Tab.DAYS -> ReligiousDaysScreen(initialDate = selectedDay, onOpenDuas = ::open,
                            onReminder = { reminderGroup = "Dini günler"; select(Tab.REMINDERS) }, onInitialDateUsed = { selectedDay = null })
                        Tab.REMINDERS -> RemindersScreen(reminderGroup, { reminderGroup = it }, { edit(it) })
                        Tab.WORSHIP -> WorshipScreen(::open) { edit() }
                        Tab.VIDEOS -> VideoFeedScreen()
                        Tab.SETTINGS -> SettingsScreen { reminderGroup = "Namaz vakitleri"; select(Tab.REMINDERS) }
                    }
                }
            }
        }
    }
    editId?.let { id ->
        val existing = RoutineStore.all(context).firstOrNull { it.id == id }
        val routine = existing ?: Routine(id = id, title = editTitle, prayerId = editPrayer)
        RoutineEditor(routine, existing == null) { editId = null }
    }
    notificationRoutine?.let { destination ->
        val routine = RoutineStore.all(context).firstOrNull { it.id == destination.substringBefore(':') }
        val today = currentIstanbulTime().toLocalDate()
        val date = runCatching { LocalDate.parse(destination.substringAfter(':')) }.getOrNull()
        if (routine == null || date == null || date > today || !routine.applies(date)) {
            LaunchedEffect(destination) { notificationRoutine = null }
        } else {
            AlertDialog(onDismissRequest = { notificationRoutine = null }, title = { Text(routine.title) },
                text = { Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", turkish))) },
                confirmButton = { TextButton(onClick = {
                    RoutineStore.mark(context, routine, date, true)
                    notificationRoutine = null
                }) { Text("Tamamlandı olarak işaretle") } },
                dismissButton = { TextButton(onClick = { notificationRoutine = null }) { Text("Kapat") } })
        }
    }
}

@Composable
internal fun DashboardCard(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text(symbol, fontSize = 22.sp, color = Emerald)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", color = Emerald, fontSize = 23.sp)
        }
    }
}

@Composable
internal fun InnerHeader(title: String, onBack: () -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("‹ Geri") }
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    }
}
