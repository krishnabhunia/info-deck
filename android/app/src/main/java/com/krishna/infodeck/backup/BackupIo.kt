package com.krishna.infodeck.backup

import android.content.Context
import android.net.Uri
import com.krishna.infodeck.core.Logger
import androidx.room.withTransaction
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.data.CardEntity
import com.krishna.infodeck.data.CategoryEntity
import com.krishna.infodeck.data.TagEntity
import com.krishna.infodeck.data.CardTagEntity
import com.krishna.infodeck.data.DataVarEntity
import com.krishna.infodeck.data.DateVarEntity
import com.krishna.infodeck.data.RingDirection
import com.krishna.infodeck.data.RingStatus
import com.krishna.infodeck.data.RingUnit
import com.krishna.infodeck.data.SchedulerEntity
import com.krishna.infodeck.data.UNCATEGORISED_ID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import com.krishna.infodeck.data.FileEntity
import com.krishna.infodeck.data.FileRules
import com.krishna.infodeck.data.FileStore
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable data class BCategory(val id: Long, val name: String, val colorIndex: Int, val sortOrder: Int, val builtIn: Boolean)
@Serializable data class BData(val name: String, val value: String, val order: Int)
@Serializable data class BDate(val localId: Long, val name: String, val epochDay: Long, val order: Int)
@Serializable data class BSched(
    val dateLocalId: Long, val unit: String, val amount: Int, val direction: String,
    val ringMinute: Int, val enabled: Boolean
)
@Serializable data class BCard(
    val id: Long, val categoryId: Long, val label: String, val keyDateLocalId: Long?,
    val manualOrder: Int, val createdAt: Long, val updatedAt: Long,
    val data: List<BData>, val dates: List<BDate>, val schedulers: List<BSched>
)
@Serializable data class BTag(val id: Long, val name: String)
@Serializable data class BCardTag(val cardId: Long, val tagId: Long)
/** Schema 3 (full backup ZIP): one attached file; the bytes live at files/<cardId>/<storedName>. */
@Serializable data class BFile(
    val cardId: Long, val displayName: String, val storedName: String,
    val mime: String, val sizeBytes: Long, val order: Int
)
@Serializable data class BackupFile(
    val app: String = "InfoDeck",
    val version: Int = 2,
    val exportedAt: Long,
    val categories: List<BCategory>,
    val cards: List<BCard>,
    val tags: List<BTag> = emptyList(),
    val cardTags: List<BCardTag> = emptyList(),
    val files: List<BFile> = emptyList()
)

/** JSON backup / restore through the system file picker. No network, ever. */
object BackupIo {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun build(db: AppDatabase, withFiles: Boolean = false): BackupFile {
        val dao = db.dao()
        val cats = dao.categories().map { BCategory(it.id, it.name, it.colorIndex, it.sortOrder, it.builtIn) }
        val cards = dao.cardsFlowOnce().map { c ->
            val dates = dao.dateVars(c.id)
            BCard(
                id = c.id, categoryId = c.categoryId, label = c.label,
                keyDateLocalId = c.keyDateVarId, manualOrder = c.manualOrder,
                createdAt = c.createdAt, updatedAt = c.updatedAt,
                data = dao.dataVars(c.id).map { BData(it.name, it.value, it.order) },
                dates = dates.map { BDate(it.id, it.name, it.epochDay, it.order) },
                schedulers = dao.schedulers(c.id).map {
                    BSched(it.dateVarId, it.unit.name, it.amount, it.direction.name, it.ringMinute, it.enabled)
                }
            )
        }
        return BackupFile(
            exportedAt = System.currentTimeMillis(), categories = cats, cards = cards,
            tags = dao.tagsFlow().first().map { BTag(it.id, it.name) },
            cardTags = dao.cardTagsFlow().first().map { BCardTag(it.cardId, it.tagId) },
            version = if (withFiles) FullBackupRules.SCHEMA_WITH_FILES else 2,
            files = if (!withFiles) emptyList() else dao.allFiles().map {
                BFile(it.cardId, it.displayName, it.storedName, it.mime, it.sizeBytes, it.order)
            }
        )
    }

    fun encode(b: BackupFile): String = json.encodeToString(b)

    fun decode(text: String): BackupFile? = try {
        json.decodeFromString(BackupFile.serializer(), text)
    } catch (t: Throwable) {
        Logger.e("BackupIo.decode", "not a valid InfoDeck backup", t); null
    }

    suspend fun export(context: Context, uri: Uri, db: AppDatabase): Boolean = try {
        val text = encode(build(db))
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            ?: throw IllegalStateException("no output stream")
        true
    } catch (t: Throwable) {
        Logger.e("BackupIo.export", "write failed", t); false
    }

