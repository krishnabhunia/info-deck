package com.krishna.infodeck

import com.krishna.infodeck.update.ReleaseChooser
import com.krishna.infodeck.update.ReleaseVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdatesTest {
    private fun release(version: String, beta: Boolean = false, draft: Boolean = false, asset: Boolean = true): String {
        val name = "InfoDeck_$version.zip"
        val uri = "https://github.com/krishnabhunia/info-deck/releases/download/v$version/$name"
        val assets = if (asset) """[{"name":"$name","browser_download_url":"$uri"}]""" else "[]"
        return """{"tag_name":"v$version","draft":$draft,"prerelease":$beta,"assets":$assets}"""
    }

    private fun list(vararg releases: String) = releases.joinToString(",", "[", "]")

    @Test fun semanticOrderingHandlesDoubleDigitMinorVersions() {
        assertTrue(ReleaseVersion.parse("1.14.0")!! > ReleaseVersion.parse("1.9.9")!!)
        assertTrue(ReleaseVersion.parse("1.14.0")!! > ReleaseVersion.parse("1.14.0-beta.999")!!)
        assertTrue(ReleaseVersion.parse("1.14.0-beta.10")!! > ReleaseVersion.parse("1.14.0-beta.2")!!)
    }

    @Test fun rejectsInvalidOrMalformedVersions() {
        assertNull(ReleaseVersion.parse("1.14"))
        assertNull(ReleaseVersion.parse("1.14.0-rc.1"))
        assertNull(ReleaseVersion.parse("1.014.0"))
    }

    @Test fun stableDefaultDoesNotShowPreReleases() {
        val data = list(release("1.14.0-beta.7", beta = true), release("1.13.1"))
        assertEquals("1.13.1", ReleaseChooser.newest(data, "1.13.0", false)?.version)
        assertEquals("1.14.0-beta.7", ReleaseChooser.newest(data, "1.13.0", true)?.version)
    }

    @Test fun stableBeatsBetaForSameBaseVersion() {
        val data = list(release("1.14.0-beta.99", beta = true), release("1.14.0"))
        assertEquals("1.14.0", ReleaseChooser.newest(data, "1.14.0-beta.4", true)?.version)
    }

    @Test fun installedStableNeverDowngradesToBeta() {
        assertNull(ReleaseChooser.newest(list(release("1.14.0-beta.99", beta = true)), "1.14.0", true))
    }

    @Test fun draftAndMissingZipAreIgnored() {
        val data = list(release("1.17.0", draft = true), release("1.16.0", asset = false), release("1.15.0"))
        assertEquals("1.15.0", ReleaseChooser.newest(data, "1.14.0", false)?.version)
    }

    @Test fun alreadyInstalledAndBadChannelMetadataAreIgnored() {
        assertNull(ReleaseChooser.newest(list(release("1.13.0")), "1.13.0", false))
        assertNull(ReleaseChooser.newest(list(release("1.14.0-beta.1", beta = false)), "1.13.0", true))
    }
}
