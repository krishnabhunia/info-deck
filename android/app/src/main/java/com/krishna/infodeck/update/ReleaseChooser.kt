package com.krishna.infodeck.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** For the same x.y.z, stable is newer than every beta. */
data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int, val beta: Int?) :
    Comparable<ReleaseVersion> {
    val text: String get() = "$major.$minor.$patch" + (beta?.let { "-beta.$it" } ?: "")

    override fun compareTo(other: ReleaseVersion): Int {
        for ((a, b) in listOf(major to other.major, minor to other.minor, patch to other.patch)) {
            if (a != b) return a.compareTo(b)
        }
        return when {
            beta == null && other.beta != null -> 1
            beta != null && other.beta == null -> -1
            else -> (beta ?: 0).compareTo(other.beta ?: 0)
        }
    }

    companion object {
        private val VERSION = Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-beta\.(0|[1-9]\d*))?$""")
        fun parse(value: String): ReleaseVersion? {
            val m = VERSION.matchEntire(value.removePrefix("v")) ?: return null
            val pieces = (1..3).map { m.groupValues[it].toIntOrNull() ?: return null }
            if (pieces[1] >= 1000 || pieces[2] >= 1000) return null
            val beta = m.groupValues[4].takeIf { it.isNotEmpty() }?.toIntOrNull()
            if (m.groupValues[4].isNotEmpty() && beta == null) return null
            return ReleaseVersion(pieces[0], pieces[1], pieces[2], beta)
        }
    }
}

data class ReleaseUpdate(val version: String, val zipUrl: String, val pageUrl: String, val beta: Boolean)

/** Only select a correctly named ZIP published as an official InfoDeck GitHub release. */
object ReleaseChooser {
    private const val REPO = "https://github.com/krishnabhunia/info-deck"

    fun newest(releasesJson: String, installedVersion: String, includeBeta: Boolean): ReleaseUpdate? {
        val installed = ReleaseVersion.parse(installedVersion) ?: return null
        return official(releasesJson)
            .filter { (version, update) -> version > installed && (!update.beta || includeBeta) }
            .maxByOrNull { it.first }?.second
    }

    /** Every official stable release, newest first (used to return from a beta, design 1.15.0.D1). */
    fun stables(releasesJson: String): List<ReleaseUpdate> =
        official(releasesJson).filter { !it.second.beta }.sortedByDescending { it.first }.map { it.second }

    private fun official(releasesJson: String): List<Pair<ReleaseVersion, ReleaseUpdate>> {
        val releases = runCatching { Json.parseToJsonElement(releasesJson) }.getOrNull() as? JsonArray
            ?: return emptyList()
        return releases.mapNotNull { item ->
            val obj = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
            if (obj["draft"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
            val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val version = ReleaseVersion.parse(tag) ?: return@mapNotNull null
            if (tag != "v${version.text}") return@mapNotNull null
            val isBeta = obj["prerelease"]?.jsonPrimitive?.booleanOrNull ?: return@mapNotNull null
            if (isBeta != (version.beta != null)) return@mapNotNull null
            val file = "InfoDeck_${version.text}.zip"
            val base = "$REPO/releases/download/$tag/"
            val assets = runCatching { obj["assets"]?.jsonArray }.getOrNull() ?: return@mapNotNull null
            val asset = assets.firstOrNull { raw ->
                val a = runCatching { raw.jsonObject }.getOrNull() ?: return@firstOrNull false
                a["name"]?.jsonPrimitive?.contentOrNull == file &&
                    a["browser_download_url"]?.jsonPrimitive?.contentOrNull == base + file
            }?.jsonObject ?: return@mapNotNull null
            val url = asset["browser_download_url"]!!.jsonPrimitive.content
            version to ReleaseUpdate(version.text, url, "$REPO/releases/tag/$tag", isBeta)
        }
    }
}
