package com.namaz.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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

class MainActivity : ComponentActivity() {
    private var opening by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Cream.toArgb(), Cream.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb())
        )
        super.onCreate(savedInstanceState)
        RoutineAlarm.refresh(this, replace = false)
        if (savedInstanceState == null) readDestination(intent)
        setContent { NamazTheme { NamazApp(opening) { opening = null } } }
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

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Ana Sayfa", Icons.Filled.Home), DAYS("Dini Günler", Icons.Filled.DateRange),
    REMINDERS("Hatırlatıcı", Icons.Filled.Notifications), WORSHIP("İbadet", Icons.Filled.Favorite),
    SETTINGS("Ayarlar", Icons.Filled.Settings)
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
        NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
            Tab.entries.forEach { item ->
                NavigationBarItem(selected = tab == item, onClick = { select(item) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald, selectedTextColor = Emerald,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ))
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
                    route == "ablution" -> AblutionGuideScreen(::back)
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
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
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
            ChevronIcon()
        }
    }
}

@Composable
internal fun InnerHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) { BackIcon() }
        Text(title, Modifier.weight(1f).offset(x = (-8).dp), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
