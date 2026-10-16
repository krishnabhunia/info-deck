package com.krishna.infodeck.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sell
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.CardRow
import com.krishna.infodeck.core.ExpiryFilter
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.ReminderFilter
import com.krishna.infodeck.core.Section
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenCard: (Long) -> Unit,
    onNewCard: (Long?) -> Unit,
    onEditCard: (Long) -> Unit = {},
    onShareCard: (Long) -> Unit = {},
    onUpcoming: () -> Unit,
    onSettings: () -> Unit,
    onCategories: () -> Unit,
    onTags: () -> Unit = {}
) {
    val state by vm.home.collectAsState()
    val settings = state.settings
    val selection by vm.selection.collectAsState()
    val selectionMode by vm.selectionMode.collectAsState()
    val query by vm.search.collectAsState()

    var searching by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var bucket by remember { mutableStateOf<com.krishna.infodeck.core.SearchField.Kind?>(null) }
    LaunchedEffect(query) { if (query.isEmpty()) bucket = null }
    var overflow by remember { mutableStateOf(false) }
    var showGroup by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showDanger by remember { mutableStateOf(false) }
    var typedDelete by remember { mutableStateOf("") }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    val overdueCount = state.allRows.count { it.isOverdue(today) }
    val upcomingCount = state.allRows.count {
        it.nextRingAt != null && it.nextRingAt.toLocalDate().isBefore(today.plusDays(31))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selectionMode) {
                DeckHeader(
                    title = "${selection.size} selected",
                    onBack = { vm.clearSelection() }, backIcon = Icons.Filled.Close, backLabel = "Close",
                    actions = { HeaderIcon(Icons.Filled.DoneAll, "Select all") { vm.selectAllVisible() } }
                )
            } else if (searching) {
                SlimTopBar(
                    title = "", searching = true, query = query,
                    onQuery = { vm.search.value = it },
                    onSearchClose = { searching = false; vm.search.value = "" }
                )
            } else {
                val counts = mapOf(
                    com.krishna.infodeck.core.HomeChip.OVERDUE to overdueCount,
                    com.krishna.infodeck.core.HomeChip.MONTH to state.allRows.count {
                        it.keyDate != null && !it.keyDate.isBefore(today) &&
                            java.time.temporal.ChronoUnit.DAYS.between(today, it.keyDate) <= 31
                    },
                    com.krishna.infodeck.core.HomeChip.NO_ALARM to state.allRows.count { !it.hasReminder }
                )
                val shown = com.krishna.infodeck.core.ChipRules.visible(counts)
                DeckHomeHero(
                    total = state.allRows.size,
                    overdue = overdueCount,
                    onSearch = { searching = true },
                    onFilters = { showGroup = true },
                    actions = {
                        HeroAction(Icons.Filled.Notifications, "Upcoming", onUpcoming)
                        Box {
                            HeroAction(Icons.Filled.MoreVert, "More") { overflow = true }
                            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                                DropdownMenuItem(text = { Text("Select cards") }, onClick = {
                                    overflow = false
                                    state.visibleRows.firstOrNull()?.let { vm.startSelection(it.id) }
                                })
                                DropdownMenuItem(text = { Text("Collapse all") },
                                    onClick = { overflow = false; vm.setOpenSection("") })
                                DropdownMenuItem(text = { Text("Manage categories") }, onClick = { overflow = false; onCategories() })
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { overflow = false; onSettings() })
                            }
                        }
                    },
                    stats = {
                        shown.forEach { chip ->
                            val (label, h, on) = when (chip) {
                                com.krishna.infodeck.core.HomeChip.OVERDUE -> Triple("Overdue", com.krishna.infodeck.core.Hue.ROSE,
                                    settings.filter.expiry == ExpiryFilter.OVERDUE)
                                com.krishna.infodeck.core.HomeChip.MONTH -> Triple("This month", com.krishna.infodeck.core.Hue.AMBER,
                                    settings.filter.expiry == ExpiryFilter.D30)
                                com.krishna.infodeck.core.HomeChip.NO_ALARM -> Triple("No reminder", com.krishna.infodeck.core.Hue.SLATE,
                                    settings.filter.reminder == ReminderFilter.NONE)
                            }
                            HeroStat(counts[chip] ?: 0, label, h, on, Modifier.weight(1f)) {
                                vm.setFilter(com.krishna.infodeck.core.HomeStatRules.toggle(settings.filter, chip))
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) DeckFab {
                val open = state.sections.firstOrNull { it.key == settings.openSection }
                onNewCard(open?.categoryId)
            }
        },
        bottomBar = {
            if (!selectionMode) InfoBottomBar(
                selected = 0, overdue = state.allRows.count { it.isOverdue(today) },
                onHome = {}, onUpcoming = onUpcoming, onSettings = onSettings, onTags = onTags,
                onNew = {
                    val open = state.sections.firstOrNull { it.key == settings.openSection }
                    onNewCard(open?.categoryId)
                }
            )
            else SelectionBar(
                enabled = selection.isNotEmpty(),
                onMove = { showMove = true },
                onReminders = {
                    vm.remindersForSelected(false) { n ->
                        scope.launch { snackbar.showSnackbar("Reminders turned off on $n cards") }
                    }
                },
                onDuplicate = {
                    vm.duplicateSelected { n -> scope.launch { snackbar.showSnackbar("Duplicated $n cards") } }
                },
                onMore = { showMore = true }
            )
        }
    ) { pad ->
        val scaleNow = settings.ui.textScale
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .pointerInput(scaleNow) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) vm.pinchTextScale(zoom)
                    }
                }
        ) {

            if (!selectionMode && !searching) {
                HomeToolbar(
                    groupLabel = settings.groupBy.label,
                    activeFilters = settings.filter.activeGroups,
                    onView = { showGroup = true },
                    onFilter = { showFilter = true }
                )
            }

            if (state.sections.isEmpty() && query.isNotEmpty()) {
                SearchEmpty(query, state.missingWords)
            } else if (state.sections.isEmpty()) {
                EmptyState(
                    hasAnyCard = state.allRows.isNotEmpty(),
                    filterActive = settings.filter.isActive,
                    onClear = { vm.setFilter(FilterState()) }
                )
            } else {
                val flat = settings.groupBy == com.krishna.infodeck.core.GroupBy.NONE || query.isNotEmpty()
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(com.krishna.infodeck.core.Density.SECTION_GAP.dp)
                ) {
                    if (flat) {
                        val allCards = state.sections.flatMap { it.cards }
                        val cards = if (query.isEmpty() || bucket == null) allCards
                            else allCards.filter { state.kindsHit[it.id]?.contains(bucket) == true }
                        if (query.isNotEmpty()) item(key = "searchline") {
                            SearchCountLine(
                                total = allCards.size, query = query,
                                kinds = state.kindsHit.values.flatten().groupingBy { it }.eachCount(),
                                bucket = bucket, onBucket = { bucket = it }
                            )
                        } else item(key = "deckline") {
                            Text(
                                "${cards.size} cards - ${settings.sortBy.label} ${if (settings.sortAsc) "↑" else "↓"}",
                                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(cards, key = { "flat" + it.id }) { row ->
                            MiniCard(
                                row = row, today = today, showCategoryChip = true,
                                selectionMode = selectionMode, selected = selection.contains(row.id),
                                onClick = { if (selectionMode) vm.toggleSelect(row.id) else onOpenCard(row.id) },
                                onLong = { if (!selectionMode) vm.startSelection(row.id) else vm.toggleSelect(row.id) },
                                flat = true,
                                density = settings.density,
                                expandable = settings.tapExpand,
                                expanded = expandedId == row.id,
                                onExpand = {
                                    expandedId = com.krishna.infodeck.core.ExpandRules.next(
                                        expandedId, row.id, cards.map { it.id }.toSet()
                                    )
                                },
                                onEdit = { onEditCard(row.id) },
                                onShare = { onShareCard(row.id) },
                                hit = state.hits[row.id]
                            )
                        }
                        return@LazyColumn
                    }
                    items(state.sections, key = { it.key }) { section ->
                        val open = settings.openSection == section.key
                        SectionCard(
                            section = section,
                            open = open,
                            showCategoryChip = settings.groupBy != com.krishna.infodeck.core.GroupBy.CATEGORY,
                            filterActive = settings.filter.isActive,
                            selectionMode = selectionMode,
                            selection = selection,
                            today = today,
                            onToggle = {
                                // One section open at a time (D2 C5 row 1) when the switch is on.
                                if (open) vm.setOpenSection("")
                                else vm.setOpenSection(if (settings.singleOpen) section.key else section.key)
                            },
                            onCard = { row ->
                                if (selectionMode) vm.toggleSelect(row.id) else onOpenCard(row.id)
                            },
                            onCardLong = { row -> if (!selectionMode) vm.startSelection(row.id) else vm.toggleSelect(row.id) },
                            onAddHere = { onNewCard(section.categoryId) },
                            density = settings.density,
                            expandable = settings.tapExpand,
                            expandedId = expandedId,
                            onExpand = { row ->
                                expandedId = com.krishna.infodeck.core.ExpandRules.next(
                                    expandedId, row.id, section.cards.map { it.id }.toSet()
                                )
                            },
                            onEdit = { row -> onEditCard(row.id) },
                            onShare = { row -> onShareCard(row.id) }
                        )
                    }
                    if (state.hiddenSections > 0) {
                        item {
                            Text(
                                "${state.hiddenSections} categories hidden by filters",
                                modifier = Modifier.fillMaxWidth().clickable { showFilter = true }.padding(8.dp),
                                fontSize = 11.5.sp, color = Hint,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGroup) GroupBySheet(
        current = settings.groupBy, order = settings.groupOrder, singleOpen = settings.singleOpen,
        onDismiss = { showGroup = false },
        onApply = { g, o, s -> vm.setGroupBy(g, o, s); showGroup = false },
        density = settings.density,
        tapExpand = settings.tapExpand,
        onDensity = { vm.setDensity(it) },
        onTapExpand = { vm.setTapExpand(it) }
    )

    if (showSort) SortBySheet(
        current = settings.sortBy, asc = settings.sortAsc, groupedByCategory = settings.groupBy == com.krishna.infodeck.core.GroupBy.CATEGORY,
        onDismiss = { showSort = false },
        onApply = { by, asc -> vm.setSort(by, asc); showSort = false }
    )

    if (showFilter) FilterSheet(
        current = settings.filter,
        categories = state.categories,
        countFor = { vm.previewCount(it) },
        onDismiss = { showFilter = false },
        onApply = { vm.setFilter(it); showFilter = false }
    )

    if (showMove) MoveToCategorySheet(
        count = selection.size,
        categories = state.categories,
        currentCategoryId = state.allRows.firstOrNull { selection.contains(it.id) }?.categoryId,
        onCreate = { name, color, cb -> vm.addCategory(name, color) { cb(it) } },
        onDismiss = { showMove = false },
        onMove = { catId ->
            showMove = false
            val target = state.categories.firstOrNull { it.id == catId }?.name ?: "category"
            vm.moveSelected(catId) { n ->
                scope.launch {
                    val res = snackbar.showSnackbar("Moved $n cards to $target", actionLabel = "UNDO")
                    if (res == SnackbarResult.ActionPerformed) {
                        snackbar.showSnackbar("Open the cards to move them back")
                    }
                }
            }
        }
    )

    if (showMore) InfoSheet(onDismiss = { showMore = false }) {
        SheetTitle("More · ${selection.size} cards", com.krishna.infodeck.core.Hue.INDIGO, Icons.Filled.MoreVert)
        Row(
            Modifier.fillMaxWidth().clickable {
                showMore = false
                vm.remindersForSelected(true) { n ->
                    scope.launch { snackbar.showSnackbar("Reminders turned on for $n cards") }
                }
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Turn reminders back on", fontSize = 14.sp)
        }
        androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().clickable { showMore = false; showDanger = true }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Danger zone...", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)
                Text("Delete ${selection.size} cards", fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        EditorSheetButtons(onCancel = { showMore = false }, onSave = { showMore = false }, saveText = "Close")
    }

    if (showDanger) DangerSheet(
        title = "${selection.size} selected cards",
        detail = "Their data, dates, reminders and attached files go with them.",
        deleteLabel = if (selection.size == 1) "Delete card" else "Delete ${selection.size} cards",
        onDismiss = { showDanger = false },
        onDelete = { showDanger = false; typedDelete = ""; confirmDelete = true }
    )

    if (confirmDelete) {
        val needsTyping = selection.size >= 5
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selection.size} cards?") },
            text = {
                Column {
                    Text("Their data, dates, reminders and files go with them.")
                    if (needsTyping) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = typedDelete, onValueChange = { typedDelete = it },
                            label = { Text("Type DELETE to confirm") }, singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !needsTyping || typedDelete.trim() == "DELETE",
                    onClick = {
                        confirmDelete = false
                        vm.deleteSelected { n -> scope.launch { snackbar.showSnackbar("Deleted $n cards") } }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    LaunchedEffect(query) { if (query.isNotEmpty()) searching = true }
}

@Composable
private fun ControlRow(
    groupLabel: String, sortLabel: String, filterCount: Int,
    onGroup: () -> Unit, onSort: () -> Unit, onSortLong: () -> Unit, onFilter: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ControlChip(Icons.Filled.ViewAgenda, groupLabel, true, Modifier.weight(1f, false), onGroup, null)
        ControlChip(Icons.Filled.Sort, sortLabel, false, Modifier.weight(1f, false), onSort, onSortLong)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(width = 40.dp, height = 32.dp)
                .background(
                    if (filterCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.5.dp,
                    if (filterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onFilter() },
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(badge = { if (filterCount > 0) Badge { Text("$filterCount") } }) {
                Icon(Icons.Filled.FilterList, "Filters", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ControlChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, filled: Boolean, modifier: Modifier,
    onClick: () -> Unit, onLong: (() -> Unit)?
) {
    Row(
        modifier
            .height(32.dp)
            .background(
                if (filled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.5.dp,
                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Filled.ExpandMore, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickChips(filter: FilterState, overdueCount: Int, onSet: (FilterState) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QuickChip("All", !filter.isActive) { onSet(FilterState()) }
        if (overdueCount > 0) QuickChip(
            "Overdue $overdueCount",
            filter.expiry == ExpiryFilter.OVERDUE,
            danger = true
        ) {
            onSet(
                if (filter.expiry == ExpiryFilter.OVERDUE) filter.copy(expiry = ExpiryFilter.ANY)
                else filter.copy(expiry = ExpiryFilter.OVERDUE)
            )
        }
        QuickChip("≤ 30 days", filter.expiry == ExpiryFilter.D30) {
            onSet(
                if (filter.expiry == ExpiryFilter.D30) filter.copy(expiry = ExpiryFilter.ANY)
                else filter.copy(expiry = ExpiryFilter.D30)
            )
        }
        QuickChip("No reminder", filter.reminder == ReminderFilter.NONE) {
            onSet(
                if (filter.reminder == ReminderFilter.NONE) filter.copy(reminder = ReminderFilter.ANY)
                else filter.copy(reminder = ReminderFilter.NONE)
            )
        }
    }
}

@Composable
private fun QuickChip(text: String, on: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val bg = when {
        on && danger -> MaterialTheme.colorScheme.errorContainer
        on -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        danger -> MaterialTheme.colorScheme.error
        on -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        Modifier
            .height(28.dp)
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.5.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionCard(
    section: Section,
    open: Boolean,
    showCategoryChip: Boolean,
    filterActive: Boolean,
    selectionMode: Boolean,
    selection: Set<Long>,
    today: LocalDate,
    onToggle: () -> Unit,
    onCard: (CardRow) -> Unit,
    onCardLong: (CardRow) -> Unit,
    onAddHere: () -> Unit,
    density: com.krishna.infodeck.core.Density = com.krishna.infodeck.core.Density.DEFAULT,
    expandable: Boolean = false,
    expandedId: Long? = null,
    onExpand: (CardRow) -> Unit = {},
    onEdit: (CardRow) -> Unit = {},
    onShare: (CardRow) -> Unit = {}
) {
    val p = deck
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    val h = section.colorIndex?.let { com.krishna.infodeck.core.Hues.forCategory(it) } ?: com.krishna.infodeck.core.Hue.INDIGO
    val hs = p.hue(h)
    val shape = RoundedCornerShape(16.dp)
    // Sunrise: an open section is a heading on the page with its wallet cards below.
    val openOnPage = p.walletRows && open
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (openOnPage) Modifier
                else Modifier
                    .clip(shape)
                    .background(p.surface)
                    .border(
                        if (open) 1.5.dp else 1.dp,
                        if (open) hs.glyph.copy(alpha = if (p.dark) 0.7f else 0.45f) else p.line,
                        shape
                    )
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (open && !openOnPage) (if (p.dark) hs.tint else hs.tint.copy(alpha = 0.55f)) else Color.Transparent)
                .clickable { onToggle() }
                .heightIn(min = 50.dp)
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HueTile(glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(section.title)), h, size = 30)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f).padding(vertical = 7.dp)) {
                Text(section.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!open) Text(section.subtitle, fontSize = 11.sp, color = p.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Pill(
                if (filterActive && section.totalCount != section.cards.size)
                    "${section.cards.size} of ${section.totalCount}" else "${section.cards.size}",
                p.surface2, p.ink2
            )
            Spacer(Modifier.width(2.dp))
            Icon(Icons.Filled.ExpandMore, if (open) "Collapse" else "Expand",
                Modifier.size(22.dp).rotate(rotation), tint = p.ink2)
        }

        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(horizontal = if (openOnPage) 0.dp else 8.dp, vertical = 6.dp)) {
                section.cards.forEach { row ->
                    MiniCard(
                        row = row,
                        today = today,
                        showCategoryChip = showCategoryChip,
                        selectionMode = selectionMode,
                        selected = selection.contains(row.id),
                        onClick = { onCard(row) },
                        onLong = { onCardLong(row) },
                        density = density,
                        expandable = expandable,
                        expanded = expandedId == row.id,
                        onExpand = { onExpand(row) },
                        onEdit = { onEdit(row) },
                        onShare = { onShare(row) }
                    )
                    Spacer(Modifier.height(density.gapDp.dp + if (p.walletRows) 2.dp else 0.dp))
                }
                if (section.categoryId != null && !selectionMode) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onAddHere() }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = if (p.dark) hs.glyph else hs.onTint)
                        Spacer(Modifier.width(5.dp))
                        Text("Add card to ${section.title}", fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold, color = if (p.dark) hs.glyph else hs.onTint, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MiniCard(
    row: CardRow, today: LocalDate, showCategoryChip: Boolean,
    selectionMode: Boolean, selected: Boolean,
    onClick: () -> Unit, onLong: () -> Unit, flat: Boolean = false,
    density: com.krishna.infodeck.core.Density = com.krishna.infodeck.core.Density.DEFAULT,
    expandable: Boolean = false,
    expanded: Boolean = false,
    onExpand: () -> Unit = {},
    onEdit: () -> Unit = {},
    onShare: () -> Unit = {},
    hit: com.krishna.infodeck.core.SearchHit? = null
) {
    val p = deck
    val overdue = row.isOverdue(today)
    val d = density
    val showChips = d.showChips || expanded
    val showMeta = d.showMeta || expanded
    val rowTap: () -> Unit = if (expandable && !selectionMode) onExpand else onClick
    val catHue = com.krishna.infodeck.core.Hues.forCategory(row.colorIndex)
    val ch = p.hue(catHue)
    val rose = p.hue(com.krishna.infodeck.core.Hue.ROSE)
    // Sunrise: every card is a gradient wallet card; overdue ones turn rose-red.
    val wallet = p.walletRows
    val onCard = if (wallet) Color.White else MaterialTheme.colorScheme.onSurface
    val sub = if (wallet) Color.White.copy(alpha = 0.88f) else p.ink2
    val shape = RoundedCornerShape(if (wallet) 18.dp else 14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (wallet) Modifier.androidxShadow(if (expanded) 10.dp else 6.dp, shape, if (overdue) rose.glyph else ch.glyph)
                  else if (!p.dark && flat) Modifier.androidxShadow(3.dp, shape, p.brand.first()) else Modifier)
            .clip(shape)
            .background(
                when {
                    wallet && overdue -> Brush.linearGradient(listOf(rose.glyph, Color(0xFFFF8A65)))
                    wallet -> Brush.linearGradient(listOf(ch.glyph, ch.glyph2))
                    selected -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primaryContainer)
                    overdue -> androidx.compose.ui.graphics.SolidColor(rose.tint)
                    else -> androidx.compose.ui.graphics.SolidColor(p.surface)
                }
            )
            .then(
                if (wallet) Modifier.border(if (selected) 3.dp else 0.dp, if (selected) p.ink else Color.Transparent, shape)
                else Modifier.border(
                    if (expanded || selected) 1.5.dp else 1.dp,
                    when {
                        selected -> MaterialTheme.colorScheme.primary
                        expanded -> ch.glyph
                        overdue -> rose.glyph.copy(alpha = 0.35f)
                        else -> p.line
                    },
                    shape
                )
            )
            .then(if (p.dark && !wallet) Modifier.drawBehind {
                drawRect(if (overdue) rose.glyph else ch.glyph, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
            } else Modifier)
            .then(if (wallet) Modifier.drawBehind {
                drawCircle(Color.White.copy(alpha = 0.12f), radius = size.height * 0.9f,
                    center = androidx.compose.ui.geometry.Offset(size.width - size.height * 0.2f, -size.height * 0.15f))
            } else Modifier)
            .combinedClickable(onClick = rowTap, onLongClick = onLong)
            .padding(
                horizontal = 10.dp,
                vertical = when {
                    d == com.krishna.infodeck.core.Density.COMPACT -> 5.dp
                    wallet -> 11.dp
                    else -> 8.dp
                }
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!selectionMode) {
                if (wallet) Box(
                    Modifier.size(scaledDp(d.tileDp)).clip(RoundedCornerShape((d.tileDp / 2.9f).dp))
                        .background(Color.White.copy(alpha = 0.22f)).border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape((d.tileDp / 2.9f).dp))
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(row.categoryName)), null,
                        Modifier.size((d.tileDp * 0.48f).dp), tint = Color.White)
                }
                else HueTile(
                    glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(row.categoryName)),
                    catHue,
                    size = d.tileDp.toInt(),
                    modifier = Modifier.clickable(onClick = onClick)
                )
                Spacer(Modifier.width(if (d == com.krishna.infodeck.core.Density.COMPACT) 8.dp else 10.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.label, fontSize = d.titleSp.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = onCard,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).clickable(onClick = onClick)
                    )
                    if (wallet && overdue) {
                        Spacer(Modifier.width(6.dp))
                        Text("OVERDUE", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = rose.glyph,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color.White).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
                if (hit != null) {
                    HitLine(hit)
                } else if (showMeta) {
                    val line = when {
                        row.keyDate != null -> "${row.keyDateName} ${Fmt.date(row.keyDate)}"
                        row.firstDataName != null -> "${row.firstDataName} ${row.firstDataValue}"
                        else -> "No date"
                    }
                    Text(line, fontSize = 11.5.sp, color = if (overdue && !wallet) rose.onTint else sub,
                        fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (showChips) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val wc = Color.White.copy(alpha = 0.22f)
                    if (showCategoryChip) Pill(row.categoryName, if (wallet) wc else ch.tint, if (wallet) Color.White else ch.onTint)
                    row.tagNames.take(2).forEach { t ->
                        val tg = p.hue(com.krishna.infodeck.core.Hue.VIOLET)
                        Pill("#" + t, if (wallet) wc else tg.tint, if (wallet) Color.White else tg.onTint)
                    }
                    if (row.fileCount > 0) {
                        val fl = p.hue(com.krishna.infodeck.core.Hue.PURPLE)
                        Pill(if (row.fileCount == 1) "1 file" else "${row.fileCount} files",
                            if (wallet) wc else fl.tint, if (wallet) Color.White else fl.onTint)
                    }
                    val am = p.hue(com.krishna.infodeck.core.Hue.AMBER)
                    if (row.nextRingAt != null) Pill(
                        "Rings ${Fmt.short(row.nextRingAt.toLocalDate())} - ${Fmt.relativeShort(row.nextRingAt.toLocalDate(), today)}",
                        if (wallet) wc else am.tint, if (wallet) Color.White else am.onTint
                    ) else Pill("No reminder", if (wallet) wc else p.surface2, if (wallet) Color.White else p.ink2)
                }
            }
            if (selectionMode) {
                Checkbox(
                    checked = selected, onCheckedChange = { onClick() },
                    colors = if (wallet) androidx.compose.material3.CheckboxDefaults.colors(
                        checkedColor = Color.White, uncheckedColor = Color.White, checkmarkColor = ch.glyph
                    ) else androidx.compose.material3.CheckboxDefaults.colors()
                )
            } else {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.clickable(onClick = onClick)) {
                    CountdownRing(
                        row.keyDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) },
                        compact = d == com.krishna.infodeck.core.Density.COMPACT,
                        sizeDp = d.ringDp,
                        onColour = wallet
                    )
                }
                if (expandable) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        if (expanded) "Collapse" else "Expand", Modifier.size(18.dp),
                        tint = when {
                            wallet -> Color.White
                            expanded -> ch.glyph
                            else -> p.hint
                        }
                    )
                }
            }
        }
        if (expanded && !selectionMode) {
            Divider(Modifier.padding(top = 8.dp, bottom = 6.dp), color = if (wallet) Color.White.copy(alpha = 0.35f) else p.line)
            com.krishna.infodeck.core.ExpandRules.visibleValues(row.dataPreview).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(k, fontSize = 12.sp, color = sub)
                    Spacer(Modifier.weight(1f))
                    Text(v, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onCard, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ExpandAction(Icons.Filled.OpenInNew, "Open", false, wallet, ch.glyph, onClick)
                ExpandAction(Icons.Filled.Edit, "Edit", false, wallet, ch.glyph, onEdit)
                ExpandAction(Icons.Filled.Share, "Share", true, wallet, ch.glyph, onShare)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ExpandAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, primary: Boolean,
    wallet: Boolean, cardColour: Color, onClick: () -> Unit
) {
    val p = deck
    val g = p.hue(com.krishna.infodeck.core.Hue.GREEN)
    val pri = p.hue(com.krishna.infodeck.core.Hue.INDIGO)
    val bg: Brush = when {
        wallet && primary -> androidx.compose.ui.graphics.SolidColor(Color.White)
        wallet -> androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.22f))
        primary -> Brush.linearGradient(listOf(g.glyph, g.glyph2))
        else -> androidx.compose.ui.graphics.SolidColor(pri.tint)
    }
    val fg = when {
        wallet && primary -> cardColour
        wallet -> Color.White
        primary -> if (p.dark) Color(0xFF0A0E1F) else Color.White
        else -> if (p.dark) pri.glyph else pri.onTint
    }
    Row(
        Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bg)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = fg)
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

@Composable
private fun SelectionBar(
    enabled: Boolean, onMove: () -> Unit, onReminders: () -> Unit,
    onDuplicate: () -> Unit, onMore: () -> Unit
) {
    val p = deck
    Row(
        Modifier.fillMaxWidth()
            .background(if (p.dark) androidx.compose.ui.graphics.SolidColor(p.surface) else Brush.linearGradient(p.brand))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SelAction(Icons.Filled.DriveFileMove, "Move", enabled, onMove)
        SelAction(Icons.Filled.Alarm, "Reminders", enabled, onReminders)
        SelAction(Icons.Filled.ContentCopy, "Duplicate", enabled, onDuplicate)
        SelAction(Icons.Filled.MoreVert, "More", enabled, onMore)
    }
}

@Composable
private fun SelAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    enabled: Boolean, onClick: () -> Unit, danger: Boolean = false
) {
    val p = deck
    val base = if (p.dark) p.hue(com.krishna.infodeck.core.Hue.INDIGO).glyph else Color.White
    val tint = when {
        !enabled -> base.copy(alpha = 0.45f)
        danger -> MaterialTheme.colorScheme.error
        else -> base
    }
    Column(
        Modifier.clickable(enabled = enabled) { onClick() }.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = tint)
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

/** Header action on the Home hero — white on colour, ink on the Sunrise page. */
@Composable
private fun HeroAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val p = deck
    if (p.heroOnColour) IconButton(onClick = onClick) {
        Icon(icon, label, tint = if (p.dark) p.ink else Color.White)
    } else Box(
        Modifier.padding(horizontal = 3.dp).size(36.dp).androidxShadow(4.dp, RoundedCornerShape(18.dp), p.brand.first())
            .clip(RoundedCornerShape(18.dp)).background(p.surface).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, Modifier.size(19.dp), tint = p.ink) }
}

/** "Grouped by Category · View · Filter" under the hero (design 1.15.0.D1 frame 02). */
@Composable
private fun HomeToolbar(groupLabel: String, activeFilters: Int, onView: () -> Unit, onFilter: () -> Unit) {
    val p = deck
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                append("Grouped by ")
                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = p.ink)) { append(groupLabel) }
            },
            fontSize = 11.5.sp, color = p.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        ToolbarChip(Icons.Filled.ViewAgenda, "View", 0, onView)
        ToolbarChip(Icons.Filled.FilterList, "Filter", activeFilters, onFilter)
    }
}

@Composable
private fun ToolbarChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, badge: Int, onClick: () -> Unit) {
    val p = deck
    val pri = p.hue(com.krishna.infodeck.core.Hue.INDIGO)
    val c = if (p.dark) pri.glyph else pri.onTint
    Row(
        Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(p.surface)
            .border(1.dp, if (badge > 0) pri.glyph else p.line, RoundedCornerShape(15.dp))
            .clickable { onClick() }.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = c)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = c)
        if (badge > 0) {
            Spacer(Modifier.width(4.dp))
            Text("$badge", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = if (p.dark) Color(0xFF0A0E1F) else Color.White,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(pri.glyph).padding(horizontal = 5.dp))
        }
    }
}

