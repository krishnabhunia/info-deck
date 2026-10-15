package com.krishna.infodeck.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.FileNameRules
import com.krishna.infodeck.data.FileEntity
import com.krishna.infodeck.data.FileRenameRules

/**
 * Design A (PR #3): protect the original extension until explicitly unlocked.
 * A changed extension, including case-only changes, needs a separate acknowledgement.
 *
 * File bytes, stored filename and MIME are never changed by the rename operation.
 * The caller retains N62's Undo snackbar and the file row's real-type warnings.
 */
@Composable
fun ProtectedFileRenameDialog(
    file: FileEntity,
    onDismiss: () -> Unit,
    onRename: (String, Boolean) -> Unit
) {
    val original = remember(file.id, file.displayName) {
        FileNameRules.split(file.displayName)
    }
    var stem by remember(file.id, file.displayName) { mutableStateOf(original.base) }
    var extension by remember(file.id, file.displayName) { mutableStateOf(original.ext) }
    var extensionUnlocked by remember(file.id, file.displayName) { mutableStateOf(false) }
    var pendingName by remember(file.id, file.displayName) { mutableStateOf<String?>(null) }
    var acknowledged by remember(file.id, file.displayName) { mutableStateOf(false) }

    val actualExtension = if (extensionUnlocked) extension else original.ext
    val proposedName = FileNameRules.join(stem, actualExtension)
    val invalidExtension = actualExtension.isNotEmpty() && !FileNameRules.isExtShape(actualExtension)
    val nameError = FileRenameRules.validate(
        file.displayName, proposedName, extensionConfirmed = true
    )
    val error = if (invalidExtension) {
        "Extension must contain a letter and be 1–8 letters or digits"
    } else nameError
    val realType = FileNameRules.realType(file.mime)
    val typedExtension = if (extensionUnlocked) null else FileNameRules.typedExtension(stem)
    val pending = pendingName

    if (pending != null) {
        AlertDialog(
            onDismissRequest = { acknowledged = false; pendingName = null },
            title = { Text("Change file extension?") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        "Changing the extension does not convert the document. The original file contents and format stay unchanged.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Current: ${file.displayName}", fontSize = 13.sp)
                    Text("New: $pending", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (realType != null && !FileNameRules.sameExt(
                            FileNameRules.split(pending).ext, realType
                        )
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The file is still a ${FileNameRules.realLabel(realType)}. Some apps may not open it under the new extension.",
                            color = MaterialTheme.colorScheme.error, fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = acknowledged,
                            onCheckedChange = { acknowledged = it }
                        )
                        Text(
                            "I understand changing the extension does not convert the file.",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = acknowledged,
                    onClick = {
                        // A second repository guard rejects unconfirmed extension changes.
                        onRename(pending, true)
                    }
                ) { Text("Confirm extension change") }
            },
            dismissButton = {
                TextButton(onClick = { acknowledged = false; pendingName = null }) {
                    Text("Back to editing")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Rename file") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        "Rename the file without changing its type.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = stem,
                            onValueChange = {
                                // Reject oversize input rather than silently cutting the extension.
                                if (it.length <= FileRenameRules.MAX_FILENAME_LENGTH) stem = it
                            },
                            label = { Text("File name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (original.hasExt) ".${original.ext} 🔒" else "No ext. 🔒",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Saved as $proposedName",
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (typedExtension != null && !FileNameRules.sameExt(typedExtension, original.ext)) {
                        TextButton(onClick = {
                            stem = FileNameRules.split(stem).base
                            extension = typedExtension
                            extensionUnlocked = true
                        }) { Text("Use .$typedExtension as the extension…") }
                    }
                    TextButton(onClick = {
                        extensionUnlocked = !extensionUnlocked
                        if (!extensionUnlocked) extension = original.ext
                    }) {
                        Text(if (extensionUnlocked) "Keep original extension" else "Change extension")
                    }
                    if (extensionUnlocked) {
                        OutlinedTextField(
                            value = extension,
                            onValueChange = { if (it.length <= FileNameRules.MAX_EXT) extension = it },
                            label = { Text("Extension (without dot)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Letters and digits, up to 8 with at least one letter. Leave blank to remove.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            FileNameRules.CHIPS.forEach { choice ->
                                TextButton(onClick = { extension = choice }) {
                                    Text(".$choice")
                                }
                            }
                        }
                        Text(
                            "Changing an extension does not convert the file.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                    if (error != null) {
                        Text(
                            error, color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = error == null && proposedName != file.displayName,
                    onClick = {
                        if (FileRenameRules.extensionChanged(file.displayName, proposedName)) {
                            acknowledged = false
                            pendingName = proposedName
                        } else {
                            onRename(proposedName, false)
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}
