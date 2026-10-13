package com.krishna.infodeck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.data.Repo
import com.krishna.infodeck.data.RingDirection
import com.krishna.infodeck.data.RingUnit
import java.time.LocalDate
import java.time.ZoneOffset

private data class DataRow(val localId: Long, var name: String, var value: String)
private data class DateRow(val localId: Long, var name: String, var epochDay: Long)
private data class SchedRow(
    val localId: Long, val existingId: Long, var dateLocalId: Long, var unit: RingUnit,
    var amount: Int, var direction: RingDirection, var ringMinute: Int, var enabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CardEditorScreen(
    vm: AppViewModel,
    cardId: Long?,
    presetCategoryId: Long?,
    onDone: () -> Unit
) {
    val home by vm.home.collectAsState()
    val settings = home.settings

    var loaded by remember { mutableStateOf(cardId == null || cardId <= 0) }
    var label by remember { mutableStateOf("") }
    var categoryId by remember { mutableLongStateOf(presetCategoryId ?: 0L) }
    var keyLocalId by remember { mutableLongStateOf(-1L) }
    val data = remember { mutableStateListOf<DataRow>() }
    val dates = remember { mutableStateListOf<DateRow>() }
    val scheds = remember { mutableStateListOf<SchedRow>() }
    var nextLocal by remember { mutableLongStateOf(1L) }
    var dirty by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingSched by remember { mutableStateOf<SchedRow?>(null) }
    var newSched by remember { mutableStateOf(false) }
    var datePickerFor by remember { mutableStateOf<Long?>(null) }
    var catMenu by remember { mutableStateOf(false) }
    var deleteDateConfirm by remember { mutableStateOf<DateRow?>(null) }
    var tagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showTagPicker by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<com.krishna.infodeck.data.FileEntity>>(emptyList()) }
    var fileMsg by remember { mutableStateOf<String?>(null) }
    var editorMenu by remember { mutableStateOf(false) }
    var showDanger by remember { mutableStateOf(false) }
    var renamingFile by remember { mutableStateOf<com.krishna.infodeck.data.FileEntity?>(null) }
    var removingFile by remember { mutableStateOf<com.krishna.infodeck.data.FileEntity?>(null) }
    val allTags by vm.tags.collectAsState()
    val recentTags by vm.recentTags.collectAsState()
    val tagUsage by vm.tagUsage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val caps = settings.autoCaps
    val capsValues = settings.autoCapsValues

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && cardId != null && cardId > 0) {
            vm.attachFile(cardId, uri) { err ->
                fileMsg = err ?: "File attached"
                vm.viewModelRefreshFiles(cardId) { files = it }
            }
        } else if (uri != null) {
            fileMsg = "Save the card first, then attach files"
        }
    }

    LaunchedEffect(cardId) {
        if (cardId != null && cardId > 0) {
            val d = vm.detail(cardId)
            if (d != null) {
                label = d.card.label
                categoryId = d.card.categoryId
                d.data.forEach { data.add(DataRow(nextLocal++, it.name, it.value)) }
                d.dates.forEach { dv ->
                    val local = nextLocal++
                    dates.add(DateRow(local, dv.name, dv.epochDay))
                    if (d.card.keyDateVarId == dv.id) keyLocalId = local
                    d.schedulers.filter { it.entity.dateVarId == dv.id }.forEach { s ->
                        scheds.add(
                            SchedRow(
                                nextLocal++, s.entity.id, local, s.entity.unit, s.entity.amount,
                                s.entity.direction, s.entity.ringMinute, s.entity.enabled
                            )
                        )
                    }
                }
                if (keyLocalId < 0) keyLocalId = dates.firstOrNull()?.localId ?: -1L
                tagIds = d.tags.map { it.id }.toSet()
                files = d.files
            }
        }
        if (categoryId <= 0) categoryId = home.categories.firstOrNull { !it.builtIn }?.id
            ?: home.categories.firstOrNull()?.id ?: 0L
        loaded = true
    }

    val categoryName = home.categories.firstOrNull { it.id == categoryId }?.name ?: "Select category"

    fun leave() { if (dirty) confirmLeave = true else onDone() }
    BackHandler { leave() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (cardId != null && cardId > 0) "Edit card" else "New card") },
                navigationIcon = { IconButton(onClick = { leave() }) { Icon(Icons.Filled.Close, "Close") } },
                actions = {
                    if (cardId != null && cardId > 0) Box {
                        IconButton(onClick = { editorMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "More", tint = Color.White)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = editorMenu, onDismissRequest = { editorMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Danger zone...", color = MaterialTheme.colorScheme.error) },
                                onClick = { editorMenu = false; showDanger = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Box(Modifier.background(MaterialTheme.colorScheme.background).padding(12.dp)) {
                EditorSheetButtons(
                    onCancel = { leave() },
                    onSave = {
                        vm.saveCard(
                            cardId = cardId, label = label, categoryId = categoryId,
                            data = data.map { Repo.DataInput(it.name, it.value) },
                            dates = dates.map { Repo.DateInput(it.localId, it.name, it.epochDay, it.localId == keyLocalId) },
                            scheds = scheds.map {
                                Repo.SchedInput(
                                    it.existingId, it.dateLocalId, it.unit, it.amount,
                                    it.direction, it.ringMinute, it.enabled
                                )
                            },
                            tagIds = tagIds
                        ) { onDone() }
                    },
                    saveEnabled = label.isNotBlank() && categoryId > 0
                )
            }
        }
    ) { pad ->
        if (!loaded) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...") }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(60); dirty = true },
                label = { Text("Label name") },
                singleLine = true,
                keyboardOptions = capsOptions(caps),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Box {
                OutlinedTextField(
                    value = categoryName, onValueChange = {}, readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Filled.ExpandMore, "Choose category") }
                )
                Box(Modifier.matchParentSize().clickable { catMenu = true })
                DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                    home.categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            onClick = { categoryId = c.id; dirty = true; catMenu = false }
                        )
                    }
                }
            }
            Text(
                "Changing the category moves this card when you save.",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )

            // ---------------- tags ----------------
            EditorHeader("Tags", enabled = true) { showTagPicker = true }
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allTags.filter { tagIds.contains(it.id) }.forEach { t ->
                    Box(Modifier.padding(bottom = 6.dp)) {
                        TagChip(t.name, onRemove = { tagIds = tagIds - t.id; dirty = true })
                    }
                }
                Box(Modifier.padding(bottom = 6.dp)) {
                    Row(
                        Modifier
                            .heightIn(min = 30.dp)
                            .clickable { showTagPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Tag", fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                "A card sits in one category but can carry any number of tags.",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ---------------- data ----------------
            EditorHeader("Data") {
                data.add(DataRow(nextLocal++, "", "")); dirty = true
            }
            data.forEachIndexed { i, row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = row.name,
                        onValueChange = { data[i] = row.copy(name = it.take(40)); dirty = true },
                        label = { Text("Name") }, singleLine = true,
                        keyboardOptions = capsOptions(caps),
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = row.value,
                        onValueChange = { data[i] = row.copy(value = it.take(500)); dirty = true },
                        label = { Text("Value") }, singleLine = true,
                        keyboardOptions = capsOptions(caps, isValueField = true, valuesIncluded = capsValues),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { data.removeAt(i); dirty = true }) { Icon(Icons.Filled.Close, "Remove") }
                }
            }
            Text(
                "Value: any text, up to 500 characters",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ---------------- dates ----------------
            EditorHeader("Dates") {
                val id = nextLocal++
                dates.add(DateRow(id, "", LocalDate.now().toEpochDay()))
                if (keyLocalId < 0) keyLocalId = id
                dirty = true
            }
            dates.forEachIndexed { i, row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = row.name,
                        onValueChange = { dates[i] = row.copy(name = it.take(40)); dirty = true },
                        label = { Text("Name") }, singleLine = true,
                        keyboardOptions = capsOptions(caps),
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = Fmt.date(LocalDate.ofEpochDay(row.epochDay)),
                            onValueChange = {}, readOnly = true,
                            label = { Text("Date") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Filled.CalendarMonth, null) }
                        )
                        Box(Modifier.matchParentSize().clickable { datePickerFor = row.localId })
                    }
                    IconButton(onClick = { keyLocalId = row.localId; dirty = true }) {
                        if (keyLocalId == row.localId)
                            Icon(Icons.Filled.Star, "Key date", tint = MaterialTheme.colorScheme.secondary)
                        else Icon(Icons.Outlined.StarBorder, "Make key date", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = {
                        if (scheds.any { it.dateLocalId == row.localId }) deleteDateConfirm = row
                        else { dates.removeAt(i); if (keyLocalId == row.localId) keyLocalId = dates.firstOrNull()?.localId ?: -1L; dirty = true }
                    }) { Icon(Icons.Filled.Close, "Remove") }
                }
            }
            Text(
                "★ marks the key date - the one this card is grouped and sorted by.",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ---------------- schedulers ----------------
            EditorHeader("Schedulers", enabled = dates.isNotEmpty()) { newSched = true }
            if (dates.isEmpty()) Text(
                "Add a date first - a scheduler always hangs off a date.",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            scheds.forEachIndexed { i, s ->
                val dateRow = dates.firstOrNull { it.localId == s.dateLocalId }
                val baseName = dateRow?.name?.ifBlank { "(unnamed date)" } ?: "(date removed)"
                val ringAt = dateRow?.let {
                    com.krishna.infodeck.core.RingCalculator.ringDateTime(
                        LocalDate.ofEpochDay(it.epochDay), s.unit, s.amount, s.direction, s.ringMinute
                    )
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { editingSched = s }
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(28.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Alarm, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                com.krishna.infodeck.core.RingCalculator.ruleText(s.unit, s.amount, s.direction, baseName),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${Fmt.dateTime(ringAt)} - tap to edit",
                                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { scheds.removeAt(i); dirty = true }) { Icon(Icons.Filled.Close, "Remove") }
                    }
                }
            }
            // ---------------- files ----------------
            EditorHeader("Files", enabled = cardId != null && cardId > 0) {
                filePicker.launch(
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                )
            }
            if (cardId == null || cardId <= 0) Text(
                "Save the card once, then attach files to it.",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            files.forEach { f ->
                Box(Modifier.padding(bottom = 7.dp)) {
                    FileRow(
                        file = f,
                        onOpen = { fileMsg = com.krishna.infodeck.data.FileStore.open(context, f) },
                        onShare = { fileMsg = com.krishna.infodeck.data.FileStore.share(context, f) },
                        onRename = { renamingFile = f },
                        onRemove = { removingFile = f }
                    )
                }
            }
            if (files.isNotEmpty()) Text(
                "PDF and Word files, up to 25 MB each, 10 per card. Copied into InfoDeck, so moving or deleting the original never breaks the card.",
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            fileMsg?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTagPicker) TagPickerSheet(
        all = allTags, recent = recentTags, usage = tagUsage, selected = tagIds,
        onToggle = { id -> tagIds = if (tagIds.contains(id)) tagIds - id else tagIds + id; dirty = true },
        onCreate = { name, cb -> vm.addTag(name) { cb(it) } },
        onDismiss = { showTagPicker = false },
        onDone = { showTagPicker = false }
    )

    val rf = renamingFile
    if (rf != null) NameDialog("Rename file", rf.displayName, { renamingFile = null }) { name ->
        vm.renameFile(rf.id, name) { renamingFile = null; vm.viewModelRefreshFiles(cardId ?: 0L) { files = it } }
    }

    val xf = removingFile
    if (xf != null) AlertDialog(
        onDismissRequest = { removingFile = null },
        title = { Text("Remove \"${xf.displayName}\"?") },
        text = { Text("Only InfoDeck's copy is deleted. Your original file is untouched.") },
        confirmButton = {
            TextButton(onClick = {
                vm.removeFile(xf.id) { vm.viewModelRefreshFiles(cardId ?: 0L) { files = it } }
                removingFile = null
            }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { removingFile = null }) { Text("Cancel") } }
    )

    if (showDanger) DangerSheet(
        title = label.ifBlank { "This card" },
        detail = "${data.size} data, ${dates.size} dates, ${scheds.size} schedulers and ${files.size} files go with it.",
        deleteLabel = "Delete card",
        onDismiss = { showDanger = false },
        onDelete = { showDanger = false; confirmDelete = true }
    )

    // ---------- date picker ----------
    val pickFor = datePickerFor
    if (pickFor != null) {
        val row = dates.firstOrNull { it.localId == pickFor }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (row?.epochDay ?: LocalDate.now().toEpochDay()) * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val ms = state.selectedDateMillis
                    if (ms != null && row != null) {
                        val d = java.time.Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        val idx = dates.indexOfFirst { it.localId == pickFor }
                        if (idx >= 0) { dates[idx] = row.copy(epochDay = d.toEpochDay()); dirty = true }
                    }
                    datePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePickerFor = null }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // ---------- scheduler sheet ----------
    if (newSched || editingSched != null) {
        val editing = editingSched
        SchedulerSheet(
            dates = dates.map { it.localId to (it.name.ifBlank { "(unnamed date)" } to it.epochDay) },
            initial = editing?.let {
                SchedulerDraft(it.dateLocalId, it.unit, it.amount, it.direction, it.ringMinute)
            } ?: SchedulerDraft(
                dates.firstOrNull { it.localId == keyLocalId }?.localId ?: dates.first().localId,
                RingUnit.SAME_DAY, 1, RingDirection.BEFORE, settings.defaultRingMinute
            ),
            isEditing = editing != null,
            onDismiss = { newSched = false; editingSched = null },
            onDelete = {
                editing?.let { e -> scheds.removeAll { it.localId == e.localId } }
                newSched = false; editingSched = null; dirty = true
            },
            onSave = { draft ->
                if (editing != null) {
                    val idx = scheds.indexOfFirst { it.localId == editing.localId }
                    if (idx >= 0) scheds[idx] = editing.copy(
                        dateLocalId = draft.dateLocalId, unit = draft.unit, amount = draft.amount,
                        direction = draft.direction, ringMinute = draft.ringMinute
                    )
                } else {
                    scheds.add(
                        SchedRow(
                            nextLocal++, 0L, draft.dateLocalId, draft.unit, draft.amount,
                            draft.direction, draft.ringMinute, true
                        )
                    )
                }
                dirty = true; newSched = false; editingSched = null
            }
        )
    }

    // ---------- dialogs ----------
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("Discard changes?") },
        text = { Text("This card has unsaved changes.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; onDone() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep editing") } }
    )

    if (confirmDelete && cardId != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this card?") },
        text = { Text("Its data, dates and reminders go with it.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; vm.deleteCard(cardId) { onDone() } }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )

    val delDate = deleteDateConfirm
    if (delDate != null) {
        val n = scheds.count { it.dateLocalId == delDate.localId }
        AlertDialog(
            onDismissRequest = { deleteDateConfirm = null },
            title = { Text("Remove this date?") },
            text = { Text("$n scheduler(s) use this date and will be deleted with it.") },
            confirmButton = {
                TextButton(onClick = {
                    scheds.removeAll { it.dateLocalId == delDate.localId }
                    dates.removeAll { it.localId == delDate.localId }
                    if (keyLocalId == delDate.localId) keyLocalId = dates.firstOrNull()?.localId ?: -1L
                    dirty = true; deleteDateConfirm = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteDateConfirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EditorHeader(title: String, enabled: Boolean = true, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(title, Modifier.weight(1f))
        Row(
            Modifier.clickable(enabled = enabled) { onAdd() }.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                Icons.Filled.Add, null, Modifier.size(16.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Text(
                "Add", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}
