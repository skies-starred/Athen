package xyz.aerii.athen.updater

import com.google.gson.JsonElement
import moe.nea.libautoupdate.CurrentVersion
import moe.nea.libautoupdate.PotentialUpdate
import moe.nea.libautoupdate.UpdateContext
import moe.nea.libautoupdate.UpdateTarget
import net.minecraft.SharedConstants
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.PrefixType
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.impl.Dev
import java.util.concurrent.CompletableFuture

@Priority(-6)
object ModUpdater {
    private var skippedVersion: String by Dev.file.string("version")
    var trulySkip: String by Dev.file.string("trulySkipVersion")

    private val context = UpdateContext(
        ModrinthUpdateSource("avbpWn0t", SharedConstants.getCurrentVersion().name()),
        UpdateTarget.deleteAndSaveInTheSameFolder(Athen::class.java),
        getCurrent(),
        Athen.modId
    )

    init {
        context.cleanup()
    }

    private fun getCurrent() = object : CurrentVersion {
        override fun display() = Athen.modVersion

        override fun isOlderThan(element: JsonElement): Boolean {
            if (!element.isJsonPrimitive) return true

            fun String.parse() = removePrefix("v").split('.', '-').map { it.toIntOrNull() ?: 0 }

            val local = Athen.modVersion.parse()
            val remote = element.asString.parse()

            return local
                .zip(remote + List(maxOf(0, local.size - remote.size)) { 0 })
                .any { (l, r) -> l < r }
        }
    }

    fun checkForUpdate(stream: String = "release"): CompletableFuture<PotentialUpdate> {
        return context.checkUpdate(stream)
    }

    fun checkAndNotify(stream: String = "release") {
        checkForUpdate(stream).thenAccept { update ->
            if (update.isUpdateAvailable) {
                val newVersion = update.update.versionName
                if (skippedVersion == newVersion && trulySkip == newVersion) return@thenAccept

                "Update available: $newVersion".modMessage()
                "Run /${Athen.modId} update to install".modMessage()

                if (newVersion != skippedVersion) {
                    client.execute {
                        McClient.setScreen(UpdateGUI(
                            Athen.modVersion,
                            newVersion,
                            onUpdate = { installUpdate(stream) },
                            onSkip = { skippedVersion = newVersion },
                            onRemind = {}
                        ))
                    }
                }
            }
        }.exceptionally {
            Athen.LOGGER.error("Failed to check for updates: ${it.message}")
            null
        }
    }

    fun installUpdate(stream: String = "release"): CompletableFuture<Boolean> {
        return checkForUpdate(stream).thenCompose { update ->
            if (!update.isUpdateAvailable) {
                "Already on latest version".modMessage(PrefixType.ERROR)
                return@thenCompose CompletableFuture.completedFuture(false)
            }

            "Downloading update: ${update.update.versionName}".modMessage()
            update.launchUpdate().thenApply {
                "Update downloaded! Restart to apply.".modMessage(PrefixType.SUCCESS)
                true
            }
        }.exceptionally {
            "Update failed: ${it.message}".modMessage(PrefixType.ERROR)
            Athen.LOGGER.error("Failed to install update: ${it.message}")
            false
        }
    }
}