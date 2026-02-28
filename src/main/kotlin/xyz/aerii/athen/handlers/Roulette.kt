@file:Suppress("unused", "FunctionName", "BlockingMethodInNonBlockingContext")

package xyz.aerii.athen.handlers

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.NativeImage
import kotlinx.coroutines.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import tech.thatgravyboat.skyblockapi.utils.json.Json
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Smoothie.mainThread
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.impl.Dev
import java.io.File
import java.util.zip.ZipInputStream

/**
 * You either get it, or you don't.
 * Loads all of Athen's assets from the cloudflare worker at [Website](https://api.aerii.xyz/).
 */
@Priority
object Roulette {
    private val cacheDir = File(FabricLoader.getInstance().configDir.toFile(), "${Athen.modId}/cache/assets")
    private val manifestFile = File(cacheDir, ".manifest.json")
    private val loaded = mutableMapOf<String, ResourceLocation>()
    private val pending = mutableMapOf<String, MutableList<() -> Unit>>()
    val download = CompletableDeferred<Unit>()
    val scope = CoroutineScope(Dispatchers.IO)

    private var excludePatterns = listOf<String>()
    private var additionalFiles = mapOf<String, String>()
    private var localHash: String by Dev.file.string("assetsVersion")
    private var loadSuccess = true

    private const val MANIFEST_URL = "${Athen.apiUrl}/manifest"
    private const val DOWNLOAD_URL = "${Athen.apiUrl}/download"

    init {
        cacheDir.mkdirs()
        scope.launch {
            Athen.ping.await()
            download()
        }

        on<LocationEvent.ServerConnect> {
            if (!loadSuccess) {
                Chronos.Tick after 20 then {
                    "Failed to download assets. Some features may not work correctly.".modMessage(Typo.PrefixType.ERROR)
                }
            }

            if (download.isActive) {
                Chronos.Tick after 20 then {
                    "Still downloading assets... is your wifi ok?".modMessage(Typo.PrefixType.ERROR)
                }
            }
        }.once()
    }

    @JvmStatic
    fun file(path: String): File = File(cacheDir, path)

    @JvmStatic
    @JvmOverloads
    fun texture(path: String, namespace: String = Athen.modId): ResourceLocation =
        loaded.getOrPut(path) {
            val resourcePath = "dynamic/${path.replace("/", "_")}"
            val location = ResourceLocation.fromNamespaceAndPath(namespace, resourcePath)
            texture(path, location)
            location
        }

    private fun texture(path: String, location: ResourceLocation) {
        val cachedFile = file(path)

        if (cachedFile.exists()) {
            registerTexture(cachedFile, location, path)
            return
        }

        Athen.LOGGER.warn("Texture $path not found in cache, will register after download")

        pending.getOrPut(path) { mutableListOf() }.add {
            registerTexture(cachedFile, location, path)
            Athen.LOGGER.info("Registered downloaded texture: $path")
        }
    }

    private fun registerTexture(cachedFile: File, location: ResourceLocation, path: String) = mainThread {
        runCatching {
            val texture = DynamicTexture(
                { cachedFile.path },
                NativeImage.read(cachedFile.inputStream())
            )
            textureManager.register(location, texture)
        }.onFailure {
            Athen.LOGGER.error("Failed to register texture $location", it)
        }
    }

    private suspend fun fetchManifest() {
        val deferred = CompletableDeferred<Unit>()

        Beacon.get(MANIFEST_URL) {
            onJsonSuccess { manifest ->
                manifest.getAsJsonArray("exclude")?.let { array ->
                    excludePatterns = array.map { it.asString }
                }

                manifest.getAsJsonObject("additional")?.let { obj ->
                    additionalFiles = obj.entrySet().associate { it.key to it.value.asString }
                }

                deferred.complete(Unit)
            }

            onError {
                Athen.LOGGER.warn("Failed to fetch manifest: ${it.message}, using defaults")
                excludePatterns = listOf("README.md", ".gitignore", ".git/", "LICENSE")
                deferred.complete(Unit)
            }
        }

        deferred.await()
    }

