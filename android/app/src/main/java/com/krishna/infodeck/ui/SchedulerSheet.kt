package com.krishna.infodeck.ui

import androidx.compose.foundation.layout.heightIn

import androidx.compose.ui.draw.clip

import androidx.compose.material.icons.filled.NotificationsActive

import androidx.compose.material.icons.filled.Warning

import androidx.compose.material.icons.filled.Star

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.RingCalculator
import com.krishna.infodeck.data.RingDirection
import com.krishna.infodeck.data.RingUnit
import java.time.LocalDate
import java.time.LocalDateTime

data class SchedulerDraft(
    val dateLocalId: Long,
    val unit: RingUnit,
    val amount: Int,
    val direction: RingDirection,
    val ringMinute: Int
)

/** D1 S5 — Based on / When to ring / Before-After / Ring at / live preview. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SchedulerSheet(
    dates: List<Pair<Long, Pair<String, Long>>>,   // localId -> (name, epochDay)
    initial: SchedulerDraft,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (SchedulerDraft) -> Unit
) {
    var dateLocalId by remember { mutableLongStateOf(initial.dateLocalId) }
    var unit by remember { mutableStateOf(initial.unit) }
    var amount by remember { mutableIntStateOf(RingCalculator.clamp(initial.unit, initial.amount)) }
    var direction by remember { mutableStateOf(initial.direction) }
    var ringMinute by remember { mutableIntStateOf(initial.ringMinute) }
    var dateMenu by remember { mutableStateOf(false) }
    var unitMenu by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val chosen = dates.firstOrNull { it.first == dateLocalId } ?: dates.firstOrNull()
    val baseName = chosen?.second?.first ?: "(no date)"
    val baseDate = chosen?.second?.second?.let { LocalDate.ofEpochDay(it) }
    val preview: LocalDateTime? = baseDate?.let {
        RingCalculator.ringDateTime(it, unit, amount, direction, ringMinute)
    }
    val past = preview != null && preview.isBefore(LocalDateTime.now())

    InfoSheet(onDismiss = onDismiss) {
        val p = deck
        val am = p.hue(com.krishna.infodeck.core.Hue.AMBER)
        val amInk = if (p.dark) am.glyph else am.onTint
        SheetTitle(if (isEditing) "Edit reminder" else "Add reminder", com.krishna.infodeck.core.Hue.AMBER, Icons.Filled.Alarm)

        // Based on — 2.0 themed field (design 1.15.0.D1 frame 09)
        SectionLabel("Based on")
        Box {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(14.dp)).background(p.surface2)
                    .clickable { dateMenu = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Star, null, Modifier.size(20.dp), tint = am.glyph)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(baseName, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (baseDate != null) Text(Fmt.date(baseDate), fontSize = 11.5.sp, color = p.ink2)
                }
                Icon(Icons.Filled.ExpandMore, "Choose the date", tint = p.ink2)
            }
            DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                dates.forEach { (id, pair) ->
                    DropdownMenuItem(
                        text = { Text("${pair.first}  ${Fmt.date(LocalDate.ofEpochDay(pair.second))}") },
                        onClick = { dateLocalId = id; dateMenu = false }
                    )
                }
            }
        }

        SectionLabel("When to ring")
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RingUnit.values().forEach { u ->
                FChip(RingCalculator.unitLabel(u), unit == u, role = com.krishna.infodeck.core.Hue.AMBER) {
                    unit = u; amount = RingCalculator.clamp(u, amount)
                }
            }
        }
        if (unit != RingUnit.SAME_DAY) {
            Row(
                Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(16.dp)).background(am.tint).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(50.dp).clip(RoundedCornerShape(13.dp)).background(p.surface)
                        .clickable { amount = RingCalculator.clamp(unit, amount - 1) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Remove, "Less", tint = amInk) }
                Text("$amount", Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = amInk,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box(
                    Modifier.size(50.dp).clip(RoundedCornerShape(13.dp)).background(p.surface)
                        .clickable { amount = RingCalculator.clamp(unit, amount + 1) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, "More", tint = amInk) }
            }
        }
        Text(
            RingCalculator.rangeHint(unit),
            fontSize = 11.5.sp, color = p.ink2,
            modifier = Modifier.padding(start = 4.dp, top = 5.dp, bottom = 8.dp)
        )

        if (unit != RingUnit.SAME_DAY) {
            val opts = listOf(RingDirection.BEFORE to "Before", RingDirection.AFTER to "After")
            SegRow(opts.map { it.second }, opts.indexOfFirst { it.first == direction }.coerceAtLeast(0),
                com.krishna.infodeck.core.Hue.AMBER) { direction = opts[it].first }
        }

        SectionLabel("Ring at")
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(p.surface2)
                .clickable { showTime = true }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Schedule, null, Modifier.size(20.dp), tint = am.glyph)
            Spacer(Modifier.width(10.dp))
            Text(Fmt.time(ringMinute), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, "Change the time", tint = p.ink2)
        }

        Spacer(Modifier.height(12.dp))
        val rose = p.hue(com.krishna.infodeck.core.Hue.ROSE)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (past) rose.tint else am.tint)
                .border(1.dp, (if (past) rose.glyph else am.glyph).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (past) Icons.Filled.Warning else Icons.Filled.NotificationsActive, null, Modifier.size(20.dp),
                tint = if (past) rose.glyph else am.glyph
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (preview == null) "Pick a date first"
                else if (past) "This moment has passed - it will not ring"
                else "Rings ${Fmt.dateTime(preview)} - ${Fmt.relativeShort(preview.toLocalDate(), LocalDate.now())}",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (past) (if (p.dark) rose.glyph else rose.onTint) else amInk,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(14.dp))
        EditorSheetButtons(
            onCancel = onDismiss,
            onSave = { onSave(SchedulerDraft(dateLocalId, unit, amount, direction, ringMinute)) },
            saveEnabled = chosen != null,
            onDelete = if (isEditing) onDelete else null
        )
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = ringMinute / 60, initialMinute = ringMinute % 60, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = { ringMinute = state.hour * 60 + state.minute; showTime = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { Column { TimePicker(state = state) } }
        )
    }
}
