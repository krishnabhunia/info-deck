package com.krishna.infodeck

import com.krishna.infodeck.core.FileNameRules
import com.krishna.infodeck.core.FileNameRules.Decision
import com.krishna.infodeck.core.FileNameRules.Parts
import com.krishna.infodeck.data.FileRules
import com.krishna.infodeck.data.TagRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** N60 + B13 · design 1.9.D1 — rename keeps the extension; changing it is a confirmed second step. */
class FileRenameRulesTest {
    private val pdf = "application/pdf"
    private val doc = "application/msword"
    private val docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    // ------------------------------------------------------------------ split / join

    @Test fun splitsTheTwelveShapes() {
        val cases = mapOf(
            "Car_Insurance_Policy_2026.pdf" to Parts("Car_Insurance_Policy_2026", "pdf"),
            "claim.docx" to Parts("claim", "docx"),
            "Policy v2.1.pdf" to Parts("Policy v2.1", "pdf"),        // several dots: the LAST one counts
            "Scan" to Parts("Scan", ""),                              // no extension
            ".hidden" to Parts(".hidden", ""),                        // leading dot: all name
            "report." to Parts("report.", ""),                        // trailing dot: no extension
            "archive.verylongext" to Parts("archive.verylongext", ""),// 11 chars: not an extension
            "a.abcdefgh" to Parts("a", "abcdefgh"),                   // exactly 8: still an extension
            "My Policy.PDF" to Parts("My Policy", "PDF"),             // case kept as stored
            "पॉलिसी.pdf" to Parts("पॉलिसी", "pdf"),                    // Unicode name
            "Policy v2.1" to Parts("Policy v2.1", ""),                // ".1" has no letter
            "scan.12345678" to Parts("scan.12345678", "")             // digits only: no letter
        )
        cases.forEach { (name, want) -> assertEquals(name, want, FileNameRules.split(name)) }
    }

    @Test fun splitThenJoinRoundTrips() {
        listOf("a.pdf", "Policy v2.1.pdf", "Scan", ".hidden", "report.", "My Policy.PDF", "x.y.z.docx")
            .forEach { assertEquals(it, FileNameRules.split(it).full) }
    }

    @Test fun nullOrBlankSplitsToNothing() {
        assertEquals(Parts("", ""), FileNameRules.split(null))
        assertEquals(Parts("", ""), FileNameRules.split(""))
    }

    @Test fun joinAddsTheDotOnlyWhenThereIsAnExtension() {
        assertEquals("a.pdf", FileNameRules.join("a", "pdf"))
        assertEquals("a", FileNameRules.join("a", ""))
    }

    // ------------------------------------------------------------------ extension box

    @Test fun extensionInputIsSanitised() {
        assertEquals("docx", FileNameRules.cleanExt(".docx"))
        assertEquals("PDF", FileNameRules.cleanExt("..PDF"))
        assertEquals("doc", FileNameRules.cleanExt("d o c"))
        assertEquals("abcdefgh", FileNameRules.cleanExt("abcdefghij"))
        assertEquals("", FileNameRules.cleanExt("पीडीएफ"))
        assertEquals("", FileNameRules.cleanExt(null))
    }

    // ------------------------------------------------------------------ the confirm gate

    @Test fun nameOnlyChangeSavesDirectly() =
        assertEquals(Decision.Direct("Car Policy.pdf"), FileNameRules.decide("Old.pdf", "Car Policy", "pdf"))

    @Test fun caseOnlyExtensionChangeSavesDirectlyWithTheNewCase() =
        assertEquals(Decision.Direct("Old.pdf"), FileNameRules.decide("Old.PDF", "Old", "pdf"))

    @Test fun newExtensionNeedsConfirm() =
        assertEquals(
            Decision.NeedsConfirm("Old.docx", "pdf", "docx"),
            FileNameRules.decide("Old.pdf", "Old", ".docx")
        )

    @Test fun blankExtensionMeansRemoveAndNeedsConfirm() =
        assertEquals(Decision.NeedsConfirm("Old", "pdf", ""), FileNameRules.decide("Old.pdf", "Old", ""))

    @Test fun addingAnExtensionNeedsConfirm() =
        assertEquals(Decision.NeedsConfirm("Scan.pdf", "", "pdf"), FileNameRules.decide("Scan", "Scan", "pdf"))

    @Test fun blankNameOrDigitOnlyExtensionIsInvalid() {
        assertEquals(Decision.Invalid, FileNameRules.decide("Old.pdf", "   ", "pdf"))
        assertEquals(Decision.Invalid, FileNameRules.decide("Old.pdf", "Old", "123"))
    }

