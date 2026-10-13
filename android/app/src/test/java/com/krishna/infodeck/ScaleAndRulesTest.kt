package com.krishna.infodeck

import com.krishna.infodeck.core.ScaleRules
import com.krishna.infodeck.core.TextRole
import com.krishna.infodeck.core.UiScale
import com.krishna.infodeck.data.FileRules
import com.krishna.infodeck.data.TagRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleAndRulesTest {

    // ---------- N30b: continuous scale ----------
    @Test fun scaleClampsToRange() {
        assertEquals(0.70f, ScaleRules.clampScale(0.1f), 0.001f)
        assertEquals(2.00f, ScaleRules.clampScale(9f), 0.001f)
        assertEquals(1.37f, ScaleRules.clampScale(1.37f), 0.001f)
    }

    @Test fun nanScaleFallsBackToOne() = assertEquals(1f, ScaleRules.clampScale(Float.NaN), 0.001f)

    @Test fun everyOnePercentStepIsDistinct() {
        val seen = (70..200).map { ScaleRules.percent(ScaleRules.fromPercent(it)) }.toSet()
        assertEquals(131, seen.size)
    }

    @Test fun layoutIsDampedAndClamped() {
        assertEquals(1f, ScaleRules.layoutScale(1f), 0.001f)
        assertEquals(1.30f, ScaleRules.layoutScale(1.5f), 0.001f)   // 1 + .5*.6
        assertEquals(1.60f, ScaleRules.layoutScale(2.0f), 0.001f)   // ceiling
        assertEquals(0.82f, ScaleRules.layoutScale(0.70f), 0.001f)  // 1 - .3*.6
    }

    @Test fun layoutNeverBelowFloor() =
        assertTrue(ScaleRules.layoutScale(ScaleRules.MIN) >= ScaleRules.LAYOUT_MIN)

    @Test fun spCombinesScaleAndOffset() =
        assertEquals(13.5f * 1.15f * 1.18f, ScaleRules.sp(13.5f, 1.15f, 0.18f), 0.01f)

    @Test fun spIsClampedAtBothEnds() {
        assertEquals(8f, ScaleRules.sp(9f, 0.70f, -0.30f), 0.001f)
        assertEquals(46f, ScaleRules.sp(40f, 2.0f, 0.30f), 0.001f)
    }

    @Test fun offsetsAreLimitedToThirtyPercent() {
        assertEquals(0.30f, ScaleRules.clampOffset(5f), 0.001f)
        assertEquals(-0.30f, ScaleRules.clampOffset(-5f), 0.001f)
    }

    @Test fun iconsHaveAFloorAndCeiling() {
        assertEquals(18f, ScaleRules.iconDp(20f, 0.70f), 0.001f)
        assertEquals(40f, ScaleRules.iconDp(30f, 2.0f), 0.001f)
    }

    @Test fun buttonsNeverDropBelowFortyFour() =
        assertEquals(44f, ScaleRules.buttonDp(50f, 0.70f), 0.001f)

    @Test fun uiScaleAppliesTheRightRoleOffset() {
        val ui = UiScale(textScale = 1.2f, offsets = mapOf(TextRole.TITLE to 0.2f))
        assertEquals(13.5f * 1.2f * 1.2f, ui.sp(13.5f, TextRole.TITLE), 0.01f)
        assertEquals(11.5f * 1.2f, ui.sp(11.5f, TextRole.SECONDARY), 0.01f)
    }

    @Test fun presetsAreInsideTheRange() =
        assertTrue(ScaleRules.PRESETS.all { it in ScaleRules.MIN..ScaleRules.MAX })

    // ---------- N34: file rules ----------
    @Test fun pdfAndWordAreAccepted() {
        assertTrue(FileRules.typeAllowed("application/pdf", "policy.pdf"))
        assertTrue(FileRules.typeAllowed("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "claim.docx"))
        assertTrue(FileRules.typeAllowed(null, "scan.PDF"))
    }

    @Test fun otherTypesAreRejected() {
        assertFalse(FileRules.typeAllowed("image/jpeg", "photo.jpg"))
        assertFalse(FileRules.typeAllowed("application/zip", "archive.zip"))
        assertFalse(FileRules.typeAllowed(null, "notes.txt"))
    }

    @Test fun sanitiserStripsSeparatorsAndTraversal() {
        assertEquals("etc_passwd", FileRules.sanitise("/etc/passwd"))
        assertEquals("a_b_c", FileRules.sanitise("a\\b/c"))
        assertFalse(FileRules.sanitise("../../secret.pdf").contains("/"))
    }

    @Test fun sanitiserNeverReturnsBlank() = assertEquals("file", FileRules.sanitise("   "))

    @Test fun sanitiserBoundsLength() =
        assertEquals(80, FileRules.sanitise("x".repeat(400)).length)

    @Test fun sizesAreHumanReadable() {
        assertEquals("240 KB", FileRules.humanSize(240L * 1024))
        assertEquals("1.8 MB", FileRules.humanSize((1.8 * 1048576).toLong()))
        assertEquals("512 B", FileRules.humanSize(512))
    }

    @Test fun limitsAreTheAgreedOnes() {
        assertEquals(25L * 1024 * 1024, FileRules.MAX_BYTES)
        assertEquals(10, FileRules.MAX_PER_CARD)
    }

    @Test fun kindIsPdfOrDoc() {
        assertEquals("PDF", FileRules.kind("application/pdf", "a.pdf"))
        assertEquals("DOC", FileRules.kind("application/msword", "a.doc"))
    }

    // ---------- N35: tag names ----------
    @Test fun tagNamesAreNormalised() {
        assertEquals("renewal", TagRules.normalise("  #renewal  "))
        assertEquals("kyc", TagRules.normalise("kyc"))
    }

    @Test fun tagNamesAreBounded() = assertEquals(24, TagRules.normalise("t".repeat(60)).length)

    @Test fun tagDisplayAddsTheHash() = assertEquals("#renewal", TagRules.display("renewal"))
}
