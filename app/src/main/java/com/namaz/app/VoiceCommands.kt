package com.namaz.app

import java.util.Locale

/**
 * Turns recognised speech into one of a few fixed commands.
 *
 * No AI is involved: Android's speech recogniser converts the voice to text, and this
 * object looks for known Turkish keywords in that text. Anything else is answered with
 * a short help sentence.
 */
internal sealed interface VoiceCommand {
    data object NextPrayer : VoiceCommand
    data class PrayerTime(val timeKey: String) : VoiceCommand
    data object MarkPrayed : VoiceCommand
    data object OpenDhikr : VoiceCommand
    data object OpenDuas : VoiceCommand
    data object OpenTimes : VoiceCommand
    data object Today : VoiceCommand
    data object Help : VoiceCommand
    data object Unknown : VoiceCommand
}

internal object VoiceCommands {
    private val tr = Locale.forLanguageTag("tr-TR")

    private val prayerWords = listOf(
        "İmsak" to listOf("sabah", "imsak"),
        "Öğle" to listOf("öğle", "öğlen"),
        "İkindi" to listOf("ikindi"),
        "Akşam" to listOf("akşam"),
        "Yatsı" to listOf("yatsı")
    )

    /** Tries every alternative the recogniser returned and keeps the first one it understands. */
    fun parse(alternatives: List<String>): VoiceCommand =
        alternatives.asSequence().map(::parse).firstOrNull { it != VoiceCommand.Unknown } ?: VoiceCommand.Unknown

    fun parse(raw: String): VoiceCommand {
        val text = raw.lowercase(tr).trim()
        if (text.isEmpty()) return VoiceCommand.Unknown
        fun has(vararg words: String) = words.any { it in text }

        // Order matters: "akşam namazını kıldım" is a mark, not a time question.
        if (has("kıldım", "kildim", "kıldık")) return VoiceCommand.MarkPrayed
        if (has("tespih", "tesbih", "zikir", "zikr")) return VoiceCommand.OpenDhikr
        if (has("dua")) return VoiceCommand.OpenDuas
        if (has("yardım", "ne diyebilirim", "nasıl kullan")) return VoiceCommand.Help
        prayerWords.firstOrNull { (_, words) -> words.any { it in text } }?.let { (key, _) ->
            // "bugün akşam ne yapsam" would also match; keep it simple and answer the time.
            return VoiceCommand.PrayerTime(key)
        }
        if (has("bütün vakit", "tüm vakit", "vakitleri", "vakitler")) return VoiceCommand.OpenTimes
        if (has("tarih", "günlerden", "bugün ne", "hangi gün", "ayın kaçı")) return VoiceCommand.Today
        if (has("kaç", "kaldı", "vakit", "ezan", "sonraki", "ne zaman", "namaz")) return VoiceCommand.NextPrayer
        return VoiceCommand.Unknown
    }

    const val HELP = "Şunları söyleyebilirsiniz: Namaza ne kadar kaldı. Öğle ne zaman. " +
        "Namazımı kıldım. Tespih. Dua. Bugün günlerden ne."
}
