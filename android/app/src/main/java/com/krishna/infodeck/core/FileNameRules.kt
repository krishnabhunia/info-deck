package com.krishna.infodeck.core

/**
 * N62 · design 1.9.D1 — a file's display name is NAME + optional .EXT.
 *
 * Renaming changes the NAME only; the extension is locked. Changing it is a separate,
 * deliberate step followed by a confirmation, because a new extension never converts
 * the file — it only changes how other apps treat it.
 *
 * Everything here is pure and total: no Android, no I/O, never throws.
 */
object FileNameRules {
    /** Longest whole display name, extension included. */
    const val MAX_TOTAL = 80

    /** Longest extension, without the dot. */
    const val MAX_EXT = 8

    /** Extensions that trigger the "you typed an extension into the name" hint (Q5). */
    val KNOWN_TYPED = setOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "jpg", "jpeg", "png")

    /** Quick chips in Step 2 — the three kinds InfoDeck stores. */
    val CHIPS = listOf("pdf", "doc", "docx")

    /** Separators, reserved characters and every control character (matches FileRenameRules.validate). */
    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F\\u007F]")
    private val EXT_SHAPE = Regex("^[A-Za-z0-9]{1,$MAX_EXT}$")

    /** [ext] is stored without the dot; "" means the name has no extension. */
    data class Parts(val base: String, val ext: String) {
        val full: String get() = join(base, ext)
        val hasExt: Boolean get() = ext.isNotEmpty()
    }

    /**
     * Extension = the text after the LAST dot, when it is 1..8 ASCII letters/digits with at
     * least one letter, and something other than dots comes before it. The "one letter" rule
     * keeps "Policy v2.1" or "12.05.2026" whole instead of reading ".1" / ".2026" as an extension.
     */
    fun split(name: String?): Parts {
        val n = name ?: return Parts("", "")
        val dot = n.lastIndexOf('.')
        if (dot <= 0 || dot == n.length - 1) return Parts(n, "")
        val ext = n.substring(dot + 1)
        val base = n.substring(0, dot)
        if (!isExtShape(ext) || base.all { it == '.' }) return Parts(n, "")
        return Parts(base, ext)
    }

    fun join(base: String, ext: String): String = if (ext.isEmpty()) base else "$base.$ext"

    fun isExtShape(ext: String): Boolean = EXT_SHAPE.matches(ext) && ext.any { it.isLetter() }

    /** What the Extension box keeps: leading dots and anything but ASCII letters/digits dropped, max 8. */
    fun cleanExt(raw: String?): String =
        (raw ?: "").trimStart('.').filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }.take(MAX_EXT)

    /** Extensions compared ignoring case: .PDF → .pdf is not a change (Q1). */
    fun sameExt(a: String, b: String): Boolean = a.equals(b, ignoreCase = true)

    /** Longest NAME the box accepts so the whole name still fits in [MAX_TOTAL] (B13). */
    fun maxBase(ext: String): Int = MAX_TOTAL - (if (ext.isEmpty()) 0 else ext.length + 1)

    /**
     * Cleans a NAME and joins it with [ext], cutting the NAME — never the extension — to fit.
     * Separators and control characters become "_", dots/underscores/spaces are trimmed from
     * both ends, and a blank name becomes "file".
     */
    fun fit(base: String, ext: String): String {
        val e = cleanExt(ext).takeIf { isExtShape(it) } ?: ""
        val cleaned = trimEnds(base.replace(ILLEGAL, "_"))
        val safe = cleaned.ifBlank { "file" }
        val cut = trimEnds(safe.take(maxBase(e))).ifBlank { "file" }
        return join(cut, e)
    }

    /** Full-name cleaner used for every stored display name (attach and rename). */
    fun clean(name: String?): String {
        val p = split((name ?: "").replace(ILLEGAL, "_"))
        return fit(p.base, p.ext)
    }

    private fun trimEnds(s: String): String = s.trim { it == '.' || it == '_' || it.isWhitespace() }

    // ------------------------------------------------------------------ the confirm gate

    sealed interface Decision {
        /** Name-only (or case-only) change: saved straight away. */
        data class Direct(val name: String) : Decision
        /** The extension changes, is added or removed: nothing is written until confirmed. */
        data class NeedsConfirm(val name: String, val oldExt: String, val newExt: String) : Decision
        /** Blank name or an extension that is not a valid shape: Save stays disabled. */
        data object Invalid : Decision
    }

    /**
     * [newExt] is the extension the user ended with: the locked original in Step 1, the
     * Extension box in Step 2. Blank = remove the extension.
     */
    fun decide(oldName: String, newBase: String, newExt: String): Decision {
        if (newBase.isBlank()) return Decision.Invalid
        val old = split(oldName)
        val ext = cleanExt(newExt)
        if (ext.isNotEmpty() && !isExtShape(ext)) return Decision.Invalid
        val name = fit(newBase, ext)
        return if (sameExt(old.ext, ext)) Decision.Direct(name)
        else Decision.NeedsConfirm(name, old.ext, ext)
    }

    // ------------------------------------------------------------------ hints

    /** A known extension typed at the end of the NAME, as typed ("Policy.docx" → "docx"), else null. */
    fun typedExtension(base: String): String? {
        val p = split(base)
        return p.ext.takeIf { it.isNotEmpty() && it.lowercase() in KNOWN_TYPED }
    }

    /** The file's real type from its stored MIME: "pdf", "doc", "docx", or null when unknown. */
    fun realType(mime: String?): String? = when (mime?.trim()?.lowercase()) {
        "application/pdf" -> "pdf"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        else -> null
    }

    /** "PDF", "Word .doc file", "Word .docx file" — what the file really is. */
    fun realLabel(type: String): String = when (type) {
        "pdf" -> "PDF"
        "doc" -> "Word .doc file"
        "docx" -> "Word .docx file"
        else -> ".$type file"
    }

    /**
     * Label of the real type when the name's extension disagrees with it (Q3), else null.
     * Unknown MIME → null: never a wrong claim.
     */
    fun mismatch(displayName: String, mime: String?): String? {
        val real = realType(mime) ?: return null
        val ext = split(displayName).ext
        return if (sameExt(ext, real)) null else realLabel(real)
    }

    /** Snackbar text after a successful rename. */
    fun outcome(oldName: String, newName: String): String {
        val a = split(oldName).ext
        val b = split(newName).ext
        return when {
            sameExt(a, b) -> "Renamed"
            b.isEmpty() -> "Extension removed"
            a.isEmpty() -> "Extension .$b added"
            else -> "Extension changed to .$b"
        }
    }
}
