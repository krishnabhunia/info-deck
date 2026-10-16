package com.krishna.infodeck.update

/**
 * Pure rules for going back from a beta to a stable version (design 1.15.0.D2).
 *
 * Android refuses an APK whose versionCode is LOWER than the installed one, but accepts an
 * EQUAL one and keeps the app's data. Beta builds therefore carry the versionCode of the stable
 * they are built on (CI: -PversionCodeOverride), which makes the stable installable in place.
 * When that is not possible — an older beta with a high code, or a beta whose database schema
 * is newer than the stable understands — the app falls back to a guided reinstall.
 */
object ReturnPlanner {
    const val PACKAGE = "com.krishna.infodeck"

    /** Every InfoDeck release before the DB_SCHEMA manifest entry existed used Room schema 2. */
    const val LEGACY_DB_SCHEMA = 2

    /** What the app could read from a downloaded APK file without installing it. */
    data class ApkFacts(
        val packageName: String?, val versionCode: Long, val dbSchema: Int?,
        /** True when that build can restore a full-backup ZIP (manifest FULL_BACKUP_RESTORE). */
        val zipRestore: Boolean = false,
        /** Same signing certificate(s) as the installed app; null when it could not be read. */
        val sameSigner: Boolean? = null
    )

    /** SHA-256 certificate digests compared as sets; null when either side is unknown. */
    fun signersMatch(archive: Set<String>, installed: Set<String>): Boolean? =
        if (archive.isEmpty() || installed.isEmpty()) null else archive == installed

    /** Which backup the guided reinstall must save so the target version can restore it. */
    fun guidedBackup(apk: ApkFacts?): GuidedBackup =
        if (apk?.zipRestore == true) GuidedBackup.FULL_ZIP else GuidedBackup.JSON_ONLY

    enum class GuidedBackup { FULL_ZIP, JSON_ONLY }

    sealed class Plan {
        /** Android will install it over the top; all data stays. */
        object InPlace : Plan()
        /** Must uninstall first; [reason] is shown to the user. */
        data class Reinstall(val reason: String) : Plan()
        /** Not an APK InfoDeck may install; nothing changes. */
        data class Refuse(val reason: String) : Plan()
    }

    fun isBeta(versionName: String): Boolean = ReleaseVersion.parse(versionName)?.beta != null

    /** Plan for returning to a stable APK that has already been downloaded and read. */
    fun forReturn(installedCode: Long, installedSchema: Int, apk: ApkFacts?): Plan {
        if (apk == null) return Plan.Refuse("The downloaded file is not a valid Android app")
        if (apk.packageName != PACKAGE) return Plan.Refuse("The downloaded app is not InfoDeck")
        if (apk.versionCode <= 0) return Plan.Refuse("The downloaded app has no version number")
        if (apk.sameSigner == false) return Plan.Refuse("The downloaded app is signed with a different key")
        val schema = apk.dbSchema?.takeIf { it > 0 } ?: LEGACY_DB_SCHEMA
        val reinstallReason = when {
            apk.versionCode < installedCode -> "This beta has a higher Android version number than the stable version"
            schema < installedSchema -> "This beta changed the database, which the stable version cannot read"
            else -> return Plan.InPlace // Android itself verifies the signer for an in-place install.
        }
        // After an uninstall Android has nothing to compare against, so the signer must be proven here.
        if (apk.sameSigner != true) return Plan.Refuse("Could not confirm the downloaded app's signature")
        return Plan.Reinstall(reinstallReason)
    }

    /**
     * Guard for an ordinary update: stop a lower versionCode with a clear message before
     * Android shows its generic "App not installed". Returns null when the install may proceed.
     */
    fun updateBlocker(installedCode: Long, apk: ApkFacts): String? {
        if (apk.packageName != PACKAGE) return "The downloaded app is not InfoDeck"
        if (apk.versionCode < installedCode) {
            return "This release was built on an older version. Return to stable first, or wait for the next build."
        }
        return null
    }

    /** A stable build records itself so a later beta knows where to return to. */
    fun stableToRecord(versionName: String): String? {
        val v = ReleaseVersion.parse(versionName) ?: return null
        return if (v.beta == null) v.text else null
    }

    /**
     * Return targets, best first: the previous stable this phone ran (if still published),
     * then the newest stable when it differs. Without a usable record, only the newest.
     */
    fun targets(stables: List<ReleaseUpdate>, previousStable: String?): List<ReleaseUpdate> {
        val sorted = stables.filter { !it.beta }
            .mapNotNull { r -> ReleaseVersion.parse(r.version)?.let { it to r } }
            .sortedByDescending { it.first }
            .map { it.second }
        val newest = sorted.firstOrNull() ?: return emptyList()
        val previous = previousStable?.let { p -> sorted.firstOrNull { it.version == p } }
        return if (previous == null || previous.version == newest.version) listOf(newest)
        else listOf(previous, newest)
    }
}
