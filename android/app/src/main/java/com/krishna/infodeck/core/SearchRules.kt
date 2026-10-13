package com.krishna.infodeck.core

import java.text.Normalizer

/** One searchable field on a card, kept for the row hint. */
data class SearchField(val kind: Kind, val name: String, val value: String) {
    enum class Kind(val badge: String, val rank: Int) {
        LABEL("", 0), DATA("DATA", 1), DATE("DATE", 2), FILE("FILE", 3), TAG("TAG", 4), REMINDER("REMINDER", 5), CATEGORY("CATEGORY", 6)
    }
}

/** A hit on one row: the field that matched best and where the first word sits in its value. */
data class SearchHit(val field: SearchField, val start: Int, val end: Int)

/**
 * B11 — search reads EVERY field. One lowercase, accent-folded index per card built at load time;
 * every value is also indexed with separators stripped so "wb24ab" finds "WB 24 AB 1234".
 */
object SearchRules {
    const val MAX_QUERY = 100
    const val DEBOUNCE_MS = 150L

    fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

    fun stripped(s: String): String = fold(s).replace(Regex("[^\\p{L}\\p{N}]+"), "")

    /** The index string for a card: every field folded, plus its stripped twin. */
    fun index(fields: List<SearchField>): String = Logger.guard("Search.index", "") {
        fields.joinToString("\u0001") { f ->
            val n = fold(f.name); val v = fold(f.value)
            listOf(n, v, stripped(f.value)).filter { it.isNotEmpty() }.distinct().joinToString("\u0001")
        }
    }

    fun words(query: String): List<String> =
        fold(query.take(MAX_QUERY)).split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun wordHits(index: String, w: String): Boolean =
        index.contains(w) || stripped(w).let { it.isNotEmpty() && index.contains(it) }

    /** AND across words: every word must land somewhere on the card. */
    fun matches(index: String, query: String): Boolean {
        val ws = words(query)
        return ws.isEmpty() || ws.all { wordHits(index, it) }
    }

    /** The best matching field for the row hint — label hits show the normal meta line (null). */
    fun hit(fields: List<SearchField>, query: String): SearchHit? {
        val ws = words(query); if (ws.isEmpty()) return null
        val first = ws.first()
        val candidates = fields.mapNotNull { f ->
            val v = fold(f.value); val n = fold(f.name)
            val i = v.indexOf(first)
            when {
                i >= 0 -> SearchHit(f, i, i + first.length)
                n.contains(first) || stripped(f.value).contains(stripped(first)) -> SearchHit(f, -1, -1)
                else -> null
            }
        }
        val best = candidates.minByOrNull { it.field.kind.rank } ?: return null
        return if (best.field.kind == SearchField.Kind.LABEL) null else best
    }

    /** Rank for ordering results: label first, then data, dates, files, reminders. */
    fun rank(fields: List<SearchField>, query: String): Int {
        val ws = words(query); if (ws.isEmpty()) return 99
        val first = ws.first()
        return fields.filter { fold(it.value).contains(first) || fold(it.name).contains(first) }
            .minOfOrNull { it.kind.rank } ?: 9
    }

    /** Which words matched no card at all — for the empty state. */
    fun missing(indexes: List<String>, query: String): List<String> =
        words(query).filter { w -> indexes.none { wordHits(it, w) } }

    /** Bucket counts for the chips. */
    fun kindsHit(fields: List<SearchField>, query: String): Set<SearchField.Kind> {
        val ws = words(query); if (ws.isEmpty()) return emptySet()
        return fields.filter { f -> ws.any { w -> fold(f.value).contains(w) || fold(f.name).contains(w) || stripped(f.value).contains(stripped(w)) } }
            .map { it.kind }.toSet()
    }
}
