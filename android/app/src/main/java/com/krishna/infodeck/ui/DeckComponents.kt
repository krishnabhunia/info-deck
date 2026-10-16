package com.krishna.infodeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.infodeck.core.Hue
import com.krishna.infodeck.core.GreetingRules
import com.krishna.infodeck.core.NavStyleRules

/** True inside a coloured header band: icon buttons switch to translucent white. */
val LocalOnHeader = compositionLocalOf { false }

/** The colour text and icons take on a header band of [role]. */
@Composable
fun headerInk(): Color = if (deck.dark) deck.ink else Color.White

@Composable
private fun headerBrush(role: Hue): Brush {
    val p = deck
    val s = p.hue(role)
    return if (p.dark) Brush.verticalGradient(listOf(Color(0xFF0D1226), Color(0xFF0D1226)))
    else Brush.linearGradient(listOf(s.glyph, s.glyph2))
}

/** Neon paints a soft glow of the role colour into the top-left corner of the band. */
private fun Modifier.neonGlow(colour: Color, on: Boolean): Modifier = if (!on) this else drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(colour.copy(alpha = 0.38f), Color.Transparent),
            center = Offset(0f, 0f), radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f, center = Offset(0f, 0f)
    )
}

/** Aurora and Sunrise lay a soft light circle in the top-right corner of a coloured band. */
private fun Modifier.sheen(on: Boolean): Modifier = if (!on) this else drawBehind {
    val r = size.height * 1.1f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(size.width - r * 0.2f, -r * 0.25f), radius = r),
        radius = r, center = Offset(size.width - r * 0.2f, -r * 0.25f)
    )
}

/**
 * 2.0 screen header (design 1.15.0.D1 · every screen): a band in the screen's role colour —
 * a gradient in Aurora and Sunrise, a glowing dark band with a gradient title in Neon.
 * The status bar is painted to continue the band.
 */
@Composable
fun DeckHeader(
    title: String,
    role: Hue = Hue.INDIGO,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backIcon: ImageVector = Icons.Filled.ArrowBack,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
    below: (@Composable () -> Unit)? = null
) {
    val p = deck
    val s = p.hue(role)
    val ink = headerInk()
    StatusBarColour(if (p.dark) Color(0xFF0D1226) else s.glyph)
    val radius = if (p.dark) 0.dp else 24.dp
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = radius, bottomEnd = radius))
            .background(headerBrush(role))
            .neonGlow(s.glyph, p.dark)
            .sheen(!p.dark)
            .then(if (p.dark) Modifier.drawBehind {
                drawLine(p.line, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 2f)
            } else Modifier)
            .padding(bottom = if (below != null) 12.dp else 8.dp)
    ) {
        CompositionLocalProvider(LocalOnHeader provides true, LocalContentColor provides ink) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = scaledDp(54f)).padding(start = 4.dp, end = 6.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) IconButton(onClick = onBack) { Icon(backIcon, backLabel, tint = ink) }
                else Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    HeaderTitle(title, role, 19f)
                    if (subtitle != null) Text(
                        subtitle, fontSize = 11.sp, color = ink.copy(alpha = 0.85f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                actions()
            }
            if (below != null) Box(Modifier.padding(horizontal = 14.dp)) { below() }
        }
    }
}

/** Title text — white on colour, or the role gradient in Neon. */
@Composable
fun HeaderTitle(text: String, role: Hue, size: Float, modifier: Modifier = Modifier) {
    val p = deck
    val s = p.hue(role)
    val style = if (p.dark) TextStyle(brush = Brush.linearGradient(listOf(s.glyph, s.glyph2)))
    else TextStyle(color = Color.White)
    Text(
        text, style = style, fontFamily = p.heading, fontSize = size.sp,
        fontWeight = if (p.heading == FredokaFamily) FontWeight.SemiBold else FontWeight.ExtraBold,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier
    )
}

/** Round icon button for use inside a header band. */
@Composable
fun HeaderIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, label, tint = headerInk()) }
}

