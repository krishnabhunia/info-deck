package com.krishna.infodeck.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.BuildConfig
import com.krishna.infodeck.alarm.Notifications
import com.krishna.infodeck.core.Fmt
import kotlinx.coroutines.launch

const val RELEASE_DATE = "15-Oct-2026"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onErrorLogs: () -> Unit,
    onTextSize: () -> Unit = {},
    onTags: () -> Unit = {},
    onCategories: () -> Unit = {},
    onHome: () -> Unit = {},
    onUpcoming: () -> Unit = {}
) {
    val s by vm.settings.collectAsState()
    val errors by vm.errorCount.collectAsState()
    val fileBytes by vm.totalFileBytes.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showTime by remember { mutableStateOf(false) }
    var snoozeMenu by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDefaults by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var openGroup by remember { mutableStateOf<com.krishna.infodeck.core.SettingsGroup?>(null) }
    val hits = com.krishna.infodeck.core.SettingsIndex.search(query)
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.backup(uri) { ok ->
            scope.launch { snackbar.showSnackbar(if (ok) "Backup written" else "Backup failed - see Error Logs") }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) restoreUri = uri }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { snackbar.showSnackbar(if (granted) "Notifications allowed" else "Notifications still blocked") }
    }

    val notifOk = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    val exactOk = Notifications.canUseExactAlarms(context)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (searching) SlimTopBar(
                title = "", searching = true, query = query,
                onQuery = { query = it },
                onSearchClose = { searching = false; query = "" }
            ) else SlimTopBar(
                title = "Settings",
                subtitle = "InfoDeck " + BuildConfig.VERSION_NAME,
                onBack = onBack,
                onSearchOpen = { searching = true }
            )
        },
        bottomBar = {
            InfoBottomBar(selected = 2, overdue = 0, onHome = onHome, onUpcoming = onUpcoming, onSettings = {})
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)
        ) {
            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.ORGANISE,
                summary = "Categories and tags",
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.ORGANISE,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = {
                    openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.ORGANISE) null
                    else com.krishna.infodeck.core.SettingsGroup.ORGANISE
                }
            ) {
                SettingRow("Categories", "Add, rename, colour, reorder, delete", null) { onCategories() }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow("Tags", "Rename, merge, delete - no card is lost", null) { onTags() }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow("Group Home by", "Set from the View button on Home", s.groupBy.label) { }
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.REMINDERS,
                summary = "Ring " + Fmt.time(s.defaultRingMinute) + " - snooze " + snoozeLabel(s.snoozeMinutes),
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.REMINDERS,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.REMINDERS) null else com.krishna.infodeck.core.SettingsGroup.REMINDERS }
            ) {

                SettingRow("Default ring time", "Used by every new scheduler", Fmt.time(s.defaultRingMinute)) { showTime = true }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibrate", fontSize = 14.sp)
                        Text("Applies to new installs of the channel", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = s.vibrate, onCheckedChange = { vm.setVibrate(it) })
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Box {
                    SettingRow("Snooze length", "Snooze action on the notification", snoozeLabel(s.snoozeMinutes)) { snoozeMenu = true }
                    DropdownMenu(expanded = snoozeMenu, onDismissRequest = { snoozeMenu = false }) {
                        listOf(10, 30, 60, 180, -1).forEach { m ->
                            DropdownMenuItem(text = { Text(snoozeLabel(m)) }, onClick = { vm.setSnooze(m); snoozeMenu = false })
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Test ring", fontSize = 14.sp)
                        Text("Fires a sample notification now", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { vm.testRing() }) { Text("Ring now", fontWeight = FontWeight.Bold) }
                }
            }

            // merged into the group above
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Notifications", Modifier.weight(1f), fontSize = 14.sp)
                    if (notifOk) StatusOk("Granted") else {
                        StatusWarn("Blocked")
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            else openAppSettings(context)
                        }) { Text("Allow") }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Exact alarms", fontSize = 14.sp)
                        Text(
                            if (exactOk) "Rings at the exact minute"
                            else "Without this the ring lands in a few-minute window",
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (exactOk) StatusOk("Allowed") else {
                        StatusWarn("Not allowed")
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { openExactAlarmSettings(context) }) { Text("Allow") }
                    }
                }
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.LOOK,
                summary = "Text size " + com.krishna.infodeck.core.FriendlySize.nameFor(s.ui.textScale) + " - theme " + s.theme.lowercase(),
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.LOOK,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.LOOK) null else com.krishna.infodeck.core.SettingsGroup.LOOK }
            ) {
                SettingRow(
                    "Card density", "How much each card shows on Home",
                    s.density.label
                ) {
                    val next = com.krishna.infodeck.core.Density.values()[(s.density.ordinal + 1) % 3]
                    vm.setDensity(next)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tap to expand", fontSize = 14.sp)
                        Text("Open a card in place with its details", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = s.tapExpand, onCheckedChange = { vm.setTapExpand(it) })
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Box {
                    SettingRow("Theme", null, s.theme.lowercase().replaceFirstChar { it.uppercase() }) { themeMenu = true }
                    DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                        listOf("SYSTEM", "LIGHT", "DARK").forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { vm.setTheme(t); themeMenu = false }
                            )
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    "Text size", "Any size from 70 % to 200 %, applied live",
                    "${com.krishna.infodeck.core.ScaleRules.percent(s.ui.textScale)} %"
                ) { onTextSize() }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow("Default grouping", "Set from the Group by chip on Home", s.groupBy.label) { }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("One section open at a time", fontSize = 14.sp)
                        Text("Opening a section closes the previous one", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = s.singleOpen,
                        onCheckedChange = { vm.setGroupBy(s.groupBy, s.groupOrder, it) }
                    )
                }
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.TYPING,
                summary = (if (s.autoCaps) "Auto-capitalise on" else "Auto-capitalise off") + (if (s.autoCapsValues) " - all fields" else " - names only"),
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.TYPING,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.TYPING) null else com.krishna.infodeck.core.SettingsGroup.TYPING }
            ) {

                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-capitalise words", fontSize = 14.sp)
                        Text("Keyboard opens in caps for the first letter of every word", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = s.autoCaps, onCheckedChange = { vm.setAutoCaps(it) })
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Include data values", fontSize = 14.sp)
                        Text(
                            if (s.autoCapsValues) "All fields - names and values"
                            else "Names only - values like e-mails stay lower case",
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = s.autoCapsValues, enabled = s.autoCaps,
                        onCheckedChange = { vm.setAutoCapsValues(it) }
                    )
                }
            }

            // merged into the group above
            Column {
                SettingRow("Manage tags", "Rename, merge, delete - no card is ever deleted with a tag", null) { onTags() }
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.DATA,
                summary = "Files " + com.krishna.infodeck.data.FileRules.humanSize(fileBytes),
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.DATA,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.DATA) null else com.krishna.infodeck.core.SettingsGroup.DATA }
            ) {

                SettingRow("Backup to file", "JSON - cards, dates, tags and reminders (excludes files)", null) {
                    backupLauncher.launch("infodeck-backup-${System.currentTimeMillis()}.json")
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow("Restore from file", "Replace or merge, asked before restoring", null) {
                    restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    "Attached files", "PDF and Word files copied into InfoDeck",
                    com.krishna.infodeck.data.FileRules.humanSize(fileBytes)
                ) { }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    "Last backup", null,
                    if (s.lastBackupAt == 0L) "Never" else Fmt.date(Fmt.fromMillis(s.lastBackupAt).toLocalDate())
                ) { }
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.SECURITY,
                summary = if (s.appLock) "App lock on" else "App lock off",
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.SECURITY,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.SECURITY) null else com.krishna.infodeck.core.SettingsGroup.SECURITY }
            ) {

                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("App lock", fontSize = 14.sp)
                        Text("Fingerprint / device PIN on open", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = s.appLock, onCheckedChange = { vm.setAppLock(it) })
                }
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.DIAGNOSTICS,
                summary = errors.toString() + " error logs",
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.DIAGNOSTICS,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.DIAGNOSTICS) null else com.krishna.infodeck.core.SettingsGroup.DIAGNOSTICS }
            ) {

                SettingRow("Error Logs", "Logger.e entries - view, share, clear", "$errors") { onErrorLogs() }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                GitHubUpdatesSettings(s.betaUpdates, vm::setBetaUpdates)
            }

            SettingsGroupCard(
                group = com.krishna.infodeck.core.SettingsGroup.DANGER,
                summary = "Put settings back - Erase everything",
                open = openGroup == com.krishna.infodeck.core.SettingsGroup.DANGER,
                searching = query.isNotBlank(),
                hits = hits,
                onToggle = { openGroup = if (openGroup == com.krishna.infodeck.core.SettingsGroup.DANGER) null else com.krishna.infodeck.core.SettingsGroup.DANGER }
            ) {

                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { confirmDefaults = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Put settings back to normal", fontSize = 14.sp)
                        Text("Text size, theme, reminders and typing return to their defaults - asks first, cards untouched",
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Hint)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { confirmReset = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Erase everything", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Text("Every category, card, date, reminder and attached file. Type ERASE, then confirm",
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Hint)
                }
            }

            AboutMeCard()
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = s.defaultRingMinute / 60, initialMinute = s.defaultRingMinute % 60, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = { vm.setRingMinute(state.hour * 60 + state.minute); showTime = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { Column { TimePicker(state = state) } }
        )
    }

    if (confirmReset) {
        var typed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Erase everything?") },
            text = {
                Column {
                    Text("Every category, card, date and reminder is deleted. Settings are kept.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = typed, onValueChange = { typed = it },
                        label = { Text("Type ERASE to confirm") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = typed.trim() == "ERASE", onClick = {
                    confirmReset = false
                    vm.resetAll { scope.launch { snackbar.showSnackbar("Everything erased") } }
                }) { Text("Erase", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    if (confirmDefaults) ConfirmDialog(
        title = "Put settings back to normal?",
        body = "Text size returns to Standard, theme to System, ring time to 09:00, snooze to 1 hour and typing help back on. Your cards, dates, reminders and files are not touched.",
        keepText = "Keep my settings",
        goText = "Put back",
        danger = false,
        onKeep = { confirmDefaults = false },
        onGo = {
            confirmDefaults = false
            vm.restoreSettingsDefaults { scope.launch { snackbar.showSnackbar("Settings back to normal") } }
        }
    )

    val ru = restoreUri
    if (ru != null) AlertDialog(
        onDismissRequest = { restoreUri = null },
        title = { Text("Restore backup") },
        text = { Text("JSON backups exclude attached PDF/Word documents. Replace all permanently removes existing cards and attached files. Merge retains existing cards and files. Continue?") },
        confirmButton = {
            TextButton(onClick = {
                restoreUri = null
                vm.restore(ru, true) { n ->
                    scope.launch { snackbar.showSnackbar(if (n >= 0) "Replaced - $n cards restored" else "Restore failed - see Error Logs") }
                }
            }) { Text("Replace all") }
        },
        dismissButton = {
            TextButton(onClick = {
                restoreUri = null
                vm.restore(ru, false) { n ->
                    scope.launch { snackbar.showSnackbar(if (n >= 0) "Merged - $n cards added" else "Restore failed - see Error Logs") }
                }
            }) { Text("Merge") }
        }
    )
}

@Composable
private fun GitHubUpdatesSettings(includeBeta: Boolean, onBetaChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val manager = remember(context) { com.krishna.infodeck.update.UpdateManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var latest by remember(includeBeta) { mutableStateOf(manager.cached(includeBeta)) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(includeBeta) {
        checking = true
        try {
            latest = manager.check(force = false, includeBeta = includeBeta)
            status = if (latest == null) "InfoDeck is up to date" else "A newer version is available"
        } catch (e: Exception) {
            status = "Could not check GitHub: " + (e.message ?: "Network error")
        } finally {
            checking = false
        }
    }

    SettingRow(
        "Check for updates",
        "Search GitHub Releases for an updated Android APK",
        if (checking) "Checking…" else null
    ) {
        if (!checking) scope.launch {
            checking = true
            try {
                latest = manager.check(force = true, includeBeta = includeBeta)
                status = if (latest == null) "InfoDeck is up to date" else "A newer version is available"
            } catch (e: Exception) {
                status = "Could not check GitHub: " + (e.message ?: "Network error")
            } finally {
                checking = false
            }
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Include beta updates", fontSize = 14.sp)
            Text("Optional early releases from pull requests", fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = includeBeta, onCheckedChange = onBetaChange)
    }
    val available = latest
    if (available != null) {
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Update to v${available.version}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (available.beta) "Beta release" else "Stable release", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(enabled = !downloading, onClick = {
                scope.launch {
                    if (!manager.canInstall()) {
                        status = "Allow InfoDeck to install unknown apps, then tap Update again."
                        try { manager.requestInstallPermission() }
                        catch (e: Exception) { status = "Cannot open install permission settings" }
                    } else {
                        downloading = true
                        try {
                            val apk = manager.downloadApk(available)
                            manager.install(apk) // Android must ask the user to confirm.
                            status = "Confirm installation in Android"
                        } catch (e: Exception) {
                            status = "Update failed: " + (e.message ?: "Download error")
                        } finally {
                            downloading = false
                        }
                    }
                }
            }) {
                Text(if (downloading) "Downloading…" else "Update")
            }
        }
    }
    if (status.isNotBlank()) Text(status,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) { Column { content() } }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) {
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp), tint = Hint)
    }
}

@Composable
private fun StatusOk(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = Ok)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ok)
    }
}

@Composable
private fun StatusWarn(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
}

private fun snoozeLabel(m: Int): String = when (m) {
    10 -> "10 min"; 30 -> "30 min"; 60 -> "1 hour"; 180 -> "3 hours"; -1 -> "Tomorrow"
    else -> "$m min"
}

private fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openExactAlarmSettings(context: android.content.Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else openAppSettings(context)
    }
}


