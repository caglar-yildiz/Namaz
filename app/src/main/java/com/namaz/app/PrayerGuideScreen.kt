package com.namaz.app

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val guideSource = "https://yayin.diyanet.gov.tr/File/Download?id=394&path=namaz_ilmihali.pdf"
private data class GuideStep(val title: String, val text: String)

private fun farzSteps(prayer: String): List<GuideStep> {
    val units = when (prayer) { "Sabah" -> 2; "Akşam" -> 3; else -> 4 }
    return buildList {
        add(GuideStep("Hazırlık", "Abdestli olarak kıbleye dön. Hangi namazın farzını kılacağına niyet et. Allâhü ekber diyerek başlangıç tekbirini al."))
        for (unit in 1..units) {
            val recitation = when (unit) {
                1 -> "Ayakta Sübhaneke duasını oku. Eûzü ve besmeleden sonra Fâtiha’yı ve ardından bildiğin bir sûreyi ya da ayetleri oku."
                2 -> "Ayakta besmeleyle Fâtiha’yı, ardından bildiğin bir sûreyi ya da ayetleri oku."
                else -> "Ayakta besmeleyle Fâtiha’yı oku."
            }
            add(GuideStep("$unit. rekât · Kıyam", recitation))
            add(GuideStep("$unit. rekât · Rükû", "Allâhü ekber diyerek rükûya eğil. Üç kez Sübhâne Rabbiye’l-azîm de. Semiallâhü limen hamideh diyerek doğrul ve Rabbenâ leke’l-hamd de."))
            add(GuideStep("$unit. rekât · Secdeler", "Allâhü ekber diyerek secdeye var ve üç kez Sübhâne Rabbiye’l-a‘lâ de. Tekbirle doğrulup kısa bir süre otur. Tekrar tekbirle ikinci secdeye var ve aynı tesbihi üç kez söyle."))
            if (unit == units) {
                add(GuideStep("Son oturuş ve selam", "İkinci secdeden tekbirle kalkıp otur. Ettehiyyâtü, Allahümme salli, Allahümme bârik ve Rabbenâ dualarını oku. Sağa ve sola dönerek Esselâmü aleyküm ve rahmetullâh de."))
            } else if (unit == 2) {
                add(GuideStep("İlk oturuş", "İkinci secdeden tekbirle kalkıp otur. Ettehiyyâtü duasını oku. Sonraki rekât için tekbirle ayağa kalk."))
            } else {
                add(GuideStep("Sonraki rekâta geçiş", "İkinci secdeden sonra tekbirle sonraki rekât için ayağa kalk."))
            }
        }
    }
}

@Composable
internal fun PrayerGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var prayer by rememberSaveable { mutableStateOf<String?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var speech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle) {
        var disposed = false
        val engine = TextToSpeech(context) { status ->
            handler.post {
                if (!disposed) {
                    val result = if (status == TextToSpeech.SUCCESS) speech?.setLanguage(turkish) else null
                    ready = result != null && result >= TextToSpeech.LANG_AVAILABLE
                    error = !ready
                }
            }
        }
        speech = engine
        engine.setSpeechRate(0.88f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { handler.post { if (!disposed) speaking = false } }
            @Deprecated("Android compatibility")
            override fun onError(utteranceId: String?) { handler.post { if (!disposed) { speaking = false; error = true } } }
        })
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { engine.stop(); speaking = false }
        }
        lifecycle.addObserver(observer)
        onDispose {
            disposed = true
            lifecycle.removeObserver(observer)
            engine.stop()
            engine.shutdown()
        }
    }
    fun stop() { speech?.stop(); speaking = false }
    val selected = prayer
    androidx.activity.compose.BackHandler(selected != null) { stop(); prayer = null }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { InnerHeader(selected?.let { "$it namazının farzı" } ?: "Namazı Öğren") {
            if (selected == null) onBack() else { stop(); prayer = null }
        } }
        if (selected == null) {
            item { Text("Beş vaktin farzını Hanefî uygulamasına göre adım adım prova et.") }
            WorshipStore.prayers.forEach { name ->
                item { DashboardCard(name, "Adım adım prova", "▷") { index = 0; prayer = name } }
            }
        } else {
            val steps = farzSteps(selected)
            val step = steps[index]
            item {
                Text("Adım ${index + 1} / ${steps.size}", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator({ (index + 1f) / steps.size }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            item { ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(step.title, style = MaterialTheme.typography.headlineSmall)
                    Text(step.text, style = MaterialTheme.typography.bodyLarge)
                }
            } }
            item {
                Button(enabled = ready || speaking, modifier = Modifier.fillMaxWidth(), onClick = {
                    if (speaking) stop() else {
                        error = speech?.speak(step.text, TextToSpeech.QUEUE_FLUSH, null, "step-$index") != TextToSpeech.SUCCESS
                        speaking = !error
                    }
                }) { Text(if (speaking) "Durdur" else "Adımı dinle") }
                if (error) Text("Ses için Android’in metin okuma ayarlarından Türkçe ses yükle. Yazılı prova kullanılabilir.",
                    style = MaterialTheme.typography.bodySmall)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(enabled = index > 0, onClick = { stop(); index-- }) { Text("Önceki") }
                    Button(onClick = {
                        stop()
                        if (index == steps.lastIndex) prayer = null else index++
                    }) { Text(if (index == steps.lastIndex) "Bitir" else "Sonraki") }
                }
            }
            item { Text("Türkçe anlatım cihazın ses motoruyla okunur.", style = MaterialTheme.typography.bodySmall) }
        }
        item {
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(guideSource))) }
            }) { Text("Kaynak · Diyanet Namaz İlmihali") }
        }
    }
}