/** The search pill drawn inside a hero or header. Tapping it opens search. */
@Composable
fun HeaderSearchPill(hint: String, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    val p = deck
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .shadow(if (p.dark) 0.dp else 6.dp, RoundedCornerShape(21.dp), ambientColor = p.brand.first(), spotColor = p.brand.first())
            .clip(RoundedCornerShape(21.dp))
            .background(if (p.dark) Color.White.copy(alpha = 0.06f) else p.surface)
            .then(if (p.dark) Modifier.border(1.dp, p.outline, RoundedCornerShape(21.dp)) else Modifier)
            .clickable { onClick() }
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, Modifier.size(18.dp), tint = p.ink2)
        Spacer(Modifier.width(8.dp))
        Text(hint, fontSize = 13.sp, color = p.hint, maxLines = 1, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** One stat on the Home hero — tapping it toggles the matching filter. */
@Composable
fun HeroStat(value: Int, label: String, h: Hue, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = deck
    val s = p.hue(h)
    val shape = RoundedCornerShape(14.dp)
    when {
        !p.heroOnColour -> Row(
            modifier.clip(shape).background(if (on) s.tint else p.surface)
                .border(if (on) 1.5.dp else 1.dp, if (on) s.glyph else p.line, shape)
                .clickable { onClick() }.padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(s.glyph, s.glyph2))),
                contentAlignment = Alignment.Center
            ) { Text("$value", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1) }
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = p.ink, maxLines = 2, lineHeight = 12.sp)
        }
        p.dark -> Column(
            modifier.clip(shape).background(if (on) s.tint else Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (on) s.glyph else p.outline, shape)
                .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text("$value", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = s.glyph, maxLines = 1)
            Text(label, fontSize = 10.5.sp, color = p.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        else -> Column(
            modifier.clip(shape).background(if (on) Color.White else Color.White.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
                .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text("$value", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = if (on) s.onTint else Color.White, maxLines = 1)
            Text(label, fontSize = 10.5.sp, color = if (on) s.onTint else Color.White.copy(alpha = 0.92f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Home hero (design 1.15.0.D1 frame 02): title, counts, search pill and the three stats.
 * Aurora — gradient band; Neon — glowing dark band; Sunrise — greeting on the page.
 */
@Composable
fun DeckHomeHero(
    total: Int,
    overdue: Int,
    onSearch: () -> Unit,
    onFilters: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    stats: @Composable RowScope.() -> Unit
) {
    val p = deck
    val subtitle = if (overdue > 0) "$total cards · $overdue need attention" else "$total cards · all on track"
    StatusBarColour(if (p.dark) Color(0xFF0D1226) else if (p.heroOnColour) p.brand.first() else p.bg)
    val shape = RoundedCornerShape(bottomStart = if (p.heroOnColour && !p.dark) 28.dp else 0.dp,
        bottomEnd = if (p.heroOnColour && !p.dark) 28.dp else 0.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                when {
                    p.dark -> Modifier.background(Color(0xFF0D1226)).neonGlow(p.brand.first(), true)
                        .drawBehind {
                            drawCircle(Brush.radialGradient(listOf(p.brand.last().copy(alpha = 0.32f), Color.Transparent),
                                center = Offset(size.width, 0f), radius = size.width * 0.7f),
                                radius = size.width * 0.7f, center = Offset(size.width, 0f))
                        }
                    p.heroOnColour -> Modifier.background(Brush.linearGradient(p.brand)).sheen(true)
                    else -> Modifier.background(p.bg)
                }
            )
            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 14.dp)
    ) {
        val ink = if (p.heroOnColour && !p.dark) Color.White else p.ink
        CompositionLocalProvider(LocalOnHeader provides p.heroOnColour, LocalContentColor provides ink) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!p.heroOnColour) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(p.brand)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Home, null, Modifier.size(19.dp), tint = Color.White) }
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (!p.heroOnColour) Text(GreetingRules.now(), fontSize = 11.sp, color = p.ink2)
                    if (p.dark) HeaderTitle(if (p.heroOnColour) "Your cards" else "Your wallet", Hue.INDIGO, 22f)
                    else Text(
                        if (p.heroOnColour) "Your cards" else "Your wallet",
                        fontFamily = p.heading, fontSize = 22.sp, color = ink,
                        fontWeight = if (p.heading == FredokaFamily) FontWeight.SemiBold else FontWeight.ExtraBold
                    )
                    if (p.heroOnColour) Text(subtitle, fontSize = 11.sp, color = ink.copy(alpha = 0.85f), maxLines = 1)
                }
                actions()
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(end = 10.dp)) {
                HeaderSearchPill("Search every field…", onSearch) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(p.hue(Hue.INDIGO).tint).clickable { onFilters() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Tune, "View and filters", Modifier.size(16.dp), tint = p.hue(Hue.INDIGO).glyph) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(end = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { stats() }
        }
    }
}

