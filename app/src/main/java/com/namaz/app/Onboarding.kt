package com.namaz.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val STEPS = 5

private val easyTour = listOf(
    "🕌" to "En üstte sıradaki namaz vakti ve kalan süre yazar. Dokununca sesli söylerim.",
    "🎤" to "“Konuşarak sor” düğmesine basıp “öğle ne zaman” ya da “namazımı kıldım” diyebilirsiniz.",
    "📿" to "“Tespih çek” ile zikir sayarsınız. Her 33’te sesli haber veririm.",
    "🤲" to "“Dua dinle” ile duaları okuyup dinleyebilirsiniz.",
    "✅" to "Namazınızı kıldığınızda “Namazımı kıldım” düğmesine basın."
)

private val fullTour = listOf(
    "🏠" to "Ana Sayfa: namaz vakitleri, günlük özet ve rutinlerin.",
    "📅" to "Dini Günler: kandil ve bayram takvimi.",
    "🔔" to "Hatırlatıcı: hangi vakitlerde bildirim geleceğini seçersin.",
    "📿" to "İbadet: namaz takibi, tespih, dualar, Kur’an ezberi, namaz ve abdest rehberi.",
    "⚙️" to "Ayarlar: bildirim sesi ve sade kullanıma geçiş."
)

/**
 * First-run introduction. Every step is read aloud (can be muted) and uses large text,
 * so a family member and an older user can complete it together.
 */
