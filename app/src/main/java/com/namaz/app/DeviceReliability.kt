package com.namaz.app

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDate
import java.time.ZoneId

/**
 * Phone makers such as Xiaomi, Huawei, Oppo, Vivo and Samsung add their own battery managers
 * on top of Android. They can stop the app's alarms entirely, and an app cannot read those
 * settings. We therefore (1) check and fix what Android does expose, (2) open the maker's own
 * settings page with plain instructions and let the user confirm, and (3) measure whether
 * alarms actually arrive on time so problems surface even when (1) and (2) miss something.
 */
internal enum class PhoneBrand(val label: String) {
    XIAOMI("Xiaomi / Redmi / POCO"), HUAWEI("Huawei"), HONOR("Honor"), OPPO("Oppo / Realme / OnePlus"),
    VIVO("Vivo"), SAMSUNG("Samsung"), OTHER("Android");

    companion object {
        fun current(): PhoneBrand {
            val maker = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
            return when {
                listOf("xiaomi", "redmi", "poco").any { it in maker } -> XIAOMI
                "huawei" in maker -> HUAWEI
                "honor" in maker -> HONOR
                listOf("oppo", "realme", "oneplus").any { it in maker } -> OPPO
                listOf("vivo", "iqoo").any { it in maker } -> VIVO
                "samsung" in maker -> SAMSUNG
                else -> OTHER
            }
        }
    }
}

