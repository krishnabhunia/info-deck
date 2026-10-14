package com.krishna.infodeck.data

import androidx.room.withTransaction
import java.io.File

import com.krishna.infodeck.core.CardRow
import com.krishna.infodeck.core.CategoryRow
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.core.RingCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

/** A scheduler with everything needed to show and fire it. */
data class SchedulerView(
    val entity: SchedulerEntity,
    val dateName: String,
    val baseDate: LocalDate?,
    val ringAt: LocalDateTime?,
    val ruleText: String,
    val statusText: String
)

data class CardDetail(
    val card: CardEntity,
    val categoryName: String,
    val colorIndex: Int,
    val data: List<DataVarEntity>,
    val dates: List<DateVarEntity>,
    val schedulers: List<SchedulerView>,
    val tags: List<TagEntity> = emptyList(),
    val files: List<FileEntity> = emptyList()
)

class Repo(private val db: AppDatabase) {

    private val dao = db.dao()

    val categoriesFlow: Flow<List<CategoryRow>> = dao.categoriesFlow().map { list ->
        list.map { CategoryRow(it.id, it.name, it.colorIndex, it.sortOrder, it.builtIn) }
    }

    val errorCountFlow: Flow<Int> = dao.errorCountFlow()
    val tagsFlow: Flow<List<TagEntity>> = dao.tagsFlow()
    val recentTagsFlow: Flow<List<TagEntity>> = dao.recentTagsFlow()
    val totalFileBytesFlow: Flow<Long> = dao.totalFileBytesFlow()
    val errorLogFlow = dao.errorLogFlow()
    val ringLogFlow = dao.ringLogFlow()

    /** Home rows — every derived value (key date, next ring) computed here, never stored. */
    private data class Extras(
        val links: List<CardTagEntity>, val tags: List<TagEntity>, val files: List<FileEntity>
    )

    private val extrasFlow: Flow<Extras> = combine(
        dao.cardTagsFlow(), dao.tagsFlow(), dao.filesFlow()
    ) { links, tags, files -> Extras(links, tags, files) }

    private data class Core(
        val cards: List<CardEntity>, val cats: List<CategoryEntity>,
        val dataVars: List<DataVarEntity>, val dateVars: List<DateVarEntity>,
        val schedulers: List<SchedulerEntity>
    )

    private val coreFlow: Flow<Core> = combine(
        dao.cardsFlow(), dao.categoriesFlow(), dao.dataVarsFlow(), dao.dateVarsFlow(), dao.schedulersFlow()
    ) { cards, cats, dataVars, dateVars, schedulers -> Core(cards, cats, dataVars, dateVars, schedulers) }

