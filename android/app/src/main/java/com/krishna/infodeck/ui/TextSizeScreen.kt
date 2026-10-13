package com.krishna.infodeck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.ElementChoice
import com.krishna.infodeck.core.ElementGroup
import com.krishna.infodeck.core.FriendlySize
import com.krishna.infodeck.core.ScaleRules
import com.krishna.infodeck.core.SizeStep
import com.krishna.infodeck.core.TextRole
import com.krishna.infodeck.core.UiScale
import com.krishna.infodeck.core.UndoStack
import kotlinx.coroutines.launch

/**
 * Friendly text size (design 1.2.D1 S4/S5): names not percentages, two big A buttons,
 * live preview, Easy-read mode — and nothing is written until Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeScreen(vm: AppViewModel, onBack: () -> Unit, onFineTune: () -> Unit) {
    val saved by vm.settings.collectAsState()
    val opened = remember { saved.ui }
    var draft by remember { mutableStateOf(saved.ui) }
    val stack = remember { UndoStack(opened) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun change(next: UiScale) { stack.push(draft); draft = next }
    val dirty = draft != opened

    fun leave() { if (dirty) confirmDiscard = true else onBack() }
    BackHandler { leave() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Hero(
                title = "Text size",
                subtitle = if (dirty) "${stack.dirtyCount} unsaved changes - try it, then Save"
                else "Try it - nothing is saved until you tap Save",
                onBack = { leave() }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // ---- the name, and two big A buttons ----
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            FriendlySize.nameFor(draft.textScale),
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryDark
                        )
                        Text(
                            FriendlySize.blurbFor(draft.textScale),
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ABtn("A", 19) { change(draft.copy(textScale = FriendlySize.next(draft.textScale, false))) }
                            ABtn("A", 30) { change(draft.copy(textScale = FriendlySize.next(draft.textScale, true))) }
                        }
                        Row(
                            Modifier.padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FriendlySize.STEPS.forEach { step ->
                                val on = FriendlySize.stepFor(draft.textScale) == step
                                Box(
                                    Modifier
                                        .width(if (on) 34.dp else 22.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (on) MaterialTheme.colorScheme.primary else Color(0xFFDDE2EE))
                                )
                            }
                        }
                    }
                }

                // ---- five size cards ----
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SizeStep.SMALL, SizeStep.STANDARD, SizeStep.COMFORTABLE, SizeStep.LARGE, SizeStep.HUGE)
                        .forEach { step ->
                            val on = FriendlySize.stepFor(draft.textScale) == step
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { change(draft.copy(textScale = step.scale)) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Aa",
                                    fontSize = (11 + step.ordinal * 2.2f).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (on) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (step == SizeStep.COMFORTABLE) "Comfy" else step.label,
                                    fontSize = 10.sp, maxLines = 1,
                                    color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                }

                SectionLabel("Live preview")
                PreviewSlice(draft)

                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(Icons.Filled.Visibility, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Easy-read mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Large text, extra spacing, heavier strokes - one tap",
                                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = FriendlySize.isEasyRead(draft),
                            onCheckedChange = { on ->
                                change(if (on) FriendlySize.EASY_READ.copy(offsets = draft.offsets) else FriendlySize.DEFAULTS)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(9.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (dirty) scope.launch { snackbar.showSnackbar("Save this first, then fine-tune") } else onFineTune()
                    }
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(Icons.Filled.Restore, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("What to enlarge...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Titles, dates, chips - set each one separately",
                                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(">", color = Hint, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Box(Modifier.background(MaterialTheme.colorScheme.background).padding(14.dp)) {
                EditorSheetButtons(
                    onCancel = { leave() },
                    onSave = {
                        vm.saveUiScale(draft) {
                            scope.launch {
                                val r = snackbar.showSnackbar(
                                    "Saved - text ${FriendlySize.nameFor(draft.textScale)}", actionLabel = "UNDO"
                                )
                                if (r == SnackbarResult.ActionPerformed) vm.saveUiScale(opened) {}
                            }
                        }
                        stack.clear()
                        draft = draft
                    },
                    saveEnabled = dirty,
                    onUndo = { stack.undo()?.let { draft = it } },
                    undoEnabled = stack.canUndo
                )
            }
        }
    }

    if (confirmDiscard) ConfirmDialog(
        title = "Discard ${stack.dirtyCount} changes?",
        body = "Text goes back to ${FriendlySize.nameFor(opened.textScale)} - the size you had when you opened this screen. Your cards are not touched.",
        keepText = "Keep editing",
        goText = "Discard",
        danger = true,
        onKeep = { confirmDiscard = false },
        onGo = { confirmDiscard = false; draft = stack.discard(); onBack() }
    )
}

/** S5 — four groups, three plain choices each, and a confirmed reset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FineTuneScreen(vm: AppViewModel, onBack: () -> Unit) {
    val saved by vm.settings.collectAsState()
    val opened = remember { saved.ui }
    var draft by remember { mutableStateOf(saved.ui) }
    val stack = remember { UndoStack(opened) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun change(next: UiScale) { stack.push(draft); draft = next }
    val dirty = draft != opened
    fun leave() { if (dirty) confirmDiscard = true else onBack() }
    BackHandler { leave() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Hero(
                title = "What to enlarge",
                subtitle = "on top of ${FriendlySize.nameFor(draft.textScale)}",
                onBack = { leave() }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                Spacer(Modifier.height(12.dp))
                PreviewSlice(draft)

                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        ElementGroup.values().forEachIndexed { i, group ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            Text(group.label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(7.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFEFF2F8))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ElementChoice.values().forEach { choice ->
                                    val on = FriendlySize.groupChoice(draft.offsets, group) == choice
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                                            .clickable {
                                                change(draft.copy(offsets = FriendlySize.applyGroup(draft.offsets, group, choice)))
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            choice.label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable { confirmReset = true }
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(Icons.Filled.Restore, Amber, MaterialTheme.colorScheme.secondaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Put everything back to normal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Asks you first - nothing resets on a stray tap",
                                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Box(Modifier.background(MaterialTheme.colorScheme.background).padding(14.dp)) {
                EditorSheetButtons(
                    onCancel = { leave() },
                    onSave = {
                        vm.saveUiScale(draft) {
                            scope.launch {
                                val r = snackbar.showSnackbar("Saved", actionLabel = "UNDO")
                                if (r == SnackbarResult.ActionPerformed) vm.saveUiScale(opened) {}
                            }
                        }
                        stack.clear()
                    },
                    saveEnabled = dirty,
                    onUndo = { stack.undo()?.let { draft = it } },
                    undoEnabled = stack.canUndo
                )
            }
        }
    }

    if (confirmReset) ConfirmDialog(
        title = "Put text back to normal?",
        body = "Size returns to Standard (100 %), all four choices return to Normal, and spacing returns to Comfortable. Your cards are not touched.",
        keepText = "Keep my settings",
        goText = "Put back",
        danger = false,
        onKeep = { confirmReset = false },
        onGo = { confirmReset = false; change(FriendlySize.DEFAULTS) }
    )

    if (confirmDiscard) ConfirmDialog(
        title = "Discard ${stack.dirtyCount} changes?",
        body = "Everything goes back to how it was when you opened this screen.",
        keepText = "Keep editing",
        goText = "Discard",
        danger = true,
        onKeep = { confirmDiscard = false },
        onGo = { confirmDiscard = false; draft = stack.discard(); onBack() }
    )
}

@Composable
private fun ABtn(text: String, size: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
}

/** A real slice of Home, drawn at the DRAFT size rather than the saved one. */
@Composable
private fun PreviewSlice(draft: UiScale) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "PREVIEW", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                color = Hint, modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Car Insurance - HDFC Ergo",
                        fontSize = draft.sp(13.5f, TextRole.TITLE).sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Policy Expiry 11-Oct-2026",
                        fontSize = draft.sp(11.5f, TextRole.SECONDARY).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Insurance", fontSize = draft.sp(10.5f, TextRole.CHIP).sp,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Rings 11-Sep", fontSize = draft.sp(10.5f, TextRole.CHIP).sp,
                                fontWeight = FontWeight.Bold, color = Amber
                            )
                        }
                    }
                }
                CountdownRing(54L)
            }
            Divider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text("A settings row", fontSize = draft.sp(14f, TextRole.SETTINGS).sp, fontWeight = FontWeight.SemiBold)
            Text(
                "and its explaining line", fontSize = draft.sp(11.5f, TextRole.SECONDARY).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The one dialog every reset and every discard goes through. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    keepText: String,
    goText: String,
    danger: Boolean,
    onKeep: () -> Unit,
    onGo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeep,
        icon = {
            IconTile(
                if (danger) Icons.Filled.ArrowBack else Icons.Filled.Restore,
                if (danger) MaterialTheme.colorScheme.error else Amber,
                if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                size = 46
            )
        },
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = { Text(body, fontSize = 13.5.sp, textAlign = TextAlign.Start) },
        confirmButton = {
            TextButton(onClick = onGo) {
                Text(goText, fontWeight = FontWeight.Bold,
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onKeep) { Text(keepText) } }
    )
}

/** Gradient hero used as the top bar on pushed screens. */
@Composable
fun Hero(title: String, subtitle: String?, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(BrandGradient)
            .padding(start = 8.dp, end = 12.dp, top = 34.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.width(4.dp))
            } else Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                if (subtitle != null) Text(
                    subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.92f), maxLines = 2
                )
            }
            actions()
        }
    }
}
