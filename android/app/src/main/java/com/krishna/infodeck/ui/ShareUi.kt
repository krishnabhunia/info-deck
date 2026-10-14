package com.krishna.infodeck.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.core.QrRules
import com.krishna.infodeck.core.ShareField
import com.krishna.infodeck.core.SharePreset
import com.krishna.infodeck.core.ShareTarget
import com.krishna.infodeck.core.ShareText

/** QR drawn on the phone — no service, no network. */
object Qr {
    fun bitmap(content: String, px: Int = 640): Bitmap? = Logger.guard("Qr.encode", null) {
        if (!QrRules.fits(content)) {
            Logger.e("Qr.encode", "content ${content.length} chars is over the ${QrRules.MAX_CHARS} limit")
            return@guard null
        }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px, hints)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        for (x in 0 until px) for (y in 0 until px) {
            bmp.setPixel(x, y, if (matrix[x, y]) AColor.BLACK else AColor.WHITE)
        }
        bmp
    }
}

/** Sends text (and optionally a file) to a named app, or the chooser. */
object Sharer {

    fun installed(context: Context, target: ShareTarget): Boolean = Logger.guard("Share.installed", false) {
        when (target) {
            ShareTarget.SMS, ShareTarget.EMAIL, ShareTarget.CHOOSER -> true
            else -> {
                val pkg = target.pkg ?: return@guard true
                context.packageManager.getLaunchIntentForPackage(pkg) != null
            }
        }
    }

