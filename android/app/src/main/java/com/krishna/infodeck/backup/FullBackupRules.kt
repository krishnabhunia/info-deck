package com.krishna.infodeck.backup

/**
 * Layout of the full backup ZIP (design 1.15.0.D1):
 *
 *   infodeck-backup.json               backup schema 3 (cards, tags, reminders + file list)
 *   files/<cardId>/<storedName>        each attached PDF/Word file, byte for byte
 *
 * Pure rules only, so path safety is unit-tested without Android.
 */
object FullBackupRules {
    const val JSON_ENTRY = "infodeck-backup.json"
    const val SCHEMA_WITH_FILES = 3
    const val MAX_JSON_BYTES = 20L * 1024 * 1024
    const val MAX_ENTRIES = 5_000

    /** No slashes, no leading dot: an entry can never escape its card folder. */
    private val FILE_ENTRY = Regex("""^files/([1-9][0-9]{0,17})/([A-Za-z0-9][A-Za-z0-9._-]{0,127})$""")

    fun fileEntry(cardId: Long, storedName: String): String? =
        "files/$cardId/$storedName".takeIf { parseFileEntry(it) != null }

    /** (cardId, storedName) for a safe file entry; null for anything else. */
    fun parseFileEntry(name: String): Pair<Long, String>? {
        val m = FILE_ENTRY.matchEntire(name) ?: return null
        val id = m.groupValues[1].toLongOrNull() ?: return null
        val stored = m.groupValues[2]
        if (stored.contains("..")) return null
        return id to stored
    }

    /** ZIP local-file header "PK\u0003\u0004". JSON backups start with '{' or whitespace. */
    fun isZip(head: ByteArray): Boolean =
        head.size >= 4 && head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
            head[2] == 0x03.toByte() && head[3] == 0x04.toByte()
}
