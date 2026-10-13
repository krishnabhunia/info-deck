package com.krishna.infodeck.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.krishna.infodeck.core.ContentFilter
import com.krishna.infodeck.core.ExpiryFilter
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.GroupBy
import com.krishna.infodeck.core.GroupOrder
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.core.ReminderFilter
import com.krishna.infodeck.core.RowSpacing
import com.krishna.infodeck.core.ScaleRules
import com.krishna.infodeck.core.SortBy
import com.krishna.infodeck.core.TextRole
import com.krishna.infodeck.core.TextWeightSetting
import com.krishna.infodeck.core.UiScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "infodeck_settings")

data class AppSettings(
    val defaultRingMinute: Int = 9 * 60,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 60,
    val density: com.krishna.infodeck.core.Density = com.krishna.infodeck.core.Density.DEFAULT,
    val tapExpand: Boolean = false,
    val theme: String = "SYSTEM",
    val appLock: Boolean = false,
    val lastBackupAt: Long = 0L,
    val groupBy: GroupBy = GroupBy.CATEGORY,
    val groupOrder: GroupOrder = GroupOrder.MANUAL,
    val singleOpen: Boolean = true,
    val sortBy: SortBy = SortBy.EXPIRY,
    val sortAsc: Boolean = true,
    val openSection: String = "",
    val filter: FilterState = FilterState(),
    val ui: UiScale = UiScale(),
    val followSystemText: Boolean = true,
    val autoCaps: Boolean = true,
    val autoCapsValues: Boolean = true
)

class Prefs(private val context: Context) {

    private object K {
        val ring = intPreferencesKey("default_ring_minute")
        val vibrate = booleanPreferencesKey("vibrate")
        val snooze = intPreferencesKey("snooze_minutes")
        val density = stringPreferencesKey("density")
        val tapExpand = booleanPreferencesKey("tap_expand")
        val theme = stringPreferencesKey("theme")
        val lock = booleanPreferencesKey("app_lock")
        val backup = longPreferencesKey("last_backup")
        val groupBy = stringPreferencesKey("group_by")
        val groupOrder = stringPreferencesKey("group_order")
        val singleOpen = booleanPreferencesKey("single_open")
        val sortBy = stringPreferencesKey("sort_by")
        val sortAsc = booleanPreferencesKey("sort_asc")
        val openSection = stringPreferencesKey("open_section")
        val fCats = stringPreferencesKey("f_cats")
        val fExpiry = stringPreferencesKey("f_expiry")
        val fReminder = stringPreferencesKey("f_reminder")
        val fContent = stringPreferencesKey("f_content")
        val fFrom = longPreferencesKey("f_from")
        val fTo = longPreferencesKey("f_to")
        val fTags = stringPreferencesKey("f_tags")
        val fTagsAll = booleanPreferencesKey("f_tags_all")
        val scale = floatPreferencesKey("text_scale")
        val spacing = stringPreferencesKey("row_spacing")
        val weight = stringPreferencesKey("text_weight")
        val followSys = booleanPreferencesKey("follow_system_text")
        val caps = booleanPreferencesKey("auto_caps")
        val capsValues = booleanPreferencesKey("auto_caps_values")
        val offsets = TextRole.values().associateWith { floatPreferencesKey("off_" + it.name) }
    }

    private fun readScale(p: androidx.datastore.preferences.core.Preferences): UiScale {
        val raw = p[K.scale] ?: 1f
        val clamped = ScaleRules.clampScale(raw)
        if (raw != clamped) Logger.e("Prefs", "stored text scale " + raw + " out of range, using " + clamped)
        return UiScale(
            textScale = clamped,
            offsets = TextRole.values().associateWith { role ->
                ScaleRules.clampOffset(p[K.offsets.getValue(role)] ?: 0f)
            },
            spacing = enumOr(p[K.spacing], RowSpacing.COMFORTABLE),
            weight = enumOr(p[K.weight], TextWeightSetting.NORMAL)
        )
    }

