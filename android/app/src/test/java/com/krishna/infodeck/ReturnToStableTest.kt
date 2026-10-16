package com.krishna.infodeck

import com.krishna.infodeck.backup.BFile
import com.krishna.infodeck.backup.BackupFile
import com.krishna.infodeck.backup.BackupIo
import com.krishna.infodeck.backup.FullBackupRules
import com.krishna.infodeck.data.DB_SCHEMA_VERSION
import com.krishna.infodeck.update.ReleaseChooser
import com.krishna.infodeck.update.ReleaseUpdate
import com.krishna.infodeck.update.ReturnPlanner
import com.krishna.infodeck.update.ReturnPlanner.ApkFacts
import com.krishna.infodeck.update.ReturnPlanner.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Design 1.15.0.D2 — return from a beta to the stable version. */
class ReturnToStableTest {
    private val pkg = ReturnPlanner.PACKAGE

    // ---------------------------------------------------------------- planner
    @Test fun equalVersionCodeAndSameSchemaReturnsInPlace() {
        // Beta 1.16.0-beta.N built on 1.15.0 carries 1015000; stable 1.15.0 carries 1015000.
        assertEquals(Plan.InPlace, ReturnPlanner.forReturn(1_015_000, 2, ApkFacts(pkg, 1_015_000, 2)))
    }

    @Test fun newerStableThanTheBetaBaseAlsoReturnsInPlace() {
        assertEquals(Plan.InPlace, ReturnPlanner.forReturn(1_015_000, 2, ApkFacts(pkg, 1_015_001, 2)))
    }

    @Test fun oldStyleBetaWithTargetCodeNeedsReinstall() {
        // 2.0.0-beta.78 was built before this change and carries 2000000.
        val plan = ReturnPlanner.forReturn(2_000_000, 2, ApkFacts(pkg, 1_015_000, 2, sameSigner = true))
        assertTrue(plan is Plan.Reinstall)
        assertTrue((plan as Plan.Reinstall).reason.contains("version number"))
    }

    @Test fun betaWithNewerDatabaseNeedsReinstallEvenWithEqualCode() {
        val plan = ReturnPlanner.forReturn(1_015_000, 3, ApkFacts(pkg, 1_015_000, 2, sameSigner = true))
        assertTrue(plan is Plan.Reinstall)
        assertTrue((plan as Plan.Reinstall).reason.contains("database"))
    }

    @Test fun releasesWithoutSchemaMetaDataAreTreatedAsLegacySchemaTwo() {
        assertEquals(Plan.InPlace, ReturnPlanner.forReturn(1_015_000, 2, ApkFacts(pkg, 1_015_000, null)))
        assertTrue(ReturnPlanner.forReturn(1_015_000, 3, ApkFacts(pkg, 1_015_000, null, sameSigner = true)) is Plan.Reinstall)
    }

    @Test fun guidedReinstallNeedsAProvenSigner() {
        // After uninstalling, Android has nothing to compare the signer with — so it is checked here.
        assertTrue(ReturnPlanner.forReturn(2_000_000, 2, ApkFacts(pkg, 1_015_000, 2, sameSigner = null)) is Plan.Refuse)
        assertTrue(ReturnPlanner.forReturn(2_000_000, 2, ApkFacts(pkg, 1_015_000, 2, sameSigner = false)) is Plan.Refuse)
        // A different signer is refused even when an in-place install would be possible.
        assertTrue(ReturnPlanner.forReturn(1_015_000, 2, ApkFacts(pkg, 1_015_000, 2, sameSigner = false)) is Plan.Refuse)
        // Unknown signer in place: Android's installer still enforces it.
        assertEquals(Plan.InPlace, ReturnPlanner.forReturn(1_015_000, 2, ApkFacts(pkg, 1_015_000, 2, sameSigner = null)))
    }

    @Test fun signerSetsCompareExactly() {
        assertEquals(true, ReturnPlanner.signersMatch(setOf("ab"), setOf("ab")))
        assertEquals(false, ReturnPlanner.signersMatch(setOf("ab"), setOf("cd")))
        assertEquals(false, ReturnPlanner.signersMatch(setOf("ab", "cd"), setOf("ab")))
        assertNull(ReturnPlanner.signersMatch(emptySet(), setOf("ab")))
        assertNull(ReturnPlanner.signersMatch(setOf("ab"), emptySet()))
    }

