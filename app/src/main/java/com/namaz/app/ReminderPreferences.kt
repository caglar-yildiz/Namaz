package com.namaz.app

import android.content.Context

/** Shared persistent settings used by Settings and Reminders screens. */
object ReminderPreferences {
    private const val STORE = "prayer_reminder_preferences"
    val categories = listOf(
        "Namaz vakitleri",
        "Kandil ve bayramlar",
        "Cuma hatırlatması",
        "Ramazan sahur ve iftar",
        "İbadet takibi"
    )
    val prayers = listOf("İmsak", "Öğle", "İkindi", "Akşam", "Yatsı")
    fun enabled(context: Context, category: String): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getBoolean("category_$category", false)
    fun setEnabled(context: Context, category: String, value: Boolean) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putBoolean("category_$category", value).apply()
    }
    fun sound(context: Context): String =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getString("sound", "soft") ?: "soft"
    fun setSound(context: Context, value: String) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString("sound", value).apply()
    }
}