/** New-card button: floating in Aurora and Neon; Sunrise puts it in the bottom bar instead. */
@Composable
fun DeckFab(onClick: () -> Unit) {
    val p = deck
    if (!NavStyleRules.showsFloatingNew(p.nav.name)) return
    Row(
        Modifier
            .height(50.dp)
            .shadow(10.dp, RoundedCornerShape(25.dp), ambientColor = p.brand.first(), spotColor = p.brand.first())
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(p.brand))
            .clickable { onClick() }
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, "New card", Modifier.size(24.dp), tint = p.onBrand)
        Spacer(Modifier.width(6.dp))
        Text("New card", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.onBrand)
    }
}

/**
 * 2.0 bottom bar — Home · Upcoming · Tags · Settings.
 * Aurora: floating pill with a gradient bubble on the active tab.
 * Neon: glass bar, the active tab glows with an underline.
 * Sunrise: curved bar with a raised centre New-card button.
 */
@Composable
fun DeckBottomBar(
    selected: Int,
    overdue: Int,
    onHome: () -> Unit,
    onUpcoming: () -> Unit,
    onTags: () -> Unit,
    onSettings: () -> Unit,
    onNew: () -> Unit
) {
    val p = deck
    val items = listOf(
        Triple(Icons.Filled.Home, "Home", onHome),
        Triple(Icons.Filled.DateRange, "Upcoming", onUpcoming),
        Triple(Icons.Filled.Sell, "Tags", onTags),
        Triple(Icons.Filled.Settings, "Settings", onSettings)
    )
    val barMod = when (p.nav) {
        NavStyle.PILL -> Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = p.brand.first(), spotColor = p.brand.first())
            .clip(RoundedCornerShape(28.dp))
            .background(p.surface)
        NavStyle.GLASS -> Modifier
            .background(Color(0xF00D1226))
            .drawBehind { drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 2f) }
        NavStyle.CENTER -> Modifier
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), ambientColor = p.brand.first(), spotColor = p.brand.first())
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(p.surface)
    }
    Box(Modifier.fillMaxWidth().background(if (p.nav == NavStyle.PILL) p.bg else Color.Transparent)) {
        Row(
            barMod.fillMaxWidth().height(scaledDp(if (p.nav == NavStyle.CENTER) 64f else 60f)).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (icon, label, go) ->
                if (p.nav == NavStyle.CENTER && i == 2) Spacer(Modifier.width(64.dp))
                DeckNavItem(icon, label, selected == i, if (i == 0) overdue else 0, go, Modifier.weight(1f))
            }
        }
        if (p.nav == NavStyle.CENTER) Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp)
                .size(56.dp)
                .shadow(10.dp, CircleShape, ambientColor = p.brand.first(), spotColor = p.brand.first())
                .clip(CircleShape)
                .background(Brush.linearGradient(p.brand))
                .border(4.dp, p.bg, CircleShape)
                .clickable { onNew() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, "New card", Modifier.size(28.dp), tint = Color.White) }
    }
}

@Composable
private fun DeckNavItem(icon: ImageVector, label: String, on: Boolean, badge: Int, onClick: () -> Unit, modifier: Modifier) {
    val p = deck
    val active = p.hue(Hue.INDIGO)
    val idle = p.ink2
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    when {
                        !on -> Modifier
                        p.nav == NavStyle.PILL -> Modifier.background(Brush.linearGradient(listOf(p.brand.first(), p.brand.last())))
                        p.nav == NavStyle.CENTER -> Modifier.background(active.tint)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, label, Modifier.size(21.dp),
                tint = when {
                    on && p.nav == NavStyle.PILL -> Color.White
                    on -> active.glyph
                    else -> idle
                }
            )
            if (badge > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = (-1).dp)
                    .clip(RoundedCornerShape(7.dp)).background(p.hue(Hue.ROSE).glyph).padding(horizontal = 4.dp)
            ) { Text("$badge", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1,
            color = if (on) active.glyph.let { if (p.nav == NavStyle.PILL) p.hue(Hue.INDIGO).onTint else it } else idle)
        if (on && p.nav == NavStyle.GLASS) Box(
            Modifier.padding(top = 2.dp).size(width = 18.dp, height = 3.dp)
                .shadow(6.dp, RoundedCornerShape(2.dp), ambientColor = active.glyph, spotColor = active.glyph)
                .clip(RoundedCornerShape(2.dp)).background(active.glyph)
        )
    }
}

