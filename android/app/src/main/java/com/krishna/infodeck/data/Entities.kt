package com.krishna.infodeck.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fixed row id of the built-in "Uncategorised" category. Never deleted, never renamed. */
const val UNCATEGORISED_ID: Long = 1L

/** 8 fixed category colours (index into UI palette). */
const val CATEGORY_COLOURS = 8

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val sortOrder: Int = 0,
    val builtIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cards",
    indices = [Index("categoryId")]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val label: String,
    /** id of the DateVar marked as the key date (the "expiry" this card is grouped/sorted by). */
    val keyDateVarId: Long? = null,
    val manualOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "data_vars",
    foreignKeys = [ForeignKey(
        entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId")]
)
data class DataVarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val name: String,
    val value: String,
    @ColumnInfo(name = "varOrder") val order: Int = 0
)

@Entity(
    tableName = "date_vars",
    foreignKeys = [ForeignKey(
        entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId")]
)
data class DateVarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val name: String,
    /** LocalDate.toEpochDay() — a date, never a moment. */
    val epochDay: Long,
    @ColumnInfo(name = "varOrder") val order: Int = 0
)

enum class RingUnit { SAME_DAY, DAYS, WEEKS, MONTHS, YEARS }
enum class RingDirection { BEFORE, AFTER }
enum class RingStatus { SCHEDULED, SNOOZED, RANG, DONE, PAST, OFF }
enum class RingAction { DONE, SNOOZED, DISMISSED }

@Entity(
    tableName = "schedulers",
    foreignKeys = [ForeignKey(
        entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId"), Index("dateVarId")]
)
data class SchedulerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val dateVarId: Long,
    val unit: RingUnit = RingUnit.SAME_DAY,
    val amount: Int = 1,
    val direction: RingDirection = RingDirection.BEFORE,
    /** minutes after midnight, 0..1439 */
    val ringMinute: Int = 9 * 60,
    val enabled: Boolean = true,
    val status: RingStatus = RingStatus.SCHEDULED,
    val snoozedUntil: Long? = null,
    val lastFiredAt: Long? = null
)

@Entity(tableName = "ring_log")
data class RingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schedulerId: Long,
    val cardId: Long,
    val cardLabel: String,
    val categoryName: String,
    val ruleText: String,
    val firedAt: Long,
    val action: RingAction? = null,
    val actedAt: Long? = null,
    val late: Boolean = false
)

/** Logger.e sink — surfaced on Settings > Error Logs. */
@Entity(tableName = "error_log")
data class ErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val tag: String,
    val message: String,
    val detail: String? = null
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "card_tags",
    primaryKeys = ["cardId", "tagId"],
    indices = [Index("cardId"), Index("tagId")],
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CardTagEntity(val cardId: Long, val tagId: Long)

/** PDF / Word attachment. The bytes live in files/cards/<cardId>/<storedName>. */
@Entity(
    tableName = "files",
    indices = [Index("cardId")],
    foreignKeys = [ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val displayName: String,
    val storedName: String,
    val mime: String,
    val sizeBytes: Long,
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "fileOrder") val order: Int = 0
)

object FileRules {
    const val MAX_BYTES: Long = 25L * 1024 * 1024
    const val MAX_PER_CARD = 10
    val ALLOWED = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )

    fun typeAllowed(mime: String?, name: String): Boolean {
        if (mime != null && ALLOWED.contains(mime)) return true
        val n = name.lowercase()
        return n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx")
    }

    fun kind(mime: String, name: String): String =
        if (mime == "application/pdf" || name.lowercase().endsWith(".pdf")) "PDF" else "DOC"

    /** Filenames are never trusted: no separators, no traversal, bounded length. */
    fun sanitise(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|\\u0000]"), "_")
            .trim { it == '.' || it == '_' || it.isWhitespace() }
        val safe = if (cleaned.isBlank()) "file" else cleaned
        return safe.take(80)
    }

    fun humanSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 -> String.format(java.util.Locale.ENGLISH, "%.1f MB", bytes / 1048576.0)
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}

object TagRules {
    const val MAX_NAME = 24
    fun normalise(raw: String): String = raw.trim().removePrefix("#").trim().take(MAX_NAME)
    fun display(name: String): String = "#" + name
}
