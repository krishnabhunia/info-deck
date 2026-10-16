package com.krishna.infodeck.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.BuildConfig
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.DB_SCHEMA_VERSION
import com.krishna.infodeck.update.ReleaseUpdate
import com.krishna.infodeck.update.ReturnPlanner
import com.krishna.infodeck.update.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Return from a beta to a stable version (design 1.15.0.D2, screens A–C).
 * Shown only while a beta is installed. Path 1 reinstalls the stable in place (data kept);
 * path 2 saves a full backup and the stable installer, then opens Android's uninstall prompt.
 */
@Composable
fun ReturnToStableSection(vm: AppViewModel, lastStable: String, snackbar: SnackbarHostState) {
    if (!ReturnPlanner.isBeta(BuildConfig.VERSION_NAME)) return
    val context = LocalContext.current
    val manager = remember(context) { UpdateManager(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var targets by remember { mutableStateOf<List<ReleaseUpdate>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf<String?>(null) }
    // The chosen target, its downloaded APK and the plan for it.
    var chosen by remember { mutableStateOf<ReleaseUpdate?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }
    var plan by remember { mutableStateOf<ReturnPlanner.Plan?>(null) }
    var backupSaved by remember { mutableStateOf(false) }
    var apkSaved by remember { mutableStateOf(false) }

    LaunchedEffect(lastStable) {
        loading = true
        try {
            targets = ReturnPlanner.targets(manager.stableReleases(), lastStable.ifBlank { null })
            message = if (targets.isEmpty()) "No stable release found on GitHub" else ""
        } catch (e: Exception) {
            Logger.e("ReturnToStable", "could not list stable releases", e)
            message = "Could not check GitHub: " + (e.message ?: "Network error")
        } finally {
            loading = false
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) vm.fullBackup(uri) { ok ->
            backupSaved = backupSaved || ok
            scope.launch { snackbar.showSnackbar(if (ok) "Full backup saved" else "Backup failed - see Error Logs") }
        }
    }
    val apkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        val file = apk
        if (uri != null && file != null) scope.launch {
            val ok = manager.saveCopy(file, uri)
            apkSaved = apkSaved || ok
            snackbar.showSnackbar(if (ok) "Installer saved" else "Could not save the installer - see Error Logs")
        }
    }

    fun start(target: ReleaseUpdate) {
        if (busy != null) return
        scope.launch {
            busy = target.version
            message = ""
            try {
                val file = manager.downloadApk(target, allowOlder = true)
                val p = ReturnPlanner.forReturn(manager.installedCode, DB_SCHEMA_VERSION, manager.inspect(file))
                if (p is ReturnPlanner.Plan.Refuse) {
                    Logger.e("ReturnToStable", "return to ${target.version} refused: ${p.reason}")
                    message = p.reason
                } else {
                    chosen = target; apk = file; plan = p
                    backupSaved = false; apkSaved = false
                }
            } catch (e: Exception) {
                Logger.e("ReturnToStable", "download of ${target.version} failed", e)
                message = "Download failed: " + (e.message ?: "Network error")
            } finally {
                busy = null
            }
        }
    }

    Divider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("You are on a beta · ${BuildConfig.VERSION_NAME}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Go back to the stable version. Cards, reminders and files stay.", fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    targets.forEachIndexed { i, t ->
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        val isPrevious = i == 0 && t.version == lastStable
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (i == 0) "Return to v${t.version}" else "Newest stable v${t.version}", fontSize = 14.sp)
                Text(
                    when {
                        isPrevious -> "Your previous stable version"
                        i == 0 -> "Newest stable release"
                        else -> "Newer than your previous stable version"
                    },
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (i == 0) Button(enabled = busy == null, onClick = { start(t) }) {
                Text(if (busy == t.version) "Downloading…" else "Return")
            } else OutlinedButton(enabled = busy == null, onClick = { start(t) }) {
                Text(if (busy == t.version) "Downloading…" else "Return")
            }
        }
    }
    val note = if (loading) "Checking GitHub for stable releases…" else message
    if (note.isNotBlank()) Text(note, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    val target = chosen
    val file = apk
    val current = plan
    if (target != null && file != null && current != null) {
        val close = { chosen = null; apk = null; plan = null }
        when (current) {
            is ReturnPlanner.Plan.InPlace -> InPlaceSheet(target.version, onDismiss = close) {
                scope.launch {
                    try {
                        if (!manager.canInstall()) {
                            message = "Allow InfoDeck to install unknown apps, then tap Return again."
                            manager.requestInstallPermission()
                        } else {
                            // Q1 default: stop offering betas straight away after the return.
                            vm.setBetaUpdates(false)
                            manager.install(file) // Android asks the user to confirm.
                            message = "Confirm the installation in Android"
                        }
                    } catch (e: Exception) {
                        Logger.e("ReturnToStable", "could not start the in-place return", e)
                        message = "Could not start the installation: " + (e.message ?: "error")
                    }
                    close()
                }
            }
            is ReturnPlanner.Plan.Reinstall -> GuidedReinstallSheet(
                version = target.version, reason = current.reason,
                backupSaved = backupSaved, apkSaved = apkSaved, onDismiss = close,
                onSaveBackup = { backupLauncher.launch("infodeck-full-backup-${System.currentTimeMillis()}.zip") },
                onSaveApk = { apkLauncher.launch("InfoDeck_${target.version}.apk") },
                onUninstall = {
                    try {
                        manager.uninstallSelf()
                    } catch (e: Exception) {
                        Logger.e("ReturnToStable", "could not open the uninstall prompt", e)
                        message = "Could not open uninstall. Uninstall InfoDeck from Android Settings → Apps."
                    }
                }
            )
            is ReturnPlanner.Plan.Refuse -> close()
        }
    }
}

@Composable
private fun InPlaceSheet(version: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    InfoSheet(onDismiss = onDismiss) {
        Text("Return to v$version?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("The stable installer was downloaded from GitHub and checked: same app, and Android can install it over this beta.",
            fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        Notice("All cards, reminders, tags, settings and attached files stay on the phone.", good = true)
        Spacer(Modifier.height(8.dp))
        Notice("\"Include beta updates\" will be switched off so the beta is not offered again straight away.", good = false)
        StepRow(3, StepState.TODO, "Android asks you to confirm",
            "Tap Update or Install in the Android prompt. InfoDeck closes and reopens as v$version.")
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(50.dp)) { Text("Return to stable") }
        }
    }
}

@Composable
private fun GuidedReinstallSheet(
    version: String, reason: String, backupSaved: Boolean, apkSaved: Boolean,
    onDismiss: () -> Unit, onSaveBackup: () -> Unit, onSaveApk: () -> Unit, onUninstall: () -> Unit
) {
    InfoSheet(onDismiss = onDismiss) {
        Text("Reinstall v$version", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("$reason. Save everything first, then reinstall.", fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
        StepRow(1, if (backupSaved) StepState.DONE else StepState.TODO, "Save full backup",
            "ZIP with cards, tags, reminders and all attached files",
            action = if (backupSaved) "Save again" else "Save", onAction = onSaveBackup)
        StepRow(2, if (apkSaved) StepState.DONE else StepState.TODO, "Save the v$version installer",
            "InfoDeck_$version.apk - keep it in Downloads",
            action = if (apkSaved) "Save again" else "Save", onAction = onSaveApk)
        val ready = backupSaved && apkSaved
        StepRow(3, if (ready) StepState.TODO else StepState.WAIT, "Uninstall this beta",
            "Enabled only after steps 1 and 2. Removes the app and its data.",
            action = "Uninstall", danger = true, enabled = ready, onAction = onUninstall)
        StepRow(4, StepState.WAIT, "Install and restore",
            "Open the saved APK in Files → open InfoDeck → Settings → Backup → Restore from file → pick the ZIP → Replace all")
    }
}

private enum class StepState { TODO, DONE, WAIT }

@Composable
private fun StepRow(
    n: Int, state: StepState, title: String, detail: String,
    action: String? = null, danger: Boolean = false, enabled: Boolean = true, onAction: () -> Unit = {}
) {
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        val (bg, fg) = when (state) {
            StepState.DONE -> Color(0xFF12A150) to Color.White
            StepState.TODO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            StepState.WAIT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(Modifier.size(26.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Text(if (state == StepState.DONE) "✓" else "$n", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            if (danger) Button(
                onClick = onAction, enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(action) }
            else OutlinedButton(onClick = onAction, enabled = enabled) { Text(action) }
        }
    }
}

@Composable
private fun Notice(text: String, good: Boolean) {
    val bg = if (good) Color(0xFFDFF5E8) else Color(0xFFFDF0DA)
    val fg = if (good) Color(0xFF0B4D2A) else Color(0xFF5C3B00)
    Text(text, color = fg, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).padding(10.dp))
}
