package com.krishna.infodeck.ui

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
@OptIn(ExperimentalMaterial3Api::class)
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
        Text(
            if (isEditing) "Edit scheduler" else "Add scheduler",
            fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Based on
        Box {
            OutlinedTextField(
                value = baseName, onValueChange = {}, readOnly = true,
                label = { Text("Based on") },
                trailingIcon = { Icon(Icons.Filled.ExpandMore, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Box(Modifier.matchParentSize().clickable { dateMenu = true })
            DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                dates.forEach { (id, pair) ->
                    DropdownMenuItem(
                        text = { Text("${pair.first}  ${Fmt.date(LocalDate.ofEpochDay(pair.second))}") },
                        onClick = { dateLocalId = id; dateMenu = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = RingCalculator.unitLabel(unit), onValueChange = {}, readOnly = true,
                    label = { Text("When to ring") },
                    trailingIcon = { Icon(Icons.Filled.ExpandMore, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { unitMenu = true })
                DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                    RingUnit.values().forEach { u ->
                        DropdownMenuItem(
                            text = { Text(RingCalculator.unitLabel(u)) },
                            onClick = { unit = u; amount = RingCalculator.clamp(u, amount); unitMenu = false }
                        )
                    }
                }
            }
            if (unit != RingUnit.SAME_DAY) {
                Spacer(Modifier.width(10.dp))
                Row(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { amount = RingCalculator.clamp(unit, amount - 1) }) {
                        Icon(Icons.Filled.Remove, "Less", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("$amount", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { amount = RingCalculator.clamp(unit, amount + 1) }) {
                        Icon(Icons.Filled.Add, "More", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Text(
            RingCalculator.rangeHint(unit),
            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 5.dp, bottom = 10.dp)
        )

        if (unit != RingUnit.SAME_DAY) {
            Row(
                Modifier.fillMaxWidth().height(38.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(19.dp))
            ) {
                listOf(RingDirection.BEFORE to "Before", RingDirection.AFTER to "After").forEach { (d, label) ->
                    Row(
                        Modifier.weight(1f).fillMaxWidth()
                            .background(
                                if (direction == d) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(19.dp)
                            )
                            .clickable { direction = d },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (direction == d) {
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (direction == d) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ring at", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier
                    .height(44.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showTime = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Schedule, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(Fmt.time(ringMinute), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    if (past) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Alarm, null, Modifier.size(20.dp),
                tint = if (past) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (preview == null) "Pick a date first"
                else if (past) "This moment has passed - it will not ring"
                else "Rings ${Fmt.dateTime(preview)} - ${Fmt.relativeShort(preview.toLocalDate(), LocalDate.now())}",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (past) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
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