    /** Returns an error message, or null when the app was opened. */
    fun send(
        context: Context,
        target: ShareTarget,
        text: String,
        fileUris: List<Uri>,
        subject: String
    ): String? =
        Logger.guard("Share.send", "Could not share") {
            if (text.isBlank()) {
                Logger.e("Share.send", "empty share blocked")
                return@guard "Tick at least one field first"
            }
            val fileUri = fileUris.firstOrNull()
            val intent = when (target) {
                ShareTarget.SMS -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                    putExtra("sms_body", text)
                }
                ShareTarget.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                else -> Intent(
                    if (fileUris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
                ).apply {
                    type = if (fileUris.isEmpty()) "text/plain" else "*/*"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, text)
                    when {
                        fileUris.size > 1 -> {
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fileUris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        fileUri != null -> {
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    target.pkg?.let { setPackage(it) }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                // Do not silently copy potentially secret card data to the system clipboard.
                // The share sheet already warns that receiving apps may drop the text.
                if (target == ShareTarget.CHOOSER) {
                    context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    context.startActivity(intent)
                }
                null
            } catch (t: Throwable) {
                Logger.e("Share.send", "no app for ${target.label}, falling back to the chooser", t)
                try {
                    val plain = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(plain, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    null
                } catch (t2: Throwable) {
                    Logger.e("Share.send", "chooser failed too", t2); "Nothing on this phone can share that"
                }
            }
        }
}

/** Step 1 — what to share (design 1.4.D2 S2). */
@Composable
fun ShareFieldsSheet(
    cardLabel: String,
    fields: List<ShareField>,
    fileSizes: Map<String, Long> = emptyMap(),
    onChange: (List<ShareField>) -> Unit,
    onPreset: (SharePreset) -> Unit,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onQr: () -> Unit
) {
    InfoSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Text("What to share", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }

        Row(Modifier.padding(top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SharePreset.values().forEach { p ->
                Box(
                    Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPreset(p) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(p.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }

        fields.forEachIndexed { i, f ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .clickable { onChange(fields.toMutableList().also { it[i] = f.copy(selected = !f.selected) }) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = f.selected,
                    onCheckedChange = { on ->
                        onChange(fields.toMutableList().also { it[i] = f.copy(selected = on) })
                    }
                )
                if (f.kind == ShareField.Kind.FILE) {
                    IconTile(
                        Icons.Filled.Description, hue(com.krishna.infodeck.core.Hue.PURPLE),
                        tint(com.krishna.infodeck.core.Hue.PURPLE), size = 32
                    )
                    Spacer(Modifier.width(9.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (f.kind == ShareField.Kind.FILE)
                            f.label + " - " + com.krishna.infodeck.core.FileShare.humanSize(fileSizes[f.key] ?: 0L)
                        else f.label,
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        ShareText.shown(f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (f.kind == ShareField.Kind.DATA) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                onChange(fields.toMutableList().also { it[i] = f.copy(masked = !f.masked) })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (f.masked) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            "Mask this value", Modifier.size(17.dp),
                            tint = if (f.masked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        val tickedFiles = fields.filter { it.selected && it.kind == ShareField.Kind.FILE }
        val totalBytes = com.krishna.infodeck.core.FileShare.totalBytes(
            tickedFiles.map { fileSizes[it.key] ?: 0L }
        )

        if (tickedFiles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Files go as a second send right after the text - real documents, never names in the message.",
                fontSize = 11.sp, color = hue(com.krishna.infodeck.core.Hue.GREEN)
            )
            if (com.krishna.infodeck.core.FileShare.tooBig(totalBytes)) Text(
                com.krishna.infodeck.core.FileShare.humanSize(totalBytes) + " is over the 25 MB most mail allows.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (com.krishna.infodeck.core.FileShare.maskingConflict(fields)) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(11.dp)
            ) {
                Text(
                    "You masked a value, but an attached file probably still shows it. " +
                        "Masking hides a value in the message - it cannot edit a document.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.error
                )
            }
        }

        SectionLabel("Preview")
        val text = ShareText.build(fields)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1522))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text.ifBlank { "Nothing ticked yet" },
                    fontSize = 10.5.sp, color = Color(0xFFD8E2F2), fontFamily = FontFamily.Monospace, lineHeight = 15.sp
                )
                if (tickedFiles.isNotEmpty()) Text(
                    "then, as files: " + tickedFiles.joinToString(" - ") { it.label },
                    fontSize = 10.5.sp, color = Color(0xFF9DB4FF), modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = onQr,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.QrCode2, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("QR", fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .weight(1.4f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandGradient)
                    .clickable(enabled = text.isNotBlank()) { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Next", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/** Step 2 — send with (design 1.4.D2 S3). */
@Composable
fun ShareTargetSheet(
    summary: String,
    hasFile: Boolean,
    available: List<ShareTarget>,
    onTarget: (ShareTarget) -> Unit,
    onExport: () -> Unit,
    onCopy: () -> Unit,
    onQr: () -> Unit,
    onDismiss: () -> Unit
) {
    InfoSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Text("Send with", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            available.filter { it != ShareTarget.CHOOSER && com.krishna.infodeck.core.FileShare.offers(it, hasFile) }
                .forEach { t ->
                    val badge = com.krishna.infodeck.core.FileShare.badge(t, hasFile)
                Column(
                    Modifier.weight(1f).clickable { onTarget(t) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(targetColour(t)),
                        contentAlignment = Alignment.Center
                    ) { Icon(targetIcon(t), t.label, Modifier.size(24.dp), tint = Color.White) }
                    Spacer(Modifier.height(5.dp))
                    Text(t.label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    if (hasFile) Text(
                        when (badge) {
                            com.krishna.infodeck.core.CarryBadge.BOTH_FINE -> "both fine"
                            com.krishna.infodeck.core.CarryBadge.TEXT_MAY_DROP -> "text may drop"
                            com.krishna.infodeck.core.CarryBadge.NO_FILES -> "no files"
                            com.krishna.infodeck.core.CarryBadge.UNKNOWN -> ""
                        },
                        fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        color = when (badge) {
                            com.krishna.infodeck.core.CarryBadge.BOTH_FINE -> hue(com.krishna.infodeck.core.Hue.GREEN)
                            com.krishna.infodeck.core.CarryBadge.TEXT_MAY_DROP -> Amber
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
        if (hasFile) Row(
            Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, null, Modifier.size(15.dp), tint = Amber)
            Spacer(Modifier.width(6.dp))
            Text("SMS is hidden while a file is ticked - Android cannot carry attachments there", fontSize = 11.sp, color = Amber)
        }

        SectionLabel("Or keep it as a file")
        SheetRow(Icons.Filled.Description, "Export as text file", "A .txt written wherever you choose", onExport)
        SheetRow(Icons.Filled.ContentCopy, "Copy to clipboard", "Paste it anywhere yourself", onCopy)
        SheetRow(Icons.Filled.QrCode2, "Show as QR code", "The same fields, scannable", onQr)
        SheetRow(Icons.Filled.Share, "All other apps...", "The usual Android chooser") { onTarget(ShareTarget.CHOOSER) }

        Spacer(Modifier.height(10.dp))
        Text(
            "Once it is in another app it is out of InfoDeck's hands. InfoDeck itself still has no internet permission.",
            fontSize = 11.sp, color = Amber
        )
        Spacer(Modifier.height(12.dp))
        EditorSheetButtons(onCancel = onDismiss, onSave = onDismiss, saveText = "Done", dirty = false)
    }
}

@Composable
private fun SheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, size = 32)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun targetIcon(t: ShareTarget) = when (t) {
    ShareTarget.WHATSAPP -> Icons.Filled.Message
    ShareTarget.GMAIL -> Icons.Filled.Email
    ShareTarget.SMS -> Icons.Filled.Message
    ShareTarget.EMAIL -> Icons.Filled.Email
    ShareTarget.CHOOSER -> Icons.Filled.Share
}

private fun targetColour(t: ShareTarget) = when (t) {
    ShareTarget.WHATSAPP -> Color(0xFF25D366)
    ShareTarget.GMAIL -> Color(0xFFEA4335)
    ShareTarget.SMS -> Color(0xFF1E88E5)
    ShareTarget.EMAIL -> Color(0xFF546E7A)
    ShareTarget.CHOOSER -> Color(0xFF6A6F7A)
}

/** The QR sheet — code, what it contains, save and share (design 1.4.D2 S5/S6). */
@Composable
fun QrSheet(
    cardLabel: String,
    fields: List<ShareField>,
    perField: Boolean,
    pageIndex: Int,
    onPerField: (Boolean) -> Unit,
    onPage: (Int) -> Unit,
    onSavePng: (Bitmap, String) -> Unit,
    onShareImage: (Bitmap, String) -> Unit,
    onDismiss: () -> Unit
) {
    val pages = if (perField) QrRules.perField(fields) else listOf(cardLabel to QrRules.combined(cardLabel, fields))
    val safeIndex = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val content = pages.getOrNull(safeIndex)?.second ?: ""
    val bmp = remember(content) { Qr.bitmap(content) }

    InfoSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QrCode2, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Text("QR code", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }

        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(false to "One code, all fields", true to "One code per field").forEach { (v, label) ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (perField == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPerField(v); onPage(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        color = if (perField == v) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), "QR code", Modifier.size(210.dp))
                } else {
                    Icon(Icons.Filled.Warning, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.error)
                    Text(
                        "Too much text for one code - switch to one code per field",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    QrRules.caption(content, pages.size), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pages.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pages.forEachIndexed { i, _ ->
                            Box(
                                Modifier
                                    .width(if (i == safeIndex) 20.dp else 7.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (i == safeIndex) MaterialTheme.colorScheme.primary else Color(0xFFD5DBE7))
                                    .clickable { onPage(i) }
                            )
                        }
                    }
                    Text(
                        pages[safeIndex].first, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        SectionLabel("What it contains")
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0F1522)).padding(11.dp)
        ) {
            Text(
                content.ifBlank { "Nothing selected" }, fontSize = 10.5.sp,
                color = Color(0xFFD8E2F2), fontFamily = FontFamily.Monospace, lineHeight = 15.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Anyone who can see your screen can scan this. A QR is not a secret - it is just faster typing.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(12.dp))
        EditorSheetButtons(
            onCancel = onDismiss,
            onSave = { bmp?.let { onShareImage(it, cardLabel) } },
            saveText = "Share image",
            saveEnabled = bmp != null,
            cancelText = "Close",
            dirty = false
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = { bmp?.let { onSavePng(it, cardLabel) } },
            enabled = bmp != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Save as PNG", fontWeight = FontWeight.Bold) }
    }
}


/** Builds the one-zip bundle: a .txt of the card plus every ticked file. */
object Bundler {
    fun build(
        context: Context,
        cardLabel: String,
        text: String,
        files: List<java.io.File>
    ): java.io.File? = Logger.guard("Share.zip", null) {
        val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
        val name = com.krishna.infodeck.core.FileShare.zipName(cardLabel, java.time.LocalDate.now())
        val tmp = java.io.File(dir, "$name.part")
        val out = java.io.File(dir, name)
        val present = files.filter { it.exists() && it.canRead() }
        files.filterNot { present.contains(it) }.forEach {
            Logger.e("Share.zip", "file missing, skipped: " + it.name)
        }
        val entries = com.krishna.infodeck.core.FileShare.uniqueEntryNames(present.map { it.name })
        java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(tmp))).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry(com.krishna.infodeck.core.FileShare.textEntryName(cardLabel)))
            zip.write(text.toByteArray())
            zip.closeEntry()
            present.forEachIndexed { i, f ->
                zip.putNextEntry(java.util.zip.ZipEntry(entries[i]))
                java.io.FileInputStream(f).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        if (out.exists()) out.delete()
        // Rename only on success, so a failure never leaves a half zip behind.
        if (!tmp.renameTo(out)) {
            Logger.e("Share.zip", "could not rename the finished zip")
            tmp.delete()
            return@guard null
        }
        out
    }

    fun uri(context: Context, f: java.io.File): Uri =
        androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
}


/** B4 — share ONE file with its real MIME through the system chooser. Returns an error message or null. */
fun shareOneFile(context: Context, file: java.io.File, displayName: String): String? =
    Logger.guard("Share.file", "Could not share that file") {
        if (!file.exists() || !file.canRead()) {
            Logger.e("Share.file", "missing: " + displayName)
            return@guard "That file is no longer on this phone"
        }
        val uri = Bundler.uri(context, file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = com.krishna.infodeck.core.FileShare.mime(displayName)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(i, "Share " + displayName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    }

/** B5 — the follow-up send: files only, real MIME, pinned to the app the text went to. */
fun sendFilesOnly(context: Context, target: ShareTarget, files: List<java.io.File>): String? =
    Logger.guard("Share.files", "Could not send the files") {
        // files here are already the exact-name copies from ShareFiles.prepare
        val present = files.filter { it.exists() && it.canRead() }
        files.filterNot { present.contains(it) }.forEach { Logger.e("Share.files", "missing, skipped: " + it.name) }
        if (present.isEmpty()) return@guard "None of those files are on this phone any more"
        val uris = present.map { Bundler.uri(context, it) }
        val i = if (uris.size == 1) Intent(Intent.ACTION_SEND).apply {
            type = com.krishna.infodeck.core.FileShare.mime(present.first().name)
            putExtra(Intent.EXTRA_STREAM, uris.first())
        } else Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = com.krishna.infodeck.core.FileShare.commonMime(present.map { it.name })
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        target.pkg?.let { i.setPackage(it) }
        try {
            context.startActivity(i)
            if (present.size < files.size) "${files.size - present.size} file was missing and was skipped" else null
        } catch (e: android.content.ActivityNotFoundException) {
            Logger.e("Share.files", "target cannot take these files, offering the chooser", e)
            i.setPackage(null)
            context.startActivity(Intent.createChooser(i, "Send files").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            target.label + " could not take the files - pick another app"
        }
    }


/**
 * B10 — receivers show whatever name the URI carries, and InfoDeck stores attachments under a UUID.
 * So every share copies the file into cache/share under its EXACT display name first.
 */
object ShareFiles {
    fun safeName(displayName: String): String {
        val cleaned = displayName.map { c -> if (c in "\\/:*?\"<>|" || c.code < 32) '_' else c }.joinToString("").trim()
        return cleaned.ifBlank { "file" }
    }

    private fun dir(context: Context) = java.io.File(context.cacheDir, "share").apply { mkdirs() }

    /** Copies each stored file to cache/share/<display name>; clashes within one share get " (1)". */
    fun prepare(context: Context, files: List<Pair<java.io.File, String>>): List<java.io.File> {
        val names = com.krishna.infodeck.core.FileShare.uniqueEntryNames(files.map { safeName(it.second) })
        return files.mapIndexedNotNull { i, (src, display) ->
            Logger.guard("Share.prepare", src.takeIf { it.exists() }) {
                if (!src.exists() || !src.canRead()) { Logger.e("Share.prepare", "missing: $display"); return@guard null }
                val out = java.io.File(dir(context), names[i])
                src.copyTo(out, overwrite = true)
                out
            }
        }
    }

    /** One file, exact name, real MIME, system chooser. */
    fun shareOne(context: Context, stored: java.io.File, displayName: String): String? =
        Logger.guard("Share.file", "Could not share that file") {
            val prepared = prepare(context, listOf(stored to displayName)).firstOrNull()
                ?: return@guard "That file is no longer on this phone"
            val i = Intent(Intent.ACTION_SEND).apply {
                type = com.krishna.infodeck.core.FileShare.mime(displayName)
                putExtra(Intent.EXTRA_STREAM, Bundler.uri(context, prepared))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(i, "Share " + displayName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        }

    /** Swept at app start so copies never accumulate. */
    fun sweep(context: Context) = Logger.guard("Share.sweep", Unit) {
        dir(context).listFiles()?.forEach { it.delete() }
    }
}
