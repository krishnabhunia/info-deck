package com.krishna.infodeck.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The whole text-sizing model, in one place (design 1.1.D2 C1).
 *
 *  final sp   = base sp * textScale * (1 + roleOffset), clamped 8..46
 *  layout dp  = base dp * (1 + (textScale - 1) * 0.6), clamped 0.80..1.60
 */
object ScaleRules {
    const val MIN = 0.70f
    const val MAX = 2.00f
    const val DAMPING = 0.6f
    const val LAYOUT_MIN = 0.80f
    const val LAYOUT_MAX = 1.60f
    const val SP_MIN = 8f
    const val SP_MAX = 46f
    const val OFFSET_LIMIT = 0.30f

    val PRESETS = listOf(0.85f, 1.00f, 1.15f, 1.30f, 1.50f, 2.00f)

    fun clampScale(v: Float): Float = if (v.isNaN()) 1f else v.coerceIn(MIN, MAX)
    fun clampOffset(v: Float): Float = if (v.isNaN()) 0f else v.coerceIn(-OFFSET_LIMIT, OFFSET_LIMIT)

    fun layoutScale(textScale: Float): Float =
        (1f + (clampScale(textScale) - 1f) * DAMPING).coerceIn(LAYOUT_MIN, LAYOUT_MAX)

    fun sp(baseSp: Float, textScale: Float, roleOffset: Float): Float =
        (baseSp * clampScale(textScale) * (1f + clampOffset(roleOffset))).coerceIn(SP_MIN, SP_MAX)

    fun percent(scale: Float): Int = Math.round(clampScale(scale) * 100f)

    fun fromPercent(p: Int): Float = clampScale(p / 100f)

    /** Icons follow the layout, but never below 18 dp or above 40 dp. */
    fun iconDp(baseDp: Float, textScale: Float): Float =
        (baseDp * layoutScale(textScale)).coerceIn(18f, 40f)

    /** Buttons follow the layout with a 44 dp accessibility floor. */
    fun buttonDp(baseDp: Float, textScale: Float): Float =
        (baseDp * layoutScale(textScale)).coerceAtLeast(44f)
}

enum class TextRole { TITLE, SECONDARY, CHIP, HEADER, SHEET, SETTINGS }

enum class RowSpacing(val label: String, val factor: Float) {
    COMPACT("Compact", 0.85f), COMFORTABLE("Comfortable", 1.0f), SPACIOUS("Spacious", 1.2f)
}

enum class TextWeightSetting(val label: String) { NORMAL("Normal"), MEDIUM("Medium"), BOLD("Bold") }

data class UiScale(
    val textScale: Float = 1f,
    val offsets: Map<TextRole, Float> = emptyMap(),
    val spacing: RowSpacing = RowSpacing.COMFORTABLE,
    val weight: TextWeightSetting = TextWeightSetting.NORMAL
) {
    val layout: Float get() = ScaleRules.layoutScale(textScale)
    fun offset(role: TextRole): Float = ScaleRules.clampOffset(offsets[role] ?: 0f)
    fun sp(base: Float, role: TextRole): Float = ScaleRules.sp(base, textScale, offset(role))
    fun pad(base: Float): Float = base * layout * spacing.factor
    fun dp(base: Float): Float = base * layout
}

val LocalUiScale = staticCompositionLocalOf { UiScale() }
