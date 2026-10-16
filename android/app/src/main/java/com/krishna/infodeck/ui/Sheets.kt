package com.krishna.infodeck.ui

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.material.icons.filled.DriveFileMove

import androidx.compose.material.icons.filled.FilterList

import androidx.compose.material.icons.filled.Sort

import androidx.compose.material.icons.filled.ViewAgenda

import com.krishna.infodeck.core.Hue

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

/**
 * 2.0 sheet title (design 1.15.0.D1 frames 05–12): a role tile, the title in the theme's
 * heading face, and an optional trailing action.
 */
@Composable
internal fun SheetTitle(
    text: String,
    role: Hue = Hue.INDIGO,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val p = deck
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            HueTile(icon, role, size = 34)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text, fontFamily = p.heading, fontSize = 18.sp, modifier = Modifier.weight(1f),
            fontWeight = if (p.heading == FredokaFamily) FontWeight.SemiBold else FontWeight.ExtraBold
        )
        trailing?.invoke()
    }
}

/** A selectable row: tinted with a check when chosen (replaces the 1.x radio rows). */
@Composable
internal fun OptionRow(
    title: String, subtitle: String? = null, selected: Boolean,
    role: Hue = Hue.INDIGO, onClick: () -> Unit
) {
    val p = deck
    val s = p.hue(role)
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) s.tint else p.surface2)
            .border(if (selected) 1.5.dp else 0.dp, if (selected) s.glyph else Color.Transparent, shape)
            .clickable { onClick() }
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) (if (p.dark) s.glyph else s.onTint) else p.ink)
            if (subtitle != null) Text(subtitle, fontSize = 11.5.sp, color = p.ink2)
        }
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(11.dp))
                .background(if (selected) s.glyph else Color.Transparent)
                .border(if (selected) 0.dp else 1.5.dp, if (selected) Color.Transparent else p.outline, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Filled.Check, "Selected", Modifier.size(15.dp),
                tint = if (p.dark) Color(0xFF0A0E1F) else Color.White)
        }
    }
}

/** Segmented control: a soft track with the chosen segment lifted out of it. */
@Composable
internal fun SegRow(options: List<String>, selectedIndex: Int, role: Hue = Hue.INDIGO, onSelect: (Int) -> Unit) {
    val p = deck
    val s = p.hue(role)
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        when {
                            !on -> Color.Transparent
                            p.dark -> s.tint
                            else -> p.surface
                        }
                    )
                    .then(if (on && p.dark) Modifier.border(1.dp, s.glyph, RoundedCornerShape(11.dp)) else Modifier)
                    .clickable { onSelect(i) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label, fontSize = 12.5.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (on) (if (p.dark) s.glyph else s.onTint) else p.ink2,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A switch row inside a sheet. */
@Composable
internal fun SheetSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 11.5.sp, color = deck.ink2)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------- Group by (D2 S3) ----------------
@OptIn(ExperimentalLayoutApi::class)
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
        val p = deck
        val pri = p.hue(Hue.INDIGO)
        SheetTitle("View", Hue.INDIGO, Icons.Filled.ViewAgenda)
        SectionLabel("Group by")
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                GroupBy.CATEGORY to "Category", GroupBy.EXPIRY_MONTH to "Expiry month",
                GroupBy.EXPIRY_WINDOW to "Expiry window", GroupBy.NEXT_REMINDER to "Next reminder",
                GroupBy.NONE to "None"
            ).forEach { (k, label) -> FChip(label, g == k, role = Hue.INDIGO) { g = k } }
        }
        Text(
            when (g) {
                GroupBy.CATEGORY -> "One section per category"
                GroupBy.EXPIRY_MONTH -> "By key date - Aug 2026, Sep 2026 ..."
                GroupBy.EXPIRY_WINDOW -> "Overdue - This week - This month - 3 months - Later"
                GroupBy.NEXT_REMINDER -> "Overdue - Today - This week - This month - Later"
                else -> "One flat list, no sections"
            },
            fontSize = 11.5.sp, color = p.ink2, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
        )

        SectionLabel("Order sections by")
        SegRow(GroupOrder.values().map { it.label }, GroupOrder.values().indexOf(o)) { o = GroupOrder.values()[it] }

        SectionLabel("Density")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.surface2).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            com.krishna.infodeck.core.Density.values().forEach { d ->
                val on = density == d
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                        .background(if (!on) Color.Transparent else if (p.dark) pri.tint else p.surface)
                        .then(if (on && p.dark) Modifier.border(1.dp, pri.glyph, RoundedCornerShape(11.dp)) else Modifier)
                        .clickable { onDensity(d) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(d.label, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1,
                        color = if (on) (if (p.dark) pri.glyph else pri.onTint) else p.ink2)
                    Text(d.rowDp.toInt().toString() + " dp", fontSize = 9.sp, color = p.hint)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        SheetSwitch("Tap to expand", "Open one card in place with its details", tapExpand, onTapExpand)
        SheetSwitch("One section open at a time", "Opening a section closes the previous one", s) { s = it }

        Spacer(Modifier.height(12.dp))
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
        SheetTitle("Sort cards by", Hue.INDIGO, Icons.Filled.Sort)
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
        SheetTitle("Filters", Hue.SKY, Icons.Filled.FilterList) {
            TextButton(onClick = { confirmClear = true }, enabled = f.isActive) { Text("Clear all") }
        }

        SectionLabel("Categories")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { c ->
                val on = f.categoryIds.contains(c.id)
                FChip(c.name, on, category = c.colorIndex) {
                    f = f.copy(
                        categoryIds = if (on) f.categoryIds - c.id else f.categoryIds + c.id
                    )
                }
            }
        }

        SectionLabel("Expiry window")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExpiryFilter.values().filter { it != ExpiryFilter.CUSTOM }.forEach { e ->
                FChip(e.label, f.expiry == e, role = if (e == ExpiryFilter.OVERDUE) Hue.ROSE else Hue.AMBER) { f = f.copy(expiry = e) }
            }
        }

        SectionLabel("Reminder")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReminderFilter.values().forEach { r -> FChip(r.label, f.reminder == r, role = Hue.AMBER) { f = f.copy(reminder = r) } }
        }

        SectionLabel("Content")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ContentFilter.values().forEach { c -> FChip(c.label, f.content == c, role = Hue.SKY) { f = f.copy(content = c) } }
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

