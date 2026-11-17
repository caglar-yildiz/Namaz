package com.namaz.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DhikrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("namaz_display", 0) }
    var vibrate by remember { mutableStateOf(prefs.getBoolean("dhikr_haptics", false)) }
    var selected by rememberSaveable { mutableStateOf(WorshipStore.phrases.first()) }
    val today = currentIstanbulTime().toLocalDate()
    val revision = preferenceRevision(WorshipStore.STORE)
    val count = remember(today, selected, revision) { WorshipStore.count(context, selected) }
    val target = remember(revision) { WorshipStore.target(context) }
    val fraction = (count.toFloat() / target).coerceIn(0f, 1f)
    val progress by animateFloatAsState(fraction, tween(220), label = "dhikr-progress")
    val bead by animateFloatAsState(count.toFloat(), tween(220), label = "dhikr-bead")
    val green = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.primaryContainer
    var reset by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { InnerHeader("Zikirmatik", onBack) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val inset = 14.dp.toPx()
                        val radius = size.minDimension / 2 - inset
                        val arc = Size(size.width - inset * 2, size.height - inset * 2)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(5.dp.toPx()))
                        drawArc(green, -90f, 360 * progress, false, Offset(inset, inset), arc,
                            style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                        for (point in 0 until 24) {
                            val angle = point * PI / 12 - PI / 2
                            drawCircle(if (point / 24f <= progress) green else track, 4.dp.toPx(),
                                center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius))
                        }
                        val angle = bead / target * PI * 2 - PI / 2
                        drawCircle(gold, 8.dp.toPx(), center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$count", style = MaterialTheme.typography.displayMedium)
                        Text("Hedef: $target")
                    }
                }
                Text(selected, style = MaterialTheme.typography.titleMedium)
                if (count >= target) Text("Günlük hedef tamamlandı", color = green)
            }
        }
        item {
            Button(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), onClick = {
                WorshipStore.increment(context, selected)
                if (vibrate) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }) { Text("+1 Zikir", style = MaterialTheme.typography.titleMedium) }
        }
        item {
            Text("Zikir seçimi", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorshipStore.phrases.forEach { phrase ->
                    FilterChip(selected == phrase, { selected = phrase }, { Text(phrase) })
                }
            }
        }
        item {
            Text("Günlük hedef", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { goal ->
                    FilterChip(goal == target, { WorshipStore.setTarget(context, goal) }, { Text("$goal") })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dokunmada titreşim", Modifier.weight(1f))
                Switch(vibrate, { vibrate = it; prefs.edit().putBoolean("dhikr_haptics", it).apply() })
            }
            TextButton(onClick = { reset = true }, enabled = count > 0) { Text("Bugünkü sayacı sıfırla") }
        }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("Sayacı sıfırla?") },
        text = { Text("Bugünkü $selected sayısı sıfırlanır.") },
        confirmButton = { TextButton(onClick = { WorshipStore.reset(context, selected); reset = false }) { Text("Sıfırla") } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text("Vazgeç") } })
}

private val kindnessIdeas = listOf(
    "Bir yakınını ara ve hâlini sor.",
    "Birine, yaptığı bir şey için içtenlikle teşekkür et.",
    "Yardıma ihtiyacı olan birine zaman ayır.",
    "Kullanmadığın temiz bir eşyayı ihtiyaç sahibine ulaştır.",
    "Bir komşuna alışverişte yardım teklif et.",
    "Bir arkadaşına güzel bir anınızı hatırlatan mesaj yaz.",
    "Bugün birine içten bir iltifat et.",
    "Yaşlı bir yakınını ziyaret et veya ara.",
    "Sokaktaki bir hayvan için temiz su bırak.",
    "Bir arkadaşının derdini bölmeden dinle.",
    "Birine öğrendiğin yararlı bir bilgiyi öğret.",
    "Çevrende gördüğün çöpleri uygun şekilde topla.",
    "Birine teşekkür mesajı gönder.",
    "Bir aile üyesinin işini kolaylaştır.",
    "İhtiyaç sahibi için küçük bir bağış yap.",
    "Birine kitap öner veya kitap hediye et.",
    "Gönüllü bir çalışmaya katılma fırsatı araştır.",
    "Bugün birine yol göstererek yardımcı ol.",
    "Bir arkadaşına destekleyici bir mesaj yaz.",
    "Toplu taşımada ihtiyaç sahibine yer ver.",
    "İsrafı azaltmak için kullanmadığın ışıkları kapat.",
    "Birinden özür dilemen gerekiyorsa iletişime geç.",
    "Birinin başarısını içtenlikle kutla.",
    "Bir iyilik yap ve karşılık bekleme."
)

@Composable
internal fun KindnessWheel(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val sweep = 360f / kindnessIdeas.size
    fun landing(index: Int) = (360f - (index + 0.5f) * sweep) % 360f
    val angle = remember { Animatable(if (selected < 0) 0f else landing(selected)) }
    var spinning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = with(MaterialTheme.colorScheme) { listOf(primary, secondary, tertiaryContainer, primaryContainer, tertiary, surfaceVariant) }
    val arrow = MaterialTheme.colorScheme.onSurface
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { InnerHeader("İyilik Çarkı", onBack) }
        item { Text("${kindnessIdeas.size} farklı iyilik önerisinden birini keşfet.") }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(230.dp)) {
                    val inset = 12.dp.toPx()
                    val wheel = Size(size.width - 2 * inset, size.height - 2 * inset)
                    kindnessIdeas.indices.forEach { index ->
                        drawArc(colors[index % colors.size], -90f + index * sweep + angle.value, sweep, true, Offset(inset, inset), wheel)
                    }
                    drawLine(arrow, Offset(center.x, 0f), Offset(center.x, 22.dp.toPx()), 5.dp.toPx(), StrokeCap.Round)
                }
            }
        }
        item {
            Button(enabled = !spinning, modifier = Modifier.fillMaxWidth(), onClick = {
                spinning = true
                val picked = kindnessIdeas.indices.filter { it != selected }.random()
                val remaining = (landing(picked) - angle.value % 360f + 360f) % 360f
                scope.launch {
                    angle.animateTo(angle.value + 1080f + remaining, tween(1150, easing = FastOutSlowInEasing))
                    selected = picked
                    spinning = false
                }
            }) { Text("Çarkı çevir") }
        }
        if (selected >= 0 && !spinning) item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(kindnessIdeas[selected], Modifier.padding(20.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
