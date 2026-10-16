package com.krishna.infodeck.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.krishna.infodeck.BuildConfig
import com.krishna.infodeck.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Releases are fetched over HTTPS from GitHub; only a ZIP named InfoDeck_x.y.z[-beta.N].zip
 * and its exact Android/InfoDeck_*.apk entry are accepted. Android's installer always asks
 * the user to confirm and verifies that the APK signing certificate matches the installed app.
 */
class UpdateManager(private val context: Context) {
    private val store = context.getSharedPreferences("infodeck_updates", Context.MODE_PRIVATE)

    companion object {
        private const val API = "https://api.github.com/repos/krishnabhunia/info-deck/releases?per_page=50"
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val MAX_JSON = 2 * 1024 * 1024
        private const val MAX_ZIP = 120L * 1024 * 1024
        private const val MAX_APK = 150L * 1024 * 1024
        /** Manifest meta-data carrying the Room schema version (see AndroidManifest.xml). */
        const val DB_SCHEMA_KEY = "com.krishna.infodeck.DB_SCHEMA"

        private fun connection(url: String): HttpURLConnection {
            require(url.startsWith("https://")) { "Only HTTPS downloads are allowed" }
            return (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 45_000
                setRequestProperty("User-Agent", "InfoDeck-Android-UpdateCheck")
                setRequestProperty("Accept", "application/vnd.github+json")
                instanceFollowRedirects = true
            }
        }
    }

    fun cached(includeBeta: Boolean): ReleaseUpdate? {
        val version = store.getString("version", null) ?: return null
        val parsed = ReleaseVersion.parse(version) ?: return null
        val installed = ReleaseVersion.parse(BuildConfig.VERSION_NAME) ?: return null
        if (parsed <= installed || (parsed.beta != null && !includeBeta)) return null
        val url = store.getString("zip_url", null) ?: return null
        val page = store.getString("page_url", null) ?: return null
        val name = "InfoDeck_$version.zip"
        if (url != "https://github.com/krishnabhunia/info-deck/releases/download/v$version/$name") return null
        return ReleaseUpdate(version, url, page, parsed.beta != null)
    }

    /** The periodic worker and app startup share a daily throttle; the Settings button bypasses it. */
    suspend fun check(force: Boolean, includeBeta: Boolean): ReleaseUpdate? = withContext(Dispatchers.IO) {
        val last = store.getLong("checked_at", 0L)
        val now = System.currentTimeMillis()
        if (!force && last > 0L && now >= last && now - last < DAY_MS &&
            store.getBoolean("included_beta", false) == includeBeta) {
            return@withContext cached(includeBeta)
        }

        val json = fetchReleasesJson()
        val result = ReleaseChooser.newest(json, BuildConfig.VERSION_NAME, includeBeta)
        store.edit().apply {
            putLong("checked_at", System.currentTimeMillis())
            putBoolean("included_beta", includeBeta)
            if (result == null) {
                remove("version"); remove("zip_url"); remove("page_url")
            } else {
                putString("version", result.version)
                putString("zip_url", result.zipUrl)
                putString("page_url", result.pageUrl)
            }
        }.apply()
        result
    }

    /** Official stable releases, newest first. Always a fresh GitHub call (the user asked). */
    suspend fun stableReleases(): List<ReleaseUpdate> = withContext(Dispatchers.IO) {
        ReleaseChooser.stables(fetchReleasesJson())
    }

    private fun fetchReleasesJson(): String = connection(API).let { conn ->
        try {
            if (conn.responseCode != 200) error("GitHub HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(MAX_JSON + 1)
                val content = StringBuilder()
                while (content.length < buffer.size) {
                    val count = reader.read(buffer, 0, buffer.size - content.length)
                    if (count < 0) break
                    content.append(buffer, 0, count)
                }
                require(content.length <= MAX_JSON) { "GitHub response too large" }
                content.toString()
            }
        } finally { conn.disconnect() }
    }

    /** Extract only the expected APK from the official release ZIP into private cache storage. */
    suspend fun downloadApk(update: ReleaseUpdate, allowOlder: Boolean = false): File = withContext(Dispatchers.IO) {
        val installed = ReleaseVersion.parse(BuildConfig.VERSION_NAME)
        val target = ReleaseVersion.parse(update.version)
        require(installed != null && target != null) { "Unreadable version" }
        // A return to stable (design 1.15.0.D2) may target an older versionName, never a beta.
        if (allowOlder) require(target.beta == null && target != installed) { "Not a stable release" }
        else require(target > installed) { "Not a newer update" }
        val name = "InfoDeck_${update.version}"
        require(update.zipUrl == "https://github.com/krishnabhunia/info-deck/releases/download/v${update.version}/$name.zip") {
            "Unexpected GitHub download URL"
        }
        val folder = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(folder, "$name.apk")
        val temp = File(folder, "$name.apk.part")
        temp.delete()
        val expected = "Android/$name.apk"
        var found = false
        val conn = connection(update.zipUrl)
        try {
            if (conn.responseCode != 200) error("Download HTTP ${conn.responseCode}")
            val size = conn.contentLengthLong
            require(size <= MAX_ZIP || size < 0) { "Release archive is too large" }
            // Limit compressed bytes even if Content-Length is missing.
            val incoming = object : java.io.FilterInputStream(conn.inputStream) {
                var bytes = 0L
                override fun read(): Int {
                    val b = super.read()
                    if (b >= 0) { bytes++; require(bytes <= MAX_ZIP) { "Archive too large" } }
                    return b
                }
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val n = super.read(b, off, len)
                    if (n > 0) { bytes += n; require(bytes <= MAX_ZIP) { "Archive too large" } }
                    return n
                }
            }
            ZipInputStream(incoming.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name == expected) {
                        require(entry.size <= MAX_APK || entry.size < 0) { "APK too large" }
                        FileOutputStream(temp).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var written = 0L
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                written += n
                                require(written <= MAX_APK) { "APK too large" }
                                output.write(buffer, 0, n)
                            }
                            require(written > 0) { "APK is empty" }
                        }
                        found = true
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            require(found) { "Release ZIP contains no expected Android APK" }
            if (apk.exists()) apk.delete()
            require(temp.renameTo(apk)) { "Could not save update APK" }
            apk
        } catch (e: Exception) {
            temp.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }


    val installedCode: Long get() = BuildConfig.VERSION_CODE.toLong()

    /** Read package name, versionCode and DB schema from a downloaded APK without installing it. */
    fun inspect(apk: File): ReturnPlanner.ApkFacts? = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_META_DATA)
        if (info == null) {
            Logger.e("UpdateManager.inspect", "not a readable APK: ${apk.name}")
            null
        } else {
            val schema = info.applicationInfo?.metaData?.getInt(DB_SCHEMA_KEY, -1)?.takeIf { it > 0 }
            ReturnPlanner.ApkFacts(info.packageName, PackageInfoCompat.getLongVersionCode(info), schema)
        }
    } catch (e: Exception) {
        Logger.e("UpdateManager.inspect", "APK inspection failed", e); null
    }

    /** Copy a downloaded file to a place the user picked, so it survives an uninstall. */
    suspend fun saveCopy(source: File, target: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(target)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: error("no output stream")
            true
        } catch (e: Exception) {
            Logger.e("UpdateManager.saveCopy", "could not save ${source.name}", e); false
        }
    }

    /** Opens Android's own uninstall confirmation for InfoDeck. Never silent. */
    fun uninstallSelf() {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** No silent installs: the system package installer requires user approval. */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
