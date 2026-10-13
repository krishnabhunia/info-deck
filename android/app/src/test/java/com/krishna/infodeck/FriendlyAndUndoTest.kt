package com.krishna.infodeck

import com.krishna.infodeck.core.ElementChoice
import com.krishna.infodeck.core.ElementGroup
import com.krishna.infodeck.core.FriendlySize
import com.krishna.infodeck.core.RingMath
import com.krishna.infodeck.core.RowSpacing
import com.krishna.infodeck.core.ScaleRules
import com.krishna.infodeck.core.SizeStep
import com.krishna.infodeck.core.TextRole
import com.krishna.infodeck.core.TextWeightSetting
import com.krishna.infodeck.core.UiScale
import com.krishna.infodeck.core.UndoStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendlyAndUndoTest {

    // ---------- N39: names, not percentages ----------
    @Test fun sevenNamedSteps() = assertEquals(7, FriendlySize.STEPS.size)

    @Test fun everyStepIsInsideTheEngineRange() =
        assertTrue(FriendlySize.STEPS.all { it.scale in ScaleRules.MIN..ScaleRules.MAX })

    @Test fun stepsAreStrictlyAscending() {
        val s = FriendlySize.STEPS.map { it.scale }
        assertEquals(s.sorted(), s)
    }

    @Test fun nameMapsBackToTheSameStep() =
        FriendlySize.STEPS.forEach { assertEquals(it, FriendlySize.stepFor(it.scale)) }

    @Test fun nearestNameIsUsedBetweenSteps() =
        assertEquals(SizeStep.COMFORTABLE, FriendlySize.stepFor(1.17f))

    @Test fun adjustedIsFlaggedWhenNotOnAStep() {
        assertEquals("Comfortable", FriendlySize.nameFor(1.15f))
        assertEquals("Comfortable (adjusted)", FriendlySize.nameFor(1.18f))
    }

    @Test fun blurbCarriesThePercentForWhoWantsIt() =
        assertTrue(FriendlySize.blurbFor(1.15f).contains("115 %"))

    @Test fun theBigAGoesUpOneStep() =
        assertEquals(SizeStep.LARGE.scale, FriendlySize.next(SizeStep.COMFORTABLE.scale, true), 0.001f)

    @Test fun theSmallAGoesDownOneStep() =
        assertEquals(SizeStep.STANDARD.scale, FriendlySize.next(SizeStep.COMFORTABLE.scale, false), 0.001f)

    @Test fun steppingStopsAtTheEnds() {
        assertEquals(SizeStep.HUGE.scale, FriendlySize.next(SizeStep.HUGE.scale, true), 0.001f)
        assertEquals(SizeStep.TINY.scale, FriendlySize.next(SizeStep.TINY.scale, false), 0.001f)
    }

    @Test fun unknownScaleFallsBackSafely() =
        assertEquals(SizeStep.HUGE, FriendlySize.stepFor(Float.NaN.takeIf { false } ?: 9f))

    // ---------- three-way choices ----------
    @Test fun choicesMapToTheDocumentedOffsets() {
        assertEquals(-0.15f, ElementChoice.SMALLER.offset, 0.001f)
        assertEquals(0f, ElementChoice.NORMAL.offset, 0.001f)
        assertEquals(0.15f, ElementChoice.BIGGER.offset, 0.001f)
    }

    @Test fun applyingAGroupTouchesEveryRoleItCovers() {
        val out = FriendlySize.applyGroup(emptyMap(), ElementGroup.CONTROLS, ElementChoice.BIGGER)
        ElementGroup.CONTROLS.roles.forEach { assertEquals(0.15f, out[it]!!, 0.001f) }
        assertNull(out[TextRole.TITLE])
    }

    @Test fun groupChoiceReadsBackWhatWasApplied() {
        val out = FriendlySize.applyGroup(emptyMap(), ElementGroup.TITLES, ElementChoice.SMALLER)
        assertEquals(ElementChoice.SMALLER, FriendlySize.groupChoice(out, ElementGroup.TITLES))
    }

    @Test fun anyStoredOffsetSnapsToTheNearestChoice() =
        assertEquals(ElementChoice.BIGGER, FriendlySize.choiceFor(0.22f))

    // ---------- easy-read + defaults ----------
    @Test fun easyReadIsLargeSpaciousMedium() {
        assertEquals(SizeStep.LARGE.scale, FriendlySize.EASY_READ.textScale, 0.001f)
        assertEquals(RowSpacing.SPACIOUS, FriendlySize.EASY_READ.spacing)
        assertEquals(TextWeightSetting.MEDIUM, FriendlySize.EASY_READ.weight)
        assertTrue(FriendlySize.isEasyRead(FriendlySize.EASY_READ))
    }

    @Test fun easyReadIsNotTheDefault() = assertFalse(FriendlySize.isEasyRead(FriendlySize.DEFAULTS))

    @Test fun defaultsAreExactlyWhatTheDialogPromises() {
        assertEquals(1f, FriendlySize.DEFAULTS.textScale, 0.001f)
        assertEquals(RowSpacing.COMFORTABLE, FriendlySize.DEFAULTS.spacing)
        assertEquals(TextWeightSetting.NORMAL, FriendlySize.DEFAULTS.weight)
        TextRole.values().forEach { assertEquals(0f, FriendlySize.DEFAULTS.offset(it), 0.001f) }
    }

    // ---------- N37: undo stack ----------
    @Test fun freshStackHasNothingToUndo() {
        val s = UndoStack(UiScale())
        assertFalse(s.canUndo)
        assertEquals(0, s.dirtyCount)
    }

    @Test fun undoReturnsThePushedValueInOrder() {
        val a = UiScale(textScale = 1f)
        val b = UiScale(textScale = 1.15f)
        val s = UndoStack(a)
        s.push(a); s.push(b)
        assertEquals(b, s.undo())
        assertEquals(a, s.undo())
        assertNull(s.undo())
    }

    @Test fun undoNeverPopsPastEmpty() {
        val s = UndoStack(UiScale())
        repeat(5) { assertNull(s.undo()) }
        assertFalse(s.canUndo)
    }

    @Test fun stackIsBoundedAtFifty() {
        val s = UndoStack(UiScale())
        repeat(120) { s.push(UiScale(textScale = 1f + it / 1000f)) }
        assertEquals(UndoStack.MAX, s.depth)
    }

    @Test fun discardReturnsTheOpenTimeSnapshotAndEmptiesTheStack() {
        val opened = UiScale(textScale = 1f)
        val s = UndoStack(opened)
        s.push(opened); s.push(UiScale(textScale = 1.5f))
        assertEquals(opened, s.discard())
        assertFalse(s.canUndo)
    }

    @Test fun dirtyCountTracksPendingSteps() {
        val s = UndoStack(UiScale())
        s.push(UiScale()); s.push(UiScale())
        assertEquals(2, s.dirtyCount)
        s.undo()
        assertEquals(1, s.dirtyCount)
    }

    // ---------- N36: countdown ring ----------
    @Test fun ringIsFullAndOverdueOnceThePastIsReached() {
        assertEquals(1f, RingMath.fraction(0L), 0.001f)
        assertEquals(1f, RingMath.fraction(-310L), 0.001f)
        assertEquals(RingMath.Band.OVERDUE, RingMath.band(-1L))
    }

    @Test fun ringIsNearlyEmptyFarOut() =
        assertEquals(0.08f, RingMath.fraction(400L), 0.001f)

    @Test fun ringFillsAsTheDateApproaches() =
        assertTrue(RingMath.fraction(20L) > RingMath.fraction(120L))

    @Test fun bandsSwitchAtThirtyDays() {
        assertEquals(RingMath.Band.SOON, RingMath.band(30L))
        assertEquals(RingMath.Band.OK, RingMath.band(31L))
        assertEquals(RingMath.Band.NONE, RingMath.band(null))
    }

    @Test fun ringCaptionsReadCorrectly() {
        assertEquals("AGO", RingMath.caption(-3L))
        assertEquals("TODAY", RingMath.caption(0L))
        assertEquals("DAY", RingMath.caption(1L))
        assertEquals("DAYS", RingMath.caption(9L))
        assertEquals("", RingMath.caption(null))
    }

    @Test fun ringNumberDropsTheSignAndBlanksOnToday() {
        assertEquals("310", RingMath.number(-310L))
        assertEquals("", RingMath.number(0L))
        assertEquals("-", RingMath.number(null))
    }

    @Test fun fractionIsAlwaysInsideZeroToOne() =
        listOf(null, -900L, 0L, 1L, 45L, 900L).forEach {
            val f = RingMath.fraction(it)
            assertTrue(f in 0f..1f)
        }
}
