package com.krishna.infodeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.data.RingLogEntity
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenCard: (Long) -> Unit,
    onHome: () -> Unit = onBack,
    onSettings: () -> Unit = {}
) {
    val state by vm.home.collectAsState()
    val history by vm.repo.ringLogFlow.collectAsState(initial = emptyList())
    var showHistory by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val today = LocalDate.now()

    val upcomingAll = state.allRows.filter { it.nextRingAt != null }.sortedBy { it.nextRingAt }
    val upcoming = if (query.isBlank()) upcomingAll else upcomingAll.filter {
        it.label.contains(query, true) || it.categoryName.contains(query, true) ||
            (it.nextRingRule?.contains(query, true) == true)
    }

    Scaffold(
        topBar = {
            if (searching) SlimTopBar(
                title = "", searching = true, query = query,
                onQuery = { query = it },
                onSearchClose = { searching = false; query = "" }
            ) else SlimTopBar(
                title = "Upcoming",
                subtitle = "${upcoming.size} reminders",
                onBack = onBack,
                onSearchOpen = { searching = true }
            )
        },
        bottomBar = {
            InfoBottomBar(
                selected = 1, overdue = 0,
                onHome = onHome, onUpcoming = {}, onSettings = onSettings
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                FilterStrip(
                    chips = listOf(
                        Triple("Upcoming", upcoming.size, !showHistory),
                        Triple("History", history.size, showHistory)
                    ),
                    onChip = { i -> showHistory = i == 1 },
                    trailingLabel = "Soonest",
                    onTrailing = { }
                )
            }

            if (!showHistory) {
                if (upcoming.isEmpty()) {
                    EmptyLine("No reminders scheduled.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        var lastMonth = ""
                        upcoming.forEach { row ->
                            val at = row.nextRingAt!!
                            val monthKey = Fmt.month(at.toLocalDate())
                            if (monthKey != lastMonth) {
                                lastMonth = monthKey
                                item(key = "h$monthKey") {
                                    Text(
                                        monthKey.uppercase(),
                                        fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp)
                                    )
                                }
                            }
                            item(key = "c${row.id}${at}") {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { onOpenCard(row.id) }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            Modifier.width(46.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(Fmt.day(at.toLocalDate()), fontSize = 19.sp,
                                                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                            Text(Fmt.dow(at.toLocalDate()), fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(Fmt.time(at.hour * 60 + at.minute), fontSize = 10.5.sp, color = Hint)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(row.label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                "${row.categoryName} - ${row.nextRingRule ?: ""}",
                                                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Pill(
                                            Fmt.relativeShort(at.toLocalDate(), today),
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (history.isEmpty()) EmptyLine("Nothing has rung yet.")
                else LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    items(history.size, key = { history[it].id }) { i -> HistoryRow(history[i], onOpenCard) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(log: RingLogEntity, onOpenCard: (Long) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { onOpenCard(log.cardId) }
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(log.cardLabel, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${log.categoryName} - ${log.ruleText}",
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    Fmt.dateTime(Fmt.fromMillis(log.firedAt)) + if (log.late) " - late" else "",
                    fontSize = 11.sp, color = Hint
                )
            }
            Pill(
                log.action?.name ?: "Shown",
                MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(text, Modifier.padding(top = 60.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
