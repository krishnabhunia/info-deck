package com.krishna.infodeck.ui

import androidx.compose.material.icons.filled.Category

import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sell
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.krishna.infodeck.core.CardRow
import com.krishna.infodeck.core.CategoryRow
import com.krishna.infodeck.core.Fmt
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.home.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var editing by remember { mutableStateOf<CategoryRow?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryRow?>(null) }
    var dangerCat by remember { mutableStateOf<CategoryRow?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            DeckHeader(
                title = "Categories",
                role = com.krishna.infodeck.core.Hue.SKY,
                subtitle = "${state.categories.count { !it.builtIn }} categories · ${state.allRows.size} cards · drag to reorder",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                containerColor = hue(com.krishna.infodeck.core.Hue.SKY),
                contentColor = if (deck.dark) Color(0xFF0A0E1F) else Color.White
            ) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("New category", fontWeight = FontWeight.Bold)
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val ordered = state.categories.sortedWith(compareBy({ it.builtIn }, { it.sortOrder }, { it.name.lowercase() }))
            items(ordered, key = { it.id }) { c ->
                CategoryRowItem(
                    category = c,
                    cards = state.allRows.filter { it.categoryId == c.id },
                    today = today,
                    canMoveUp = !c.builtIn && ordered.indexOf(c) > 0,
                    canMoveDown = !c.builtIn && ordered.indexOf(c) < ordered.count { !it.builtIn } - 1,
                    onEdit = { editing = c },
                    onDelete = { dangerCat = c },
                    onMove = { up ->
                        val list = ordered.filter { !it.builtIn }.map { it.id }.toMutableList()
                        val i = list.indexOf(c.id)
                        val j = if (up) i - 1 else i + 1
                        if (i >= 0 && j >= 0 && j < list.size) {
                            list[i] = list[j].also { list[j] = list[i] }
                            vm.reorderCategories(list)
                        }
                    }
                )
            }
            item {
                Text(
                    "Use the arrows to reorder - that order is \"Manual\" in Group by.",
                    fontSize = 11.5.sp, color = Hint, modifier = Modifier.padding(6.dp)
                )
                Spacer(Modifier.height(70.dp))
            }
        }
    }

    if (creating) CategoryEditSheet(
        title = "New category",
        initialName = "", initialColor = state.categories.size % CategoryColors.size,
        onDismiss = { creating = false },
        onSave = { name, color ->
            vm.addCategory(name, color) { id ->
                creating = false
                scope.launch {
                    if (id > 0) snackbar.showSnackbar("Category created")
                    else snackbar.showSnackbar("A category with this name exists")
                }
            }
        }
    )

    val ed = editing
    if (ed != null) CategoryEditSheet(
        title = "Edit category",
        initialName = ed.name, initialColor = ed.colorIndex,
        onDismiss = { editing = null },
        onSave = { name, color ->
            vm.updateCategory(ed.id, name, color) { ok ->
                editing = null
                scope.launch { if (!ok) snackbar.showSnackbar("A category with this name exists") }
            }
        }
    )

    val dcat = dangerCat
    if (dcat != null) DangerSheet(
        title = dcat.name,
        detail = "${state.allRows.count { it.categoryId == dcat.id }} cards are in this category.",
        deleteLabel = "Delete category",
        onDismiss = { dangerCat = null },
        onDelete = { dangerCat = null; deleting = dcat }
    )

    val del = deleting
    if (del != null) DeleteCategoryDialog(
        category = del,
        cardCount = state.allRows.count { it.categoryId == del.id },
        others = state.categories.filter { it.id != del.id },
        onDismiss = { deleting = null },
        onConfirm = { moveTo ->
            deleting = null
            vm.deleteCategory(del.id, moveTo) {
                scope.launch {
                    snackbar.showSnackbar(
                        if (moveTo == null) "Category and its cards deleted" else "Category deleted, cards moved"
                    )
                }
            }
        }
    )
}

