package com.krishna.infodeck

import com.krishna.infodeck.core.Density
import com.krishna.infodeck.core.ExpandRules
import com.krishna.infodeck.core.SendPlan
import com.krishna.infodeck.core.SharePlanner
import com.krishna.infodeck.core.ShareField
import com.krishna.infodeck.core.ShareTarget
import com.krishna.infodeck.core.ShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Release17Test {

    private fun fields() = listOf(
        ShareField("name", "Card name", "Car Insurance", ShareField.Kind.NAME, selected = true),
        ShareField("d1", "Policy No", "HD-2291", ShareField.Kind.DATA, selected = true),
        ShareField("f1", "Policy document.pdf", "Policy document.pdf", ShareField.Kind.FILE, selected = true),
        ShareField("f2", "Claim form.docx", "Claim form.docx", ShareField.Kind.FILE, selected = true)
    )

    // ---------- B5: files are never text ----------
    @Test fun fileNamesNeverAppearInTheMessage() {
        val text = ShareText.build(fields())
        assertFalse(text.contains("Policy document.pdf"))
        assertFalse(text.contains("Claim form.docx"))
        assertTrue(text.contains("Policy No: HD-2291"))
    }

    @Test fun anAllFilesSelectionYieldsNoMessage() =
        assertEquals("", ShareText.build(fields().filter { it.kind == ShareField.Kind.FILE }))

    @Test fun whatsAppGetsTextThenFiles() =
        assertEquals(SendPlan.TEXT_THEN_FILES, SharePlanner.plan(ShareTarget.WHATSAPP, 2))

    @Test fun mailGetsEverythingInOneSend() {
        assertEquals(SendPlan.ONE_SEND_WITH_FILES, SharePlanner.plan(ShareTarget.GMAIL, 1))
        assertEquals(SendPlan.ONE_SEND_WITH_FILES, SharePlanner.plan(ShareTarget.EMAIL, 3))
    }

    @Test fun smsNeverCarriesFiles() = assertEquals(SendPlan.TEXT_ONLY, SharePlanner.plan(ShareTarget.SMS, 2))

    @Test fun noFilesMeansTextOnlyEverywhere() =
        ShareTarget.values().forEach { assertEquals(it.name, SendPlan.TEXT_ONLY, SharePlanner.plan(it, 0)) }

    @Test fun followUpMessageCountsCorrectly() {
        assertEquals("1 file ready for WhatsApp", SharePlanner.followUpMessage(1, ShareTarget.WHATSAPP))
        assertEquals("3 files ready for WhatsApp", SharePlanner.followUpMessage(3, ShareTarget.WHATSAPP))
    }

    // ---------- B9: the footer is gone for good ----------
    @Test fun noShareTextEverCarriesTheOldFooter() {
        val text = ShareText.build(fields())
        assertFalse(text.contains("Shared from InfoDeck"))
        assertTrue(text.endsWith("Policy No: HD-2291"))
    }

    // ---------- N58: density ----------
    @Test fun threeDensitiesAscendInHeight() {
        val h = Density.values().map { it.rowDp }
        assertEquals(h.sorted(), h)
        assertEquals(3, h.size)
    }

    @Test fun comfortableIsTheDefault() = assertEquals(Density.COMFORTABLE, Density.DEFAULT)

    @Test fun unknownStoredDensityFallsBack() {
        assertEquals(Density.COMFORTABLE, Density.of("nonsense"))
        assertEquals(Density.COMFORTABLE, Density.of(null))
        assertEquals(Density.COMPACT, Density.of("COMPACT"))
    }

    @Test fun compactShowsNothingButTheTitle() {
        assertFalse(Density.COMPACT.showMeta); assertFalse(Density.COMPACT.showChips); assertFalse(Density.COMPACT.ringUnit)
    }

    @Test fun detailedShowsEverything() {
        assertTrue(Density.DETAILED.showMeta); assertTrue(Density.DETAILED.showChips)
    }

    @Test fun comfortableShowsMetaButNoChips() {
        assertTrue(Density.COMFORTABLE.showMeta); assertFalse(Density.COMFORTABLE.showChips)
    }

    @Test fun expandDefaultsOnOnlyForCompact() {
        assertTrue(Density.defaultExpand(Density.COMPACT))
        assertFalse(Density.defaultExpand(Density.COMFORTABLE))
        assertFalse(Density.defaultExpand(Density.DETAILED))
    }

    // ---------- N59: expand rules ----------
    @Test fun tappingOpensAndTappingAgainCloses() {
        val ids = setOf(1L, 2L, 3L)
        assertEquals(2L, ExpandRules.next(null, 2L, ids))
        assertNull(ExpandRules.next(2L, 2L, ids))
    }

    @Test fun openingAnotherClosesThePrevious() =
        assertEquals(3L, ExpandRules.next(2L, 3L, setOf(1L, 2L, 3L)))

    @Test fun aVanishedCardCollapses() {
        assertNull(ExpandRules.keep(2L, setOf(1L, 3L)))
        assertEquals(2L, ExpandRules.keep(2L, setOf(2L)))
    }

    @Test fun tappingSomethingNotVisibleIsIgnored() = assertNull(ExpandRules.next(null, 9L, setOf(1L)))

    @Test fun expansionShowsAtMostThreeValues() =
        assertEquals(3, ExpandRules.visibleValues(listOf("a", "b", "c", "d", "e")).size)
}