    @Test fun foreignOrUnreadableApksAreRefused() {
        assertTrue(ReturnPlanner.forReturn(1, 2, null) is Plan.Refuse)
        assertTrue(ReturnPlanner.forReturn(1, 2, ApkFacts("com.evil.app", 9_000_000, 2)) is Plan.Refuse)
        assertTrue(ReturnPlanner.forReturn(1, 2, ApkFacts(null, 9_000_000, 2)) is Plan.Refuse)
        assertTrue(ReturnPlanner.forReturn(1, 2, ApkFacts(pkg, 0, 2)) is Plan.Refuse)
    }

    @Test fun updateGuardStopsOnlyLowerCodesAndForeignApks() {
        assertNull(ReturnPlanner.updateBlocker(1_015_000, ApkFacts(pkg, 1_015_000, 2)))
        assertNull(ReturnPlanner.updateBlocker(1_015_000, ApkFacts(pkg, 1_016_000, 2)))
        assertNotNull(ReturnPlanner.updateBlocker(1_016_000, ApkFacts(pkg, 1_015_000, 2)))
        assertNotNull(ReturnPlanner.updateBlocker(1, ApkFacts("x.y", 9, 2)))
    }

    @Test fun guidedBackupMatchesWhatTheTargetCanRestore() {
        assertEquals(ReturnPlanner.GuidedBackup.FULL_ZIP,
            ReturnPlanner.guidedBackup(ApkFacts(pkg, 1, 2, zipRestore = true)))
        // 1.15.0 and older have no FULL_BACKUP_RESTORE flag: only the JSON backup is readable there.
        assertEquals(ReturnPlanner.GuidedBackup.JSON_ONLY, ReturnPlanner.guidedBackup(ApkFacts(pkg, 1, 2)))
        assertEquals(ReturnPlanner.GuidedBackup.JSON_ONLY, ReturnPlanner.guidedBackup(null))
    }

    @Test fun onlyStableBuildsRecordThemselves() {
        assertEquals("1.15.0", ReturnPlanner.stableToRecord("1.15.0"))
        assertNull(ReturnPlanner.stableToRecord("1.16.0-beta.4"))
        assertNull(ReturnPlanner.stableToRecord("garbage"))
        assertTrue(ReturnPlanner.isBeta("2.0.0-beta.78"))
        assertFalse(ReturnPlanner.isBeta("1.15.0"))
        assertFalse(ReturnPlanner.isBeta("not-a-version"))
    }

    // ---------------------------------------------------------------- targets
    private fun upd(v: String, beta: Boolean = false) = ReleaseUpdate(v, "zip", "page", beta)

    @Test fun previousStableFirstThenNewest() {
        val t = ReturnPlanner.targets(listOf(upd("1.15.1"), upd("1.15.0"), upd("1.14.0")), "1.15.0")
        assertEquals(listOf("1.15.0", "1.15.1"), t.map { it.version })
    }

    @Test fun onlyNewestWhenPreviousIsNewestUnknownOrUnpublished() {
        val all = listOf(upd("1.14.0"), upd("1.15.0"))
        assertEquals(listOf("1.15.0"), ReturnPlanner.targets(all, "1.15.0").map { it.version })
        assertEquals(listOf("1.15.0"), ReturnPlanner.targets(all, null).map { it.version })
        assertEquals(listOf("1.15.0"), ReturnPlanner.targets(all, "1.13.0").map { it.version })
    }

    @Test fun betasAreNeverReturnTargets() {
        assertEquals(emptyList<ReleaseUpdate>(), ReturnPlanner.targets(listOf(upd("1.16.0-beta.3", true)), null))
    }

    private fun release(version: String, beta: Boolean = false): String {
        val name = "InfoDeck_$version.zip"
        val uri = "https://github.com/krishnabhunia/info-deck/releases/download/v$version/$name"
        return """{"tag_name":"v$version","draft":false,"prerelease":$beta,"assets":[{"name":"$name","browser_download_url":"$uri"}]}"""
    }

    @Test fun stableListIsNewestFirstAndSkipsBetasAndBadAssets() {
        val bad = """{"tag_name":"v1.13.0","draft":false,"prerelease":false,"assets":[]}"""
        val json = listOf(release("1.14.0"), release("1.16.0-beta.9", true), release("1.15.0"), bad)
            .joinToString(",", "[", "]")
        assertEquals(listOf("1.15.0", "1.14.0"), ReleaseChooser.stables(json).map { it.version })
        assertTrue(runCatching { ReleaseChooser.stables("{}") }.isFailure)
        assertTrue(runCatching { ReleaseChooser.newest("not json", "1.0.0", false) }.isFailure)
    }

