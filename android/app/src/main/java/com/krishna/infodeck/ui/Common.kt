package com.krishna.infodeck.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.em
import com.krishna.infodeck.core.Hue
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UI STANDING RULE (all Krishna's Android apps): every edit popup / bottom sheet
 * reserves max(25 % of screen height, real nav-bar height + 24 dp) of clear space
 * at the bottom, so it can never overlap Back / Home / Recents.
 * Nav-bar height is measured ONCE from the activity decor view (SystemBars.navBottomDp),
 * never from insets read inside the sheet; sheets pass windowInsets bottom = 0.
 */
object SystemBars {
    @Volatile var navBottomDp: Int = 0
}

const val SHEET_CLEAR_PCT = 0.25f

@Composable
fun sheetBottomSpaceDp(): Int {
    val screenH = LocalConfiguration.current.screenHeightDp
    val quarter = (screenH * SHEET_CLEAR_PCT).toInt()
    val navPlus = SystemBars.navBottomDp + 24
    return maxOf(quarter, navPlus)
}

@Composable
fun SheetBottomSpace() {
    Spacer(Modifier.height(sheetBottomSpaceDp().dp))
}

/** windowInsets value every ModalBottomSheet in this app uses. */
fun zeroSheetInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)

/**
 * ONE shared editor chrome: Cancel (outlined) + Save (filled) + Delete slot when
 * editing, equal width, 50 dp.
 */
/**
 * THE standard action bar (design 1.2.D1 C2): Discard - Undo - Save.
 * Equal width, 50 dp x layout scale, Save carries the brand gradient.
 * Discard confirms whenever something is pending; Undo greys out at the bottom of the stack.
 */
/**
 * THE standard action bar (design 1.3.D2): two buttons only.
 * Left is "Cancel" while nothing is pending and "Discard" once something is —
 * the label change is the warning. Undo lives in snackbars, not here.
 */
@Composable
fun EditorSheetButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    saveText: String = "Save",
    cancelText: String? = null,
    onDelete: (() -> Unit)? = null,
    deleteText: String = "Delete",
    onUndo: (() -> Unit)? = null,
    undoEnabled: Boolean = false,
    dirty: Boolean = saveEnabled,
    modifier: Modifier = Modifier,
    /** N60: the rename sheet paints "Save…" amber when a confirmation follows. */
    saveBrush: androidx.compose.ui.graphics.Brush = BrandGradient
) {
    val h = scaledDp(50f)
    val leftLabel = cancelText ?: if (dirty) "Discard" else "Cancel"
    val destructive = dirty

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(h),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Filled.Close, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(leftLabel, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        Box(
            Modifier
                .weight(1f)
                .height(h)
                .clip(RoundedCornerShape(14.dp))
                .background(if (saveEnabled) saveBrush else androidx.compose.ui.graphics.SolidColor(Color(0xFFC7CEE4)))
                .clickable(enabled = saveEnabled) { onSave() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Save, null, Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(saveText, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
        }
    }

    if (onDelete != null) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().height(h),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(deleteText, fontWeight = FontWeight.Bold) }
    }
}

/** N53 — white 48 dp bar with a 3 dp gradient hairline. The colour lives in the icons now. */
@Composable
fun SlimTopBar(
    title: String,
    subtitle: String? = null,
    searching: Boolean = false,
    query: String = "",
    onQuery: (String) -> Unit = {},
    onSearchOpen: (() -> Unit)? = null,
    onSearchClose: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().height(scaledDp(48f)).padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searching) {
                IconButton(onClick = onSearchClose) {
                    Icon(Icons.Filled.ArrowBack, "Close search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SearchPill(query, onQuery, Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
            } else {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (subtitle != null) Text(
                        subtitle, fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (onSearchOpen != null) HueIconButton(Icons.Filled.Search, "Search", Hue.INDIGO, onSearchOpen)
                actions()
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(hue(Hue.INDIGO), hue(Hue.VIOLET), hue(Hue.TEAL))
                    )
                )
        )
    }
}

/** A 38 dp tinted icon button — the colour form the top bar uses. */
@Composable
fun HueIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    h: Hue,
    onClick: () -> Unit,
    badge: Boolean = false
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tint(h))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(18.dp), tint = onTint(h))
        if (badge) Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(hue(Hue.ROSE))
        )
    }
}

