package com.krishna.infodeck.data

/**
 * Pure validation for user-visible attachment names. Renaming never converts or
 * modifies the bytes or the MIME type of the stored attachment.
 */
object FileRenameRules {
    const val MAX_FILENAME_LENGTH = 80
    const val MAX_EXTENSION_LENGTH = 12

    data class NameParts(val stem: String, val extension: String)

    /** Only the final dot separates the extension; other dots stay in the stem. */
    fun parts(name: String): NameParts {
        val dot = name.lastIndexOf('.')
        return if (dot > 0 && dot < name.lastIndex) {
            NameParts(name.substring(0, dot), name.substring(dot + 1))
        } else {
            NameParts(name, "")
        }
    }

    fun compose(stem: String, extension: String): String =
        if (extension.isEmpty()) stem else stem + "." + extension

    /**
     * Design A: compare the actual extension spelling exactly, including case.
     * FileNameRules.split still protects version-like suffixes (Policy v2.1).
     */
    fun extensionChanged(oldName: String, newName: String): Boolean =
        com.krishna.infodeck.core.FileNameRules.split(oldName).ext !=
            com.krishna.infodeck.core.FileNameRules.split(newName).ext

    /**
     * Even callers bypassing the UI cannot change extensions without an
     * explicit confirmation flag. Reject invalid input instead of sanitising
     * it into a different, surprising name.
     */
    fun validate(oldName: String, newName: String, extensionConfirmed: Boolean): String? {
        if (newName.isBlank() || parts(newName).stem.isBlank()) return "Enter a file name"
        if (newName.length > MAX_FILENAME_LENGTH) return "File name must be 80 characters or fewer"
        if (newName != newName.trim() || newName.startsWith('.') || newName.endsWith('.')) {
            return "Remove leading/trailing spaces or dots"
        }
        if (newName.any { it.code < 32 || it.code == 127 || it.code == 92 || it == '"' || it in "/:*?<>|" }) {
            return "File name contains unsupported characters"
        }
        val ext = parts(newName).extension
        if (ext.length > MAX_EXTENSION_LENGTH || ext.any { it.code > 127 || !it.isLetterOrDigit() }) {
            return "Extension must contain at most 12 English letters or digits"
        }
        if (extensionChanged(oldName, newName) && !extensionConfirmed) {
            return "Confirm the extension change before saving"
        }
        return null
    }
}
