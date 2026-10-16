package com.krishna.infodeck.backup

/**
 * Layout of the full backup ZIP (design 1.15.0.D2):
 *
 *   infodeck-backup.json               backup schema 3 (cards, tags, reminders + file list)
 *   files/<index>                      each attached PDF/Word file, byte for byte (BFile.index)
 *
 * Pure rules only, so path safety is unit-tested without Android.
 */
object FullBackupRules {
    const val JSON_ENTRY = "infodeck-backup.json"
    const val SCHEMA_WITH_FILES = 3
    const val MAX_JSON_BYTES = 20L * 1024 * 1024
    const val MAX_ENTRIES = 5_000
    /** Upper bound for all restored attachment bytes together. */
    const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024
    /** Restore stops before the phone's free space drops below this. */
    const val FREE_SPACE_MARGIN = 64L * 1024 * 1024

    /** Digits only: an entry can never carry a path, a separator or a dot. */
    private val FILE_ENTRY = Regex("""^files/(0|[1-9][0-9]{0,5})$""")
    private val EXTENSION = Regex("""^[A-Za-z0-9]{1,10}$""")

    fun fileEntry(index: Int): String {
        require(index >= 0) { "negative file index" }
        return "files/$index"
    }

    /** File index for a safe file entry; null for anything else. */
    fun parseFileEntry(name: String): Int? = FILE_ENTRY.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()

    /** Lower-case extension of a display name when it is plain letters/digits, else "". */
    fun safeExtension(displayName: String): String {
        val ext = displayName.substringAfterLast('.', "")
        return if (EXTENSION.matches(ext)) ext.lowercase() else ""
    }

    /** ZIP local-file header "PK\u0003\u0004". JSON backups start with '{' or whitespace. */
    fun isZip(head: ByteArray): Boolean =
        head.size >= 4 && head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
            head[2] == 0x03.toByte() && head[3] == 0x04.toByte()
}
