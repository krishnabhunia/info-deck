package com.krishna.infodeck.ui

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Warning

import androidx.compose.ui.draw.clip

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
    onSettings: () -> Unit = {},
    onTags: () -> Unit = {},
    onNewCard: () -> Unit = {}
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
                onSearchClose = { searching = false; query = "" },
                role = com.krishna.infodeck.core.Hue.AMBER
            ) else DeckHeader(
                title = "Upcoming",
                role = com.krishna.infodeck.core.Hue.AMBER,
                subtitle = "${upcoming.size} reminders · soonest first",
                onBack = onBack,
                actions = { HueIconButton(Icons.Filled.Search, "Search", com.krishna.infodeck.core.Hue.AMBER, onClick = { searching = true }) },
                below = {
                    UpcomingTabs(upcoming.size, history.size, showHistory) { showHistory = it }
                }
            )
        },
        bottomBar = {
            InfoBottomBar(
                selected = 1, overdue = 0,
                onHome = onHome, onUpcoming = {}, onSettings = onSettings,
                onTags = onTags, onNew = onNewCard
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            val overdueRows = state.allRows.filter { it.isOverdue(today) }
            if (!showHistory && overdueRows.isNotEmpty()) OverdueBanner(overdueRows.first(), overdueRows.size, today, onOpenCard)
            Spacer(Modifier.height(4.dp))
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
                                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp)
                                    )
                                }
                            }
                            item(key = "c${row.id}${at}") { UpcomingItem(row, at, today, onOpenCard) }
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

/** Upcoming / History tabs inside the amber header band. */
@Composable
private fun UpcomingTabs(upcoming: Int, history: Int, showHistory: Boolean, onPick: (Boolean) -> Unit) {
    val p = deck
    val a = p.hue(com.krishna.infodeck.core.Hue.AMBER)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (p.dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.22f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(false to "Upcoming $upcoming", true to "History $history").forEach { (hist, label) ->
            val on = hist == showHistory
            Box(
                Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (!on) Color.Transparent else if (p.dark) a.tint else Color.White)
                    .then(if (on && p.dark) Modifier.border(1.dp, a.glyph, RoundedCornerShape(11.dp)) else Modifier)
                    .clickable { onPick(hist) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = when { on && p.dark -> a.glyph; on -> a.onTint; p.dark -> p.ink2; else -> Color.White })
            }
        }
    }
}

/** Rose banner for the first overdue card (design 1.15.0.D1 frame 13). */
@Composable
private fun OverdueBanner(row: com.krishna.infodeck.core.CardRow, count: Int, today: LocalDate, onOpenCard: (Long) -> Unit) {
    val p = deck
    val r = p.hue(com.krishna.infodeck.core.Hue.ROSE)
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(r.tint).border(1.dp, r.glyph.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onOpenCard(row.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, null, Modifier.size(20.dp), tint = r.glyph)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (count == 1) "${row.label} is overdue" else "${row.label} and ${count - 1} more are overdue",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            row.keyDate?.let {
                Text("${row.keyDateName} ${Fmt.date(it)} · ${Fmt.relativeShort(it, today)}", fontSize = 11.sp,
                    color = if (p.dark) r.glyph else r.onTint, maxLines = 1)
            }
        }
        Text("Open", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = if (p.dark) Color(0xFF0A0E1F) else Color.White,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(r.glyph).padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

@Composable
private fun UpcomingItem(row: com.krishna.infodeck.core.CardRow, at: java.time.LocalDateTime, today: LocalDate, onOpenCard: (Long) -> Unit) {
    val p = deck
    val a = p.hue(com.krishna.infodeck.core.Hue.AMBER)
    val catHue = com.krishna.infodeck.core.Hues.forCategory(row.colorIndex)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 7.dp).deckCard(p, 14.dp, p.hue(catHue).glyph)
            .clickable { onOpenCard(row.id) }.padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.width(50.dp).clip(RoundedCornerShape(11.dp)).background(a.tint).padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(Fmt.day(at.toLocalDate()), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (p.dark) a.glyph else a.onTint)
            Text(Fmt.dow(at.toLocalDate()), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (p.dark) a.glyph else a.onTint)
            Text(Fmt.time(at.hour * 60 + at.minute), fontSize = 9.5.sp, color = p.ink2)
        }
        Spacer(Modifier.width(9.dp))
        HueTile(glyphIcon(com.krishna.infodeck.core.Hues.glyphFor(row.categoryName)), catHue, size = 30)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(row.label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${row.categoryName} · ${row.nextRingRule ?: ""}", fontSize = 11.sp, color = p.ink2,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Pill(Fmt.relativeShort(at.toLocalDate(), today), a.tint, if (p.dark) a.glyph else a.onTint)
    }
}

@Composable
private fun HistoryRow(log: RingLogEntity, onOpenCard: (Long) -> Unit) {
    val p = deck
    val done = log.action?.name?.contains("DONE", true) == true
    val h = if (done) com.krishna.infodeck.core.Hue.GREEN else com.krishna.infodeck.core.Hue.SLATE
    Column(
        Modifier.fillMaxWidth().padding(bottom = 7.dp).deckCard(p, 14.dp, p.hue(h).glyph).clickable { onOpenCard(log.cardId) }
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
                p.hue(h).tint, if (p.dark) p.hue(h).glyph else p.hue(h).onTint
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