    private suspend fun download() {
        val remoteHash = Dev.remoteAssetsVersion.takeIf { it.isNotEmpty() } ?: localHash.takeIf { it.isNotEmpty() }
        val missing = files()

        if (remoteHash != null && remoteHash != localHash) {
            fetchManifest()

            Athen.LOGGER.info("Version mismatch, downloading all assets (Remote: $remoteHash, Local: ${localHash.ifEmpty { "none" }})")
            extract(remoteHash, replaceAll = true)
            `files$additional`()

            pending.values.flatten().toList().forEach { it() }
            pending.clear()
        } else if (missing.isNotEmpty()) {
            Athen.LOGGER.info("Found ${missing.size} missing files, extracting from archive")
            extract(localHash, replaceAll = false, onlyFiles = missing)

            pending.values.flatten().toList().forEach { it() }
            pending.clear()
        } else {
            Athen.LOGGER.info("Assets up-to-date (Version: $localHash)")
        }

        download.complete(Unit)
    }

    private fun files(): Set<String> {
        return try {
            if (!manifestFile.exists()) return emptySet()

            val manifest = JsonParser.parseString(manifestFile.readText()).asJsonObject
            val files = manifest.getAsJsonArray("files")?.map { it.asString } ?: return emptySet()

            files.filter { !File(cacheDir, it).exists() }.toSet()
        } catch (e: Exception) {
            Athen.LOGGER.warn("Failed to check for missing files: ${e.message}")
            emptySet()
        }
    }

    private suspend fun extract(newHash: String, replaceAll: Boolean, onlyFiles: Set<String> = emptySet()) {
        val tempZip = File.createTempFile("${Athen.modId}-assets-", ".zip")
        val deferred = CompletableDeferred<Boolean>()

        Beacon.download(DOWNLOAD_URL, tempZip) {
            onComplete {
                deferred.complete(true)
            }

            onError {
                Athen.LOGGER.error("Failed to download assets: ${it.message}")
                loadSuccess = false
                deferred.complete(false)
            }
        }

        if (deferred.await()) {
            try {
                if (replaceAll && cacheDir.exists()) cacheDir.deleteRecursively()
                cacheDir.mkdirs()

                val extractedFiles = tempZip.unzip(onlyFiles).also { it.save() }
                localHash = newHash

                Athen.LOGGER.info("Assets extracted successfully (${extractedFiles.size} files)")
            } catch (e: Exception) {
                Athen.LOGGER.error("Failed to extract assets", e)
                loadSuccess = false
            } finally {
                tempZip.delete()
            }
        }
    }

    private suspend fun `files$additional`() {
        if (additionalFiles.isEmpty()) return

        Athen.LOGGER.info("Downloading ${additionalFiles.size} additional files")

        coroutineScope {
            additionalFiles.map { (path, url) ->
                async {
                    val targetFile = File(cacheDir, path)
                    val deferred = CompletableDeferred<Boolean>()

                    Beacon.download(url, targetFile) {
                        onComplete {
                            Athen.LOGGER.info("Downloaded additional file: $path")
                            deferred.complete(true)
                        }

                        onError {
                            Athen.LOGGER.error("Failed to download additional file $path: ${it.message}")
                            deferred.complete(false)
                        }
                    }

                    deferred.await()
                }
            }.awaitAll()
        }
    }

    private fun String.exclude(): Boolean {
        return excludePatterns.any { pattern ->
            when {
                pattern.endsWith("/") -> startsWith(pattern)
                else -> this == pattern || endsWith("/$pattern")
            }
        }
    }

    private fun File.unzip(onlyFiles: Set<String> = emptySet()): List<String> {
        val extractedFiles = mutableListOf<String>()
        val filterMode = onlyFiles.isNotEmpty()

        ZipInputStream(inputStream()).use { zis ->
            var rootDir: String? = null

            generateSequence { zis.nextEntry }.forEach { entry ->
                if (rootDir == null) rootDir = entry.name.substringBefore('/') + "/"
                val entryName = entry.name.removePrefix(rootDir)

                if (entryName.isNotEmpty() && !entryName.exclude()) {
                    if (filterMode && entryName !in onlyFiles) return@forEach

                    val targetFile = File(cacheDir, entryName)

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        targetFile.outputStream().use { output -> zis.copyTo(output) }
                        extractedFiles.add(entryName)
                    }
                }
            }
        }

        return extractedFiles
    }

    private fun List<String>.save() {
        try {
            val existingFiles = if (manifestFile.exists()) {
                val manifest = JsonParser.parseString(manifestFile.readText()).asJsonObject
                manifest.getAsJsonArray("files")?.map { it.asString }?.toMutableSet() ?: mutableSetOf()
            } else mutableSetOf()

            existingFiles.addAll(this)
            val manifest = JsonArray().apply { for (e in existingFiles) add(e) }

            manifestFile.writeText(Json.gson.toJson(mapOf("files" to manifest)))
        } catch (e: Exception) {
            Athen.LOGGER.error("Failed to save manifest", e)
        }
    }
}