/** Hue lookups that follow the theme, so dark mode never needs a second table. */
@Composable
fun hue(h: Hue): Color =
    if (androidx.compose.foundation.isSystemInDarkTheme()) Color(h.darkGlyph) else Color(h.argb)

@Composable
fun tint(h: Hue): Color =
    if (androidx.compose.foundation.isSystemInDarkTheme()) Color(h.darkTint) else Color(h.tint)

@Composable
fun onTint(h: Hue): Color =
    if (androidx.compose.foundation.isSystemInDarkTheme()) Color(h.darkGlyph) else Color(h.onTint)

/** N55 — the gradient tile that leads every card and section row. */
@Composable
fun HueTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    h: Hue,
    size: Int = 38,
    flat: Boolean = false,
    modifier: Modifier = Modifier
) {
    val base = hue(h)
    Box(
        modifier
            .size(scaledDp(size.toFloat()))
            .clip(RoundedCornerShape((size / 2.9f).dp))
            .then(
                if (flat) Modifier.background(tint(h))
                else Modifier.background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(base.copy(alpha = 0.86f), base))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size((size * 0.46f).dp), tint = if (flat) onTint(h) else Color.White)
    }
}

/** N56 — the glyph a category shows on its tile. */
fun glyphIcon(key: String): androidx.compose.ui.graphics.vector.ImageVector = when (key) {
    "shield" -> Icons.Filled.Shield
    "car" -> Icons.Filled.DirectionsCar
    "laptop" -> Icons.Filled.Laptop
    "id" -> Icons.Filled.Badge
    "wallet" -> Icons.Filled.CreditCard
    "health" -> Icons.Filled.Favorite
    "wifi" -> Icons.Filled.Wifi
    "receipt" -> Icons.Filled.Receipt
    else -> Icons.Filled.Style
}

/** 30 dp filter strip: count chips (zeros hidden) plus a right-aligned button. */
@Composable
fun FilterStrip(
    chips: List<Triple<String, Int, Boolean>>,
    onChip: (Int) -> Unit,
    trailingLabel: String,
    trailingBadge: Int = 0,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Tune,
    onTrailing: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        chips.forEachIndexed { i, (label, count, on) ->
            val chipHue = when (label) {
                "Overdue" -> Hue.ROSE
                "Month" -> Hue.AMBER
                "No alarm" -> Hue.SLATE
                else -> Hue.INDIGO
            }
            val alert = label == "Overdue"
            Row(
                Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (on) hue(chipHue) else tint(chipHue))
                    .clickable { onChip(i) }
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$count",
                    fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (on) Color.White else onTint(chipHue)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    color = if (on) Color.White else onTint(chipHue)
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Row(
            Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onTrailing() }
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(trailingIcon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(trailingLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (trailingBadge > 0) {
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) { Text("$trailingBadge", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            }
        }
    }
}

/** The slim 50 dp bottom bar — icons only, the selected tab shows its word. */
@Composable
fun InfoBottomBar(selected: Int, overdue: Int, onHome: () -> Unit, onUpcoming: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(scaledDp(58f))
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarItem(Icons.Filled.Home, "Home", selected == 0, overdue, onHome)
        BarItem(Icons.Filled.DateRange, "Upcoming", selected == 1, 0, onUpcoming)
        BarItem(Icons.Filled.Settings, "Settings", selected == 2, 0, onSettings)
    }
}

@Composable
private fun BarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, on: Boolean, badge: Int, onClick: () -> Unit
) {
    val h = when (label) {
        "Home" -> Hue.INDIGO
        "Upcoming" -> Hue.AMBER
        else -> Hue.TEAL
    }
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(if (on) Modifier.background(tint(h)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, Modifier.size(19.dp), tint = if (on) onTint(h) else hue(h))
            if (badge > 0) Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-2).dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(hue(Hue.ROSE))
                    .padding(horizontal = 4.dp)
            ) { Text("$badge", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
            color = if (on) onTint(h) else Hint
        )
    }
}

// ---- scaling helpers (restored: the 1.2 set, unchanged) ----

/** base sp -> the size this role should render at, honouring the per-role offset. */
@Composable
fun roleSp(base: Float, role: com.krishna.infodeck.core.TextRole): androidx.compose.ui.unit.TextUnit {
    val ui = com.krishna.infodeck.core.LocalUiScale.current
    // The density already carries textScale, so only the role offset is applied here.
    return (base * (1f + ui.offset(role))).sp
}