    val rowsFlow: Flow<List<CardRow>> = combine(coreFlow, extrasFlow) { c, extras ->
        val cards = c.cards; val cats = c.cats
        val dataVars = c.dataVars; val dateVars = c.dateVars; val schedulers = c.schedulers
        val tagById = extras.tags.associateBy { it.id }
        val tagIdsByCard = extras.links.groupBy({ it.cardId }, { it.tagId })
        val filesByCard = extras.files.groupBy { it.cardId }
        Logger.guard("Repo.rows", emptyList()) {
            val catById = cats.associateBy { it.id }
            val dataByCard = dataVars.groupBy { it.cardId }
            val dateByCard = dateVars.groupBy { it.cardId }
            val dateById = dateVars.associateBy { it.id }
            val schedByCard = schedulers.groupBy { it.cardId }
            val now = LocalDateTime.now()

            cards.map { c ->
                val cat = catById[c.categoryId]
                val dates = dateByCard[c.id].orEmpty()
                val keyVar = dates.firstOrNull { it.id == c.keyDateVarId } ?: dates.firstOrNull()
                val data = dataByCard[c.id].orEmpty()
                val scheds = schedByCard[c.id].orEmpty()
                val views = scheds.map { toView(it, dateById[it.dateVarId]) }
                val next = views.filter { it.entity.enabled && it.ringAt != null && !it.ringAt.isBefore(now) }
                    .minByOrNull { it.ringAt!! }
                val searchFields = buildList {
                    add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.LABEL, "Card", c.label))
                    add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.CATEGORY, "Category", cat?.name ?: "Uncategorised"))
                    data.forEach { add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.DATA, it.name, it.value)) }
                    dates.forEach { d ->
                        val ld = LocalDate.ofEpochDay(d.epochDay)
                        add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.DATE, d.name,
                            com.krishna.infodeck.core.Fmt.date(ld) + " " + ld.toString()))
                    }
                    filesByCard[c.id].orEmpty().forEach { add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.FILE, "File", it.displayName)) }
                    tagIdsByCard[c.id].orEmpty().mapNotNull { tagById[it]?.name }.forEach { add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.TAG, "Tag", it)) }
                    views.forEach { v -> add(com.krishna.infodeck.core.SearchField(com.krishna.infodeck.core.SearchField.Kind.REMINDER, "Reminder", v.ruleText)) }
                }
                CardRow(
                    id = c.id,
                    label = c.label,
                    searchFields = searchFields,
                    searchIndex = com.krishna.infodeck.core.SearchRules.index(searchFields),
                    categoryId = c.categoryId,
                    categoryName = cat?.name ?: "Uncategorised",
                    colorIndex = cat?.colorIndex ?: 7,
                    keyDateName = keyVar?.name,
                    keyDate = keyVar?.let { LocalDate.ofEpochDay(it.epochDay) },
                    firstDataName = data.firstOrNull()?.name,
                    dataPreview = data.take(3).map { it.name to it.value },
                    firstDataValue = data.firstOrNull()?.value,
                    tagIds = tagIdsByCard[c.id].orEmpty().toSet(),
                    tagNames = tagIdsByCard[c.id].orEmpty().mapNotNull { tagById[it]?.name }.sorted(),
                    fileCount = filesByCard[c.id]?.size ?: 0,
                    hasData = data.isNotEmpty(),
                    hasDate = dates.isNotEmpty(),
                    nextRingAt = next?.ringAt,
                    nextRingRule = next?.ruleText,
                    hasReminder = scheds.any { it.enabled },
                    anySnoozed = scheds.any { it.status == RingStatus.SNOOZED },
                    anyOff = scheds.any { !it.enabled },
                    manualOrder = c.manualOrder,
                    createdAt = c.createdAt,
                    updatedAt = c.updatedAt
                )
            }
        }
    }

    fun toView(s: SchedulerEntity, dv: DateVarEntity?): SchedulerView {
        val base = dv?.let { LocalDate.ofEpochDay(it.epochDay) }
        val name = dv?.name ?: "(date removed)"
        val computed = base?.let {
            RingCalculator.ringDateTime(it, s.unit, s.amount, s.direction, s.ringMinute)
        }
        val effective = if (s.status == RingStatus.SNOOZED && s.snoozedUntil != null)
            Fmt.fromMillis(s.snoozedUntil) else computed
        val now = LocalDateTime.now()
        val status = when {
            !s.enabled -> "Off"
            s.status == RingStatus.SNOOZED && s.snoozedUntil != null -> "Snoozed until ${Fmt.dateTime(effective)}"
            s.status == RingStatus.DONE -> "Done"
            s.status == RingStatus.RANG -> "Rang"
            effective == null -> "No date"
            effective.isBefore(now) -> "Past (won't ring)"
            else -> "Scheduled"
        }
        return SchedulerView(s, name, base, effective, RingCalculator.ruleText(s.unit, s.amount, s.direction, name), status)
    }

    suspend fun cardDetail(cardId: Long): CardDetail? = Logger.guard("Repo.detail", null) {
        val card = dao.card(cardId) ?: return@guard null
        val cat = dao.category(card.categoryId)
        val dates = dao.dateVars(cardId)
        val dateById = dates.associateBy { it.id }
        CardDetail(
            card = card,
            categoryName = cat?.name ?: "Uncategorised",
            colorIndex = cat?.colorIndex ?: 7,
            data = dao.dataVars(cardId),
            dates = dates,
            schedulers = dao.schedulers(cardId).map { toView(it, dateById[it.dateVarId]) },
            tags = dao.tagIdsFor(cardId).mapNotNull { dao.tag(it) }.sortedBy { it.name.lowercase() },
            files = dao.files(cardId)
        )
    }

    // ---------------- categories ----------------

    suspend fun categoryNameTaken(name: String, exceptId: Long = 0): Boolean =
        Logger.guard("Repo.catName", false) { dao.countCategoryName(name.trim(), exceptId) > 0 }

    suspend fun addCategory(name: String, colorIndex: Int): Long = Logger.guard("Repo.addCat", -1L) {
        val order = (dao.maxCategoryOrder() ?: 0) + 1
        dao.insertCategory(
            CategoryEntity(name = name.trim().take(40), colorIndex = colorIndex.coerceIn(0, CATEGORY_COLOURS - 1), sortOrder = order)
        )
    }

    suspend fun updateCategory(id: Long, name: String, colorIndex: Int) = Logger.guardUnit("Repo.updCat") {
        val c = dao.category(id) ?: return@guardUnit
        if (c.builtIn) return@guardUnit
        dao.updateCategory(c.copy(name = name.trim().take(40), colorIndex = colorIndex, updatedAt = System.currentTimeMillis()))
    }

    suspend fun reorderCategories(idsInOrder: List<Long>) = Logger.guardUnit("Repo.reorderCat") {
        idsInOrder.forEachIndexed { i, id ->
            dao.category(id)?.let { if (!it.builtIn) dao.updateCategory(it.copy(sortOrder = i)) }
        }
    }

    /** moveTo == null means "delete the cards too". */
    suspend fun deleteCategory(context: android.content.Context, id: Long, moveTo: Long?) = Logger.guardUnit("Repo.delCat") {
        val c = dao.category(id) ?: return@guardUnit
        if (c.builtIn) return@guardUnit
        val cards = dao.cardsIn(id)
        db.withTransaction {
            if (moveTo != null) dao.moveCards(cards.map { it.id }, moveTo, System.currentTimeMillis())
            else dao.deleteCards(cards.map { it.id })
            dao.deleteCategory(id)
        }
        if (moveTo == null) cards.forEach { FileStore.deleteAllFor(context, it.id) }
    }

    suspend fun countCardsIn(id: Long): Int = Logger.guard("Repo.count", 0) { dao.countCardsIn(id) }

    // ---------------- cards ----------------

    data class DataInput(val name: String, val value: String)
    data class DateInput(val id: Long, val name: String, val epochDay: Long, val isKey: Boolean)
    data class SchedInput(
        val id: Long, val dateLocalId: Long, val unit: RingUnit, val amount: Int,
        val direction: RingDirection, val ringMinute: Int, val enabled: Boolean
    )

    /**
     * One transaction for the whole card. Date rows are rewritten, so schedulers
     * are re-pointed by their local (editor) date id.
     */
    suspend fun saveCard(
        cardId: Long?, label: String, categoryId: Long,
        data: List<DataInput>, dates: List<DateInput>, scheds: List<SchedInput>,
        tagIds: Set<Long> = emptySet()
    ): Long = Logger.guard("Repo.saveCard", -1L) {
        db.withTransaction {
        val now = System.currentTimeMillis()
        val id: Long
        if (cardId == null || cardId <= 0) {
            val order = (dao.maxCardOrder(categoryId) ?: 0) + 1
            id = dao.insertCard(
                CardEntity(categoryId = categoryId, label = label.trim().take(60), manualOrder = order, createdAt = now, updatedAt = now)
            )
        } else {
            id = cardId
            val existing = dao.card(id)
            if (existing != null) {
                dao.updateCard(existing.copy(label = label.trim().take(60), categoryId = categoryId, updatedAt = now))
            }
        }

        val oldScheds = dao.schedulers(id).associateBy { it.id }
        dao.clearDataVars(id)
        data.filter { it.name.isNotBlank() || it.value.isNotBlank() }
            .forEachIndexed { i, d ->
                dao.insertDataVar(DataVarEntity(cardId = id, name = d.name.trim().take(40), value = d.value.take(500), order = i))
            }

        // Rewrite dates, remembering local id -> new row id so schedulers keep their base.
        dao.deleteSchedulersForCardOnly(id)
        dao.clearDateVars(id)
        val localToNew = HashMap<Long, Long>()
        var keyId: Long? = null
        dates.filter { it.name.isNotBlank() }.forEachIndexed { i, d ->
            val newId = dao.insertDateVar(DateVarEntity(cardId = id, name = d.name.trim().take(40), epochDay = d.epochDay, order = i))
            localToNew[d.id] = newId
            if (d.isKey) keyId = newId
        }
        if (keyId == null) keyId = localToNew.values.firstOrNull()
        dao.card(id)?.let { dao.updateCard(it.copy(keyDateVarId = keyId, updatedAt = now)) }

        dao.clearTagsFor(id)
        tagIds.forEach { tagId -> dao.linkTag(CardTagEntity(cardId = id, tagId = tagId)) }
        if (tagIds.isNotEmpty()) dao.touchTags(tagIds.toList(), now)

        scheds.forEach { s ->
            val newDateId = localToNew[s.dateLocalId] ?: return@forEach
            val prev = oldScheds[s.id]
            dao.insertScheduler(
                SchedulerEntity(
                    cardId = id, dateVarId = newDateId, unit = s.unit,
                    amount = RingCalculator.clamp(s.unit, s.amount), direction = s.direction,
                    ringMinute = s.ringMinute, enabled = s.enabled,
                    status = if (!s.enabled) RingStatus.OFF else (prev?.status ?: RingStatus.SCHEDULED),
                    snoozedUntil = prev?.snoozedUntil, lastFiredAt = prev?.lastFiredAt
                )
            )
        }
        id
        }
    }

    suspend fun deleteCard(context: android.content.Context, id: Long) = Logger.guardUnit("Repo.delCard") {
        dao.deleteCard(id)
        FileStore.deleteAllFor(context, id)
    }

    suspend fun deleteCards(context: android.content.Context, ids: List<Long>) = Logger.guardUnit("Repo.delCards") {
        dao.deleteCards(ids)
        ids.forEach { FileStore.deleteAllFor(context, it) }
    }

    suspend fun moveCards(ids: List<Long>, categoryId: Long) = Logger.guardUnit("Repo.move") {
        var order = (dao.maxCardOrder(categoryId) ?: 0)
        dao.moveCards(ids, categoryId, System.currentTimeMillis())
        ids.forEach { id -> dao.card(id)?.let { dao.updateCard(it.copy(manualOrder = ++order)) } }
    }

    suspend fun duplicateCards(ids: List<Long>) = Logger.guardUnit("Repo.dup") {
        ids.forEach { id ->
            val c = dao.card(id) ?: return@forEach
            val newId = dao.insertCard(
                c.copy(id = 0, label = (c.label + " (copy)").take(60), keyDateVarId = null,
                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            )
            dao.dataVars(id).forEach { dao.insertDataVar(it.copy(id = 0, cardId = newId)) }
            val map = HashMap<Long, Long>()
            dao.dateVars(id).forEach { map[it.id] = dao.insertDateVar(it.copy(id = 0, cardId = newId)) }
            val keyNew = c.keyDateVarId?.let { map[it] } ?: map.values.firstOrNull()
            dao.card(newId)?.let { dao.updateCard(it.copy(keyDateVarId = keyNew)) }
            dao.schedulers(id).forEach { s ->
                val d = map[s.dateVarId] ?: return@forEach
                dao.insertScheduler(s.copy(id = 0, cardId = newId, dateVarId = d, status = RingStatus.SCHEDULED, lastFiredAt = null, snoozedUntil = null))
            }
        }
    }

    suspend fun setRemindersEnabled(cardIds: List<Long>, enabled: Boolean) = Logger.guardUnit("Repo.remEnable") {
        dao.setEnabledForCards(cardIds, enabled, if (enabled) RingStatus.SCHEDULED else RingStatus.OFF)
    }

    suspend fun setSchedulerEnabled(id: Long, enabled: Boolean) = Logger.guardUnit("Repo.schedEnable") {
        dao.scheduler(id)?.let {
            dao.updateScheduler(it.copy(enabled = enabled, status = if (enabled) RingStatus.SCHEDULED else RingStatus.OFF))
        }
    }

    suspend fun allSchedulerViews(): List<Pair<SchedulerView, CardEntity>> = Logger.guard("Repo.allSched", emptyList()) {
        val out = mutableListOf<Pair<SchedulerView, CardEntity>>()
        dao.allSchedulers().forEach { s ->
            val card = dao.card(s.cardId) ?: return@forEach
            out += toView(s, dao.dateVar(s.dateVarId)) to card
        }
        out
    }

    // ---------------- tags ----------------

    suspend fun tagNameTaken(name: String, exceptId: Long = 0): Boolean =
        Logger.guard("Repo.tagName", false) { dao.countTagName(TagRules.normalise(name), exceptId) > 0 }

    /** Returns the new id, or -1 when the name is blank or already taken. */
    suspend fun addTag(name: String): Long = Logger.guard("Repo.addTag", -1L) {
        val clean = TagRules.normalise(name)
        if (clean.isBlank()) return@guard -1L
        if (dao.countTagName(clean, 0) > 0) {
            Logger.e("Repo.addTag", "duplicate tag rejected: " + clean)
            return@guard -1L
        }
        dao.insertTag(TagEntity(name = clean))
    }

    suspend fun renameTag(id: Long, name: String): Boolean = Logger.guard("Repo.renameTag", false) {
        val clean = TagRules.normalise(name)
        if (clean.isBlank() || dao.countTagName(clean, id) > 0) return@guard false
        val t = dao.tag(id) ?: return@guard false
        dao.updateTag(t.copy(name = clean)); true
    }

    suspend fun mergeTag(from: Long, into: Long) = Logger.guardUnit("Repo.mergeTag") {
        if (from == into) return@guardUnit
        dao.retagLinks(from, into)
        dao.clearLinksOfTag(from)
        dao.deleteTag(from)
    }

    suspend fun deleteTag(id: Long) = Logger.guardUnit("Repo.deleteTag") {
        dao.clearLinksOfTag(id); dao.deleteTag(id)
    }

    suspend fun tagUsage(): Map<Long, Int> = Logger.guard("Repo.tagUsage", emptyMap()) {
        dao.cardTagsFlow().first().groupBy { it.tagId }.mapValues { it.value.size }
    }

    // ---------------- files ----------------

    suspend fun files(cardId: Long): List<FileEntity> = Logger.guard("Repo.files", emptyList()) { dao.files(cardId) }

    suspend fun attachFile(context: android.content.Context, cardId: Long, uri: android.net.Uri): String? {
        val count = Logger.guard("Repo.fileCount", 0) { dao.countFiles(cardId) }
        return when (val r = FileStore.copyIn(context, uri, cardId, count, count)) {
            is FileStore.Result.Rejected -> r.reason
            is FileStore.Result.Ok -> {
                Logger.guardUnit("Repo.attach") { dao.insertFile(r.entity) }
                null
            }
        }
    }

    suspend fun renameFile(id: Long, name: String, extensionConfirmed: Boolean = false): String? =
        Logger.guard<String?>("Repo.renameFile", "Could not rename file") {
            val file = dao.file(id) ?: run {
                Logger.e("Repo.renameFile", "File not found")
                return@guard "File no longer exists"
            }
            val problem = FileRenameRules.validate(file.displayName, name, extensionConfirmed)
            if (problem != null) {
                Logger.e("Repo.renameFile", "Rename rejected: " + problem)
                return@guard problem
            }
            if (file.displayName != name) dao.updateFile(file.copy(displayName = name))
            null
        }

    suspend fun removeFile(context: android.content.Context, id: Long) = Logger.guardUnit("Repo.removeFile") {
        val f = dao.file(id) ?: return@guardUnit
        FileStore.delete(context, f)
        dao.deleteFile(id)
    }

    suspend fun resetAll(context: android.content.Context) = Logger.guardUnit("Repo.reset") {
        dao.resetAll()
        val files = File(context.filesDir, "cards")
        if (files.exists() && !files.deleteRecursively()) {
            Logger.e("Repo.reset", "could not remove old attachment storage")
        }
    }
    suspend fun clearErrors() = Logger.guardUnit("Repo.clearErr") { dao.clearErrors() }

    fun dao(): InfoDao = dao
}
