package com.krishna.infodeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.CategoryRow
import com.krishna.infodeck.core.ContentFilter
import com.krishna.infodeck.core.ExpiryFilter
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.GroupBy
import com.krishna.infodeck.core.GroupOrder
import com.krishna.infodeck.core.ReminderFilter
import com.krishna.infodeck.core.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        windowInsets = zeroSheetInsets(),          // single source of bottom clearance
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            content()
            SheetBottomSpace()                      // max(25 % height, nav bar + 24 dp)
        }
    }
}

@Composable
private fun SheetTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun OptionRow(
    title: String, subtitle: String? = null, selected: Boolean, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.heightIn(min = 46.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SegRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(19.dp))
    ) {
        options.forEachIndexed { i, label ->
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        if (i == selectedIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(19.dp)
                    )
                    .clickable { onSelect(i) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (i == selectedIndex) {
                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                    color = if (i == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------- Group by (D2 S3) ----------------
@Composable
fun GroupBySheet(
    current: GroupBy, order: GroupOrder, singleOpen: Boolean,
    onDismiss: () -> Unit, onApply: (GroupBy, GroupOrder, Boolean) -> Unit
,
    density: com.krishna.infodeck.core.Density = com.krishna.infodeck.core.Density.DEFAULT,
    tapExpand: Boolean = false,
    onDensity: (com.krishna.infodeck.core.Density) -> Unit = {},
    onTapExpand: (Boolean) -> Unit = {}) {
    var g by remember { mutableStateOf(current) }
    var o by remember { mutableStateOf(order) }
    var s by remember { mutableStateOf(singleOpen) }

    InfoSheet(onDismiss = onDismiss) {
        SheetTitle("View")
        SectionLabel("Density")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            com.krishna.infodeck.core.Density.values().forEach { d ->
                val on = density == d
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                        .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onDensity(d) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(d.label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(d.rowDp.toInt().toString() + " dp", fontSize = 9.sp, color = Hint)
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tap to expand", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Open one card in place with its details", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Switch(checked = tapExpand, onCheckedChange = onTapExpand)
        }
        SectionLabel("Group by")

        OptionRow("Category", "One section per category", g == GroupBy.CATEGORY) { g = GroupBy.CATEGORY }
        OptionRow("Expiry month", "By key date - Aug 2026, Sep 2026 ...", g == GroupBy.EXPIRY_MONTH) { g = GroupBy.EXPIRY_MONTH }
        OptionRow("Expiry window", "Overdue - This week - This month - 3 months - Later", g == GroupBy.EXPIRY_WINDOW) { g = GroupBy.EXPIRY_WINDOW }
        OptionRow("Next reminder", "Overdue - Today - This week - This month - Later", g == GroupBy.NEXT_REMINDER) { g = GroupBy.NEXT_REMINDER }
        OptionRow("None", "One flat list, no sections", g == GroupBy.NONE) { g = GroupBy.NONE }

        SectionLabel("Order sections by")
        SegRow(GroupOrder.values().map { it.label }, GroupOrder.values().indexOf(o)) { o = GroupOrder.values()[it] }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("One section open at a time", fontSize = 14.sp)
                Text("Opening a section closes the previous one", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = s, onCheckedChange = { s = it })
        }

        Spacer(Modifier.height(14.dp))
        EditorSheetButtons(onCancel = onDismiss, onSave = { onApply(g, o, s) }, saveText = "Apply")
    }
}

// ---------------- Sort by (D2 S4) ----------------
@Composable
fun SortBySheet(
    current: SortBy, asc: Boolean, groupedByCategory: Boolean,
    onDismiss: () -> Unit, onApply: (SortBy, Boolean) -> Unit
) {
    var by by remember { mutableStateOf(current) }
    var a by remember { mutableStateOf(asc) }

    InfoSheet(onDismiss = onDismiss) {
        SheetTitle("Sort cards by")
        OptionRow("Name", "Card label, ignoring case", by == SortBy.NAME) { by = SortBy.NAME }
        OptionRow("Expiry date", "The card's key date", by == SortBy.EXPIRY) { by = SortBy.EXPIRY }
        OptionRow("Next reminder", "When it rings next", by == SortBy.REMINDER) { by = SortBy.REMINDER }
        OptionRow("Recently updated", null, by == SortBy.UPDATED) { by = SortBy.UPDATED }
        OptionRow("Recently added", null, by == SortBy.ADDED) { by = SortBy.ADDED }
        if (groupedByCategory) OptionRow("Manual", "Your own order inside a category", by == SortBy.MANUAL) { by = SortBy.MANUAL }

        SectionLabel("Direction")
        val labels = by.directionLabels()
        SegRow(listOf(labels.first, labels.second), if (a) 0 else 1) { a = it == 0 }

        Spacer(Modifier.height(14.dp))
        EditorSheetButtons(onCancel = onDismiss, onSave = { onApply(by, a) }, saveText = "Apply")
    }
}

// ---------------- Filters (D2 S5) ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    current: FilterState, categories: List<CategoryRow>,
    countFor: (FilterState) -> Int,
    onDismiss: () -> Unit, onApply: (FilterState) -> Unit
) {
    var f by remember { mutableStateOf(current) }
    var confirmClear by remember { mutableStateOf(false) }
    val undo = remember { com.krishna.infodeck.core.UndoStack(current) }

    if (confirmClear) ConfirmDialog(
        title = "Clear ${f.activeGroups} filters?",
        body = "Every filter you have set is removed and the full list comes back. Nothing is deleted.",
        keepText = "Keep them",
        goText = "Clear all",
        danger = false,
        onKeep = { confirmClear = false },
        onGo = { confirmClear = false; undo.push(f); f = FilterState() }
    )

    InfoSheet(onDismiss = onDismiss) {
        SheetTitle("Filters") {
            TextButton(onClick = { confirmClear = true }, enabled = f.isActive) { Text("Clear all") }
        }

        SectionLabel("Categories")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { c ->
                val on = f.categoryIds.contains(c.id)
                FChip(c.name, on, color = categoryColor(c.colorIndex)) {
                    f = f.copy(
                        categoryIds = if (on) f.categoryIds - c.id else f.categoryIds + c.id
                    )
                }
            }
        }

        SectionLabel("Expiry window")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExpiryFilter.values().filter { it != ExpiryFilter.CUSTOM }.forEach { e ->
                FChip(e.label, f.expiry == e) { f = f.copy(expiry = e) }
            }
        }

        SectionLabel("Reminder")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReminderFilter.values().forEach { r -> FChip(r.label, f.reminder == r) { f = f.copy(reminder = r) } }
        }

        SectionLabel("Content")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ContentFilter.values().forEach { c -> FChip(c.label, f.content == c) { f = f.copy(content = c) } }
        }

        Spacer(Modifier.height(16.dp))
        val n = countFor(f)
        EditorSheetButtons(
            onCancel = onDismiss,
            onSave = { onApply(f) },
            saveText = if (n == 1) "Show 1" else "Show $n",
            onUndo = { undo.undo()?.let { f = it } },
            undoEnabled = undo.canUndo
        )
    }
}

