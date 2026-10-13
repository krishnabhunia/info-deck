package com.krishna.infodeck.core

import java.time.LocalDate

/** One shareable field, in the order it appears on the card. */
data class ShareField(
    val key: String,
    val label: String,
    val value: String,
    val kind: Kind,
    val selected: Boolean = false,
    val masked: Boolean = false
) {
    enum class Kind { NAME, CATEGORY, DATA, DATE, REMINDER, FILE }
}

enum class SharePreset(val label: String) {
    LAST_USED("Last used"), EVERYTHING("Everything"), IDS_ONLY("IDs only"), DATES_ONLY("Dates only")
}

/**
 * Everything about what leaves the app (design 1.4.D2 C3). Pure — the sheet, the
 * preview, the .txt export and the QR all read the same builder.
 */
object ShareText {

    const val MASK_KEEP = 4

    /** Masking keeps the first four characters: HD-2291-KX-2026 -> HD-2*********** */
    fun mask(value: String): String {
        if (value.length <= MASK_KEEP) return "*".repeat(value.length.coerceAtLeast(1))
        return value.take(MASK_KEEP) + "*".repeat(value.length - MASK_KEEP)
    }

    fun shown(f: ShareField): String = if (f.masked) mask(f.value) else f.value

    /** Default ticks: card name, all data, the key date (design 1.4.D2 S2 note 5). */
    fun applyPreset(fields: List<ShareField>, preset: SharePreset, keyDateKey: String?): List<ShareField> =
        fields.map { f ->
            val on = when (preset) {
                SharePreset.EVERYTHING -> true
                SharePreset.IDS_ONLY -> f.kind == ShareField.Kind.DATA || f.kind == ShareField.Kind.NAME
                SharePreset.DATES_ONLY -> f.kind == ShareField.Kind.DATE || f.kind == ShareField.Kind.NAME
                SharePreset.LAST_USED -> when (f.kind) {
                    ShareField.Kind.NAME, ShareField.Kind.DATA -> true
                    ShareField.Kind.DATE -> f.key == keyDateKey
                    else -> false
                }
            }
            f.copy(selected = on)
        }

    /** A remembered selection is validated against the card as it is NOW; gone fields are dropped. */
    fun applyRemembered(fields: List<ShareField>, rememberedKeys: Set<String>): List<ShareField> =
        fields.map { it.copy(selected = rememberedKeys.contains(it.key)) }

    fun staleKeys(fields: List<ShareField>, rememberedKeys: Set<String>): Set<String> =
        rememberedKeys - fields.map { it.key }.toSet()

    fun selectedCount(fields: List<ShareField>): Int = fields.count { it.selected }

    /** The exact text that leaves the app. Name first, then data, dates, reminders. */
    fun build(fields: List<ShareField>): String {
        val chosen = fields.filter { it.selected && it.kind != ShareField.Kind.FILE }
        if (chosen.isEmpty()) return ""
        val sb = StringBuilder()
        chosen.firstOrNull { it.kind == ShareField.Kind.NAME }?.let { sb.append(shown(it)).append("\n\n") }
        // Files are NEVER text (bug B5): they travel as attachments or not at all.
        val order = listOf(
            ShareField.Kind.CATEGORY, ShareField.Kind.DATA,
            ShareField.Kind.DATE, ShareField.Kind.REMINDER
        )
        order.forEach { kind ->
            chosen.filter { it.kind == kind }.forEach { sb.append(it.label).append(": ").append(shown(it)).append('\n') }
        }
        return sb.toString().trim()
    }

    /** Many cards -> one CSV. Columns are the union of the ticked labels; dates ISO so sheets sort. */
    fun buildCsv(cards: List<Pair<String, List<ShareField>>>): String {
        val columns = LinkedHashSet<String>()
        cards.forEach { (_, fields) ->
            fields.filter { it.selected && it.kind != ShareField.Kind.NAME }.forEach { columns.add(it.label) }
        }
        val sb = StringBuilder()
        sb.append("Card").append(',').append(columns.joinToString(",") { csv(it) }).append('\n')
        cards.forEach { (name, fields) ->
            val byLabel = fields.filter { it.selected }.associateBy { it.label }
            sb.append(csv(name))
            columns.forEach { col -> sb.append(',').append(csv(byLabel[col]?.let { shown(it) } ?: "")) }
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun csv(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"" + v.replace("\"", "\"\"") + "\"" else v

    fun isoDate(d: LocalDate): String = d.toString()

    /** InfoDeck-CarInsurance-18Aug2026.txt — safe on every filesystem. */
    fun fileName(cardLabel: String, today: LocalDate, extension: String): String {
        val slug = cardLabel.filter { it.isLetterOrDigit() }.take(24).ifBlank { "Card" }
        val date = Fmt.date(today).replace("-", "")
        return "InfoDeck-$slug-$date.$extension"
    }
}

/** Share targets InfoDeck offers directly (design 1.4.D2 C2). */
enum class ShareTarget(val label: String, val pkg: String?, val filesOk: Boolean) {
    WHATSAPP("WhatsApp", "com.whatsapp", true),
    GMAIL("Gmail", "com.google.android.gm", true),
    SMS("SMS", null, false),
    EMAIL("Email", null, true),
    CHOOSER("All other apps", null, true);

    val isSms: Boolean get() = this == SMS
}