    /**
     * Full backup (design 1.15.0.D2): a ZIP with the schema-3 JSON plus every attached file.
     * Files missing from storage are left out of the list, so the ZIP never promises bytes it lacks.
     */
    suspend fun exportFull(context: Context, uri: Uri, db: AppDatabase): Boolean = try {
        val all = build(db, withFiles = true)
        val present = all.files.filter { f ->
            FullBackupRules.fileEntry(f.cardId, f.storedName) != null &&
                File(context.filesDir, "cards/${f.cardId}/${f.storedName}").isFile
        }
        if (present.size != all.files.size) {
            Logger.e("BackupIo.exportFull", "${all.files.size - present.size} attached files missing from storage")
        }
        val backup = all.copy(files = present)
        context.contentResolver.openOutputStream(uri)?.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(FullBackupRules.JSON_ENTRY))
                zip.write(encode(backup).toByteArray())
                zip.closeEntry()
                present.forEach { f ->
                    zip.putNextEntry(ZipEntry(FullBackupRules.fileEntry(f.cardId, f.storedName)!!))
                    File(context.filesDir, "cards/${f.cardId}/${f.storedName}").inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("no output stream")
        true
    } catch (t: Throwable) {
        Logger.e("BackupIo.exportFull", "write failed", t); false
    }

    /** Restore either a JSON backup or a full-backup ZIP, decided by the file's first bytes. */
    suspend fun import(context: Context, uri: Uri, db: AppDatabase, replaceAll: Boolean): Int {
        val staging = File(context.cacheDir, "restore-${System.currentTimeMillis()}")
        return try {
            val head = ByteArray(4)
            val read = context.contentResolver.openInputStream(uri)?.use { readFully(it, head) }
                ?: throw IllegalStateException("no input stream")
            if (read == 4 && FullBackupRules.isZip(head)) {
                val text = unzipTo(context, uri, staging) ?: return -1
                restore(context, db, text, replaceAll, staging)
            } else {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("no input stream")
                restore(context, db, text, replaceAll, null)
            }
        } catch (t: Throwable) {
            Logger.e("BackupIo.import", "restore failed", t); -1
        } finally {
            if (staging.exists() && !staging.deleteRecursively()) {
                Logger.e("BackupIo.import", "could not clear restore staging folder")
            }
        }
    }

    private fun readFully(input: java.io.InputStream, into: ByteArray): Int {
        var n = 0
        while (n < into.size) {
            val r = input.read(into, n, into.size - n)
            if (r < 0) break
            n += r
        }
        return n
    }

