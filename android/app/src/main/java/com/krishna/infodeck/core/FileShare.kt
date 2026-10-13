package com.krishna.infodeck.core

/** How the ticked files travel (design 1.5.D2 C1). */
enum class ShareMode { ATTACH, BUNDLE }

/** What an app can carry — shown as a badge rather than promised (design 1.5.D2 C2). */
enum class CarryBadge { BOTH_FINE, TEXT_MAY_DROP, NO_FILES, UNKNOWN }

object FileShare {

    /** Most mail refuses beyond this. Warned before an app is chosen, never after. */
    const val WARN_BYTES = 25L * 1024 * 1024

    fun mime(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "doc" -> "application/msword"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    /** A mixed selection has to fall back to the generic type. */
    fun commonMime(fileNames: List<String>): String {
        if (fileNames.isEmpty()) return "text/plain"
        val all = fileNames.map { mime(it) }.distinct()
        return if (all.size == 1) all.first() else "*/*"
    }

    fun badge(target: ShareTarget, withFiles: Boolean): CarryBadge = when {
        !withFiles -> CarryBadge.BOTH_FINE
        target == ShareTarget.SMS -> CarryBadge.NO_FILES
        target == ShareTarget.WHATSAPP -> CarryBadge.TEXT_MAY_DROP
        target == ShareTarget.GMAIL || target == ShareTarget.EMAIL -> CarryBadge.BOTH_FINE
        target == ShareTarget.CHOOSER -> CarryBadge.UNKNOWN
        else -> {
            Logger.e("FileShare", "unknown target ${target.name} reached the badge map")
            CarryBadge.UNKNOWN
        }
    }

    /** SMS is dropped from the sheet entirely once a file is ticked. */
    fun offers(target: ShareTarget, withFiles: Boolean): Boolean =
        !(withFiles && target == ShareTarget.SMS)

    /** The rule Krishna's masking feature needs: a mask cannot edit a document. */
    fun maskingConflict(fields: List<ShareField>): Boolean {
        val masked = fields.count { it.selected && it.masked }
        val files = fields.count { it.selected && it.kind == ShareField.Kind.FILE }
        return masked > 0 && files > 0
    }

    fun tooBig(totalBytes: Long): Boolean = totalBytes > WARN_BYTES

    fun humanSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> (bytes / 1024).toString() + " KB"
        else -> bytes.toString() + " B"
    }

    fun totalBytes(sizes: List<Long>): Long = sizes.filter { it > 0 }.sum()

    /** Two files can share a name; a zip cannot. */
    fun uniqueEntryNames(names: List<String>): List<String> {
        val seen = mutableMapOf<String, Int>()
        return names.map { raw ->
            val n = seen.getOrDefault(raw, 0)
            seen[raw] = n + 1
            if (n == 0) raw else {
                val stem = raw.substringBeforeLast('.', raw)
                val ext = raw.substringAfterLast('.', "")
                if (ext.isEmpty()) "$stem ($n)" else "$stem ($n).$ext"
            }
        }
    }

    fun zipName(cardLabel: String, today: java.time.LocalDate): String =
        ShareText.fileName(cardLabel, today, "zip")

    fun textEntryName(cardLabel: String): String {
        val safe = cardLabel.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }
            .joinToString("").trim().ifBlank { "Card" }
        return "$safe.txt"
    }

    /** Attach one or many; bundle from two upwards is offered, never forced. */
    fun bundleOffered(fileCount: Int): Boolean = fileCount >= 2

    fun defaultMode(fileCount: Int): ShareMode = if (fileCount >= 2) ShareMode.ATTACH else ShareMode.ATTACH
}


/** B5 — how a share reaches a target: one send, or text now and files as a follow-up. */
enum class SendPlan { TEXT_ONLY, ONE_SEND_WITH_FILES, TEXT_THEN_FILES }

object SharePlanner {
    /** Mail carries text and attachments together; chat apps keep the file and drop the words. */
    fun plan(target: ShareTarget, fileCount: Int): SendPlan = when {
        fileCount <= 0 -> SendPlan.TEXT_ONLY
        target == ShareTarget.SMS -> SendPlan.TEXT_ONLY
        target == ShareTarget.GMAIL || target == ShareTarget.EMAIL -> SendPlan.ONE_SEND_WITH_FILES
        target == ShareTarget.WHATSAPP -> SendPlan.TEXT_THEN_FILES
        target == ShareTarget.CHOOSER -> SendPlan.ONE_SEND_WITH_FILES
        else -> {
            Logger.e("SharePlanner", "unknown target ${target.name}, sending text only")
            SendPlan.TEXT_ONLY
        }
    }

    fun followUpMessage(fileCount: Int, target: ShareTarget): String =
        (if (fileCount == 1) "1 file ready for " else "$fileCount files ready for ") + target.label
}
