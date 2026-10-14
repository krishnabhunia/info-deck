package com.krishna.infodeck

import com.krishna.infodeck.data.FileRenameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileRenameRulesTest {
    @Test fun splitPreservesDotsInStem() {
        assertEquals(FileRenameRules.NameParts("policy.v2", "pdf"), FileRenameRules.parts("policy.v2.pdf"))
        assertEquals(FileRenameRules.NameParts("policy", ""), FileRenameRules.parts("policy"))
    }

    @Test fun ordinaryRenameDoesNotRequireSecondStep() {
        assertFalse(FileRenameRules.extensionChanged("policy.pdf", "renewed.pdf"))
        assertNull(FileRenameRules.validate("policy.pdf", "renewed.pdf", false))
    }

    @Test fun extensionChangeRequiresConfirmation() {
        assertTrue(FileRenameRules.extensionChanged("policy.pdf", "policy.docx"))
        assertEquals(
            "Confirm the extension change before saving",
            FileRenameRules.validate("policy.pdf", "policy.docx", false)
        )
        assertNull(FileRenameRules.validate("policy.pdf", "policy.docx", true))
    }

    @Test fun extensionRemovalAndAdditionAreProtected() {
        assertTrue(FileRenameRules.extensionChanged("policy.pdf", "policy"))
        assertTrue(FileRenameRules.extensionChanged("policy", "policy.pdf"))
        assertEquals(
            "Confirm the extension change before saving",
            FileRenameRules.validate("policy.pdf", "policy", false)
        )
        assertNull(FileRenameRules.validate("policy.pdf", "policy", true))
    }

    // N62 / design 1.9.D1 Q1 (accepted 15-Oct-2026) supersedes Design A here:
    // a case-only change is a plain save, not an extension change.
    @Test fun caseOnlyChangeIsNotAnExtensionChange() {
        assertFalse(FileRenameRules.extensionChanged("policy.pdf", "policy.PDF"))
        assertNull(FileRenameRules.validate("policy.pdf", "policy.PDF", false))
    }

    @Test fun versionNumbersAreNotExtensions() {
        assertFalse(FileRenameRules.extensionChanged("Policy v2.1", "Policy v2.2"))
        assertNull(FileRenameRules.validate("Policy v2.1", "Policy v2.2", false))
    }

    @Test fun composingKeepsExtensionLockedUnlessExplicitlyChanged() {
        val original = FileRenameRules.parts("policy.v2.pdf")
        assertEquals("renewed.pdf", FileRenameRules.compose("renewed", original.extension))
        assertEquals("renewed.docx", FileRenameRules.compose("renewed", "docx"))
        assertEquals("renewed", FileRenameRules.compose("renewed", ""))
    }

    @Test fun invalidNamesAreRejectedEvenAfterConfirmation() {
        listOf("", " ", ".pdf", "file.", " file.pdf", "file.pdf ", "a/b.pdf", "a\\b.pdf", "a:1.pdf",
            "bad\nname.pdf", "virus.e!e", "file.verylongextension").forEach { bad ->
            assertTrue("Expected rejection for: " + bad, FileRenameRules.validate("old.pdf", bad, true) != null)
        }
    }

    @Test fun fileLengthIsBoundedWithoutSilentTruncation() {
        assertNull(FileRenameRules.validate("old.pdf", "a".repeat(76) + ".pdf", false))
        assertTrue(FileRenameRules.validate("old.pdf", "a".repeat(77) + ".pdf", false) != null)
    }
}