@Composable
private fun CategoryRowItem(
    category: CategoryRow,
    cards: List<CardRow>,
    today: LocalDate,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Boolean) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val overdue = cards.count { it.isOverdue(today) && it.hasReminder }
    val nextExpiry = cards.mapNotNull { it.keyDate }.filter { !it.isBefore(today) }.minOrNull()

    val catHue = com.krishna.infodeck.core.Hues.forCategory(category.colorIndex)
    Column(
        Modifier.fillMaxWidth().deckCard(deck, 16.dp, if (category.builtIn) null else hue(catHue))
            .then(if (category.builtIn) Modifier.background(deck.surface2) else Modifier)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!category.builtIn) {
                Column {
                    IconButton(onClick = { onMove(true) }, enabled = canMoveUp, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.ArrowUpward, "Move up", Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onMove(false) }, enabled = canMoveDown, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.ArrowDownward, "Move down", Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
            } else Spacer(Modifier.width(30.dp))

            if (category.builtIn) HueTile(Icons.Filled.Folder, com.krishna.infodeck.core.Hue.SLATE, size = 34, flat = true)
            else HueTile(glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(category.name)), catHue, size = 34)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append("${cards.size} cards")
                        if (overdue > 0) append(" - $overdue overdue")
                        else if (nextExpiry != null) append(" - next expiry ${Fmt.date(nextExpiry)}")
                        if (category.builtIn) append(" - built-in, always last")
                    },
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (!category.builtIn) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename / colour") }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(
                            text = { Text("Danger zone...", color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryEditSheet(
    title: String, initialName: String, initialColor: Int,
    onDismiss: () -> Unit, onSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor.coerceIn(0, 7)) }

    InfoSheet(onDismiss = onDismiss) {
        SheetTitle(title, com.krishna.infodeck.core.Hue.SKY, Icons.Filled.Category)
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40) },
            label = { Text("Category name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        SectionLabel("Colour")
        ColourPicker(color) { color = it }
        Spacer(Modifier.height(18.dp))
        EditorSheetButtons(
            onCancel = onDismiss,
            onSave = { onSave(name.trim(), color) },
            saveEnabled = name.isNotBlank()
        )
    }
}

@Composable
private fun DeleteCategoryDialog(
    category: CategoryRow, cardCount: Int, others: List<CategoryRow>,
    onDismiss: () -> Unit, onConfirm: (Long?) -> Unit
) {
    var moveMode by remember { mutableStateOf(true) }
    var target by remember { mutableLongStateOf(others.firstOrNull { it.builtIn }?.id ?: others.firstOrNull()?.id ?: -1L) }
    var typed by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"${category.name}\"?") },
        text = {
            Column {
                Text("$cardCount cards are in this category.", fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { moveMode = true }) {
                    RadioButton(selected = moveMode, onClick = { moveMode = true })
                    Text("Move the cards to another category", fontSize = 13.sp)
                }
                if (moveMode && cardCount > 0) {
                    Column(Modifier.padding(start = 40.dp)) {
                        others.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { target = c.id }) {
                                RadioButton(selected = target == c.id, onClick = { target = c.id })
                                ColorDot(categoryColor(c.colorIndex), 10)
                                Spacer(Modifier.width(6.dp))
                                Text(c.name, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { moveMode = false }) {
                    RadioButton(selected = !moveMode, onClick = { moveMode = false })
                    Text("Delete the cards too", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
                if (!moveMode && cardCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = typed, onValueChange = { typed = it },
                        label = { Text("Type DELETE to confirm") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val enabled = moveMode && (cardCount == 0 || target > 0) || (!moveMode && (cardCount == 0 || typed.trim() == "DELETE"))
            TextButton(
                enabled = enabled,
                onClick = { onConfirm(if (moveMode) target.takeIf { it > 0 } else null) }
            ) { Text("Delete category", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