/** A coloured section label, e.g. "Data", with a small role tile. */
@Composable
fun RoleLabel(text: String, h: Hue, icon: ImageVector, count: Int? = null, modifier: Modifier = Modifier) {
    val s = deck.hue(h)
    Row(modifier.padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HueTile(icon, h, size = 24)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (deck.dark) s.glyph else s.onTint,
            modifier = Modifier.weight(1f))
        if (count != null) Text(
            "$count", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = deck.ink2,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(deck.surface2).padding(horizontal = 7.dp, vertical = 1.dp)
        )
    }
}

/** Surface card used across screens: white/glass with the theme's hairline. */
fun Modifier.deckCard(p: DeckPalette, radius: Dp = 16.dp, accent: Color? = null): Modifier =
    this.clip(RoundedCornerShape(radius))
        .background(p.surface)
        .border(1.dp, p.line, RoundedCornerShape(radius))
        .then(if (accent != null && p.dark) Modifier.drawBehind {
            drawRect(accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
        } else Modifier)

/** 2.0 lock screen (design 1.15.0.D1 frame 01): brand band, app tile, fingerprint target, PIN fallback. */
@Composable
fun DeckLockScreen(onUnlock: () -> Unit) {
    val p = deck
    val ink = if (p.dark) p.ink else Color.White
    StatusBarColour(if (p.dark) p.bg else p.brand.first())
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                if (p.dark) Brush.verticalGradient(listOf(p.bg, Color(0xFF151036), p.bg))
                else Brush.linearGradient(p.brand)
            )
            .neonGlow(p.brand.first(), p.dark)
            .sheen(!p.dark)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp))
                .background(if (p.dark) p.hue(Hue.INDIGO).tint else Color.White.copy(alpha = 0.2f))
                .border(1.dp, if (p.dark) p.hue(Hue.INDIGO).glyph else Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Lock, null, Modifier.size(38.dp), tint = if (p.dark) p.hue(Hue.INDIGO).glyph else Color.White) }
        Spacer(Modifier.height(16.dp))
        Text("InfoDeck is locked", fontFamily = p.heading, fontSize = 23.sp, color = ink,
            fontWeight = if (p.heading == FredokaFamily) FontWeight.SemiBold else FontWeight.ExtraBold)
        Text("Your cards stay on this phone only", fontSize = 12.5.sp, color = ink.copy(alpha = 0.85f))
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.size(96.dp)
                .shadow(if (p.dark) 18.dp else 0.dp, CircleShape, ambientColor = p.brand.first(), spotColor = p.brand.first())
                .clip(CircleShape)
                .background(if (p.dark) p.hue(Hue.INDIGO).tint else Color.White.copy(alpha = 0.18f))
                .border(2.dp, if (p.dark) p.hue(Hue.INDIGO).glyph else Color.White.copy(alpha = 0.4f), CircleShape)
                .clickable { onUnlock() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Fingerprint, "Unlock", Modifier.size(54.dp), tint = if (p.dark) p.hue(Hue.INDIGO).glyph else Color.White) }
        Spacer(Modifier.height(12.dp))
        Text("Touch the sensor to unlock", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = ink)
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.height(42.dp).clip(RoundedCornerShape(21.dp))
                .background(if (p.dark) p.surface else Color.White.copy(alpha = 0.18f))
                .border(1.dp, if (p.dark) p.outline else Color.White.copy(alpha = 0.4f), RoundedCornerShape(21.dp))
                .clickable { onUnlock() }.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Pin, null, Modifier.size(18.dp), tint = ink)
            Spacer(Modifier.width(8.dp))
            Text("Use device PIN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink)
        }
        Spacer(Modifier.height(48.dp))
        Text("Offline · no account · no cloud", fontSize = 11.sp, color = ink.copy(alpha = 0.75f))
    }
}