internal object DeviceReliability {
    private const val PREFS = "device_reliability"

    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Asks Android to stop optimising this app; core function (timed prayer reminders) depends on it. */
    @SuppressLint("BatteryLife")
    fun requestBatteryUnrestricted(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        if (!launch(context, direct)) launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    /** Settings that only exist on some brands. Tried in order; the first one that opens wins. */
    private fun brandIntents(context: Context, brand: PhoneBrand): List<Intent> {
        fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))
        return when (brand) {
            PhoneBrand.XIAOMI -> listOf(
                component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST")
                    .setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                    .putExtra("package_name", context.packageName).putExtra("package_label", "Namaz")
            )
            PhoneBrand.HUAWEI, PhoneBrand.HONOR -> listOf(
                component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                component("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            )
            PhoneBrand.OPPO -> listOf(
                component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                component("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
                component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            )
            PhoneBrand.VIVO -> listOf(
                component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            )
            PhoneBrand.SAMSUNG -> listOf(
                component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )
            PhoneBrand.OTHER -> emptyList()
        }
    }

    /** Opens the brand page, or this app's system info page when the brand page is missing. */
    fun openBrandSettings(context: Context) {
        val opened = brandIntents(context, PhoneBrand.current()).any { launch(context, it) }
        if (!opened) launch(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    /** What the user has to switch on in the maker's own settings, in their own words. */
    fun brandSteps(brand: PhoneBrand): List<String> = when (brand) {
        PhoneBrand.XIAOMI -> listOf(
            "“Otomatik başlatma” listesinde Namaz’ı açın.",
            "Pil tasarrufu ayarında Namaz için “Kısıtlama yok”u seçin.",
            "Son uygulamalar ekranında Namaz’ı aşağı çekip kilit simgesiyle kilitleyin."
        )
        PhoneBrand.HUAWEI, PhoneBrand.HONOR -> listOf(
            "“Uygulama başlatma” listesinde Namaz’ı bulun.",
            "Otomatik yönetimi kapatıp “Manuel yönet”i seçin.",
            "Otomatik başlatma, İkincil başlatma ve Arka planda çalışma seçeneklerinin üçünü de açın."
        )
        PhoneBrand.OPPO -> listOf(
            "“Otomatik başlatma” listesinde Namaz’ı açın.",
            "Uygulama bilgisi → Pil kullanımı bölümünde “Arka planda çalışmaya izin ver”i açın."
        )
        PhoneBrand.VIVO -> listOf(
            "“Arka planda başlatma / Otomatik başlatma” listesinde Namaz’ı açın.",
            "Pil ayarlarında Namaz için “Arka planda yüksek güç kullanımına izin ver”i açın."
        )
        PhoneBrand.SAMSUNG -> listOf(
            "Pil → Arka plan kullanım sınırları bölümüne girin.",
            "Namaz “Uyuyan” veya “Derin uykudaki uygulamalar” listesindeyse çıkarın.",
            "Namaz’ı “Hiç uyumayan uygulamalar” listesine ekleyin."
        )
        PhoneBrand.OTHER -> emptyList()
    }

    /** Brand settings cannot be read, so we keep the user's own confirmation. */
    fun brandConfirmed(context: Context) =
        PhoneBrand.current() == PhoneBrand.OTHER ||
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("brand_confirmed", false)

    fun setBrandConfirmed(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("brand_confirmed", value).apply()

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        // Missing on this firmware version, or not exported: try the next candidate.
        false
    }
}

/**
 * Records when each prayer alarm was due and when it actually arrived, so the app can tell
 * the user "the last reminder came 14 minutes late" or "Öğle reminder never arrived".
 */
internal object AlarmHealth {
    const val STORE = "alarm_health"
    private const val LATE_MINUTES = 3L
    private val zone = ZoneId.of("Europe/Istanbul")

    private fun prefs(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun recordScheduled(context: Context, date: LocalDate, prayer: String, atMillis: Long) {
        val editor = prefs(context).edit().putLong("due_${date}_$prayer", atMillis)
        // Keep only a few days of history.
        prefs(context).all.keys.filter { key ->
            val day = key.split('_').getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            day != null && day.isBefore(LocalDate.now(zone).minusDays(3))
        }.forEach(editor::remove)
        editor.apply()
    }

    fun recordFired(context: Context, date: String, prayer: String) {
        prefs(context).edit().putLong("fired_${date}_$prayer", System.currentTimeMillis()).apply()
    }

    data class Report(val missed: List<String>, val lateMinutes: Long?)

    /** Looks at today and yesterday. Only prayers whose reminders are still switched on are judged. */
    fun report(context: Context): Report {
        val reminders = context.getSharedPreferences("prayer_reminder_preferences", Context.MODE_PRIVATE)
        if (!ReminderPreferences.enabled(context, "Namaz vakitleri")) return Report(emptyList(), null)
        val store = prefs(context)
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val missed = mutableListOf<String>()
        var worstLate: Long? = null
        for (date in listOf(today.minusDays(1), today)) for (prayer in ReminderPreferences.prayers) {
            if (!reminders.getBoolean(prayer, false)) continue
            val due = store.getLong("due_${date}_$prayer", 0L).takeIf { it > 0 } ?: continue
            val fired = store.getLong("fired_${date}_$prayer", 0L)
            when {
                fired > 0 -> {
                    val late = (fired - due) / 60_000L
                    if (late >= LATE_MINUTES && (worstLate == null || late > worstLate)) worstLate = late
                }
                // Give a 10-minute grace period before calling it missed.
                now - due > 10 * 60_000L -> missed += (if (date == today) "" else "Dün ") + spokenPrayer(prayer)
            }
        }
        return Report(missed, worstLate)
    }

    fun clear(context: Context) = prefs(context).edit().clear().apply()
}

internal data class ReliabilityState(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val battery: Boolean,
    val brand: Boolean,
    val report: AlarmHealth.Report
) {
    val issues: Int get() = listOf(notifications, exactAlarms, battery, brand).count { !it } +
        (if (report.missed.isNotEmpty() || report.lateMinutes != null) 1 else 0)
}

/** Re-reads everything when the screen resumes, e.g. after returning from a settings page. */
@Composable
internal fun rememberReliability(): ReliabilityState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val revision = preferenceRevision("device_reliability", AlarmHealth.STORE, "prayer_reminder_preferences")
    var resumes by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumes++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return remember(revision, resumes) {
        ReliabilityState(
            notifications = ReminderNotifications.canNotify(context),
            exactAlarms = ExactAlarms.allowed(context),
            battery = DeviceReliability.batteryUnrestricted(context),
            brand = DeviceReliability.brandConfirmed(context),
            report = AlarmHealth.report(context)
        )
    }
}

/**
 * Checklist of everything that keeps reminders on time. [large] is used in simple mode
 * and in the introduction. [requestNotifications] asks for the Android 13+ permission.
 */
@Composable
internal fun ReliabilityChecklist(state: ReliabilityState, large: Boolean, requestNotifications: () -> Unit) {
    val context = LocalContext.current
    val brand = remember { PhoneBrand.current() }
    val body = if (large) 19.sp else 15.sp
    Column(verticalArrangement = Arrangement.spacedBy(if (large) 12.dp else 8.dp)) {
        HealthNotice(state.report, large)
        CheckRow("Bildirim izni", "Vakitlerde bildirim gösterebilmem için.", state.notifications, large,
            "İzin ver", requestNotifications)
        CheckRow("Tam zamanında alarm", "“Alarmlar ve hatırlatıcılar” izni. Yoksa bildirim gecikebilir.",
            state.exactAlarms, large, "İzni aç") { ExactAlarms.openSettings(context) }
        CheckRow("Pil kısıtlaması yok", "Telefon, pil tasarrufu için uygulamayı uyutmasın.",
            state.battery, large, "Kısıtlamayı kaldır") { DeviceReliability.requestBatteryUnrestricted(context) }
        if (brand != PhoneBrand.OTHER) {
            CheckRow("${brand.label} ayarları", "Bu marka uygulamaları kendi ayarlarıyla durdurabilir.",
                state.brand, large, "Ayarı aç") { DeviceReliability.openBrandSettings(context) }
            if (!state.brand) {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Açılan ekranda:", fontSize = body, fontWeight = FontWeight.SemiBold)
                    DeviceReliability.brandSteps(brand).forEachIndexed { index, step ->
                        Text("${index + 1}. $step", fontSize = body, lineHeight = if (large) 26.sp else 21.sp)
                    }
                    Text("Ekran açılmazsa telefonun Ayarlar → Uygulamalar → Namaz bölümünde aynı seçenekleri arayın.",
                        fontSize = if (large) 16.sp else 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { DeviceReliability.setBrandConfirmed(context, true) },
                        modifier = Modifier.heightIn(min = if (large) 56.dp else 44.dp)) {
                        Text("Bunları yaptım", fontSize = if (large) 19.sp else 14.sp)
                    }
                }
            } else {
                TextButton(onClick = { DeviceReliability.setBrandConfirmed(context, false) }) {
                    Text("Marka ayarlarını tekrar göster", fontSize = if (large) 16.sp else 13.sp)
                }
            }
        }
    }
}

@Composable
private fun HealthNotice(report: AlarmHealth.Report, large: Boolean) {
    val message = when {
        report.missed.isNotEmpty() -> "${report.missed.joinToString(", ")} bildirimi gelmedi. O sırada telefon kapalı değildiyse, telefon uygulamayı durdurmuş olabilir; aşağıdaki ayarları kontrol edin."
        report.lateMinutes != null -> "Son bildirimlerden biri ${report.lateMinutes} dakika geç geldi. Aşağıdaki ayarları kontrol edin."
        else -> return
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE2DF))) {
        Text("⚠ $message", Modifier.padding(if (large) 18.dp else 12.dp),
            fontSize = if (large) 19.sp else 14.sp, lineHeight = if (large) 26.sp else 20.sp)
    }
}

@Composable
private fun CheckRow(title: String, detail: String, ok: Boolean, large: Boolean, action: String, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (ok) Mint else Color(0xFFFFF4D6), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = if (large) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (ok) "✓" else "!", fontSize = if (large) 26.sp else 20.sp, fontWeight = FontWeight.Bold,
            color = if (ok) Emerald else Color(0xFF9A5B00), modifier = Modifier.width(if (large) 34.dp else 26.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = if (large) 21.sp else 16.sp, fontWeight = FontWeight.SemiBold)
            if (!ok) Text(detail, fontSize = if (large) 17.sp else 13.sp, lineHeight = if (large) 23.sp else 18.sp)
        }
        if (!ok) Button(onClick = onFix, modifier = Modifier.heightIn(min = if (large) 52.dp else 40.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(action, fontSize = if (large) 17.sp else 13.sp)
        }
    }
}
