package com.namaz.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val start: LocalDate = WorshipStore.today(),
    val prayerId: String? = null,
    val reminder: LocalTime? = null,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val archivedOn: LocalDate? = null
) {
    fun applies(date: LocalDate): Boolean =
        date >= start && (archivedOn == null || date < archivedOn) && date.dayOfWeek in days
}

object RoutineStore {
    internal const val NAME = "namaz_routines"
    private const val PREFIX = "routine_"
    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun all(context: Context): List<Routine> = prefs(context).all.mapNotNull { (key, value) ->
        if (!key.startsWith(PREFIX) || value !is String) return@mapNotNull null
        runCatching {
            val data = JSONObject(value)
            Routine(
                id = key.removePrefix(PREFIX), title = data.getString("title"),
                start = LocalDate.parse(data.getString("start")),
                prayerId = data.optString("prayer").ifEmpty { null },
                reminder = data.optString("time").takeIf(String::isNotEmpty)?.let(LocalTime::parse),
                days = data.getString("days").split(',').map { DayOfWeek.of(it.toInt()) }.toSet(),
                archivedOn = data.optString("archived").takeIf(String::isNotEmpty)?.let(LocalDate::parse)
            )
        }.getOrNull()
    }.sortedWith(compareBy(Routine::start, Routine::title))

    fun save(context: Context, routine: Routine) {
        require(routine.title.isNotBlank() && routine.days.isNotEmpty())
        val data = JSONObject().put("title", routine.title).put("start", routine.start.toString())
            .put("prayer", routine.prayerId.orEmpty()).put("time", routine.reminder?.toString().orEmpty())
            .put("days", routine.days.joinToString(",") { it.value.toString() })
            .put("archived", routine.archivedOn?.toString().orEmpty())
        prefs(context).edit().putString(PREFIX + routine.id, data.toString()).apply()
        RoutineAlarm.schedule(context, routine)
    }

    fun completed(context: Context, routine: Routine, date: LocalDate): Boolean =
        prefs(context).getBoolean("done_${routine.id}_$date", false)

    fun mark(context: Context, routine: Routine, date: LocalDate, value: Boolean) {
        prefs(context).edit().putBoolean("done_${routine.id}_$date", value).apply()
    }

    fun forDay(context: Context, date: LocalDate): List<Routine> =
        all(context).filter { it.applies(date) || completed(context, it, date) }

    fun weekCount(context: Context, routine: Routine, today: LocalDate): Int {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0 until today.dayOfWeek.value).count { completed(context, routine, monday.plusDays(it.toLong())) }
    }

    fun streak(context: Context, routine: Routine, today: LocalDate): Int {
        val first = if (completed(context, routine, today)) today else today.minusDays(1)
        return generateSequence(first) { it.minusDays(1) }
            .takeWhile { it >= routine.start }
            .filter { routine.applies(it) || completed(context, routine, it) }
            .takeWhile { completed(context, routine, it) }
            .count()
    }
}

internal object RoutineAlarm {
    private val zone = ZoneId.of("Europe/Istanbul")

    fun schedule(context: Context, routine: Routine, replace: Boolean = true) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, RoutineReceiver::class.java)
            .setData(Uri.parse("namaz://routine/${routine.id}"))
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        val previous = PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_NO_CREATE)
        if (previous != null) {
            if (!replace) return
            alarms.cancel(previous)
            previous.cancel()
        }
        val time = routine.reminder
        if (time == null || routine.archivedOn != null) return
        val now = ZonedDateTime.now(zone)
        val start = maxOf(now.toLocalDate(), routine.start)
        val next = (0L..7L).asSequence().map(start::plusDays)
            .filter(routine::applies).map { it.atTime(time).atZone(zone) }.first { it.isAfter(now) }
        val delivery = PendingIntent.getBroadcast(context, 0,
            intent.putExtra("date", next.toLocalDate().toString()), flags)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), delivery)
    }

    fun refresh(context: Context, replace: Boolean = true) =
        RoutineStore.all(context).forEach { schedule(context, it, replace) }
}

class RoutineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.data?.lastPathSegment ?: return
        val routine = RoutineStore.all(context).firstOrNull { it.id == id } ?: return
        val today = WorshipStore.today()
        if (routine.reminder != null && intent.getStringExtra("date") == today.toString() &&
            routine.applies(today) && !RoutineStore.completed(context, routine, today)) {
            ReminderNotifications.send(context, "Rutin hatırlatması", routine.title, "routine:${routine.id}:$today")
        }
        RoutineAlarm.schedule(context, routine)
    }
}
