package com.krishna.infodeck

import com.krishna.infodeck.core.Density
import com.krishna.infodeck.core.ExpandRules
import com.krishna.infodeck.core.FileShare
import com.krishna.infodeck.ui.ShareFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Release18Test {
    // ---------- B7: one gap per density ----------
    @Test fun everyDensityHasOneGap() = Density.values().forEach { assertTrue(it.label, it.gapDp > 0f) }
    @Test fun compactHasTheSmallestGap() = assertTrue(Density.values().all { it.gapDp >= Density.COMPACT.gapDp })
    @Test fun gapsAreTheDesignedNumbers() {
        assertEquals(4f, Density.COMPACT.gapDp, 0.01f)
        assertEquals(6f, Density.COMFORTABLE.gapDp, 0.01f)
        assertEquals(6f, Density.DETAILED.gapDp, 0.01f)
        assertEquals(8f, Density.SECTION_GAP, 0.01f)
    }

    // ---------- B8: three values ----------
    @Test fun expandedRowShowsThreeOfFive() =
        assertEquals(listOf("a", "b", "c"), ExpandRules.visibleValues(listOf("a", "b", "c", "d", "e")))

    // ---------- B10: exact names ----------
    @Test fun displayNameSurvivesUntouchedWhenSafe() = assertEquals("Policy document.pdf", ShareFiles.safeName("Policy document.pdf"))
    @Test fun unsafeCharactersBecomeUnderscoresAndTheExtensionSurvives() =
        assertEquals("a_b_c.pdf", ShareFiles.safeName("a/b:c.pdf"))
    @Test fun blankNameStillGetsAName() = assertEquals("file", ShareFiles.safeName("   "))
    @Test fun twoFilesWithTheSameNameAreSuffixed() =
        assertEquals(listOf("Policy.pdf", "Policy (1).pdf"), FileShare.uniqueEntryNames(listOf("Policy.pdf", "Policy.pdf")))
}
