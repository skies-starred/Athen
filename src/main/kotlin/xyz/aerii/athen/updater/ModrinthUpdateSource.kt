package xyz.aerii.athen.updater

import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import moe.nea.libautoupdate.JsonUpdateSource
import moe.nea.libautoupdate.UpdateData
import java.util.concurrent.CompletableFuture

class ModrinthUpdateSource(
    private val projectId: String,
    private val minecraftVersion: String
) : JsonUpdateSource() {

    override fun checkUpdate(updateStream: String): CompletableFuture<UpdateData> {
        val futureVersions = getJsonFromURL<List<ModrinthVersion>>(
            "https://api.modrinth.com/v2/project/$projectId/version",
            object : TypeToken<List<ModrinthVersion>>() {}.type
        )

        fun allow(type: String, stream: String) =
            when (stream) {
                "release" -> type == "release"
                "beta" -> type != "alpha"
                else -> true
            }

        return futureVersions.thenApply { versions ->
            versions
                ?.filter { it.gameVersions.contains(minecraftVersion) }
                ?.filter { allow(it.versionType, updateStream) }
                ?.maxByOrNull { it.datePublished }
                ?.let { latest ->
                    val file = latest.files.firstOrNull { it.primary } ?: latest.files.firstOrNull()
                    file?.let {
                        UpdateData(
                            latest.versionNumber,
                            JsonPrimitive(latest.versionNumber),
                            it.hashes["sha256"],
                            it.url
                        )
                    }
                }
        }
    }

    data class ModrinthVersion(
        @SerializedName("version_number")
        val versionNumber: String,
        @SerializedName("version_type")
        val versionType: String,
        @SerializedName("date_published")
        val datePublished: String,
        @SerializedName("game_versions")
        val gameVersions: List<String>,
        val files: List<ModrinthFile>
    )

    data class ModrinthFile(
        val url: String,
        val hashes: Map<String, String>,
        val primary: Boolean
    )
}