@Composable
private fun FChip(
    text: String, on: Boolean,
    color: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .padding(bottom = 6.dp)
            .height(30.dp)
            .background(
                if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(15.dp)
            )
            .border(
                1.5.dp,
                if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(15.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (color != null) { ColorDot(color, 9); Spacer(Modifier.width(5.dp)) }
        Text(
            text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- Move to category (D2 S7) ----------------
@Composable
fun MoveToCategorySheet(
    count: Int,
    categories: List<CategoryRow>,
    currentCategoryId: Long?,
    onCreate: (String, Int, (Long) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onMove: (Long) -> Unit
) {
    var target by remember { mutableStateOf(categories.firstOrNull { it.id != currentCategoryId }?.id ?: -1L) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newColor by remember { mutableIntStateOf(0) }

    InfoSheet(onDismiss = onDismiss) {
        SheetTitle(if (count == 1) "Move 1 card to" else "Move $count cards to")

        categories.forEach { c ->
            Row(
                Modifier.fillMaxWidth().clickable { target = c.id }.heightIn(min = 46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = target == c.id, onClick = { target = c.id })
                ColorDot(categoryColor(c.colorIndex), 12)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, fontSize = 14.sp)
                    if (c.id == currentCategoryId) Text(
                        "Current category", fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (!creating) {
            Row(
                Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("New category...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newName, onValueChange = { newName = it.take(40) },
                label = { Text("Category name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ColourPicker(newColor) { newColor = it }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) onCreate(newName, newColor) { id ->
                        if (id > 0) { target = id; creating = false; newName = "" }
                    }
                },
                enabled = newName.isNotBlank()
            ) { Text("Create and select") }
        }

        Spacer(Modifier.height(12.dp))
        EditorSheetButtons(
            onCancel = onDismiss,
            onSave = { if (target > 0) onMove(target) },
            saveEnabled = target > 0 && target != currentCategoryId,
            saveText = "Move"
        )
    }
}

@Composable
fun ColourPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        CategoryColors.forEachIndexed { i, c ->
            Box(
                Modifier
                    .size(30.dp)
                    .background(c, RoundedCornerShape(15.dp))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                if (i == selected) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}
