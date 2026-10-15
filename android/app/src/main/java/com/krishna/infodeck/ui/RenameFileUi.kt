package com.krishna.infodeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.krishna.infodeck.core.FileNameRules
import com.krishna.infodeck.core.FileNameRules.Decision
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.FileEntity
import com.krishna.infodeck.data.FileRenameRules
import com.krishna.infodeck.data.FileRules
import kotlinx.coroutines.delay

/* N62 · design 1.9.D1 — the amber family marks "the extension is being changed". */
internal val ExtAmber = Color(0xFFB45309)
internal val ExtAmberText = Color(0xFF8A5300)
private val ExtAmberLine = Color(0xFFEBC27A)
private val OkText = Color(0xFF0B6B36)

/**
 * Rename a file: Step 1 edits the NAME with the extension locked; "Change extension…"
 * opens Step 2 in the same sheet; a different extension then needs Step 3, a confirmation.
 * [onRename] receives the final display name and whether an extension change was confirmed;
 * it is called only for a real change — directly for a name-only change, otherwise only after
 * the confirmation. The repository re-validates and refuses an unconfirmed extension change.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RenameFileSheet(file: FileEntity, onDismiss: () -> Unit, onRename: (String, Boolean) -> Unit) {
    val original = remember(file.id) { FileNameRules.split(file.displayName) }
    var base by remember(file.id) {
        mutableStateOf(TextFieldValue(original.base, TextRange(0, original.base.length)))
    }
    var unlocked by remember(file.id) { mutableStateOf(false) }
    var newExt by remember(file.id) { mutableStateOf(original.ext) }
    var confirm by remember(file.id) { mutableStateOf<Decision.NeedsConfirm?>(null) }
    var flash by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(file.id) {
        try {
            focus.requestFocus()
            keyboard?.show()
        } catch (t: Throwable) {
            Logger.e("RenameFile", "could not focus the name box", t)
        }
    }
    LaunchedEffect(flash) { if (flash) { delay(700); flash = false } }

    val ext = if (unlocked) newExt else original.ext
    val decision = FileNameRules.decide(file.displayName, base.text, ext)
    val real = FileNameRules.realType(file.mime)
    val typed = if (unlocked) null else FileNameRules.typedExtension(base.text)
    val needsConfirm = decision is Decision.NeedsConfirm
    // Same check the repository runs, shown here so Save never leads to a refusal
    val problem = when (decision) {
        is Decision.Direct -> FileRenameRules.validate(file.displayName, decision.name, extensionConfirmed = true)
        is Decision.NeedsConfirm -> FileRenameRules.validate(file.displayName, decision.name, extensionConfirmed = true)
        Decision.Invalid -> null
    }

    fun setBase(text: String) { base = TextFieldValue(text, TextRange(text.length)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = zeroSheetInsets(),          // single source of bottom clearance
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text("Rename file", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            RenameFileHeader(file)
            Spacer(Modifier.height(14.dp))

            // ---------------- Step 1: the name, extension locked ----------------
            OutlinedTextField(
                value = base,
                onValueChange = { v ->
                    val limit = FileNameRules.maxBase(ext)      // B13: no 24-character tag cap
                    base = if (v.text.length <= limit) v else TextFieldValue(v.text.take(limit), TextRange(limit))
                },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = if (unlocked) null else {
                    { LockedExtChip(original.ext) { flash = true } }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            SavedAsLine(decision)
            if (problem != null) Text(
                problem, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 2.dp, top = 4.dp)
            )

            if (typed != null) {
                val stripped = FileNameRules.split(base.text).base
                if (FileNameRules.sameExt(typed, original.ext)) {
                    NoteBox(
                        amber = true,
                        text = "The name already ends in .$typed, so this saves as …$typed.${original.ext}.",
                        action = "Remove the extra .$typed"
                    ) { setBase(stripped) }
                } else {
                    val lockedText = if (original.hasExt) ".${original.ext}" else "no extension"
                    NoteBox(
                        amber = true,
                        text = "The name ends in .$typed, but the extension is locked at $lockedText.",
                        action = "Use .$typed as the extension…"
                    ) { setBase(stripped); newExt = FileNameRules.cleanExt(typed); unlocked = true }
                }
            }

            if (!unlocked) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (flash) AmberContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (original.hasExt) "Extension stays .${original.ext}" else "No extension",
                        fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { newExt = original.ext; unlocked = true }) {
                        Text(
                            if (original.hasExt) "Change extension…" else "Add extension…",
                            fontWeight = FontWeight.ExtraBold, color = ExtAmberText, fontSize = 13.sp
                        )
                    }
                }
            } else {
                // ---------------- Step 2: the extension, unlocked ----------------
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LockOpen, null, Modifier.size(14.dp), tint = ExtAmberText)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "EXTENSION · UNLOCKED", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = ExtAmberText, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { newExt = original.ext; unlocked = false }) {
                        Text(
                            if (original.hasExt) "Keep .${original.ext}" else "Keep no extension",
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
                        )
                    }
                }
                val badShape = newExt.isNotEmpty() && !FileNameRules.isExtShape(newExt)
                OutlinedTextField(
                    value = newExt,
                    onValueChange = { newExt = FileNameRules.cleanExt(it) },
                    label = { Text("New extension") },
                    prefix = { Text(".", fontWeight = FontWeight.ExtraBold) },
                    singleLine = true,
                    isError = badShape,
                    supportingText = {
                        Text(
                            if (badShape) "Use at least one letter"
                            else "Letters and digits, up to ${FileNameRules.MAX_EXT}. Empty = no extension."
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExtAmber, unfocusedBorderColor = ExtAmber,
                        focusedLabelColor = ExtAmberText, unfocusedLabelColor = ExtAmberText,
                        cursorColor = ExtAmber
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FileNameRules.CHIPS.forEach { c ->
                        ExtChip(
                            ext = c,
                            selected = FileNameRules.sameExt(newExt, c),
                            isReal = c == real
                        ) { newExt = c }
                    }
                }
                val realLabel = real?.let { FileNameRules.realLabel(it) }
                when {
                    real != null && FileNameRules.sameExt(newExt, real) -> NoteBox(
                        amber = false, text = "✓ .$newExt matches what this file really is."
                    )
                    newExt.isEmpty() -> NoteBox(
                        amber = true,
                        text = "Saving with no extension. Some apps may not know how to open it" +
                            (realLabel?.let { " — it is still a $it inside." } ?: ".")
                    )
                    realLabel != null -> NoteBox(
                        amber = true,
                        text = "Changing the extension does not convert the file. This is a $realLabel — " +
                            "named .$newExt it is still a $realLabel inside, and apps may refuse to open it."
                    )
                    else -> NoteBox(
                        amber = true,
                        text = "Changing the extension does not convert the file — it only changes how other apps treat it."
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            EditorSheetButtons(
                onCancel = onDismiss,
                onSave = {
                    when (val d = decision) {
                        is Decision.Direct -> if (d.name == file.displayName) onDismiss() else onRename(d.name, false)
                        is Decision.NeedsConfirm -> confirm = d          // nothing written yet
                        Decision.Invalid -> Unit
                    }
                },
                saveEnabled = decision !is Decision.Invalid && problem == null,
                saveText = if (needsConfirm) "Save…" else "Save",
                cancelText = "Cancel",
                dirty = false,
                saveBrush = if (needsConfirm) SolidColor(ExtAmber) else BrandGradient
            )
            // 25 % standard; 24 dp above the keyboard while it is up
            if (WindowInsets.isImeVisible) Spacer(Modifier.height(24.dp)) else SheetBottomSpace()
        }
    }

    val c = confirm
    if (c != null) ExtensionConfirmDialog(
        oldName = file.displayName,
        decision = c,
        realType = real,
        onKeep = { confirm = null; newExt = original.ext; unlocked = false },   // back to Step 1, name kept
        onChange = { confirm = null; onRename(c.name, true) }
    )
}

@Composable
private fun RenameFileHeader(file: FileEntity) {
    val kind = FileRules.kind(file.mime, file.displayName)
    val real = FileNameRules.realType(file.mime)
    val what = when (real) { "pdf" -> "PDF document"; "doc", "docx" -> "Word document"; else -> "Document" }
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 26.dp, height = 32.dp)
                .background(if (kind == "PDF") Color(0xFFC0392B) else Color(0xFF2B5797), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(kind, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(file.displayName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "$what · ${FileRules.humanSize(file.sizeBytes)} · added ${Fmt.date(Fmt.fromMillis(file.addedAt).toLocalDate())}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LockedExtChip(ext: String, onTap: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onTap() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (ext.isEmpty()) "none" else ".$ext", fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.Lock, "Extension locked", Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SavedAsLine(decision: Decision) {
    val text: AnnotatedString = when (decision) {
        Decision.Invalid -> AnnotatedString("Type a name to save")
        is Decision.Direct -> savedAs(decision.name, changed = false)
        is Decision.NeedsConfirm -> savedAs(decision.name, changed = true)
    }
    Text(
        text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp)
    )
}

private fun savedAs(name: String, changed: Boolean): AnnotatedString {
    val p = FileNameRules.split(name)
    return buildAnnotatedString {
        append("Saved as ")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.Unspecified)) { append(p.base) }
        if (p.hasExt) {
            val style = if (changed) SpanStyle(fontWeight = FontWeight.ExtraBold, color = ExtAmberText, background = Color(0xFFFDF0DA))
            else SpanStyle(fontWeight = FontWeight.ExtraBold)
            withStyle(style) { append(".${p.ext}") }
        } else if (changed) {
            withStyle(SpanStyle(color = ExtAmberText)) { append("  (no extension)") }
        }
    }
}

@Composable
private fun ExtChip(ext: String, selected: Boolean, isReal: Boolean, onClick: () -> Unit) {
    val bg = when { selected -> ExtAmber; isReal -> OkContainer; else -> Color.Transparent }
    val fg = when { selected -> Color.White; isReal -> OkText; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val line = when { selected -> ExtAmber; isReal -> Color(0xFF9FD7B6); else -> MaterialTheme.colorScheme.outline }
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isReal) {
            Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = fg)
            Spacer(Modifier.width(3.dp))
        }
        Text(if (isReal) ".$ext · this file" else ".$ext", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun NoteBox(amber: Boolean, text: String, action: String? = null, onAction: () -> Unit = {}) {
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (amber) AmberContainer else OkContainer)
            .border(1.dp, if (amber) ExtAmberLine else Color(0xFFA7DDBE), RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        if (amber) {
            Icon(Icons.Filled.Warning, null, Modifier.size(16.dp), tint = Amber)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = 12.5.sp, lineHeight = 17.sp, color = if (amber) Color(0xFF5C3A00) else OkText)
            if (action != null) Text(
                action, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = ExtAmberText,
                modifier = Modifier.padding(top = 4.dp).clickable { onAction() }
            )
        }
    }
}

/** Step 3 — nothing is written until "Change to .x"; Keep, Back and tap-outside return to Step 1. */
@Composable
private fun ExtensionConfirmDialog(
    oldName: String,
    decision: Decision.NeedsConfirm,
    realType: String?,
    onKeep: () -> Unit,
    onChange: () -> Unit
) {
    val old = FileNameRules.split(oldName)
    val new = FileNameRules.split(decision.name)
    val o = decision.oldExt
    val n = decision.newExt
    val title = when { n.isEmpty() -> "Remove the extension?"; o.isEmpty() -> "Add an extension?"; else -> "Change the extension?" }
    val keepLabel = if (o.isEmpty()) "Keep no extension" else "Keep .$o"
    val changeLabel = when { n.isEmpty() -> "Remove .$o"; o.isEmpty() -> "Add .$n"; else -> "Change to .$n" }
    val realLabel = realType?.let { FileNameRules.realLabel(it) }

    Dialog(onDismissRequest = onKeep) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(42.dp).clip(CircleShape).background(AmberContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.SwapHoriz, null, tint = Amber) }
                Spacer(Modifier.height(10.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(
                    "Only the name changes. The file itself is not converted.",
                    fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Spacer(Modifier.height(14.dp))
                Column(Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))) {
                    NameRow("From", buildAnnotatedString {
                        append(old.base)
                        if (old.hasExt) withStyle(SpanStyle(color = Danger, fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.LineThrough)) { append(".${old.ext}") }
                    })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NameRow("To", buildAnnotatedString {
                        append(new.base)
                        if (new.hasExt) withStyle(SpanStyle(color = ExtAmberText, fontWeight = FontWeight.ExtraBold,
                            background = AmberContainer)) { append(".${new.ext}") }
                        else withStyle(SpanStyle(color = ExtAmberText)) { append("  (no extension)") }
                    })
                }
                when {
                    realLabel == null && n.isEmpty() ->
                        DialogNote(true, "Without an extension some apps may not know how to open it.")
                    realLabel == null -> Unit
                    n.isEmpty() -> DialogNote(true, "Still a $realLabel inside — without an extension some apps may not open it.")
                    FileNameRules.sameExt(n, realType) -> DialogNote(false, "✓ .$n matches what this file really is.")
                    else -> DialogNote(true, "Still a $realLabel inside — it may not open as .$n.")
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onKeep, modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(keepLabel, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    androidx.compose.material3.Button(
                        onClick = onChange, modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExtAmber, contentColor = Color.White)
                    ) { Text(changeLabel, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@Composable
private fun NameRow(label: String, name: AnnotatedString) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp).padding(top = 2.dp))
        Text(name, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DialogNote(amber: Boolean, text: String) {
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (amber) Color(0xFFFFF8EC) else OkContainer)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (amber) {
            Icon(Icons.Filled.Warning, null, Modifier.size(14.dp), tint = Amber)
            Spacer(Modifier.width(7.dp))
        }
        Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = if (amber) Color(0xFF5C3A00) else OkText)
    }
}