/** One collapsible Settings group (design 1.3.D1 C5). Opening one closes the previous. */
@Composable
private fun SettingsGroupCard(
    group: com.krishna.infodeck.core.SettingsGroup,
    summary: String,
    open: Boolean,
    searching: Boolean,
    hits: List<com.krishna.infodeck.core.SettingsRow>,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val matches = hits.filter { it.group == group }
    // While searching, a group opens itself when it holds a match and hides when it does not.
    if (searching && matches.isEmpty()) return
    val expanded = if (searching) true else open

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 50.dp).clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(group.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (searching) matches.joinToString(" - ") { it.title } else summary,
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    if (expanded) "Collapse" else "Expand",
                    Modifier.size(20.dp), tint = Hint
                )
            }
            if (expanded) Column(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) { content() }
        }
    }
}


/**
 * B3 — About Me, project standard "ultra-compact-B": header strip, identity row with social
 * icon buttons, feedback chips + Send, two-line footer. Never collapsible, sits last.
 */
@Composable
private fun AboutMeCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val accent = Primary
    var type by remember { mutableStateOf("Feedback") }
    var mailProblem by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(34.dp).background(accent.copy(alpha = 0.12f)).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) { Text("About Me", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = accent) }

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(accent),
                    contentAlignment = Alignment.Center
                ) { Text("KB", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Krishna Dipayan Bhunia", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Developer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SocialButton(accent, "in") {
                    com.krishna.infodeck.core.Logger.guard("About.linkedin", Unit) {
                        uriHandler.openUri("https://www.linkedin.com/in/krishnabhunia/")
                    }
                }
                Spacer(Modifier.width(6.dp))
                SocialButton(accent, "f") {
                    com.krishna.infodeck.core.Logger.guard("About.facebook", Unit) {
                        uriHandler.openUri("https://www.facebook.com/kdbhunia/")
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Suggestion", "Feedback", "Bug", "Error").forEach { t ->
                    val on = type == t
                    Box(
                        Modifier.height(28.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (on) accent else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { type = t }.padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                            color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(accent)
                        .clickable { mailProblem = FeedbackMail.open(context, type) }
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Email, null, Modifier.size(13.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(
                mailProblem ?: ("\u2709 opens your mail app \u00b7 " + FeedbackMail.TO),
                fontSize = 10.sp, color = if (mailProblem != null) MaterialTheme.colorScheme.error else Hint,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    .then(if (mailProblem != null) Modifier.clickable { copy(context, FeedbackMail.TO) } else Modifier)
            )

            Divider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("InfoDeck \u00b7 " + BuildConfig.VERSION_NAME, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Released on $RELEASE_DATE", fontSize = 11.sp, color = Hint)
            }
        }
    }
}

@Composable
private fun SocialButton(accent: Color, glyph: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = 0.12f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(glyph, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = accent) }
}

private fun copy(context: android.content.Context, text: String) {
    val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cb.setPrimaryClip(android.content.ClipData.newPlainText("InfoDeck", text))
}

/** Subject + nine-line context block, read live; never any card content. */
object FeedbackMail {
    const val TO = "kri.subsc@gmail.com"

    fun subject(type: String): String = "[InfoDeck " + BuildConfig.VERSION_NAME + "] " + type

    fun body(type: String, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): String {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm zzz", java.util.Locale.ENGLISH)
        return "\n\n" +
            "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n" +
            "App: InfoDeck\n" +
            "Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n" +
            "Report type: " + type + "\n" +
            "Date: " + runCatching { now.format(fmt) }.getOrDefault("unknown") + "\n" +
            "Device: " + (android.os.Build.MANUFACTURER ?: "unknown") + " " + (android.os.Build.MODEL ?: "unknown") + "\n" +
            "Android: " + (android.os.Build.VERSION.RELEASE ?: "unknown") + " (API " + android.os.Build.VERSION.SDK_INT + ")\n" +
            "Locale: " + java.util.Locale.getDefault().toString() + "\n\n" +
            "If you have a screenshot, please attach it."
    }

    /** Returns null when the mail app opened, or a message to show when nothing answered. */
    fun open(context: android.content.Context, type: String): String? =
        com.krishna.infodeck.core.Logger.guard("About.mail", "No mail app answered \u2014 tap to copy " + TO) {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + TO)).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject(type))
                putExtra(Intent.EXTRA_TEXT, body(type))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(i); null } catch (e: android.content.ActivityNotFoundException) {
                com.krishna.infodeck.core.Logger.e("About.mail", "no mail app", e)
                "No mail app answered \u2014 tap to copy " + TO
            }
        }
}