@Composable
private fun EmptyState(hasAnyCard: Boolean, filterActive: Boolean, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Icon(Icons.Filled.Add, null, Modifier.size(40.dp), tint = Hint)
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                filterActive -> "No cards match these filters"
                hasAnyCard -> "Nothing to show"
                else -> "Create your first card"
            },
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (filterActive) "Clear the filters to see everything again."
            else "Tap New card to store something - a policy, a warranty, a document.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (filterActive) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onClear) { Text("Clear all filters") }
        }
    }
}

/** B12 — the meta line becomes the matching field: name, value with the hit highlighted, and a kind badge. */
@Composable
private fun HitLine(hit: com.krishna.infodeck.core.SearchHit) {
    val f = hit.field
    val shown = if (f.kind == com.krishna.infodeck.core.SearchField.Kind.DATE) f.value.substringBefore(' ') else f.value
    val text = androidx.compose.ui.text.buildAnnotatedString {
        withStyle(androidx.compose.ui.text.SpanStyle(color = Ink2)) { append(f.name); append(' ') }
        if (hit.start >= 0 && hit.end <= shown.length) {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(shown.substring(0, hit.start)) }
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, background = deck.hue(com.krishna.infodeck.core.Hue.AMBER).tint, color = deck.hue(com.krishna.infodeck.core.Hue.AMBER).onTint)) { append(shown.substring(hit.start, hit.end)) }
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(shown.substring(hit.end)) }
        } else withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(shown) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (f.kind.badge.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            val h = when (f.kind) {
                com.krishna.infodeck.core.SearchField.Kind.DATA -> com.krishna.infodeck.core.Hue.SKY
                com.krishna.infodeck.core.SearchField.Kind.DATE -> com.krishna.infodeck.core.Hue.GREEN
                com.krishna.infodeck.core.SearchField.Kind.FILE -> com.krishna.infodeck.core.Hue.PURPLE
                com.krishna.infodeck.core.SearchField.Kind.TAG -> com.krishna.infodeck.core.Hue.VIOLET
                com.krishna.infodeck.core.SearchField.Kind.REMINDER -> com.krishna.infodeck.core.Hue.AMBER
                else -> com.krishna.infodeck.core.Hue.SLATE
            }
            Text(f.kind.badge, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = onTint(h),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tint(h)).padding(horizontal = 5.dp, vertical = 1.dp))
        }
    }
}

