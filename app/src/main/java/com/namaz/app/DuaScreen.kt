package com.namaz.app

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal data class Dua(
    val id: String,
    val title: String,
    val meaning: String,
    val categories: Set<String>,
    val arabic: String? = null,
    val source: String? = null,
    val sourceUrl: String? = null
)

internal object Duas {
    const val DAILY = "Günlük hayat"
    const val MORNING = "Sabah ve akşam"
    const val NIGHT = "Uyku öncesi"
    const val AFTER = "Namaz sonrası"
    const val SPECIAL = "Özel günler"
    val categories = listOf(MORNING, NIGHT, AFTER, DAILY, SPECIAL)
    private const val sleepSource = "https://hadislerleislam.diyanet.gov.tr/sayfa.php?CILT=1&SAYFA=175"

    val all = listOf(
        Dua("sleep", "Uykuya yatarken", "Allah’ım, senin adınla yaşar ve senin adınla ölürüm.",
            setOf(NIGHT), "اللَّهُمَّ بِاسْمِكَ أَحْيَا وَبِاسْمِكَ أَمُوتُ", "Diyanet, Hadislerle İslâm", sleepSource),
        Dua("wake", "Uyanınca", "Canımızı aldıktan sonra bize yeniden hayat veren Allah’a hamdolsun. Dönüş O’nadır.",
            setOf(MORNING), "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", "Diyanet, Hadislerle İslâm", sleepSource),
        Dua("hasene", "Dünyada ve ahirette iyilik", "Rabbimiz, bize dünyada da ahirette de iyilik ver, bizi ateşin azabından koru.",
            setOf(AFTER, DAILY, SPECIAL), "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Bakara 2:201",
            "https://kuran.diyanet.gov.tr/kuran?ayet=201&mode=tefsir&sure=2"),
        Dua("knowledge", "Bilgi için dua", "Rabbim, bilgimi artır.", setOf(DAILY, MORNING), "رَبِّ زِدْنِي عِلْمًا", "Tâhâ 20:114",
            "https://kuran.diyanet.gov.tr/tefsir/T%C3%A2h%C3%A2-suresi/2461/113-114-ayet-tefsiri"),
        Dua("parents", "Anne ve baba için dua", "Rabbim, beni küçükken yetiştirdikleri gibi sen de onlara merhamet et.",
            setOf(DAILY, AFTER), "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "İsrâ 17:24",
            "https://kuran.diyanet.gov.tr/tefsir/%C4%B0sr%C3%A2-suresi/2052/23-24-ayet-tefsiri"),
        Dua("kadir", "Kadir Gecesi duası", "Allah’ım, sen çok bağışlayıcısın. Bağışlamayı seversin, beni de bağışla.",
            setOf(SPECIAL), "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي", "Diyanet, Kadir Gecesi",
            "https://konya.diyanet.gov.tr/altinekin/Sayfalar/contentdetail.aspx?ContentId=276&MenuCategory=Kurumsal"),
        Dua("evening", "Günü bitirirken", "Allah’ım, bugün bana verilen nimetlere şükretmeyi ve hatalarımdan ders çıkarmayı nasip et.", setOf(MORNING, NIGHT)),
        Dua("ease", "Zor bir gün için", "Allah’ım, zorlandığım işlerde bana sabır ve kolaylık ver. Yardıma ihtiyacım olduğunda destek istemeyi nasip et.", setOf(DAILY)),
        Dua("peace", "Huzur için", "Allah’ım, gönlüme huzur ver. Sevdiklerime sağlık, aramızdaki ilişkilere anlayış nasip et.", setOf(NIGHT, DAILY)),
        Dua("solidarity", "Birlik ve dayanışma için", "Allah’ım, sevdiklerimizle geçirdiğimiz günleri bereketlendir. İhtiyaç sahiplerini gözetebilmeyi nasip et.", setOf(SPECIAL)),
        Dua("gratitude", "Şükür duası", "Allah’ım, sahip olduğum iyiliklerin farkında olmayı ve onları başkalarıyla paylaşmayı nasip et.", setOf(SPECIAL, AFTER, MORNING))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DuaLibrary(initialCategory: String?, onBack: () -> Unit, onDua: (String) -> Unit) {
    val context = LocalContext.current
    val favorites = remember { context.getSharedPreferences("dua_favorites", 0) }
    val revision = preferenceRevision("dua_favorites")
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory ?: "Tümü") }
    val visible = remember(category, revision) {
        Duas.all.filter { dua -> when (category) {
            "Tümü" -> true
            "Favoriler" -> favorites.getBoolean(dua.id, false)
            else -> category in dua.categories
        } }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { InnerHeader("Dualar", onBack) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("Tümü", "Favoriler") + Duas.categories).forEach { label ->
                    FilterChip(category == label, { category = label }, { Text(label) })
                }
            }
        }
        if (visible.isEmpty()) item { Text("Favorilerine eklediğin dualar burada görünür.") }
        items(visible, key = Dua::id) { dua ->
            DashboardCard(dua.title, dua.categories.joinToString(" · "), "◇") { onDua(dua.id) }
        }
    }
}

