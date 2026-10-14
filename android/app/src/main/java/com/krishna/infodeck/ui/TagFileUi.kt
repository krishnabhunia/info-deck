package com.krishna.infodeck.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.TextRole
import com.krishna.infodeck.data.FileEntity
import com.krishna.infodeck.data.FileRules
import com.krishna.infodeck.data.TagEntity
import com.krishna.infodeck.data.TagRules
import kotlinx.coroutines.launch

/** Dashed, colourless — categories own colour in this app. */
@Composable
fun TagChip(name: String, onRemove: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .heightIn(min = 30.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(15.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 11.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            TagRules.display(name), fontSize = roleSp(12.5f, TextRole.CHIP),
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
        )
        if (onRemove != null) {
            Spacer(Modifier.width(5.dp))
            Icon(
                Icons.Filled.Close, "Remove tag",
                Modifier.size(15.dp).clickable { onRemove() }, tint = Hint
            )
        }
    }
}

/** Tag picker with search, counts and inline create (design 1.1.D2 S7). */
@Composable
fun TagPickerSheet(
    all: List<TagEntity>,
    recent: List<TagEntity>,
    usage: Map<Long, Int>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onCreate: (String, (Long) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val clean = TagRules.normalise(query)
    val matches = all.filter { clean.isBlank() || it.name.contains(clean, ignoreCase = true) }
    val exact = all.any { it.name.equals(clean, ignoreCase = true) }

    InfoSheet(onDismiss = onDismiss) {
        Text("Tags", fontSize = roleSp(17f, TextRole.SHEET), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it.take(TagRules.MAX_NAME + 1) },
            placeholder = { Text("Search or create") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        matches.take(12).forEach { t ->
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(t.id) }.heightIn(min = 46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected.contains(t.id), onCheckedChange = { onToggle(t.id) })
                Column(Modifier.weight(1f)) {
                    Text(TagRules.display(t.name), fontSize = 14.sp)
                    Text(
                        "on ${usage[t.id] ?: 0} cards", fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (clean.isNotBlank() && !exact) {
            Row(
                Modifier.fillMaxWidth().clickable {
                    onCreate(clean) { id -> if (id > 0) { onToggle(id); query = "" } }
                }.heightIn(min = 46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Create \"${TagRules.display(clean)}\"", fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("New tag, added to this card", fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (recent.isNotEmpty() && query.isBlank()) {
            SectionLabel("Recently used")
            FlowRowTags(recent.filterNot { selected.contains(it.id) }.take(8)) { onToggle(it) }
        }

        Spacer(Modifier.height(14.dp))
        EditorSheetButtons(
            onCancel = onDismiss, onSave = onDone,
            saveText = if (selected.size == 1) "Done - 1 tag" else "Done - ${selected.size} tags"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowTags(tags: List<TagEntity>, onPick: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.forEach { t ->
            Box(Modifier.padding(bottom = 6.dp)) { TagChip(t.name, onClick = { onPick(t.id) }) }
        }
    }
}

/**
 * Layer 2 of the delete rule: the only place a Delete button exists. Close is the
 * plain action; Delete is outlined red and still asks once more.
 */
@Composable
fun DangerSheet(
    title: String,
    detail: String,
    deleteLabel: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    InfoSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text("Danger zone", fontSize = roleSp(17f, TextRole.SHEET), fontWeight = FontWeight.Bold)
        }
        Text(
            "Nothing here can be undone from another device - InfoDeck keeps everything on this phone only.",
            fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
        )
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(detail, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(14.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(deleteLabel, fontWeight = FontWeight.Bold)
        }
        Text(
            "Tapping this still asks once more.",
            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Close", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun FileRow(
    file: FileEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val kind = FileRules.kind(file.mime, file.displayName)
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 34.dp, height = 40.dp)
                .background(if (kind == "PDF") Color(0xFFC0392B) else Color(0xFF2B5797), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(kind, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            // N62 (Q7): cut inside the NAME so the extension always stays visible
            val parts = com.krishna.infodeck.core.FileNameRules.split(file.displayName)
            val nameSize = roleSp(13.5f, TextRole.TITLE)
            Row {
                Text(parts.base, fontSize = nameSize, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (parts.hasExt) Text(".${parts.ext}", fontSize = nameSize, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(
                "${FileRules.humanSize(file.sizeBytes)} - ${Fmt.date(Fmt.fromMillis(file.addedAt).toLocalDate())}",
                fontSize = roleSp(11.5f, TextRole.SECONDARY), color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            // N62 (Q3): say so while the extension disagrees with what the file really is
            val really = com.krishna.infodeck.core.FileNameRules.mismatch(file.displayName, file.mime)
            if (com.krishna.infodeck.core.FileNameRules.realType(file.mime) == null) MimeNotice.once(file.id, file.mime)
            if (really != null) Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, Modifier.size(12.dp), tint = ExtAmberText)
                Spacer(Modifier.width(3.dp))
                Text("Really a $really", fontSize = roleSp(11.5f, TextRole.SECONDARY),
                    fontWeight = FontWeight.Bold, color = ExtAmberText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(onClick = onOpen) { Text("Open", fontWeight = FontWeight.Bold) }
        // B4 - a visible Share pill per file; green because green means share everywhere now
        Row(
            Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFDFF5E8))
                .clickable { onShare() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Share, "Share this file", Modifier.size(13.dp), tint = Color(0xFF0B6B36))
            Spacer(Modifier.width(4.dp))
            Text("Share", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0B6B36))
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "File actions") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) })
                DropdownMenuItem(
                    text = { Text("Remove file...", color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove() },
                    leadingIcon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

/** Manage tags: counts, rename, merge, delete behind the danger layer (S9). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val tags by vm.tags.collectAsState()
    val usage by vm.tagUsage.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var renaming by remember { mutableStateOf<TagEntity?>(null) }
    var merging by remember { mutableStateOf<TagEntity?>(null) }
    var danger by remember { mutableStateOf<TagEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<TagEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tags", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("${tags.size} tags", fontSize = 11.5.sp, color = Color.White.copy(alpha = .85f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "New tag") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White
                )
            )
        }
    ) { pad ->
        if (tags.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tags yet - add one from any card.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(tags.sortedWith(compareByDescending<TagEntity> { usage[it.id] ?: 0 }.thenBy { it.name.lowercase() }),
                    key = { it.id }) { t ->
                    var menu by remember { mutableStateOf(false) }
                    val count = usage[t.id] ?: 0
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(TagRules.display(t.name), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (count == 0) "0 cards - unused" else "$count cards",
                                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Pill("$count", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Tag actions") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; renaming = t })
                                    DropdownMenuItem(text = { Text("Merge into another tag...") }, onClick = { menu = false; merging = t })
                                    DropdownMenuItem(
                                        text = { Text("Danger zone...", color = MaterialTheme.colorScheme.error) },
                                        onClick = { menu = false; danger = t }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val ren = renaming
    if (ren != null) NameDialog("Rename tag", ren.name, { renaming = null }) { name ->
        vm.renameTag(ren.id, name) { ok ->
            renaming = null
            if (!ok) scope.launch { snackbar.showSnackbar("That name is taken") }
        }
    }

    if (creating) NameDialog("New tag", "", { creating = false }) { name ->
        vm.addTag(name) { id ->
            creating = false
            if (id <= 0) scope.launch { snackbar.showSnackbar("That name is taken") }
        }
    }

    val mrg = merging
    if (mrg != null) {
        var target by remember { mutableLongStateOf(-1L) }
        AlertDialog(
            onDismissRequest = { merging = null },
            title = { Text("Merge ${TagRules.display(mrg.name)} into") },
            text = {
                Column {
                    tags.filter { it.id != mrg.id }.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clickable { target = t.id }.heightIn(min = 42.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(selected = target == t.id, onClick = { target = t.id })
                            Text(TagRules.display(t.name), fontSize = 13.5.sp)
                        }
                    }
                    Text(
                        "Every card keeps its cards - only the label changes.",
                        fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = target > 0, onClick = {
                    vm.mergeTag(mrg.id, target); merging = null
                    scope.launch { snackbar.showSnackbar("Tags merged") }
                }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { merging = null }) { Text("Cancel") } }
        )
    }

    val dz = danger
    if (dz != null) DangerSheet(
        title = TagRules.display(dz.name),
        detail = "${usage[dz.id] ?: 0} cards carry this tag. Deleting it removes the label only - no card is deleted.",
        deleteLabel = "Delete tag",
        onDismiss = { danger = null },
        onDelete = { danger = null; confirmDelete = dz }
    )

    val cd = confirmDelete
    if (cd != null) AlertDialog(
        onDismissRequest = { confirmDelete = null },
        title = { Text("Delete ${TagRules.display(cd.name)}?") },
        text = { Text("It is removed from ${usage[cd.id] ?: 0} cards. The cards themselves stay exactly as they are.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = null; vm.deleteTag(cd.id)
                scope.launch { snackbar.showSnackbar("Tag deleted") }
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
    )
}


@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it.take(TagRules.MAX_NAME) },
                singleLine = true, label = { Text("Name") }
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onSave(value) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** N62 companion 2: an unknown stored MIME is logged once per file, never on every recomposition. */
internal object MimeNotice {
    private val seen = java.util.Collections.synchronizedSet(HashSet<Long>())
    fun once(fileId: Long, mime: String) {
        if (seen.add(fileId)) com.krishna.infodeck.core.Logger.e("FileRow.mime", "unknown MIME '$mime' on file id=$fileId - no real-type line shown")
    }
}