    /** Unpacks only safe, size-limited entries. Returns the backup JSON text, or null. */
    private fun unzipTo(context: Context, uri: Uri, staging: File): String? {
        var json: String? = null
        var entries = 0
        staging.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    entries++
                    require(entries <= FullBackupRules.MAX_ENTRIES) { "too many entries in backup" }
                    if (!e.isDirectory) {
                        if (e.name == FullBackupRules.JSON_ENTRY) {
                            json = String(readCapped(zip, FullBackupRules.MAX_JSON_BYTES))
                        } else {
                            val parsed = FullBackupRules.parseFileEntry(e.name)
                            if (parsed == null) {
                                Logger.e("BackupIo.import", "skipped unexpected backup entry")
                            } else {
                                val out = File(staging, "${parsed.first}/${parsed.second}")
                                out.parentFile?.mkdirs()
                                out.outputStream().use { o -> o.write(readCapped(zip, FileRules.MAX_BYTES)) }
                            }
                        }
                    }
                    zip.closeEntry()
                    e = zip.nextEntry
                }
            }
        } ?: throw IllegalStateException("no input stream")
        if (json == null) Logger.e("BackupIo.import", "ZIP has no ${FullBackupRules.JSON_ENTRY}")
        return json
    }

    private fun readCapped(input: java.io.InputStream, max: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            require(total <= max) { "backup entry larger than allowed" }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /** replaceAll = wipe first; otherwise merge (new rows are added, ids re-generated). */
    private suspend fun restore(
        context: Context, db: AppDatabase, text: String, replaceAll: Boolean, staged: File?
    ): Int {
        val parsed = decode(text) ?: return -1
        if (parsed.app != "InfoDeck" || parsed.version !in 1..FullBackupRules.SCHEMA_WITH_FILES) {
            Logger.e("BackupIo.import", "unsupported backup app or version")
            return -1
        }
        val cardMap = HashMap<Long, Long>()
        // All relational changes either succeed together or roll back together.
        val restored = db.withTransaction {
            val dao = db.dao()
            if (replaceAll) dao.resetAll()

            val catMap = HashMap<Long, Long>()
            catMap[UNCATEGORISED_ID] = UNCATEGORISED_ID
            parsed.categories.filter { !it.builtIn }.forEach { c ->
                val existing = dao.categories().firstOrNull { it.name.equals(c.name, ignoreCase = true) }
                catMap[c.id] = existing?.id ?: dao.insertCategory(
                    CategoryEntity(name = c.name, colorIndex = c.colorIndex, sortOrder = c.sortOrder)
                )
            }

            var count = 0
            parsed.cards.forEach { c ->
                val catId = catMap[c.categoryId] ?: UNCATEGORISED_ID
                val cardId = dao.insertCard(
                    CardEntity(
                        categoryId = catId, label = c.label, manualOrder = c.manualOrder,
                        createdAt = c.createdAt, updatedAt = c.updatedAt
                    )
                )
                cardMap[c.id] = cardId
                c.data.forEach { dao.insertDataVar(DataVarEntity(cardId = cardId, name = it.name, value = it.value, order = it.order)) }
                val dateMap = HashMap<Long, Long>()
                c.dates.forEach {
                    dateMap[it.localId] = dao.insertDateVar(
                        DateVarEntity(cardId = cardId, name = it.name, epochDay = it.epochDay, order = it.order)
                    )
                }
                val keyId = c.keyDateLocalId?.let { dateMap[it] } ?: dateMap.values.firstOrNull()
                dao.card(cardId)?.let { dao.updateCard(it.copy(keyDateVarId = keyId)) }
                c.schedulers.forEach { s ->
                    val dId = dateMap[s.dateLocalId] ?: return@forEach
                    dao.insertScheduler(
                        SchedulerEntity(
                            cardId = cardId, dateVarId = dId,
                            unit = runCatching { RingUnit.valueOf(s.unit) }.getOrDefault(RingUnit.SAME_DAY),
                            amount = s.amount,
                            direction = runCatching { RingDirection.valueOf(s.direction) }.getOrDefault(RingDirection.BEFORE),
                            ringMinute = s.ringMinute, enabled = s.enabled,
                            status = if (s.enabled) RingStatus.SCHEDULED else RingStatus.OFF
                        )
                    )
                }
                count++
            }

            // Schema v2 restores tags and links, and remaps old card/tag IDs.
            val tagMap = HashMap<Long, Long>()
            val existingTags = dao.tagsFlow().first().associateBy { it.name.lowercase() }
            parsed.tags.forEach { tag ->
                tagMap[tag.id] = existingTags[tag.name.lowercase()]?.id
                    ?: dao.insertTag(TagEntity(name = tag.name))
            }
            parsed.cardTags.forEach { link ->
                val newCard = cardMap[link.cardId]
                val newTag = tagMap[link.tagId]
                if (newCard != null && newTag != null) {
                    dao.linkTag(CardTagEntity(cardId = newCard, tagId = newTag))
                }
            }
            count
        }
        if (replaceAll) {
            // Old file storage is removed only after the database commit succeeds.
            val oldFiles = File(context.filesDir, "cards")
            if (oldFiles.exists() && !oldFiles.deleteRecursively()) {
                Logger.e("BackupIo.import", "could not clear stale attachment storage")
            }
        }
        if (staged != null) restoreFiles(context, db, parsed.files, cardMap, staged)
        return restored
    }

    /** Copies each staged attachment into its new card; one bad file never stops the rest. */
    private suspend fun restoreFiles(
        context: Context, db: AppDatabase, files: List<BFile>, cardMap: Map<Long, Long>, staged: File
    ) {
        val perCard = HashMap<Long, Int>()
        var failed = 0
        files.sortedBy { it.order }.forEach { f ->
            try {
                val newCard = cardMap[f.cardId] ?: error("card not restored")
                val source = File(staged, "${f.cardId}/${f.storedName}")
                require(FullBackupRules.parseFileEntry("files/${f.cardId}/${f.storedName}") != null) { "unsafe name" }
                require(source.isFile && source.length() in 1..FileRules.MAX_BYTES) { "file bytes missing" }
                val used = perCard.getOrDefault(newCard, 0)
                require(used < FileRules.MAX_PER_CARD) { "card file limit" }
                val ext = f.storedName.substringAfterLast('.', "").lowercase()
                val stored = UUID.randomUUID().toString() + if (ext.isBlank()) "" else ".$ext"
                val entity = FileEntity(
                    cardId = newCard, displayName = f.displayName, storedName = stored,
                    mime = f.mime, sizeBytes = source.length(), order = f.order
                )
                val target = FileStore.resolve(context, entity)
                source.copyTo(target, overwrite = false)
                try {
                    db.dao().insertFile(entity)
                } catch (t: Throwable) {
                    target.delete(); throw t
                }
                perCard[newCard] = used + 1
            } catch (t: Throwable) {
                failed++
                Logger.e("BackupIo.restoreFiles", "could not restore one attached file", t)
            }
        }
        if (failed > 0) Logger.e("BackupIo.restoreFiles", "$failed of ${files.size} attached files not restored")
    }
}