/** Filter / choice chip: the chosen chip fills with its role colour (or its category's gradient). */
@Composable
internal fun FChip(
    text: String, on: Boolean,
    role: Hue = Hue.INDIGO,
    category: Int? = null,
    onClick: () -> Unit
) {
    val p = deck
    val h = category?.let { com.krishna.infodeck.core.Hues.forCategory(it) } ?: role
    val s = p.hue(h)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .padding(bottom = 6.dp)
            .height(32.dp)
            .clip(shape)
            .background(
                when {
                    !on -> androidx.compose.ui.graphics.SolidColor(p.surface)
                    p.dark -> androidx.compose.ui.graphics.SolidColor(s.tint)
                    else -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(s.glyph, s.glyph2))
                }
            )
            .border(if (on && !p.dark) 0.dp else 1.dp, if (on) s.glyph else p.line, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category != null && !on) {
            Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(s.glyph))
            Spacer(Modifier.width(6.dp))
        }
        if (on) {
            Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = if (p.dark) s.glyph else Color.White)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text, fontSize = 12.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1,
            color = when {
                on && p.dark -> s.glyph
                on -> Color.White
                else -> p.ink2
            }
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
        SheetTitle(if (count == 1) "Move 1 card to" else "Move $count cards to", Hue.SKY, Icons.Filled.DriveFileMove)

        categories.forEach { c ->
            val h = com.krishna.infodeck.core.Hues.forCategory(c.colorIndex)
            val on = target == c.id
            val p = deck
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.padding(bottom = 6.dp).fillMaxWidth().clip(shape)
                    .background(if (on) p.hue(h).tint else p.surface2)
                    .border(if (on) 1.5.dp else 0.dp, if (on) p.hue(h).glyph else Color.Transparent, shape)
                    .clickable { target = c.id }.heightIn(min = 50.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HueTile(glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(c.name)), h, size = 30)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                    if (c.id == currentCategoryId) Text("Current category", fontSize = 11.5.sp, color = p.ink2)
                }
                if (on) Icon(Icons.Filled.Check, "Selected", Modifier.size(20.dp), tint = p.hue(h).glyph)
            }
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

/** The 8 category colours as gradient swatches in the active theme. */
@Composable
fun ColourPicker(selected: Int, onSelect: (Int) -> Unit) {
    val p = deck
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until 8).forEach { i ->
            val s = p.hue(com.krishna.infodeck.core.Hues.forCategory(i))
            val on = i == selected
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(s.glyph, s.glyph2)))
                    .border(if (on) 3.dp else 0.dp, if (on) p.ink else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                if (on) Icon(Icons.Filled.Check, "Selected", Modifier.size(18.dp), tint = Color.White)
            }
        }
    }
}
