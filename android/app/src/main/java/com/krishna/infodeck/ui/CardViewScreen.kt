package com.krishna.infodeck.ui

import androidx.compose.material.icons.filled.Event

import androidx.compose.material.icons.filled.DataObject

import androidx.compose.material.icons.filled.AttachFile

import androidx.compose.foundation.border

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.data.CardDetail
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardViewScreen(
    vm: AppViewModel,
    cardId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit,
    openShareOnStart: Boolean = false
) {
    var detail by remember { mutableStateOf<CardDetail?>(null) }
    var overflow by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showDanger by remember { mutableStateOf(false) }
    var shareStep by remember { mutableStateOf(0) }          // 0 none, 1 fields, 2 targets, 3 qr
    var fields by remember { mutableStateOf<List<com.krishna.infodeck.core.ShareField>>(emptyList()) }
    var perFieldQr by remember { mutableStateOf(false) }
    var pendingFiles by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var pendingTarget by remember { mutableStateOf<com.krishna.infodeck.core.ShareTarget?>(null) }
    var qrPage by remember { mutableStateOf(0) }

    fun buildFields(detail: com.krishna.infodeck.data.CardDetail): List<com.krishna.infodeck.core.ShareField> {
        val out = mutableListOf<com.krishna.infodeck.core.ShareField>()
        out += com.krishna.infodeck.core.ShareField(
            "name", "Card name", detail.card.label, com.krishna.infodeck.core.ShareField.Kind.NAME
        )
        out += com.krishna.infodeck.core.ShareField(
            "cat", "Category", detail.categoryName +
                (if (detail.tags.isEmpty()) "" else " - " + detail.tags.joinToString(" ") { "#" + it.name }),
            com.krishna.infodeck.core.ShareField.Kind.CATEGORY
        )
        detail.data.forEach {
            out += com.krishna.infodeck.core.ShareField(
                "d" + it.id, it.name, it.value, com.krishna.infodeck.core.ShareField.Kind.DATA
            )
        }
        detail.dates.forEach {
            out += com.krishna.infodeck.core.ShareField(
                "t" + it.id, it.name, Fmt.date(LocalDate.ofEpochDay(it.epochDay)),
                com.krishna.infodeck.core.ShareField.Kind.DATE
            )
        }
        detail.schedulers.forEach {
            out += com.krishna.infodeck.core.ShareField(
                "s" + it.entity.id, "Reminder", it.ruleText, com.krishna.infodeck.core.ShareField.Kind.REMINDER
            )
        }
        detail.files.forEach {
            out += com.krishna.infodeck.core.ShareField(
                "f" + it.id, it.displayName, it.displayName, com.krishna.infodeck.core.ShareField.Kind.FILE
            )
        }
        val keyKey = detail.card.keyDateVarId?.let { "t" + it }
        return com.krishna.infodeck.core.ShareText.applyPreset(
            out, com.krishna.infodeck.core.SharePreset.LAST_USED, keyKey
        )
    }

    fun openShare(step: Int) {
        val dd = detail ?: return
        if (fields.isEmpty()) fields = buildFields(dd)
        shareStep = step
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rows by vm.home.collectAsState()
    var exportText by remember { mutableStateOf("") }
    var exportName by remember { mutableStateOf("") }
    var pendingBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val ok = com.krishna.infodeck.core.Logger.guard("Share.export", false) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(exportText.toByteArray()) }
                true
            }
            scope.launch { snackbar.showSnackbar(if (ok) "Saved " + exportName else "Could not write the file") }
        }
    }

    val pngLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bmp = pendingBitmap
        if (uri != null && bmp != null) {
            val ok = com.krishna.infodeck.core.Logger.guard("Qr.savePng", false) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val big = android.graphics.Bitmap.createScaledBitmap(
                        bmp, com.krishna.infodeck.core.QrRules.PNG_PX, com.krishna.infodeck.core.QrRules.PNG_PX, false
                    )
                    big.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
            scope.launch { snackbar.showSnackbar(if (ok) "QR saved" else "Could not save the image") }
        }
    }
    val today = LocalDate.now()

    LaunchedEffect(cardId, rows.allRows) { detail = vm.detail(cardId) }

    val d = detail
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CardHero(
                d = d, today = today, onBack = onBack,
                onShare = { openShare(1) }, onEdit = { onEdit(cardId) },
                onCategory = { d?.let { onOpenCategory(it.card.categoryId) } },
                menu = {
                    Box {
                        HeaderIcon(Icons.Filled.MoreVert, "More") { overflow = true }
                        DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                            DropdownMenuItem(text = { Text("Share as text") }, onClick = {
                                overflow = false
                                d?.let { shareAsText(context, it) }
                            })
                            DropdownMenuItem(text = { Text("Move to category") }, onClick = { overflow = false; showMove = true })
                            DropdownMenuItem(
                                text = { Text("Danger zone...", color = MaterialTheme.colorScheme.error) },
                                onClick = { overflow = false; showDanger = true }
                            )
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (d == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...") }
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.Edit, "Edit", com.krishna.infodeck.core.Hue.INDIGO) { onEdit(cardId) }
                    ActionButton(Icons.Filled.ContentCopy, "Copy", com.krishna.infodeck.core.Hue.TEAL) {
                        copy(context, com.krishna.infodeck.core.ShareText.build(buildFields(d)))
                        scope.launch { snackbar.showSnackbar("Card copied") }
                    }
                    ActionButton(Icons.Filled.QrCode2, "QR", com.krishna.infodeck.core.Hue.VIOLET) { openShare(3) }
                    ActionButton(Icons.Filled.Share, "Share", com.krishna.infodeck.core.Hue.GREEN, primary = true) { openShare(1) }
                }

                if (d.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        d.tags.forEach { t ->
                            Box(Modifier.padding(bottom = 6.dp)) { TagChip(t.name) }
                        }
                    }
                }

                if (d.files.isNotEmpty()) {
                    RoleLabel("Files", com.krishna.infodeck.core.Hue.PURPLE, Icons.Filled.AttachFile, d.files.size, Modifier.padding(top = 10.dp))
                    d.files.forEach { f ->
                        Box(Modifier.padding(bottom = 7.dp)) {
                            FileRow(
                                file = f,
                                onOpen = {
                                    com.krishna.infodeck.data.FileStore.open(context, f)?.let { msg ->
                                        scope.launch { snackbar.showSnackbar(msg) }
                                    }
                                },
                                onShare = {
                                    com.krishna.infodeck.data.FileStore.share(context, f)?.let { msg ->
                                        scope.launch { snackbar.showSnackbar(msg) }
                                    }
                                },
                                onRename = { onEdit(cardId) },
                                onRemove = { onEdit(cardId) }
                            )
                        }
                    }
                }

                if (d.data.isNotEmpty()) {
                    RoleLabel("Data", com.krishna.infodeck.core.Hue.SKY, Icons.Filled.DataObject, d.data.size, Modifier.padding(top = 10.dp))
                    Column(Modifier.fillMaxWidth().deckCard(deck, 16.dp, hue(com.krishna.infodeck.core.Hue.SKY))) {
                        d.data.forEachIndexed { i, v ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(v.name, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(v.value, fontSize = 14.sp)
                                }
                                IconButton(onClick = {
                                    fields = buildFields(d).map { f ->
                                        f.copy(selected = f.key == "d" + v.id)
                                    }
                                    perFieldQr = true; qrPage = 0; shareStep = 3
                                }) { Icon(Icons.Filled.QrCode2, "QR code for " + v.name, Modifier.size(20.dp)) }
                                IconButton(onClick = {
                                    copy(context, v.value)
                                    scope.launch { snackbar.showSnackbar("Copied ${v.name}") }
                                }) { Icon(Icons.Filled.ContentCopy, "Copy", Modifier.size(20.dp)) }
                            }
                            if (i < d.data.lastIndex) Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                if (d.dates.isNotEmpty()) {
                    RoleLabel("Dates", com.krishna.infodeck.core.Hue.GREEN, Icons.Filled.Event, d.dates.size, Modifier.padding(top = 10.dp))
                    Column(Modifier.fillMaxWidth().deckCard(deck, 16.dp, hue(com.krishna.infodeck.core.Hue.GREEN))) {
                        d.dates.forEachIndexed { i, v ->
                            val date = LocalDate.ofEpochDay(v.epochDay)
                            val isKey = d.card.keyDateVarId == v.id
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (isKey) "★ ${v.name} · key date" else v.name,
                                        fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Fmt.date(date), fontSize = 14.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            Fmt.relative(date, today), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            color = if (date.isBefore(today)) MaterialTheme.colorScheme.onSurfaceVariant
                                                else if (deck.dark) hue(com.krishna.infodeck.core.Hue.GREEN) else Ok
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    copy(context, Fmt.date(date))
                                    scope.launch { snackbar.showSnackbar("Copied ${v.name}") }
                                }) { Icon(Icons.Filled.ContentCopy, "Copy", Modifier.size(20.dp)) }
                            }
                            if (i < d.dates.lastIndex) Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                if (d.schedulers.isNotEmpty()) {
                    RoleLabel("Reminders", com.krishna.infodeck.core.Hue.AMBER, Icons.Filled.Alarm, d.schedulers.size, Modifier.padding(top = 10.dp))
                    Column(Modifier.fillMaxWidth().deckCard(deck, 16.dp, hue(com.krishna.infodeck.core.Hue.AMBER))) {
                        d.schedulers.forEachIndexed { i, s ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HueTile(Icons.Filled.Alarm, com.krishna.infodeck.core.Hue.AMBER, size = 28, flat = true)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.ruleText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        buildString {
                                            append(Fmt.dateTime(s.ringAt))
                                            s.ringAt?.let { append(" - ${Fmt.relativeShort(it.toLocalDate(), today)}") }
                                        },
                                        fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = s.entity.enabled,
                                        onCheckedChange = { vm.setSchedulerEnabled(s.entity.id, it) }
                                    )
                                    Pill(
                                        s.statusText,
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            if (i < d.schedulers.lastIndex) Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Created ${Fmt.date(Fmt.fromMillis(d.card.createdAt).toLocalDate())} - " +
                        "Updated ${Fmt.date(Fmt.fromMillis(d.card.updatedAt).toLocalDate())}",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.5.sp, color = Hint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    if (showMove && d != null) MoveToCategorySheet(
        count = 1,
        categories = rows.categories,
        currentCategoryId = d.card.categoryId,
        onCreate = { name, color, cb -> vm.addCategory(name, color) { cb(it) } },
        onDismiss = { showMove = false },
        onMove = { catId ->
            showMove = false
            vm.moveCard(cardId, catId)
            scope.launch { snackbar.showSnackbar("Moved to ${rows.categories.firstOrNull { it.id == catId }?.name ?: "category"}") }
        }
    )

    androidx.compose.runtime.LaunchedEffect(openShareOnStart, detail?.card?.id) {
        if (openShareOnStart && detail != null && shareStep == 0 && fields.isEmpty()) openShare(1)
    }

    val pt = pendingTarget
    if (pt != null && pendingFiles.isNotEmpty()) {
        androidx.compose.runtime.LaunchedEffect(pendingFiles, pt) {
            val r = snackbar.showSnackbar(
                message = com.krishna.infodeck.core.SharePlanner.followUpMessage(pendingFiles.size, pt),
                actionLabel = "SEND FILES",
                duration = androidx.compose.material3.SnackbarDuration.Indefinite,
                withDismissAction = true
            )
            if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                val e = sendFilesOnly(context, pt, pendingFiles)
                if (e != null) snackbar.showSnackbar(e)
            }
            pendingFiles = emptyList(); pendingTarget = null
        }
    }

    if (shareStep == 1 && d != null) ShareFieldsSheet(
        cardLabel = d.card.label,
        fields = fields,
        fileSizes = d.files.associate { ("f" + it.id) to it.sizeBytes },
        onChange = { fields = it },
        onPreset = { p ->
            fields = com.krishna.infodeck.core.ShareText.applyPreset(
                fields, p, d.card.keyDateVarId?.let { "t" + it }
            )
        },
        onDismiss = { shareStep = 0 },
        onNext = { shareStep = 2 },
        onQr = { perFieldQr = false; qrPage = 0; shareStep = 3 }
    )

    if (shareStep == 2 && d != null) {
        val text = com.krishna.infodeck.core.ShareText.build(fields)
        val targets = com.krishna.infodeck.core.ShareTarget.values()
            .filter { it != com.krishna.infodeck.core.ShareTarget.CHOOSER && Sharer.installed(context, it) }
        ShareTargetSheet(
            summary = com.krishna.infodeck.core.ShareText.selectedCount(fields).toString() + " fields - " +
                text.length + " characters",
            hasFile = fields.any { it.selected && it.kind == com.krishna.infodeck.core.ShareField.Kind.FILE },
            available = targets,
            onTarget = { t ->
                val ticked = fields.filter { it.selected && it.kind == com.krishna.infodeck.core.ShareField.Kind.FILE }
                val chosen = d.files.filter { f -> ticked.any { it.key == "f" + f.id } }
                val onDisk = ShareFiles.prepare(
                    context,
                    chosen.map { f -> java.io.File(context.filesDir, "cards/" + f.cardId + "/" + f.storedName) to f.displayName }
                )
                val plan = com.krishna.infodeck.core.SharePlanner.plan(t, onDisk.size)
                val err = when (plan) {
                    com.krishna.infodeck.core.SendPlan.TEXT_ONLY -> Sharer.send(context, t, text, emptyList(), d.card.label)
                    com.krishna.infodeck.core.SendPlan.ONE_SEND_WITH_FILES ->
                        Sharer.send(context, t, text, onDisk.map { Bundler.uri(context, it) }, d.card.label)
                    com.krishna.infodeck.core.SendPlan.TEXT_THEN_FILES -> {
                        val e = Sharer.send(context, t, text, emptyList(), d.card.label)
                        if (e == null) { pendingFiles = onDisk; pendingTarget = t }
                        e
                    }
                }
                shareStep = 0
                val skipped = chosen.size - onDisk.size
                val msg = err ?: if (skipped > 0) "$skipped file was missing and was skipped" else null
                if (msg != null) scope.launch { snackbar.showSnackbar(msg) }
            },
            onExport = {
                exportText = text
                exportName = com.krishna.infodeck.core.ShareText.fileName(d.card.label, LocalDate.now(), "txt")
                exportLauncher.launch(exportName)
                shareStep = 0
            },
            onCopy = {
                copy(context, text); shareStep = 0
                scope.launch { snackbar.showSnackbar("Copied to clipboard") }
            },
            onQr = { perFieldQr = false; qrPage = 0; shareStep = 3 },
            onDismiss = { shareStep = 0 }
        )
    }

    if (shareStep == 3 && d != null) QrSheet(
        cardLabel = d.card.label,
        fields = fields,
        perField = perFieldQr,
        pageIndex = qrPage,
        onPerField = { perFieldQr = it },
        onPage = { qrPage = it },
        onSavePng = { bmp, label ->
            pendingBitmap = bmp
            pngLauncher.launch(com.krishna.infodeck.core.QrRules.pngName(label, LocalDate.now()))
        },
        onShareImage = { bmp, label ->
            val err = shareBitmap(context, bmp, label)
            if (err != null) scope.launch { snackbar.showSnackbar(err) }
        },
        onDismiss = { shareStep = 0 }
    )

    if (showDanger && d != null) DangerSheet(
        title = d.card.label,
        detail = "${d.data.size} data, ${d.dates.size} dates, ${d.schedulers.size} schedulers and ${d.files.size} files go with it.",
        deleteLabel = "Delete card",
        onDismiss = { showDanger = false },
        onDelete = { showDanger = false; confirmDelete = true }
    )

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this card?") },
        text = { Text("Its data, dates and reminders go with it. This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; vm.deleteCard(cardId) { onBack() } }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("InfoDeck", text))
}

/** Plain-text dump of label + variables. Never the schedulers (D1 S3.3). */
private fun shareAsText(context: Context, d: CardDetail) {
    val sb = StringBuilder()
    sb.appendLine(d.card.label)
    sb.appendLine("Category: ${d.categoryName}")
    d.data.forEach { sb.appendLine("${it.name}: ${it.value}") }
    d.dates.forEach { sb.appendLine("${it.name}: ${Fmt.date(LocalDate.ofEpochDay(it.epochDay))}") }
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString().trim())
    }
    context.startActivity(Intent.createChooser(i, "Share card"))
}


/** 2.0 action tile — tinted in its role colour; Share is filled (design 1.15.0.D1 frame 07). */
@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    h: com.krishna.infodeck.core.Hue,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val p = deck
    val s = p.hue(h)
    val fg = when {
        primary && p.dark -> Color(0xFF0A0E1F)
        primary -> Color.White
        p.dark -> s.glyph
        else -> s.onTint
    }
    Column(
        Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) androidx.compose.ui.graphics.Brush.linearGradient(listOf(s.glyph, s.glyph2))
                else androidx.compose.ui.graphics.SolidColor(s.tint))
            .then(if (p.dark && !primary) Modifier.border(1.dp, s.glyph.copy(alpha = 0.4f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = fg)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

/**
 * 2.0 card hero (design 1.15.0.D1 frame 07): a band in the category's colour with the glyph tile,
 * the label, category and tags, and the key date with a big countdown ring.
 */
@Composable
private fun CardHero(
    d: CardDetail?,
    today: LocalDate,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onCategory: () -> Unit,
    menu: @Composable () -> Unit
) {
    val p = deck
    val h = d?.let { com.krishna.infodeck.core.Hues.forCategory(it.colorIndex) } ?: com.krishna.infodeck.core.Hue.INDIGO
    val s = p.hue(h)
    val ink = headerInk()
    StatusBarColour(if (p.dark) Color(0xFF0D1226) else s.glyph)
    val radius = if (p.dark) 0.dp else 28.dp
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = radius, bottomEnd = radius))
            .background(
                if (p.dark) androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(s.glyph.copy(alpha = 0.4f), Color(0xFF0D1226)), radius = 900f,
                    center = androidx.compose.ui.geometry.Offset(1000f, 0f)
                ) else androidx.compose.ui.graphics.Brush.linearGradient(listOf(s.glyph, s.glyph2))
            )
            .padding(bottom = 14.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalOnHeader provides true) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderIcon(Icons.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.weight(1f))
                HeaderIcon(Icons.Filled.Share, "Share", onShare)
                HeaderIcon(Icons.Filled.Edit, "Edit", onEdit)
                menu()
            }
        }
        if (d == null) return@Column
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (p.dark) s.tint else Color.White.copy(alpha = 0.22f))
                    .border(1.dp, if (p.dark) s.glyph else Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(d.categoryName)), null, Modifier.size(30.dp),
                    tint = if (p.dark) s.glyph else Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.card.label, fontFamily = p.heading, fontSize = 21.sp, color = ink, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (p.heading == FredokaFamily) FontWeight.SemiBold else FontWeight.ExtraBold)
                Text(
                    d.categoryName + (if (d.tags.isNotEmpty()) "  ·  " + d.tags.take(3).joinToString(" ") { com.krishna.infodeck.data.TagRules.display(it.name) } else ""),
                    fontSize = 11.5.sp, color = ink.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onCategory() }
                )
            }
        }
        val key = d.dates.firstOrNull { it.id == d.card.keyDateVarId }
        if (key != null) {
            val date = LocalDate.ofEpochDay(key.epochDay)
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (p.dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.18f))
                    .border(1.dp, if (p.dark) p.outline else Color.White.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownRing(java.time.temporal.ChronoUnit.DAYS.between(today, date), sizeDp = 52f, onColour = !p.dark)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("★ Key date · ${key.name}", fontSize = 10.5.sp, color = ink.copy(alpha = 0.85f))
                    Text(Fmt.date(date), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    Text(Fmt.relative(date, today), fontSize = 10.5.sp, color = ink.copy(alpha = 0.85f))
                }
            }
        }
    }
}

/** Writes the code to a cache file and hands it to another app. */
private fun shareBitmap(context: android.content.Context, bmp: android.graphics.Bitmap, label: String): String? =
    com.krishna.infodeck.core.Logger.guard("Qr.shareImage", "Could not share the image") {
        val dir = java.io.File(context.filesDir, "cards/qr").apply { mkdirs() }
        val f = java.io.File(dir, com.krishna.infodeck.core.QrRules.pngName(label, java.time.LocalDate.now()))
        java.io.FileOutputStream(f).use { out ->
            android.graphics.Bitmap.createScaledBitmap(
                bmp, com.krishna.infodeck.core.QrRules.PNG_PX, com.krishna.infodeck.core.QrRules.PNG_PX, false
            ).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", f
        )
        val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            android.content.Intent.createChooser(i, "Share QR").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        null
    }
