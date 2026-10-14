package com.krishna.infodeck.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krishna.infodeck.alarm.AlarmScheduler
import com.krishna.infodeck.alarm.Notifications
import com.krishna.infodeck.backup.BackupIo
import com.krishna.infodeck.core.CardRow
import com.krishna.infodeck.core.CategoryRow
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.GroupBy
import com.krishna.infodeck.core.GroupOrder
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.core.Section
import com.krishna.infodeck.core.SortBy
import com.krishna.infodeck.core.ViewLogic
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.data.AppSettings
import com.krishna.infodeck.data.CardDetail
import com.krishna.infodeck.data.Prefs
import com.krishna.infodeck.data.Repo
import com.krishna.infodeck.data.TagEntity
import com.krishna.infodeck.data.FileEntity
import com.krishna.infodeck.core.RowSpacing
import com.krishna.infodeck.core.ScaleRules
import com.krishna.infodeck.core.TextRole
import com.krishna.infodeck.core.TextWeightSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeState(
    val sections: List<Section> = emptyList(),
    val allRows: List<CardRow> = emptyList(),
    val visibleRows: List<CardRow> = emptyList(),
    val hits: Map<Long, com.krishna.infodeck.core.SearchHit> = emptyMap(),
    val kindsHit: Map<Long, Set<com.krishna.infodeck.core.SearchField.Kind>> = emptyMap(),
    val missingWords: List<String> = emptyList(),
    val categories: List<CategoryRow> = emptyList(),
    val hiddenSections: Int = 0,
    val settings: AppSettings = AppSettings()
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    val repo = Repo(db)
    val prefs = Prefs(app)

    val settings: StateFlow<AppSettings> =
        prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val errorCount: StateFlow<Int> =
        repo.errorCountFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val tags: StateFlow<List<TagEntity>> =
        repo.tagsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentTags: StateFlow<List<TagEntity>> =
        repo.recentTagsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalFileBytes: StateFlow<Long> =
        repo.totalFileBytesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** live tag -> card count, derived from the same rows the list uses */
    val tagUsage: StateFlow<Map<Long, Int>> = repo.rowsFlow
        .map { rows -> rows.flatMap { it.tagIds }.groupingBy { it }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Search suspends grouping (D2 C5 row 5). */
    val search = MutableStateFlow("")
    val selection = MutableStateFlow<Set<Long>>(emptySet())
    val selectionMode = MutableStateFlow(false)

    val home: StateFlow<HomeState> =
        combine(repo.rowsFlow, repo.categoriesFlow, prefs.flow,
            @OptIn(kotlinx.coroutines.FlowPreview::class) search.debounce(com.krishna.infodeck.core.SearchRules.DEBOUNCE_MS),
            repo.tagsFlow) { rows, cats, s, q, tagList ->
            Logger.guard("VM.home", HomeState()) {
                val today = LocalDate.now()
                val query = q.trim().lowercase()
                val searching = query.isNotEmpty()
                val searched = if (!searching) rows else rows
                    .filter { r -> com.krishna.infodeck.core.SearchRules.matches(r.searchIndex, query) }
                    .sortedBy { r -> com.krishna.infodeck.core.SearchRules.rank(r.searchFields, query) }
                val hits: Map<Long, com.krishna.infodeck.core.SearchHit> = if (!searching) emptyMap() else
                    searched.mapNotNull { r -> com.krishna.infodeck.core.SearchRules.hit(r.searchFields, query)?.let { r.id to it } }.toMap()
                val kinds: Map<Long, Set<com.krishna.infodeck.core.SearchField.Kind>> = if (!searching) emptyMap() else
                    searched.associate { r -> r.id to com.krishna.infodeck.core.SearchRules.kindsHit(r.searchFields, query) }
                val missing = if (!searching) emptyList() else
                    com.krishna.infodeck.core.SearchRules.missing(rows.map { it.searchIndex }, query)
                val visible = ViewLogic.filter(searched, s.filter, today)
                val groupBy = if (searching) GroupBy.NONE else s.groupBy
                val sections = ViewLogic.group(
                    visible = visible, all = rows, by = groupBy, order = s.groupOrder,
                    categories = cats, sortBy = s.sortBy, asc = s.sortAsc, today = today,
                    tagNames = tagList.associate { it.id to it.name }
                )
                val possible = if (groupBy == GroupBy.CATEGORY)
                    cats.count { c -> rows.any { it.categoryId == c.id } } else sections.size
                HomeState(
                    sections = sections,
                    allRows = rows,
                    visibleRows = visible,
                    categories = cats,
                    hiddenSections = (possible - sections.size).coerceAtLeast(0),
                    settings = s,
                    hits = hits,
                    kindsHit = kinds,
                    missingWords = missing
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    init {
        Logger.attach(db)
        viewModelScope.launch {
            Notifications.ensureChannel(app, true)
            AlarmScheduler.planNext(app)
        }
    }

    fun replan() = viewModelScope.launch { AlarmScheduler.planNext(getApplication()) }

    // ---------------- view options ----------------
    fun setGroupBy(g: GroupBy, order: GroupOrder, singleOpen: Boolean) = viewModelScope.launch {
        prefs.setGroupBy(g); prefs.setGroupOrder(order); prefs.setSingleOpen(singleOpen); prefs.setOpenSection("")
    }

    fun setSort(by: SortBy, asc: Boolean) = viewModelScope.launch { prefs.setSort(by, asc) }
    fun flipSort() = viewModelScope.launch {
        val s = settings.value; prefs.setSort(s.sortBy, !s.sortAsc)
    }

    fun setFilter(f: FilterState) = viewModelScope.launch { prefs.setFilter(f); prefs.setOpenSection("") }
    fun setOpenSection(key: String) = viewModelScope.launch { prefs.setOpenSection(key) }

    fun previewCount(f: FilterState): Int = Logger.guard("VM.preview", 0) {
        ViewLogic.filter(home.value.allRows, f, LocalDate.now()).size
    }

    // ---------------- selection ----------------
    fun startSelection(cardId: Long) {
        selectionMode.value = true
        selection.value = setOf(cardId)
    }

    fun toggleSelect(cardId: Long) {
        val cur = selection.value.toMutableSet()
        if (!cur.add(cardId)) cur.remove(cardId)
        selection.value = cur
        if (cur.isEmpty()) selectionMode.value = false
    }

    fun selectAllVisible() {
        val all = home.value.visibleRows.map { it.id }.toSet()
        selection.value = if (selection.value.containsAll(all) && all.isNotEmpty()) emptySet() else all
    }

    fun clearSelection() { selection.value = emptySet(); selectionMode.value = false }

    // ---------------- mutations ----------------
    fun moveSelected(categoryId: Long, done: (Int) -> Unit = {}) = viewModelScope.launch {
        val ids = selection.value.toList()
        repo.moveCards(ids, categoryId)
        clearSelection(); done(ids.size); replan()
    }

    fun deleteSelected(done: (Int) -> Unit = {}) = viewModelScope.launch {
        val ids = selection.value.toList()
        repo.deleteCards(getApplication(), ids); clearSelection(); done(ids.size); replan()
    }

    fun duplicateSelected(done: (Int) -> Unit = {}) = viewModelScope.launch {
        val ids = selection.value.toList()
        repo.duplicateCards(ids); clearSelection(); done(ids.size); replan()
    }

    fun remindersForSelected(enabled: Boolean, done: (Int) -> Unit = {}) = viewModelScope.launch {
        val ids = selection.value.toList()
        repo.setRemindersEnabled(ids, enabled); clearSelection(); done(ids.size); replan()
    }

    fun moveCard(cardId: Long, categoryId: Long) = viewModelScope.launch {
        repo.moveCards(listOf(cardId), categoryId); replan()
    }

    fun deleteCard(cardId: Long, done: () -> Unit = {}) = viewModelScope.launch {
        repo.deleteCard(getApplication(), cardId); done(); replan()
    }

    // ---------------- text size (N30b) ----------------
    fun setTextScale(v: Float) = viewModelScope.launch { prefs.setTextScale(v) }

    fun nudgeTextScale(deltaPercent: Int) = viewModelScope.launch {
        val now = settings.value.ui.textScale
        prefs.setTextScale(ScaleRules.fromPercent(ScaleRules.percent(now) + deltaPercent))
    }

    /** Pinch multiplies the live value; persisted on every gesture step, clamped. */
    fun pinchTextScale(zoom: Float) = viewModelScope.launch {
        val next = ScaleRules.clampScale(settings.value.ui.textScale * zoom)
        if (ScaleRules.percent(next) != ScaleRules.percent(settings.value.ui.textScale)) prefs.setTextScale(next)
    }

    fun setDensity(v: com.krishna.infodeck.core.Density) = viewModelScope.launch {
        prefs.setDensity(v); prefs.setTapExpand(com.krishna.infodeck.core.Density.defaultExpand(v))
    }
    fun setTapExpand(v: Boolean) = viewModelScope.launch { prefs.setTapExpand(v) }

    /** Writes a whole draft at once — used by the Save button on the text screens. */
    fun saveUiScale(ui: com.krishna.infodeck.core.UiScale, done: () -> Unit = {}) = viewModelScope.launch {
        prefs.setTextScale(ui.textScale)
        TextRole.values().forEach { prefs.setOffset(it, ui.offset(it)) }
        prefs.setSpacing(ui.spacing)
        prefs.setWeight(ui.weight)
        done()
    }

    /** "Put settings back to normal" — confirmed at the call site, never silent. */
    fun restoreSettingsDefaults(done: () -> Unit = {}) = viewModelScope.launch {
        saveUiScale(com.krishna.infodeck.core.FriendlySize.DEFAULTS)
        prefs.setFollowSystem(true)
        prefs.setAutoCaps(true)
        prefs.setAutoCapsValues(true)
        prefs.setRingMinute(9 * 60)
        prefs.setVibrate(true)
        prefs.setSnooze(60)
        prefs.setTheme("SYSTEM")
        done()
    }

    fun resetTextScale() = viewModelScope.launch { prefs.setTextScale(1f); prefs.resetOffsets() }
    fun setOffset(role: TextRole, v: Float) = viewModelScope.launch { prefs.setOffset(role, v) }
    fun resetOffsets() = viewModelScope.launch { prefs.resetOffsets() }
    fun setSpacing(v: RowSpacing) = viewModelScope.launch { prefs.setSpacing(v) }
    fun setWeight(v: TextWeightSetting) = viewModelScope.launch { prefs.setWeight(v) }
    fun setFollowSystem(v: Boolean) = viewModelScope.launch { prefs.setFollowSystem(v) }
    fun setAutoCaps(v: Boolean) = viewModelScope.launch { prefs.setAutoCaps(v) }
    fun setAutoCapsValues(v: Boolean) = viewModelScope.launch { prefs.setAutoCapsValues(v) }

    // ---------------- tags (N35) ----------------
    fun addTag(name: String, done: (Long) -> Unit = {}) = viewModelScope.launch { done(repo.addTag(name)) }
    fun renameTag(id: Long, name: String, done: (Boolean) -> Unit = {}) = viewModelScope.launch { done(repo.renameTag(id, name)) }
    fun mergeTag(from: Long, into: Long) = viewModelScope.launch { repo.mergeTag(from, into) }
    fun deleteTag(id: Long) = viewModelScope.launch { repo.deleteTag(id) }

    // ---------------- files (N34) ----------------
    fun attachFile(cardId: Long, uri: android.net.Uri, done: (String?) -> Unit) = viewModelScope.launch {
        done(repo.attachFile(getApplication(), cardId, uri))
    }

    fun renameFile(
        id: Long,
        name: String,
        extensionConfirmed: Boolean = false,
        done: (String?) -> Unit = {}
    ) = viewModelScope.launch {
        done(repo.renameFile(id, name, extensionConfirmed))
    }

    fun removeFile(id: Long, done: () -> Unit = {}) = viewModelScope.launch {
        repo.removeFile(getApplication(), id); done()
    }

    suspend fun filesFor(cardId: Long): List<FileEntity> = repo.files(cardId)

    fun viewModelRefreshFiles(cardId: Long, done: (List<FileEntity>) -> Unit) = viewModelScope.launch {
        done(if (cardId > 0) repo.files(cardId) else emptyList())
    }

    fun setSchedulerEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repo.setSchedulerEnabled(id, enabled); replan()
    }

    fun saveCard(
        cardId: Long?, label: String, categoryId: Long,
        data: List<com.krishna.infodeck.data.Repo.DataInput>,
        dates: List<com.krishna.infodeck.data.Repo.DateInput>,
        scheds: List<com.krishna.infodeck.data.Repo.SchedInput>,
        tagIds: Set<Long> = emptySet(),
        done: () -> Unit
    ) = viewModelScope.launch {
        repo.saveCard(cardId, label, categoryId, data, dates, scheds, tagIds)
        AlarmScheduler.planNext(getApplication())
        done()
    }

    suspend fun detail(cardId: Long): CardDetail? = repo.cardDetail(cardId)

    // ---------------- categories ----------------
    fun addCategory(name: String, colorIndex: Int, done: (Long) -> Unit = {}) = viewModelScope.launch {
        if (repo.categoryNameTaken(name)) { done(-1L); return@launch }
        done(repo.addCategory(name, colorIndex))
    }

    fun updateCategory(id: Long, name: String, colorIndex: Int, done: (Boolean) -> Unit = {}) = viewModelScope.launch {
        if (repo.categoryNameTaken(name, id)) { done(false); return@launch }
        repo.updateCategory(id, name, colorIndex); done(true)
    }

    fun deleteCategory(id: Long, moveTo: Long?, done: () -> Unit = {}) = viewModelScope.launch {
        repo.deleteCategory(getApplication(), id, moveTo); done(); replan()
    }

    fun reorderCategories(ids: List<Long>) = viewModelScope.launch { repo.reorderCategories(ids) }

    suspend fun cardCount(categoryId: Long): Int = repo.countCardsIn(categoryId)

    // ---------------- settings ----------------
    fun setRingMinute(v: Int) = viewModelScope.launch { prefs.setRingMinute(v) }
    fun setVibrate(v: Boolean) = viewModelScope.launch { prefs.setVibrate(v) }
    fun setSnooze(v: Int) = viewModelScope.launch { prefs.setSnooze(v) }
    fun setTheme(v: String) = viewModelScope.launch { prefs.setTheme(v) }
    fun setAppLock(v: Boolean) = viewModelScope.launch { prefs.setAppLock(v) }

    fun testRing() = viewModelScope.launch {
        val app = getApplication<Application>()
        Notifications.ensureChannel(app, settings.value.vibrate)
        Notifications.show(
            app, schedulerId = 999_999L, cardId = -1L,
            title = "Test reminder",
            ruleLine = "This is what a ring looks like",
            contextLine = "InfoDeck - Settings > Test ring",
            snoozeMinutes = settings.value.snoozeMinutes
        )
    }

    fun resetAll(done: () -> Unit = {}) = viewModelScope.launch {
        repo.resetAll(getApplication()); AlarmScheduler.cancelAll(getApplication()); done()
    }

    fun clearErrors() = viewModelScope.launch { repo.clearErrors() }

    fun backup(uri: Uri, done: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = BackupIo.export(getApplication(), uri, db)
        if (ok) prefs.setLastBackup(System.currentTimeMillis())
        done(ok)
    }

    fun restore(uri: Uri, replaceAll: Boolean, done: (Int) -> Unit) = viewModelScope.launch {
        val n = BackupIo.import(getApplication(), uri, db, replaceAll)
        replan(); done(n)
    }
}