/** "N results for 'q'" plus bucket chips when the hits span more than one kind. */
@Composable
private fun SearchCountLine(
    total: Int, query: String,
    kinds: Map<com.krishna.infodeck.core.SearchField.Kind, Int>,
    bucket: com.krishna.infodeck.core.SearchField.Kind?,
    onBucket: (com.krishna.infodeck.core.SearchField.Kind?) -> Unit
) {
    val shown = listOf(
        com.krishna.infodeck.core.SearchField.Kind.DATA, com.krishna.infodeck.core.SearchField.Kind.DATE,
        com.krishna.infodeck.core.SearchField.Kind.FILE
    ).filter { (kinds[it] ?: 0) > 0 }
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold, color = hue(com.krishna.infodeck.core.Hue.INDIGO))) {
                    append(if (total == 1) "1 result" else "$total results")
                }
                append(" for \u201c$query\u201d")
            },
            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (shown.size > 1) {
            BucketChip("All $total", bucket == null) { onBucket(null) }
            shown.forEach { k -> BucketChip(k.badge.lowercase().replaceFirstChar { it.uppercase() } + " " + (kinds[k] ?: 0), bucket == k) { onBucket(k) } }
        }
    }
}

@Composable
private fun BucketChip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (on) hue(com.krishna.infodeck.core.Hue.INDIGO) else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

/** The miss is visible: which words matched something and which did not. */
@Composable
private fun SearchEmpty(query: String, missing: List<String>) {
    val words = com.krishna.infodeck.core.SearchRules.words(query)
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text(
            if (words.size == 1) "0 results for \u201c$query\u201d" else "0 results for \u201c$query\u201d",
            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(26.dp))
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(tint(com.krishna.infodeck.core.Hue.INDIGO)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Search, null, Modifier.size(32.dp), tint = hue(com.krishna.infodeck.core.Hue.INDIGO)) }
        Spacer(Modifier.height(12.dp))
        Text(
            if (words.size > 1 && missing.size < words.size) "Nothing matches all ${words.size} words" else "Nothing matches",
            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        val detail = when {
            words.size > 1 && missing.isEmpty() -> "Each word matches a different card. Try fewer words."
            missing.isNotEmpty() && words.size > 1 -> "No card contains " + missing.joinToString(", ") { "\u201c$it\u201d" } + ". Try fewer words, or a date like 11-Oct-2026."
            else -> "Search reads every name, value, date, file name and reminder on your cards. Try a date like 11-Oct-2026, or part of a number."
        }
        Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 17.sp)
    }
}