    val flow: Flow<AppSettings> = context.dataStore.data
        .catch { t ->
            Logger.e("Prefs", "read failed, using defaults", t)
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { p ->
            AppSettings(
                defaultRingMinute = p[K.ring] ?: (9 * 60),
                vibrate = p[K.vibrate] ?: true,
                snoozeMinutes = p[K.snooze] ?: 60,
                density = com.krishna.infodeck.core.Density.of(p[K.density]),
                tapExpand = p[K.tapExpand]
                    ?: com.krishna.infodeck.core.Density.defaultExpand(com.krishna.infodeck.core.Density.of(p[K.density])),
                theme = p[K.theme] ?: "SYSTEM",
                appLock = p[K.lock] ?: false,
                lastBackupAt = p[K.backup] ?: 0L,
                groupBy = enumOr(p[K.groupBy], GroupBy.CATEGORY),
                groupOrder = enumOr(p[K.groupOrder], GroupOrder.MANUAL),
                singleOpen = p[K.singleOpen] ?: true,
                sortBy = enumOr(p[K.sortBy], SortBy.EXPIRY),
                sortAsc = p[K.sortAsc] ?: true,
                openSection = p[K.openSection] ?: "",
                filter = FilterState(
                    categoryIds = (p[K.fCats] ?: "").split(",").mapNotNull { it.trim().toLongOrNull() }.toSet(),
                    expiry = enumOr(p[K.fExpiry], ExpiryFilter.ANY),
                    reminder = enumOr(p[K.fReminder], ReminderFilter.ANY),
                    content = enumOr(p[K.fContent], ContentFilter.ANY),
                    customFrom = p[K.fFrom]?.takeIf { it > 0 },
                    customTo = p[K.fTo]?.takeIf { it > 0 },
                    tagIds = (p[K.fTags] ?: "").split(",").mapNotNull { it.trim().toLongOrNull() }.toSet(),
                    tagsAll = p[K.fTagsAll] ?: false
                ),
                ui = readScale(p),
                followSystemText = p[K.followSys] ?: true,
                autoCaps = p[K.caps] ?: true,
                autoCapsValues = p[K.capsValues] ?: true
            )
        }

    private inline fun <reified T : Enum<T>> enumOr(s: String?, fallback: T): T =
        if (s == null) fallback else runCatching { enumValueOf<T>(s) }.getOrElse { fallback }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try { context.dataStore.edit(block) } catch (t: Throwable) { Logger.e("Prefs", "write failed", t) }
    }

    suspend fun setRingMinute(v: Int) = edit { it[K.ring] = v.coerceIn(0, 1439) }
    suspend fun setVibrate(v: Boolean) = edit { it[K.vibrate] = v }
    suspend fun setSnooze(v: Int) = edit { it[K.snooze] = v }
    suspend fun setDensity(v: com.krishna.infodeck.core.Density) = edit { it[K.density] = v.name }
    suspend fun setTapExpand(v: Boolean) = edit { it[K.tapExpand] = v }
    suspend fun setTheme(v: String) = edit { it[K.theme] = v }
    suspend fun setAppLock(v: Boolean) = edit { it[K.lock] = v }
    suspend fun setLastBackup(v: Long) = edit { it[K.backup] = v }
    suspend fun setGroupBy(v: GroupBy) = edit { it[K.groupBy] = v.name }
    suspend fun setGroupOrder(v: GroupOrder) = edit { it[K.groupOrder] = v.name }
    suspend fun setSingleOpen(v: Boolean) = edit { it[K.singleOpen] = v }
    suspend fun setSort(by: SortBy, asc: Boolean) = edit { it[K.sortBy] = by.name; it[K.sortAsc] = asc }
    suspend fun setOpenSection(v: String) = edit { it[K.openSection] = v }
    suspend fun setFilter(f: FilterState) = edit {
        it[K.fCats] = f.categoryIds.joinToString(",")
        it[K.fExpiry] = f.expiry.name
        it[K.fReminder] = f.reminder.name
        it[K.fContent] = f.content.name
        it[K.fFrom] = f.customFrom ?: 0L
        it[K.fTo] = f.customTo ?: 0L
        it[K.fTags] = f.tagIds.joinToString(",")
        it[K.fTagsAll] = f.tagsAll
    }

    suspend fun setTextScale(v: Float) = edit { it[K.scale] = ScaleRules.clampScale(v) }
    suspend fun setOffset(role: TextRole, v: Float) = edit { it[K.offsets.getValue(role)] = ScaleRules.clampOffset(v) }
    suspend fun resetOffsets() = edit { p -> TextRole.values().forEach { p[K.offsets.getValue(it)] = 0f } }
    suspend fun setSpacing(v: RowSpacing) = edit { it[K.spacing] = v.name }
    suspend fun setWeight(v: TextWeightSetting) = edit { it[K.weight] = v.name }
    suspend fun setFollowSystem(v: Boolean) = edit { it[K.followSys] = v }
    suspend fun setAutoCaps(v: Boolean) = edit { it[K.caps] = v }
    suspend fun setAutoCapsValues(v: Boolean) = edit { it[K.capsValues] = v }
}