    // ---------------------------------------------------------------- full backup
    @Test fun zipFileEntriesAreBareIndexes() {
        assertEquals(0, FullBackupRules.parseFileEntry("files/0"))
        assertEquals(12, FullBackupRules.parseFileEntry("files/12"))
        assertEquals("files/7", FullBackupRules.fileEntry(7))
        for (bad in listOf(
            "files/12/../evil.pdf", "files/../1", "../files/1", "files/01", "files/-1", "files/1.pdf",
            "files/abc", "/files/1", "files/", "files/1/", "files/1234567", "infodeck-backup.json"
        )) assertNull(bad, FullBackupRules.parseFileEntry(bad))
        assertTrue(runCatching { FullBackupRules.fileEntry(-1) }.isFailure)
    }

    @Test fun storedNamesWithSpacesNoLongerDropFilesFromTheBackup() {
        // Old stored names could carry "Scan 12.10.2024 copy" style extensions; entries no longer use them.
        assertEquals("pdf", FullBackupRules.safeExtension("Policy.PDF"))
        assertEquals("", FullBackupRules.safeExtension("Scan 12.10.2024 copy"))
        assertEquals("", FullBackupRules.safeExtension("noext"))
        assertEquals("", FullBackupRules.safeExtension("a.toolongextension"))
    }

    @Test fun messagesNeverHideMissingOrFailedFiles() {
        assertTrue(com.krishna.infodeck.ui.fullBackupMessage(
            com.krishna.infodeck.backup.FullBackupResult(true, 3, 1)).contains("1 attached file"))
        assertTrue(com.krishna.infodeck.ui.fullBackupMessage(
            com.krishna.infodeck.backup.FullBackupResult(false)).contains("failed"))
        val r = com.krishna.infodeck.ui.restoreMessage(
            com.krishna.infodeck.backup.RestoreResult(5, 2, 1), "Replaced", "restored")
        assertTrue(r.contains("5 cards") && r.contains("1 attached file(s) not restored"))
        assertTrue(com.krishna.infodeck.ui.restoreMessage(
            com.krishna.infodeck.backup.RestoreResult(-1), "Merged", "added").contains("failed"))
    }

    @Test fun zipIsDetectedByItsHeaderOnly() {
        assertTrue(FullBackupRules.isZip(byteArrayOf(0x50, 0x4B, 0x03, 0x04)))
        assertFalse(FullBackupRules.isZip("{\n \"app\"".toByteArray()))
        assertFalse(FullBackupRules.isZip(byteArrayOf(0x50, 0x4B)))
    }

    @Test fun schemaThreeBackupRoundTripsItsFileList() {
        val b = BackupFile(
            version = FullBackupRules.SCHEMA_WITH_FILES, exportedAt = 5L, categories = emptyList(), cards = emptyList(),
            files = listOf(BFile(10, "Policy.pdf", "u-1.pdf", "application/pdf", 1234, 0, index = 0))
        )
        val back = BackupIo.decode(BackupIo.encode(b))!!
        assertEquals(3, back.version)
        assertEquals(b.files, back.files)
    }

    @Test fun olderJsonBackupsStillDecodeWithNoFiles() {
        val old = """{"app":"InfoDeck","version":2,"exportedAt":1,"categories":[],"cards":[]}"""
        val back = BackupIo.decode(old)!!
        assertEquals(2, back.version)
        assertTrue(back.files.isEmpty())
    }

    // ---------------------------------------------------------------- class-wide gate
    @Test fun manifestSchemaMetaDataMatchesTheRoomVersion() {
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
            .map { File(it) }.first { it.exists() }.readText()
        val m = Regex("""android:name="com\.krishna\.infodeck\.DB_SCHEMA"\s+android:value="(\d+)"""").find(manifest)
        assertNotNull("DB_SCHEMA meta-data missing from AndroidManifest.xml", m)
        assertEquals(DB_SCHEMA_VERSION, m!!.groupValues[1].toInt())
        assertTrue("FULL_BACKUP_RESTORE meta-data missing",
            Regex("""android:name="com\.krishna\.infodeck\.FULL_BACKUP_RESTORE"\s+android:value="true"""").containsMatchIn(manifest))
    }
}
