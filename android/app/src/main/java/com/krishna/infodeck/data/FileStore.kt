package com.krishna.infodeck.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.krishna.infodeck.core.Logger
import java.io.File
import java.util.UUID

/**
 * Attachments are COPIED into the app's private storage, so moving or deleting the
 * original never breaks a card, and no storage permission is ever requested.
 */
object FileStore {

    sealed class Result {
        data class Ok(val entity: FileEntity) : Result()
        data class Rejected(val reason: String) : Result()
    }

    private fun dir(context: Context, cardId: Long): File =
        File(context.filesDir, "cards/$cardId").apply { mkdirs() }

    fun resolve(context: Context, f: FileEntity): File = File(dir(context, f.cardId), f.storedName)

    fun exists(context: Context, f: FileEntity): Boolean = resolve(context, f).exists()

    /** name + size straight from the picker, before anything is copied. */
    fun probe(context: Context, uri: Uri): Pair<String, Long>? = Logger.guard("FileStore.probe", null) {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@guard null
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            val name = if (ni >= 0) c.getString(ni) ?: "file" else "file"
            val size = if (si >= 0 && !c.isNull(si)) c.getLong(si) else -1L
            name to size
        }
    }

    /**
     * Copies to a temp name first and renames on success, so a failed or half-written
     * copy never leaves a visible row behind.
     */
    suspend fun copyIn(context: Context, uri: Uri, cardId: Long, existingCount: Int, order: Int): Result {
        val probed = probe(context, uri)
        val rawName = probed?.first ?: "file"
        val declaredSize = probed?.second ?: -1L
        val mime = context.contentResolver.getType(uri) ?: ""
        val display = FileRules.sanitise(rawName)

        if (!FileRules.typeAllowed(mime, display)) {
            Logger.e("FileStore", "rejected type mime=$mime name=$display")
            return Result.Rejected("Only PDF and Word files can be attached")
        }
        if (existingCount >= FileRules.MAX_PER_CARD) {
            Logger.e("FileStore", "rejected: card already holds $existingCount files")
            return Result.Rejected("A card can hold ${FileRules.MAX_PER_CARD} files")
        }
        if (declaredSize > FileRules.MAX_BYTES) {
            Logger.e("FileStore", "rejected oversize $declaredSize bytes")
            return Result.Rejected("That file is larger than 25 MB")
        }

        val ext = display.substringAfterLast('.', "").lowercase()
        val stored = UUID.randomUUID().toString() + if (ext.isBlank()) "" else ".$ext"
        val target = File(dir(context, cardId), stored)
        val temp = File(dir(context, cardId), "$stored.part")

        return try {
            var written = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        written += n
                        if (written > FileRules.MAX_BYTES) {
                            out.flush(); temp.delete()
                            Logger.e("FileStore", "aborted copy over 25 MB (stream said $written)")
                            return Result.Rejected("That file is larger than 25 MB")
                        }
                        out.write(buf, 0, n)
                    }
                }
            } ?: run {
                temp.delete()
                Logger.e("FileStore", "no input stream for $uri")
                return Result.Rejected("That file could not be read")
            }
            if (!temp.renameTo(target)) {
                temp.delete()
                Logger.e("FileStore", "rename failed for $stored")
                return Result.Rejected("That file could not be saved")
            }
            Result.Ok(
                FileEntity(
                    cardId = cardId, displayName = display, storedName = stored,
                    mime = if (mime.isBlank()) "application/octet-stream" else mime,
                    sizeBytes = written, order = order
                )
            )
        } catch (t: Throwable) {
            temp.delete()
            Logger.e("FileStore", "copy failed", t)
            Result.Rejected("That file could not be copied")
        }
    }

    fun delete(context: Context, f: FileEntity) = Logger.guardUnit("FileStore.delete") {
        val file = resolve(context, f)
        if (file.exists() && !file.delete()) Logger.e("FileStore", "could not delete ${f.storedName}")
    }

    fun deleteAllFor(context: Context, cardId: Long) = Logger.guardUnit("FileStore.deleteCard") {
        File(context.filesDir, "cards/$cardId").deleteRecursively()
    }

    private fun uriFor(context: Context, f: FileEntity): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", resolve(context, f)
    )

    /** Returns an error string, or null when the file was handed off successfully. */
    fun open(context: Context, f: FileEntity): String? = Logger.guard("FileStore.open", "Could not open the file") {
        if (!exists(context, f)) {
            Logger.e("FileStore", "missing on disk: ${f.storedName}")
            return@guard "File missing - it is no longer on this phone"
        }
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, f), f.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(i); null
        } catch (t: Throwable) {
            Logger.e("FileStore", "no app can open ${f.mime}", t)
            "No app on this phone can open that file"
        }
    }

    fun share(context: Context, f: FileEntity): String? = Logger.guard("FileStore.share", "Could not share the file") {
        if (!exists(context, f)) return@guard "File missing - it is no longer on this phone"
        // B10 - the receiver shows the URI's name, so share an exact-name copy, never the UUID file
        val prepared = com.krishna.infodeck.ui.ShareFiles.prepare(context, listOf(resolve(context, f) to f.displayName)).firstOrNull()
        val i = Intent(Intent.ACTION_SEND).apply {
            type = f.mime
            putExtra(Intent.EXTRA_STREAM, if (prepared != null) com.krishna.infodeck.ui.Bundler.uri(context, prepared) else uriFor(context, f))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(i, "Share file").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); null
        } catch (t: Throwable) {
            Logger.e("FileStore", "share failed", t); "Nothing on this phone can share that file"
        }
    }
}
