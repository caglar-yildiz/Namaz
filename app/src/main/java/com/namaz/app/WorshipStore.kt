package com.namaz.app

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/** Local-only worship journal. Never sends personal worship data to a server. */
object WorshipStore {
    private const val STORE = "worship_journal_v1"
    private val zone = ZoneId.of("Europe/Istanbul")
    val prayers = listOf("Sabah", "Öğle", "İkindi", "Akşam", "Yatsı")
    val phrases = listOf("Sübhanallah", "Elhamdülillah", "Allahu ekber", "Salavat")

    fun today(): LocalDate = LocalDate.now(zone)
    private fun prefs(context: Context) =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun completed(context: Context, date: LocalDate, prayer: String): Boolean =
        prefs(context).getBoolean("prayer_${date}_$prayer", false)

    fun setCompleted(context: Context, date: LocalDate, prayer: String, done: Boolean) {
        require(prayer in prayers)
        prefs(context).edit().putBoolean("prayer_${date}_$prayer", done).apply()
    }

    fun count(context: Context, phrase: String): Int =
        prefs(context).getInt("dhikr_${today()}_$phrase", 0)

    fun increment(context: Context, phrase: String): Int {
        require(phrase in phrases)
        val next = (count(context, phrase) + 1).coerceAtMost(999999)
        prefs(context).edit().putInt("dhikr_${today()}_$phrase", next).apply()
        return next
    }

    fun reset(context: Context, phrase: String) {
        require(phrase in phrases)
        prefs(context).edit().remove("dhikr_${today()}_$phrase").apply()
    }

    fun target(context: Context): Int =
        prefs(context).getInt("dhikr_target", 33)

    fun setTarget(context: Context, value: Int) {
        require(value in listOf(33, 99, 100))
        prefs(context).edit().putInt("dhikr_target", value).apply()
    }
}