    @Test fun unchangedNameIsStillDirect() =
        assertEquals(Decision.Direct("Old.pdf"), FileNameRules.decide("Old.pdf", "Old", "pdf"))

    // ------------------------------------------------------------------ length (B13) and cleaning

    @Test fun thirtyCharacterNameKeepsItsExtension() {
        val base = "Car_Insurance_Policy_2026_ABCD"           // 30 characters
        assertEquals("$base.pdf", FileNameRules.fit(base, "pdf"))
    }

    @Test fun overLongNameIsCutInTheNameNeverTheExtension() {
        val out = FileNameRules.fit("x".repeat(120), "pdf")
        assertEquals(FileNameRules.MAX_TOTAL, out.length)
        assertTrue(out.endsWith(".pdf"))
    }

    @Test fun b13NameBoxNoLongerStopsAt24() {
        assertEquals(76, FileNameRules.maxBase("pdf"))
        assertEquals(75, FileNameRules.maxBase("docx"))
        assertEquals(80, FileNameRules.maxBase(""))
        // the 1.9 bug: a 29-character name lost ".pdf" on the first key press
        val typed = "Car_Insurance_Policy_2026X"
        assertTrue(typed.length <= FileNameRules.maxBase("pdf"))
        assertEquals("$typed.pdf", (FileNameRules.decide("Car_Insurance_Policy_2026.pdf", typed, "pdf") as Decision.Direct).name)
    }

    @Test fun negativeControlTagNamesStillStopAt24() =
        assertEquals(TagRules.MAX_NAME, TagRules.normalise("t".repeat(30)).length)

    @Test fun fitCleansSeparatorsAndBlankNames() {
        assertEquals("a_b.pdf", FileNameRules.fit("a/b", "pdf"))
        assertEquals("file.pdf", FileNameRules.fit("  ", "pdf"))
        assertEquals("file", FileNameRules.fit("...", ""))
        assertEquals("Report.pdf", FileNameRules.fit("Report.", "pdf"))
    }

    @Test fun sanitiserKeepsTheExtensionAtTheLimit() {
        val out = FileRules.sanitise("y".repeat(200) + ".docx")
        assertEquals(80, out.length)
        assertTrue(out.endsWith(".docx"))
    }

    @Test fun sanitiserStillBlocksTraversal() {
        assertEquals("secret.pdf", FileRules.sanitise("../../secret.pdf"))
        assertEquals("etc_passwd", FileRules.sanitise("/etc/passwd"))
    }

    // ------------------------------------------------------------------ hints

    @Test fun typedExtensionIsSpotted() {
        assertEquals("docx", FileNameRules.typedExtension("Policy_2026.docx"))
        assertEquals("PDF", FileNameRules.typedExtension("Report.PDF"))
        assertNull(FileNameRules.typedExtension("Policy v2.1"))
        assertNull(FileNameRules.typedExtension("Backup.zip"))   // not a known kind
        assertNull(FileNameRules.typedExtension("Plain name"))
    }

    @Test fun realTypeComesFromTheMime() {
        assertEquals("pdf", FileNameRules.realType(pdf))
        assertEquals("doc", FileNameRules.realType(doc))
        assertEquals("docx", FileNameRules.realType(docx))
        assertNull(FileNameRules.realType("application/octet-stream"))
        assertNull(FileNameRules.realType(""))
        assertNull(FileNameRules.realType(null))
    }

    @Test fun mismatchNamesTheRealTypeOnlyWhenTheyDisagree() {
        assertEquals("PDF", FileNameRules.mismatch("Policy.docx", pdf))
        assertNull(FileNameRules.mismatch("Policy.PDF", pdf))               // case is not a mismatch
        assertEquals("Word .docx file", FileNameRules.mismatch("Claim.doc", docx))
        assertEquals("PDF", FileNameRules.mismatch("Scan", pdf))             // no extension at all
        assertNull(FileNameRules.mismatch("Policy.docx", "application/octet-stream")) // unknown: no claim
    }

    @Test fun outcomeTextMatchesWhatChanged() {
        assertEquals("Renamed", FileNameRules.outcome("A.pdf", "B.pdf"))
        assertEquals("Renamed", FileNameRules.outcome("A.PDF", "A.pdf"))
        assertEquals("Extension changed to .docx", FileNameRules.outcome("A.pdf", "A.docx"))
        assertEquals("Extension removed", FileNameRules.outcome("A.pdf", "A"))
        assertEquals("Extension .pdf added", FileNameRules.outcome("Scan", "Scan.pdf"))
    }
}
