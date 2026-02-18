@file:Suppress("ConstPropertyName")

package xyz.aerii.athen

import com.google.gson.JsonObject
import kotlinx.coroutines.CompletableDeferred
import net.fabricmc.api.ClientModInitializer
import net.minecraft.SharedConstants
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import xyz.aerii.athen.annotations.AnnotationLoader
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.*
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Texter.onUrl
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.updater.ModUpdater
import kotlin.time.Duration.Companion.hours

object Athen : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.0.9"
    const val modId: String = /*$ mod_id*/ "athen"
    const val modName: String = /*$ mod_name*/ "Athen"
    const val discordUrl: String = "https://discord.gg/DB5S3DjQVa"
    const val apiUrl: String = "https://api.aerii.xyz"

    @JvmStatic
    val ping = CompletableDeferred<Unit>()

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Athen::class.java)

    override fun onInitializeClient() {
        AnnotationLoader.load()
        ping()

        on<LocationEvent.ServerConnect> {
            if (!Dev.debug) return@on

            "<- Normal PrefixType".modMessage(Typo.PrefixType.DEFAULT)
            "<- Error PrefixType".modMessage(Typo.PrefixType.ERROR)
            "<- Success PrefixType".modMessage(Typo.PrefixType.SUCCESS)
            "<- Dev PrefixType".modMessage(Typo.PrefixType.DEV)
        }

        on<LocationEvent.ServerConnect> {
            Chronos.Tick after 20 then {
                if (Dev.lastVersion != modVersion) li()
                "Debug mode is enabled -> Run \"/$modId toggle dev\" to toggle.".devMessage()
            }

            Chronos.Tick after 60 then {
                ModUpdater.checkAndNotify()
                broadcast()
            }

            Chronos.Time every 1.hours repeat {
                broadcast()
            }
        }.once()
    }

    private fun li() {
        Dev.lastVersion = modVersion

        val divider = ("§8§m" + "-".repeatBreak()).literal()

        divider.lie()
        "§d§lWelcome to $modName!".centeredText().lie()
        divider.lie()
        "".lie()
        "§7Thank you for installing $modName §8(v$modVersion)§7.".lie()
        "".lie()
        "§7Quick Start:".lie()
        "  §b/$modId config §7- Open configuration menu".lie()
        "  §b/$modId hud §7- Position HUD elements".lie()
        "  §b/$modId help §7- View all commands".lie()
        "".lie()

        "§7Need help? Click to join our Discord!"
            .literal()
            .onUrl(discordUrl)
            .onHover("Click to join!".literal().withColor(Mocha.Lavender.argb))
            .lie()

        divider.lie()
    }

    private fun ping() {
        Beacon.post("$apiUrl/ping") {
            timeout(connect = 5_000, read = 10_000)
            retries(max = 2, delay = 1000L)
            json(mapOf(
                "uuid" to (Smoothie.client.user.profileId ?: Smoothie.client.player?.uuid ?: Smoothie.client.user.name),
                "mod_version" to modVersion,
                "game_version" to SharedConstants.getCurrentVersion().name()
            ))

            onJsonSuccess { json: JsonObject ->
                json.get("sha")?.asString?.let { Dev.remoteAssetsVersion = it }
                ping.complete(Unit)
            }

            onError {
                LOGGER.error("Failed to send ping: ${it.message}")
                ping.complete(Unit)
            }
        }
    }

    private fun broadcast() {
        val broadcastFile = Roulette.file("broadcast_text.txt")

        if (!broadcastFile.exists()) {
            "Broadcast file not found".devMessage()
            return
        }

        runCatching {
            val message = broadcastFile.readText().trim().takeIf { it.isNotBlank() } ?: return

            if (message != Dev.lastBroadcast) {
                message
                    .parse()
                    .onHover("Broadcasted message!".literal().withColor(Mocha.Lavender.argb))
                    .modMessage()

                Dev.lastBroadcast = message
            }
        }.onFailure {
            "Failed to read broadcast: ${it.message}".devMessage()
        }
    }
}