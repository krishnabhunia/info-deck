package com.krishna.infodeck

import com.krishna.infodeck.core.CarryBadge
import com.krishna.infodeck.core.FileShare
import com.krishna.infodeck.core.Hue
import com.krishna.infodeck.core.Hues
import com.krishna.infodeck.core.ShareField
import com.krishna.infodeck.core.ShareTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HuesAndFilesTest {

    // ---------- N55: the nine hues ----------
    @Test fun thereAreExactlyNineHues() = assertEquals(9, Hue.values().size)

    @Test fun everyHueHasItsOwnGlyphTintAndOnTint() =
        Hue.values().forEach {
            assertNotEquals(it.label, it.argb, it.tint)
            assertNotEquals(it.label, it.tint, it.onTint)
        }

    @Test fun everyColourIsFullyOpaque() =
        Hue.values().forEach { assertEquals(it.label, 0xFFL, (it.argb shr 24) and 0xFF) }

    @Test fun darkVariantsAreLighterThanTheLightOnes() =
        Hue.values().forEach {
            val lightR = (it.argb shr 16) and 0xFF
            val darkR = (it.darkGlyph shr 16) and 0xFF
            assertTrue(it.label, darkR >= lightR)
        }

    @Test fun rolesMapToTheDocumentedHues() {
        assertEquals(Hue.AMBER, Hues.forRole("reminder"))
        assertEquals(Hue.ROSE, Hues.forRole("overdue"))
        assertEquals(Hue.PURPLE, Hues.forRole("file"))
        assertEquals(Hue.GREEN, Hues.forRole("share"))
        assertEquals(Hue.VIOLET, Hues.forRole("qr"))
        assertEquals(Hue.TEAL, Hues.forRole("settings"))
        assertEquals(Hue.SKY, Hues.forRole("category"))
    }

    @Test fun anUnknownRoleFallsBackToSlateNotACrash() = assertEquals(Hue.SLATE, Hues.forRole("nonsense"))

    @Test fun categoryHuesWrapAndNeverThrow() {
        assertEquals(Hues.forCategory(0), Hues.forCategory(8))
        assertEquals(Hue.SLATE, Hues.forCategory(-3))
    }

    // ---------- N56: category glyphs ----------
    @Test fun glyphsAreGuessedFromTheCategoryName() {
        assertEquals("shield", Hues.glyphFor("Insurance"))
        assertEquals("car", Hues.glyphFor("Vehicle"))
        assertEquals("laptop", Hues.glyphFor("Warranties"))
        assertEquals("id", Hues.glyphFor("Identity Docs"))
        assertEquals("wifi", Hues.glyphFor("Home network"))
    }

    @Test fun anUnknownOrBlankCategoryStillGetsAGlyph() {
        assertEquals("card", Hues.glyphFor("Zzz"))
        assertEquals("card", Hues.glyphFor(""))
    }

    // ---------- N57: MIME ----------
    @Test fun mimeIsResolvedPerExtension() {
        assertEquals("application/pdf", FileShare.mime("Policy document.pdf"))
        assertEquals("image/png", FileShare.mime("qr.PNG"))
        assertEquals("application/octet-stream", FileShare.mime("mystery"))
    }

    @Test fun aMixedSelectionFallsBackToTheGenericType() {
        assertEquals("application/pdf", FileShare.commonMime(listOf("a.pdf", "b.pdf")))
        assertEquals("*/*", FileShare.commonMime(listOf("a.pdf", "b.docx")))
        assertEquals("text/plain", FileShare.commonMime(emptyList()))
    }

    // ---------- badges and SMS ----------
    @Test fun badgesFollowTheDocumentedTable() {
        assertEquals(CarryBadge.TEXT_MAY_DROP, FileShare.badge(ShareTarget.WHATSAPP, true))
        assertEquals(CarryBadge.BOTH_FINE, FileShare.badge(ShareTarget.GMAIL, true))
        assertEquals(CarryBadge.NO_FILES, FileShare.badge(ShareTarget.SMS, true))
        assertEquals(CarryBadge.UNKNOWN, FileShare.badge(ShareTarget.CHOOSER, true))
    }

    @Test fun withoutFilesEveryTargetIsFine() =
        ShareTarget.values().forEach { assertEquals(CarryBadge.BOTH_FINE, FileShare.badge(it, false)) }

    @Test fun smsDisappearsOnlyWhenAFileIsTicked() {
        assertTrue(FileShare.offers(ShareTarget.SMS, false))
        assertFalse(FileShare.offers(ShareTarget.SMS, true))
        assertTrue(FileShare.offers(ShareTarget.WHATSAPP, true))
    }

    // ---------- the masking conflict ----------
    private fun f(kind: ShareField.Kind, selected: Boolean, masked: Boolean = false) =
        ShareField("k" + kind.name + selected + masked, "L", "V", kind, selected, masked)

    @Test fun maskedValuePlusTickedFileWarns() =
        assertTrue(
            FileShare.maskingConflict(
                listOf(f(ShareField.Kind.DATA, true, true), f(ShareField.Kind.FILE, true))
            )
        )

    @Test fun maskedValueWithoutAFileDoesNotWarn() =
        assertFalse(FileShare.maskingConflict(listOf(f(ShareField.Kind.DATA, true, true))))

    @Test fun fileWithoutAMaskDoesNotWarn() =
        assertFalse(
            FileShare.maskingConflict(
                listOf(f(ShareField.Kind.DATA, true), f(ShareField.Kind.FILE, true))
            )
        )

    @Test fun anUntickedFileDoesNotWarn() =
        assertFalse(
            FileShare.maskingConflict(
                listOf(f(ShareField.Kind.DATA, true, true), f(ShareField.Kind.FILE, false))
            )
        )

    // ---------- sizes ----------
    @Test fun sizeWarningFiresPastTwentyFiveMegabytes() {
        assertFalse(FileShare.tooBig(FileShare.WARN_BYTES))
        assertTrue(FileShare.tooBig(FileShare.WARN_BYTES + 1))
    }

    @Test fun sizesAreReadableAndNegativesIgnored() {
        assertEquals("1.8 MB", FileShare.humanSize(1_887_437))
        assertEquals("12 KB", FileShare.humanSize(12_800))
        assertEquals(300L, FileShare.totalBytes(listOf(100L, 200L, -50L)))
    }

    // ---------- zip entries ----------
    @Test fun duplicateEntryNamesAreMadeUnique() =
        assertEquals(
            listOf("a.pdf", "a (1).pdf", "b.docx"),
            FileShare.uniqueEntryNames(listOf("a.pdf", "a.pdf", "b.docx"))
        )

    @Test fun namesWithoutAnExtensionStillDeduplicate() =
        assertEquals(listOf("scan", "scan (1)"), FileShare.uniqueEntryNames(listOf("scan", "scan")))

    @Test fun theTextEntryIsNamedAfterTheCard() =
        assertEquals("Car Insurance - HDFC Ergo.txt", FileShare.textEntryName("Car Insurance - HDFC Ergo"))

    @Test fun anAwkwardCardNameStillProducesASafeEntry() =
        assertEquals("A_B.txt", FileShare.textEntryName("A/B"))

    @Test fun zipIsNamedLikeTheOtherExports() =
        assertTrue(FileShare.zipName("Car Insurance", java.time.LocalDate.of(2026, 8, 20)).endsWith(".zip"))

    @Test fun bundleIsOfferedOnlyFromTwoFilesUp() {
        assertFalse(FileShare.bundleOffered(1))
        assertTrue(FileShare.bundleOffered(2))
    }
}