@Composable
internal fun DuaDetail(id: String, onBack: () -> Unit, onRoutine: (Routine) -> Unit) {
    val dua = Duas.all.firstOrNull { it.id == id } ?: return
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dua_favorites", 0) }
    var favorite by rememberSaveable(id) { mutableStateOf(prefs.getBoolean(id, false)) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsSupported by remember { mutableStateOf(false) }
    var speech by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context, id) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val support = engine?.setLanguage(Locale.forLanguageTag("ar"))
                ttsSupported = support != TextToSpeech.LANG_MISSING_DATA && support != TextToSpeech.LANG_NOT_SUPPORTED && support != null
                ttsReady = true
            } else {
                ttsReady = true
                ttsSupported = false
            }
        }
        speech = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
            speech = null
        }
    }
    val revision = preferenceRevision(RoutineStore.NAME)
    val existing = remember(id, revision) {
        RoutineStore.all(context).firstOrNull { it.prayerId == id && it.archivedOn == null }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { InnerHeader(dua.title, onBack) }
        if (dua.arabic != null) item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(dua.arabic, Modifier.fillMaxWidth().padding(20.dp),
                    style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.End)
            }
        }
        if (dua.arabic != null) item {
            OutlinedButton(
                onClick = { speech?.speak(dua.arabic, TextToSpeech.QUEUE_FLUSH, null, "dua_$id") },
                enabled = ttsReady && ttsSupported,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (!ttsReady) "Ses hazırlanıyor…" else if (ttsSupported) "Arapça dinle" else "Arapça ses paketi gerekli") }
            Text("Cihazın sentetik Arapça sesi kullanılır; kıraat kaydı değildir.",
                style = MaterialTheme.typography.bodySmall)
        }
        item {
            Text(if (dua.arabic != null) "Türkçe anlamı" else "Türkçe dua örneği", style = MaterialTheme.typography.titleMedium)
            Text(dua.meaning, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge)
        }
        dua.source?.let { source -> item {
            if (dua.sourceUrl != null) TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dua.sourceUrl))) }
            }) { Text(source) } else Text(source, style = MaterialTheme.typography.bodySmall)
        } }
        item {
            Button(onClick = {
                onRoutine(existing ?: Routine(title = dua.title, prayerId = dua.id))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (existing == null) "Rutinime ekle" else "Rutinimi düzenle") }
            OutlinedButton(onClick = {
                favorite = !favorite
                prefs.edit().putBoolean(id, favorite).apply()
            }, modifier = Modifier.fillMaxWidth()) { Text(if (favorite) "Favorilerden çıkar" else "Favoriye ekle") }
        }
    }
}
