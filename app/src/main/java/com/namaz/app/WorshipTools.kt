package com.namaz.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DhikrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("namaz_display", 0) }
    var vibrate by remember { mutableStateOf(prefs.getBoolean("dhikr_haptics", true)) }
    var selected by rememberSaveable { mutableStateOf(WorshipStore.phrases.first()) }
    val today = currentIstanbulTime().toLocalDate()
    val revision = preferenceRevision(WorshipStore.STORE)
    val count = remember(today, selected, revision) { WorshipStore.count(context, selected) }
    val target = remember(revision) { WorshipStore.target(context) }
    val fraction = (count.toFloat() / target).coerceIn(0f, 1f)
    val progress by animateFloatAsState(fraction, tween(220), label = "dhikr-progress")
    val beadPosition = remember { Animatable(count.toFloat()) }
    LaunchedEffect(count) {
        // Small steps slide bead by bead; large jumps (reset, phrase change) snap instantly.
        if (abs(count - beadPosition.value) > 3f) beadPosition.snapTo(count.toFloat())
        else beadPosition.animateTo(count.toFloat(), spring(dampingRatio = 0.72f, stiffness = 320f))
    }
    val swing = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }
    DisposableEffect(tone) { onDispose { tone.release() } }
    fun advance() {
        WorshipStore.increment(context, selected)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 28)
        if (vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            swing.animateTo(3.5f, tween(90))
            swing.animateTo(0f, spring(dampingRatio = 0.28f, stiffness = 120f))
        }
    }
    val green = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.primaryContainer
    var reset by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { InnerHeader("Zikirmatik", onBack) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tespihe dokun ya da bir taneyi aşağı çek", style = MaterialTheme.typography.bodySmall)
                Box(
                    Modifier.size(width = 280.dp, height = 360.dp)
                        .pointerInput(selected) { detectTapGestures { advance() } }
                        .pointerInput(selected) {
                            var consumed = false
                            detectDragGestures(
                                onDragStart = { consumed = false },
                                onDragEnd = { consumed = false },
                                onDragCancel = { consumed = false }
                            ) { change, _ ->
                                change.consume()
                                if (!consumed) {
                                    consumed = true
                                    advance()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TespihCanvas(beadPosition.value, swing.value, Modifier.fillMaxSize())
                    Column(
                        Modifier.align(Alignment.TopCenter).padding(top = 92.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$count", style = MaterialTheme.typography.displayMedium)
                        Text("Hedef: $target", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(selected, style = MaterialTheme.typography.titleMedium)
                if (count >= target) Text("Günlük hedef tamamlandı", color = green)
            }
        }
        item {
            Button(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), onClick = {
                advance()
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
                Text("Titreşim", Modifier.weight(1f))
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

private const val TESPIH_BEADS = 33
private val beadLight = Color(0xFFF6D08A)
private val beadBase = Color(0xFFC7802B)
private val beadDark = Color(0xFF6E3B10)
private val cordColor = Color(0xFF8A5A2B)
private val tasselColor = Color(0xFFB8862F)

/**
 * Draws a hanging 33-bead tespih. The cord forms a teardrop loop that ends at the imame.
 * Beads that are not yet counted rest on the left side; each count slides one bead across
 * the free part of the cord to the right side. Rounds alternate direction, so the beads
 * never jump back when a round of 33 is completed.
 */
@Composable
private fun TespihCanvas(position: Float, swingDegrees: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val top = 14.dp.toPx()
        val loopHeight = 228.dp.toPx()
        val halfWidth = 112.dp.toPx()
        val cx = size.width / 2
        rotate(swingDegrees, pivot = Offset(cx, top)) {
            // Sample the loop: s=0 is the bottom knot, travelling up the left side, over the top
            // and down the right side back to the knot.
            val samples = 360
            val points = Array(samples + 1) { i ->
                val theta = 2 * PI * i / samples
                val widen = 1 - 0.38 * cos(theta)
                Offset(
                    (cx - halfWidth * sin(theta) * widen / 1.18).toFloat(),
                    (top + loopHeight * (1 + cos(theta)) / 2).toFloat()
                )
            }
            val lengths = FloatArray(samples + 1)
            for (i in 1..samples) {
                lengths[i] = lengths[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
            }
            val total = lengths[samples]
            fun pointAt(distance: Float): Offset {
                val d = distance.coerceIn(0f, total)
                var lo = 0
                var hi = samples
                while (hi - lo > 1) {
                    val mid = (lo + hi) / 2
                    if (lengths[mid] < d) lo = mid else hi = mid
                }
                val span = (lengths[hi] - lengths[lo]).coerceAtLeast(0.0001f)
                val t = (d - lengths[lo]) / span
                return points[lo] + (points[hi] - points[lo]) * t
            }

            val cord = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1..samples) lineTo(points[i].x, points[i].y)
            }
            drawPath(cord, cordColor, style = Stroke(1.6.dp.toPx()))

            val spacing = total / (TESPIH_BEADS + 4.6f)
            val margin = spacing * 0.6f
            val beadRadius = spacing * 0.47f
            val round = floor(position / TESPIH_BEADS)
            val within = position - round * TESPIH_BEADS
            val moved = if (round.toInt() % 2 == 0) within else TESPIH_BEADS - within
            for (bead in 0 until TESPIH_BEADS) {
                val restLeft = margin + bead * spacing
                val restRight = total - margin - (TESPIH_BEADS - 1 - bead) * spacing
                val t = (moved - (TESPIH_BEADS - 1 - bead)).coerceIn(0f, 1f)
                val eased = t * t * (3 - 2 * t)
                val center = pointAt(restLeft + (restRight - restLeft) * eased)
                val lift = 1f + 0.14f * sin(PI * t).toFloat()
                drawBead(center, beadRadius * lift)
            }

            // Knot, small bead, imame and tassel below the loop.
            val knot = points[0]
            val smallBead = knot + Offset(0f, 9.dp.toPx())
            drawLine(cordColor, knot, smallBead, 1.6.dp.toPx())
            drawBead(smallBead, 5.dp.toPx())
            val imameTop = smallBead.y + 4.dp.toPx()
            val imameWidth = 17.dp.toPx()
            val imameHeight = 36.dp.toPx()
            drawOval(
                Brush.linearGradient(
                    listOf(beadLight, beadBase, beadDark),
                    start = Offset(cx - imameWidth / 2, imameTop),
                    end = Offset(cx + imameWidth / 2, imameTop + imameHeight)
                ),
                topLeft = Offset(cx - imameWidth / 2, imameTop),
                size = Size(imameWidth, imameHeight)
            )
            drawOval(
                beadLight.copy(alpha = 0.55f),
                topLeft = Offset(cx - imameWidth * 0.28f, imameTop + imameHeight * 0.12f),
                size = Size(imameWidth * 0.18f, imameHeight * 0.4f)
            )
            val capTop = imameTop + imameHeight - 2.dp.toPx()
            drawRoundRect(
                tasselColor,
                topLeft = Offset(cx - 5.dp.toPx(), capTop),
                size = Size(10.dp.toPx(), 7.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
            val tasselStart = Offset(cx, capTop + 6.dp.toPx())
            val tasselLength = 44.dp.toPx()
            for (strand in -5..5) {
                val spread = strand * 2.4.dp.toPx()
                drawLine(
                    if (strand % 2 == 0) tasselColor else beadBase,
                    tasselStart + Offset(spread * 0.25f, 0f),
                    tasselStart + Offset(spread, tasselLength - abs(strand) * 1.2.dp.toPx()),
                    1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawBead(center: Offset, radius: Float) {
    drawCircle(Color.Black.copy(alpha = 0.18f), radius, center + Offset(radius * 0.18f, radius * 0.25f))
    drawCircle(
        Brush.radialGradient(
            listOf(beadLight, beadBase, beadDark),
            center = center + Offset(-radius * 0.35f, -radius * 0.35f),
            radius = radius * 1.5f
        ),
        radius,
        center
    )
    drawCircle(Color.White.copy(alpha = 0.45f), radius * 0.22f, center + Offset(-radius * 0.38f, -radius * 0.4f))
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