@Composable
internal fun OnboardingScreen() {
    val context = LocalContext.current
    val speaker = rememberSpeaker()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var easy by rememberSaveable { mutableStateOf(AppMode.easy(context)) }
    var modeChosen by rememberSaveable { mutableStateOf(false) }
    var soundOn by rememberSaveable { mutableStateOf(true) }
    var city by rememberSaveable { mutableStateOf(ReminderRefresh.city(context).orEmpty()) }
    var notifyAnswer by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) speaker.speak("Bildirim izni verilmedi. Daha sonra ayarlardan açabilirsiniz.")
    }

    val tour = if (easy) easyTour else fullTour
    val speech = when (step) {
        0 -> "Hoş geldiniz. Bu uygulama namaz vakitlerini gösterir, vakit gelince haber verir ve sesli yardım eder. " +
            "Devam etmek için aşağıdaki Devam düğmesine basın."
        1 -> "Uygulamayı nasıl kullanmak istersiniz? Büyük düğmeli ve sesli sade kullanım mı, yoksa tüm özellikler mi?"
        2 -> "Hangi şehirde yaşıyorsunuz? Namaz vakitleri buna göre gösterilir."
        3 -> "Namaz vakti geldiğinde telefonunuza bildirim göndereyim mi?"
        else -> "Kısaca anlatayım. " + tour.joinToString(" ") { it.second }
    }
    LaunchedEffect(step, soundOn, easy) { if (soundOn) speaker.speak(speech) else speaker.stop() }

    fun finish() {
        speaker.stop()
        AppMode.setEasy(context, easy)
        AppMode.setOnboarded(context, true)
    }
    BackHandler(step > 0) { step-- }

    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(STEPS) { index ->
                        Box(Modifier.size(if (index == step) 14.dp else 10.dp)
                            .background(if (index <= step) Emerald else MaterialTheme.colorScheme.outline, CircleShape))
                    }
                }
                TextButton(onClick = { soundOn = !soundOn }, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text(if (soundOn) "🔊 Ses açık" else "🔇 Ses kapalı", fontSize = 18.sp)
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    0 -> {
                        Text("📿", fontSize = 72.sp)
                        StepTitle("Hoş geldiniz")
                        StepText("Bu uygulama namaz vakitlerini gösterir, vakit gelince haber verir ve sesli yardım eder.")
                        StepText("Birkaç kısa adımda hazırlayalım.")
                    }
                    1 -> {
                        StepTitle("Nasıl kullanmak istersiniz?")
                        ChoiceCard("👓", "Sade kullanım",
                            "Büyük yazılar, birkaç büyük düğme ve sesli yardım. Yaşlılar ve ilk kez kullananlar için önerilir.",
                            selected = modeChosen && easy) { easy = true; modeChosen = true; step++ }
                        ChoiceCard("🧭", "Tüm özellikler",
                            "Takvim, rutinler, Kur’an ezberi, geçmiş ve ayrıntılı ayarlar.",
                            selected = modeChosen && !easy) { easy = false; modeChosen = true; step++ }
                        StepText("Bunu daha sonra Ayarlar’dan değiştirebilirsiniz.", small = true)
                    }
                    2 -> {
                        StepTitle("Şehriniz")
                        StepText("Namaz vakitleri seçtiğiniz şehre göre gösterilir.")
                        CityPicker(city, true) { chosen ->
                            city = chosen
                            ReminderRefresh.saveCity(context, chosen)
                            PrayerRefreshWork.refreshNow(context)
                            if (soundOn) speaker.speak("$chosen seçildi.")
                        }
                        if (city.isNotBlank()) StepText("✓ $city seçildi", color = Emerald)
                    }
                    3 -> {
                        StepTitle("Vakit bildirimi")
                        StepText("Namaz vakti geldiğinde telefonunuza bildirim gelsin mi?")
                        BigButton("🔔", "Evet, haber ver", {
                            notifyAnswer = true
                            enablePrayerNotifications(context)
                            if (Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.canNotify(context)) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            step++
                        })
                        OutlinedButton(onClick = { notifyAnswer = false; step++ },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = RoundedCornerShape(22.dp)) {
                            Text("Şimdi değil", fontSize = 22.sp)
                        }
                        if (city.isBlank()) StepText("Not: Bildirim için önce şehir seçilmeli.", small = true)
                    }
                    else -> {
                        StepTitle(if (easy) "Ana ekranınız" else "Uygulamada neler var?")
                        tour.forEach { (emoji, text) ->
                            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 34.sp)
                                Spacer(Modifier.width(14.dp))
                                Text(text, fontSize = if (easy) 21.sp else 18.sp, lineHeight = if (easy) 29.sp else 25.sp)
                            }
                        }
                        if (notifyAnswer == true) StepText("🔔 Vakit bildirimleri açıldı.", small = true)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    shape = RoundedCornerShape(20.dp)) { Text("Geri", fontSize = 22.sp) }
                OutlinedButton(onClick = { speaker.speak(speech) }, modifier = Modifier.heightIn(min = 64.dp),
                    shape = RoundedCornerShape(20.dp)) { Text("🔊", fontSize = 24.sp) }
                // Steps 1 and 3 advance by choosing an answer; "Devam" still lets a helper skip them.
                Button(onClick = { if (step == STEPS - 1) finish() else step++ },
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp), shape = RoundedCornerShape(20.dp)) {
                    Text(if (step == STEPS - 1) "Başla" else "Devam", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Turns on the prayer-time category and every individual prayer, then reschedules. */
private fun enablePrayerNotifications(context: android.content.Context) {
    ReminderPreferences.setEnabled(context, "Namaz vakitleri", true)
    val editor = context.getSharedPreferences("prayer_reminder_preferences", 0).edit()
    ReminderPreferences.prayers.forEach { editor.putBoolean(it, true) }
    editor.apply()
    PrayerRefreshWork.refreshNow(context)
}

@Composable
private fun StepTitle(text: String) {
    Text(text, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = DarkEmerald)
}

@Composable
private fun StepText(text: String, small: Boolean = false, color: Color = Ink) {
    Text(text, fontSize = if (small) 17.sp else 22.sp, lineHeight = if (small) 24.sp else 30.sp, color = color)
}

@Composable
private fun ChoiceCard(emoji: String, title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White),
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Emerald else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(description, fontSize = 18.sp, lineHeight = 25.sp)
            }
        }
    }
}
