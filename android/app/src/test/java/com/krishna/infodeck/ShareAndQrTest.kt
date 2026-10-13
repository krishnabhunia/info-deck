package com.krishna.infodeck

import com.krishna.infodeck.core.QrRules
import com.krishna.infodeck.core.ShareField
import com.krishna.infodeck.core.SharePreset
import com.krishna.infodeck.core.ShareTarget
import com.krishna.infodeck.core.ShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShareAndQrTest {

    private val today = LocalDate.of(2026, 8, 18)

    private fun fields() = listOf(
        ShareField("name", "Card name", "Car Insurance - HDFC Ergo", ShareField.Kind.NAME),
        ShareField("cat", "Category", "Insurance", ShareField.Kind.CATEGORY),
        ShareField("d1", "Policy No", "HD-2291-KX-2026", ShareField.Kind.DATA),
        ShareField("d2", "Registration", "WB 24 AB 1234", ShareField.Kind.DATA),
        ShareField("t1", "Policy Expiry", "11-Oct-2026", ShareField.Kind.DATE),
        ShareField("t2", "Start date", "12-Oct-2025", ShareField.Kind.DATE),
        ShareField("s1", "Reminder", "30 days before Policy Expiry", ShareField.Kind.REMINDER)
    )

    // ---------- N51: presets and ticks ----------
    @Test fun lastUsedTicksNameDataAndTheKeyDateOnly() {
        val out = ShareText.applyPreset(fields(), SharePreset.LAST_USED, "t1")
        assertEquals(setOf("name", "d1", "d2", "t1"), out.filter { it.selected }.map { it.key }.toSet())
    }

    @Test fun everythingTicksEverything() =
        assertTrue(ShareText.applyPreset(fields(), SharePreset.EVERYTHING, "t1").all { it.selected })

    @Test fun idsOnlyTicksDataAndTheName() {
        val out = ShareText.applyPreset(fields(), SharePreset.IDS_ONLY, "t1")
        assertEquals(setOf("name", "d1", "d2"), out.filter { it.selected }.map { it.key }.toSet())
    }

    @Test fun datesOnlyTicksBothDates() {
        val out = ShareText.applyPreset(fields(), SharePreset.DATES_ONLY, "t1")
        assertEquals(setOf("name", "t1", "t2"), out.filter { it.selected }.map { it.key }.toSet())
    }

    @Test fun rememberedSelectionIsApplied() {
        val out = ShareText.applyRemembered(fields(), setOf("d1", "t1"))
        assertEquals(setOf("d1", "t1"), out.filter { it.selected }.map { it.key }.toSet())
    }

    @Test fun aFieldDeletedSinceLastTimeIsSimplyDropped() {
        val stale = ShareText.staleKeys(fields(), setOf("d1", "d9", "gone"))
        assertEquals(setOf("d9", "gone"), stale)
        assertTrue(ShareText.applyRemembered(fields(), setOf("d9")).none { it.selected })
    }

    // ---------- masking ----------
    @Test fun maskKeepsFirstFourCharacters() =
        assertEquals("HD-2***********", ShareText.mask("HD-2291-KX-2026"))

    @Test fun maskingIsAppliedInTheOutput() {
        val f = fields().map { it.copy(selected = it.key == "d1", masked = it.key == "d1") }
        assertTrue(ShareText.build(f).contains("HD-2***********"))
    }

    @Test fun shortValuesAreFullyMasked() = assertEquals("***", ShareText.mask("abc"))

    @Test fun emptyValueStillMasksToSomething() = assertEquals("*", ShareText.mask(""))

    // ---------- text builder ----------
    @Test fun nothingTickedProducesNothing() =
        assertEquals("", ShareText.build(fields().map { it.copy(selected = false) }))

    @Test fun outputOrderIsNameDataDatesReminders() {
        val text = ShareText.build(fields().map { it.copy(selected = true) })
        val iName = text.indexOf("Car Insurance")
        val iData = text.indexOf("Policy No:")
        val iDate = text.indexOf("Policy Expiry:")
        val iRem = text.indexOf("Reminder:")
        assertTrue(iName < iData && iData < iDate && iDate < iRem)
    }

    @Test fun messageEndsOnTheLastField() {
        val text = ShareText.build(fields().map { it.copy(selected = true) })
        assertFalse(text.contains("Shared from InfoDeck"))
        assertFalse(text.endsWith("\n"))
    }

    @Test fun unticketFieldsNeverAppear() {
        val text = ShareText.build(ShareText.applyPreset(fields(), SharePreset.IDS_ONLY, "t1"))
        assertFalse(text.contains("Policy Expiry"))
        assertFalse(text.contains("Reminder"))
    }

    // ---------- CSV ----------
    @Test fun csvHeaderIsTheUnionOfTickedLabels() {
        val a = "Card A" to fields().map { it.copy(selected = it.key in setOf("d1", "t1")) }
        val b = "Card B" to fields().map { it.copy(selected = it.key in setOf("d2", "t1")) }
        val csv = ShareText.buildCsv(listOf(a, b))
        assertEquals("Card,Policy No,Policy Expiry,Registration", csv.lineSequence().first())
    }

    @Test fun csvLeavesAnEmptyCellWhenACardLacksAField() {
        val a = "Card A" to fields().map { it.copy(selected = it.key == "d1") }
        val b = "Card B" to fields().map { it.copy(selected = it.key == "d2") }
        val lines = ShareText.buildCsv(listOf(a, b)).lines()
        assertTrue(lines[1].contains(",HD-2291-KX-2026,"))
        assertTrue(lines[2].startsWith("Card B,,"))
    }

    @Test fun csvQuotesValuesContainingCommas() {
        val f = listOf(ShareField("d1", "Note", "one, two", ShareField.Kind.DATA, selected = true))
        assertTrue(ShareText.buildCsv(listOf("C" to f)).contains("\"one, two\""))
    }

    @Test fun isoDatesForSpreadsheets() = assertEquals("2026-10-11", ShareText.isoDate(LocalDate.of(2026, 10, 11)))

    // ---------- file names ----------
    @Test fun fileNameIsSafeAndDated() =
        assertEquals("InfoDeck-CarInsuranceHDFCErgo-18Aug2026.txt",
            ShareText.fileName("Car Insurance - HDFC Ergo", today, "txt"))

    @Test fun fileNameSurvivesAnEmptyLabel() =
        assertTrue(ShareText.fileName("///", today, "txt").startsWith("InfoDeck-Card-"))

    // ---------- targets ----------
    @Test fun smsCannotCarryFiles() {
        assertFalse(ShareTarget.SMS.filesOk)
        assertTrue(ShareTarget.WHATSAPP.filesOk)
    }

    // ---------- N52: QR ----------
    @Test fun singleFieldEncodesJustTheValue() =
        assertEquals("HD-2291-KX-2026", QrRules.single(fields()[2]))

    @Test fun combinedCodeCarriesLabelledLines() {
        val text = QrRules.combined("Car Insurance", fields().map { it.copy(selected = it.key.startsWith("d")) })
        assertTrue(text.startsWith("Car Insurance"))
        assertTrue(text.contains("Policy No: HD-2291-KX-2026"))
        assertFalse(text.contains("Card name"))
    }

    @Test fun perFieldSplitsOnePerTickedField() {
        val pages = QrRules.perField(fields().map { it.copy(selected = it.key in setOf("d1", "d2", "t1")) })
        assertEquals(3, pages.size)
        assertEquals("Policy No", pages.first().first)
    }

    @Test fun capacityGuardRefusesTooMuchText() {
        assertTrue(QrRules.fits("x".repeat(QrRules.MAX_CHARS)))
        assertFalse(QrRules.fits("x".repeat(QrRules.MAX_CHARS + 1)))
        assertEquals(50, QrRules.tooLongBy("x".repeat(QrRules.MAX_CHARS + 50)))
    }

    @Test fun emptyContentNeverFits() = assertFalse(QrRules.fits(""))

    @Test fun captionExplainsTheProblem() {
        assertTrue(QrRules.caption("x".repeat(2000), 4).contains("one code per field"))
        assertTrue(QrRules.caption("short", 1).contains("characters"))
        assertEquals("Nothing selected", QrRules.caption("", 0))
    }

    @Test fun pngNameMatchesTheTextExport() =
        assertTrue(QrRules.pngName("Car Insurance", today).endsWith(".png"))

    @Test fun pngIsAThousandPixels() = assertEquals(1024, QrRules.PNG_PX)
}
