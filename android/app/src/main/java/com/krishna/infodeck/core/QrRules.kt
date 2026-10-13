package com.krishna.infodeck.core

/** QR content rules (design 1.4.D2 C4). Pure — the encoder only draws what this returns. */
object QrRules {

    /** Beyond this a code is too dense to scan reliably on a phone screen. */
    const val MAX_CHARS = 1200
    const val PNG_PX = 1024

    fun single(field: ShareField): String = ShareText.shown(field)

    /** One code with every chosen field: the card name, then "Label: value" lines. */
    fun combined(cardLabel: String, fields: List<ShareField>): String {
        val chosen = fields.filter { it.selected && it.kind != ShareField.Kind.NAME }
        val sb = StringBuilder()
        if (cardLabel.isNotBlank()) sb.append(cardLabel).append('\n')
        chosen.forEach { sb.append(it.label).append(": ").append(ShareText.shown(it)).append('\n') }
        return sb.toString().trim()
    }

    /** One code per field, for a counter clerk scanning one thing at a time. */
    fun perField(fields: List<ShareField>): List<Pair<String, String>> =
        fields.filter { it.selected && it.kind != ShareField.Kind.NAME }
            .map { it.label to ShareText.shown(it) }

    fun fits(content: String): Boolean = content.length in 1..MAX_CHARS

    fun tooLongBy(content: String): Int = (content.length - MAX_CHARS).coerceAtLeast(0)

    fun caption(content: String, fieldCount: Int): String = when {
        content.isEmpty() -> "Nothing selected"
        !fits(content) -> "Too long by ${tooLongBy(content)} characters - use one code per field"
        fieldCount <= 1 -> "${content.length} characters"
        else -> "$fieldCount fields - ${content.length} characters" +
            if (content.length < MAX_CHARS / 2) " - plenty of room" else ""
    }

    fun pngName(cardLabel: String, today: java.time.LocalDate): String =
        ShareText.fileName(cardLabel, today, "png")
}