@Composable
fun rowMinHeight(base: Float): androidx.compose.ui.unit.Dp {
    val ui = com.krishna.infodeck.core.LocalUiScale.current
    return ui.pad(base).dp
}

@Composable
fun scaledDp(base: Float): androidx.compose.ui.unit.Dp {
    val ui = com.krishna.infodeck.core.LocalUiScale.current
    return ui.dp(base).dp
}

@Composable
fun bodyWeight(): FontWeight = when (com.krishna.infodeck.core.LocalUiScale.current.weight) {
    com.krishna.infodeck.core.TextWeightSetting.NORMAL -> FontWeight.Normal
    com.krishna.infodeck.core.TextWeightSetting.MEDIUM -> FontWeight.Medium
    com.krishna.infodeck.core.TextWeightSetting.BOLD -> FontWeight.Bold
}

/**
 * Auto-capitalise words (N31). Names always follow the setting; values follow it only
 * when the scope is "All fields", because IDs and e-mails live there.
 */
fun capsOptions(
    enabled: Boolean,
    isValueField: Boolean = false,
    valuesIncluded: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
): androidx.compose.foundation.text.KeyboardOptions {
    val on = enabled && (!isValueField || valuesIncluded)
    return androidx.compose.foundation.text.KeyboardOptions(
        capitalization = if (on) androidx.compose.ui.text.input.KeyboardCapitalization.Words
        else androidx.compose.ui.text.input.KeyboardCapitalization.None,
        keyboardType = keyboardType
    )
}

// ---- composables restored after the 1.6 chrome rewrite (unchanged behaviour) ----

/** Small uppercase label above a group of rows. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.06.em,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 3.dp, top = 13.dp, bottom = 7.dp)
    )
}

/** A tinted rounded label. */
@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = content,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** The category colour, as a dot. */
@Composable
fun ColorDot(color: Color, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 2).dp))
            .background(color)
    )
}

/** Countdown ring on every card — green, amber, rose as the date closes. */
@Composable
fun CountdownRing(daysLeft: Long?, modifier: Modifier = Modifier, compact: Boolean = false, sizeDp: Float = 44f) {
    val band = com.krishna.infodeck.core.RingMath.band(daysLeft)
    val h = when (band) {
        com.krishna.infodeck.core.RingMath.Band.OVERDUE -> Hue.ROSE
        com.krishna.infodeck.core.RingMath.Band.SOON -> Hue.AMBER
        com.krishna.infodeck.core.RingMath.Band.OK -> Hue.GREEN
        com.krishna.infodeck.core.RingMath.Band.NONE -> Hue.SLATE
    }
    val colour = hue(h)
    val fraction = com.krishna.infodeck.core.RingMath.fraction(daysLeft)
    val size = scaledDp(sizeDp)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(size)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.5.dp.toPx())
            drawArc(color = colour.copy(alpha = 0.16f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = colour, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val n = com.krishna.infodeck.core.RingMath.number(daysLeft)
            if (n.isNotBlank()) Text(n, fontSize = if (compact) 9.5.sp else 12.5.sp, fontWeight = FontWeight.ExtraBold, color = colour, maxLines = 1)
            if (!compact) Text(
                com.krishna.infodeck.core.RingMath.caption(daysLeft),
                fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Ink2, maxLines = 1
            )
        }
    }
}

/** Soft rounded icon tile used by settings rows and dialogs (kept from 1.2). */
@Composable
fun IconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    container: Color,
    size: Int = 34,
    gradient: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(scaledDp(size.toFloat()))
            .clip(RoundedCornerShape((size / 2.8f).dp))
            .then(
                if (gradient) Modifier.background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.72f)))
                ) else Modifier.background(container)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size((size * 0.5f).dp), tint = if (gradient) Color.White else tint)
    }
}


/** B12 — a 38 dp pill in the indigo tint: focus + keyboard on open, round clear inside, Search hides the keyboard. */
@Composable
fun SearchPill(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
    Row(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(tint(Hue.INDIGO))
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, Modifier.size(17.dp), tint = hue(Hue.INDIGO))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search everything on your cards", fontSize = 13.5.sp, color = Hint, maxLines = 1)
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { onQuery(it.take(com.krishna.infodeck.core.SearchRules.MAX_QUERY)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(hue(Hue.INDIGO)),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        if (query.isNotEmpty()) Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface)
                .clickable { onQuery("") },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Close, "Clear", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
