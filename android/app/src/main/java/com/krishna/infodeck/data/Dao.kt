package com.krishna.infodeck.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InfoDao {

    // ---------- categories ----------
    @Query("SELECT * FROM categories ORDER BY builtIn ASC, sortOrder ASC, name COLLATE NOCASE ASC")
    fun categoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY builtIn ASC, sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun category(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name COLLATE NOCASE AND id != :exceptId")
    suspend fun countCategoryName(name: String, exceptId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(c: CategoryEntity): Long

    @Update
    suspend fun updateCategory(c: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND builtIn = 0")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT MAX(sortOrder) FROM categories")
    suspend fun maxCategoryOrder(): Int?

    // ---------- cards ----------
    @Query("SELECT * FROM cards")
    fun cardsFlow(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards")
    suspend fun cardsFlowOnce(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun cardFlow(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun card(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE categoryId = :categoryId")
    suspend fun cardsIn(categoryId: Long): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE categoryId = :categoryId")
    suspend fun countCardsIn(categoryId: Long): Int

    @Insert
    suspend fun insertCard(c: CardEntity): Long

    @Update
    suspend fun updateCard(c: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: Long)

    @Query("DELETE FROM cards WHERE id IN (:ids)")
    suspend fun deleteCards(ids: List<Long>)

    @Query("UPDATE cards SET categoryId = :categoryId, updatedAt = :now WHERE id IN (:ids)")
    suspend fun moveCards(ids: List<Long>, categoryId: Long, now: Long)

    @Query("SELECT MAX(manualOrder) FROM cards WHERE categoryId = :categoryId")
    suspend fun maxCardOrder(categoryId: Long): Int?

    // ---------- variables ----------
    @Query("SELECT * FROM data_vars ORDER BY varOrder ASC, id ASC")
    fun dataVarsFlow(): Flow<List<DataVarEntity>>

    @Query("SELECT * FROM data_vars WHERE cardId = :cardId ORDER BY varOrder ASC, id ASC")
    suspend fun dataVars(cardId: Long): List<DataVarEntity>

    @Query("SELECT * FROM date_vars ORDER BY varOrder ASC, id ASC")
    fun dateVarsFlow(): Flow<List<DateVarEntity>>

    @Query("SELECT * FROM date_vars WHERE cardId = :cardId ORDER BY varOrder ASC, id ASC")
    suspend fun dateVars(cardId: Long): List<DateVarEntity>

    @Query("SELECT * FROM date_vars WHERE id = :id")
    suspend fun dateVar(id: Long): DateVarEntity?

    @Insert
    suspend fun insertDataVar(v: DataVarEntity): Long

    @Insert
    suspend fun insertDateVar(v: DateVarEntity): Long

    @Query("DELETE FROM data_vars WHERE cardId = :cardId")
    suspend fun clearDataVars(cardId: Long)

    @Query("DELETE FROM date_vars WHERE cardId = :cardId")
    suspend fun clearDateVars(cardId: Long)

    // ---------- schedulers ----------
    @Query("SELECT * FROM schedulers")
    fun schedulersFlow(): Flow<List<SchedulerEntity>>

    @Query("SELECT * FROM schedulers")
    suspend fun allSchedulers(): List<SchedulerEntity>

    @Query("SELECT * FROM schedulers WHERE cardId = :cardId")
    suspend fun schedulers(cardId: Long): List<SchedulerEntity>

    @Query("SELECT * FROM schedulers WHERE id = :id")
    suspend fun scheduler(id: Long): SchedulerEntity?

    @Insert
    suspend fun insertScheduler(s: SchedulerEntity): Long

    @Update
    suspend fun updateScheduler(s: SchedulerEntity)

    @Delete
    suspend fun deleteScheduler(s: SchedulerEntity)

    @Query("DELETE FROM schedulers WHERE id = :id")
    suspend fun deleteSchedulerById(id: Long)

    @Query("DELETE FROM schedulers WHERE dateVarId = :dateVarId")
    suspend fun deleteSchedulersForDate(dateVarId: Long)

    @Query("DELETE FROM schedulers WHERE cardId = :cardId")
    suspend fun deleteSchedulersForCardOnly(cardId: Long)

    @Query("UPDATE schedulers SET enabled = :enabled, status = :status WHERE cardId IN (:cardIds)")
    suspend fun setEnabledForCards(cardIds: List<Long>, enabled: Boolean, status: RingStatus)

    // ---------- tags ----------
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun tagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY lastUsedAt DESC LIMIT 8")
    fun recentTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun tag(id: Long): TagEntity?

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name COLLATE NOCASE AND id != :exceptId")
    suspend fun countTagName(name: String, exceptId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(t: TagEntity): Long

    @Update
    suspend fun updateTag(t: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("SELECT * FROM card_tags")
    fun cardTagsFlow(): Flow<List<CardTagEntity>>

    @Query("SELECT tagId FROM card_tags WHERE cardId = :cardId")
    suspend fun tagIdsFor(cardId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTag(link: CardTagEntity)

    @Query("DELETE FROM card_tags WHERE cardId = :cardId")
    suspend fun clearTagsFor(cardId: Long)

    @Query("UPDATE OR IGNORE card_tags SET tagId = :into WHERE tagId = :from")
    suspend fun retagLinks(from: Long, into: Long)

    @Query("DELETE FROM card_tags WHERE tagId = :tagId")
    suspend fun clearLinksOfTag(tagId: Long)

    @Query("UPDATE tags SET lastUsedAt = :now WHERE id IN (:ids)")
    suspend fun touchTags(ids: List<Long>, now: Long)

    // ---------- files ----------
    @Query("SELECT * FROM files ORDER BY fileOrder ASC, id ASC")
    fun filesFlow(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE cardId = :cardId ORDER BY fileOrder ASC, id ASC")
    suspend fun files(cardId: Long): List<FileEntity>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun file(id: Long): FileEntity?

    @Query("SELECT COUNT(*) FROM files WHERE cardId = :cardId")
    suspend fun countFiles(cardId: Long): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM files")
    fun totalFileBytesFlow(): Flow<Long>

    @Insert
    suspend fun insertFile(f: FileEntity): Long

    @Update
    suspend fun updateFile(f: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFile(id: Long)

    @Query("SELECT * FROM files")
    suspend fun allFiles(): List<FileEntity>

    // ---------- ring log ----------
    @Query("SELECT * FROM ring_log ORDER BY firedAt DESC LIMIT 500")
    fun ringLogFlow(): Flow<List<RingLogEntity>>

    @Insert
    suspend fun insertRingLog(r: RingLogEntity): Long

    @Query("UPDATE ring_log SET action = :action, actedAt = :actedAt WHERE schedulerId = :schedulerId AND action IS NULL")
    suspend fun markRingAction(schedulerId: Long, action: RingAction, actedAt: Long)

    @Query("DELETE FROM ring_log WHERE id NOT IN (SELECT id FROM ring_log ORDER BY firedAt DESC LIMIT 500)")
    suspend fun trimRingLog()

    // ---------- error log ----------
    @Query("SELECT * FROM error_log ORDER BY ts DESC LIMIT 300")
    fun errorLogFlow(): Flow<List<ErrorLogEntity>>

    @Insert
    suspend fun insertError(e: ErrorLogEntity): Long

    @Query("DELETE FROM error_log")
    suspend fun clearErrors()

    @Query("SELECT COUNT(*) FROM error_log")
    fun errorCountFlow(): Flow<Int>

    // ---------- wipe ----------
    @Transaction
    suspend fun resetAll() {
        deleteAllCardTags(); deleteAllTags(); deleteAllFiles()
        deleteAllSchedulers(); deleteAllDataVars(); deleteAllDateVars()
        deleteAllCards(); deleteAllCategories(); deleteAllRingLog()
    }

    @Query("DELETE FROM schedulers") suspend fun deleteAllSchedulers()
    @Query("DELETE FROM card_tags") suspend fun deleteAllCardTags()
    @Query("DELETE FROM tags") suspend fun deleteAllTags()
    @Query("DELETE FROM files") suspend fun deleteAllFiles()
    @Query("DELETE FROM data_vars") suspend fun deleteAllDataVars()
    @Query("DELETE FROM date_vars") suspend fun deleteAllDateVars()
    @Query("DELETE FROM cards") suspend fun deleteAllCards()
    @Query("DELETE FROM categories WHERE builtIn = 0") suspend fun deleteAllCategories()
    @Query("DELETE FROM ring_log") suspend fun deleteAllRingLog()
}
