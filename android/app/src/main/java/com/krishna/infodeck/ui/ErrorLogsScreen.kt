package com.krishna.infodeck.ui

import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sell
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Fmt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorLogsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val logs by vm.repo.errorLogFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            DeckHeader(
                title = "Error Logs",
                role = com.krishna.infodeck.core.Hue.ROSE,
                subtitle = if (logs.size == 1) "1 entry" else "${logs.size} entries",
                onBack = onBack,
                actions = {
                    HeaderIcon(Icons.Filled.Share, "Share") {
                        val text = logs.joinToString("\n") {
                            "${Fmt.dateTime(Fmt.fromMillis(it.ts))} [${it.tag}] ${it.message}${it.detail?.let { d -> " - $d" } ?: ""}"
                        }
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                                    }, "Share error logs"
                                )
                            )
                        }.onFailure { com.krishna.infodeck.core.Logger.e("ErrorLogs", "share failed", it) }
                    }
                    HeaderIcon(Icons.Filled.Delete, "Clear") { vm.clearErrors() }
                }
            )
        }
    ) { pad ->
        if (logs.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No entries - nothing has gone wrong.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                items(logs, key = { it.id }) { e ->
                    val rose = deck.hue(com.krishna.infodeck.core.Hue.ROSE)
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 7.dp)
                            .deckCard(deck, 14.dp, rose.glyph)
                    ) {
                        Column(Modifier.padding(start = 12.dp, end = 10.dp, top = 9.dp, bottom = 9.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(e.tag, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (deck.dark) rose.glyph else rose.onTint,
                                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(rose.tint).padding(horizontal = 7.dp, vertical = 2.dp))
                                Spacer(Modifier.weight(1f))
                                Text(Fmt.dateTime(Fmt.fromMillis(e.ts)), fontSize = 11.sp, color = Hint)
                            }
                            Text(e.message, fontSize = 12.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(top = 5.dp))
                            e.detail?.let { Text(it, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}
