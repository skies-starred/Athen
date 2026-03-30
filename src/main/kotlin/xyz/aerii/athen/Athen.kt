@file:Suppress("ConstPropertyName")

package xyz.aerii.athen

import net.fabricmc.api.ClientModInitializer
import net.minecraft.SharedConstants
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick
import xyz.aerii.athen.annotations.AnnotationLoader
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.*
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.athen.modules.impl.render.VisualWords
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import xyz.aerii.athen.utils.api
import xyz.aerii.athen.utils.data
import kotlin.time.Duration.Companion.hours

object Athen : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/ "0.1.4"
    const val modId: String = /*$ mod_id*/ "athen"
    const val modName: String = /*$ mod_name*/ "Athen"
    const val discordUrl: String = "https://discord.gg/DB5S3DjQVa"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Athen::class.java)

    override fun onInitializeClient() {
        AnnotationLoader.load()
        ping()

        on<LocationEvent.Server.Connect> {
            Chronos.Tick after 20 then ::li
            Chronos.Tick after 60 then ::broadcast
            Chronos.Time every 1.hours repeat ::broadcast
        }.once()
    }

    private fun li() {
        if (Dev.lastVersion == modVersion) return
        Dev.lastVersion = modVersion

        val divider = ("§8§m" + "-".repeatBreak()).literal()

        divider.lie()
        "§d§l$modName".centeredText().lie()
        divider.lie()
        "<gray>Thank you for installing $modName <dark_gray>(v$modVersion)<gray>.".parse().lie()
        EMPTY_COMPONENT.lie()
        "<gray>Quick Start:".parse().lie()
        "  <aqua>/$modId config <gray>- Open configuration menu".parse().lie()
        "  <aqua>/$modId hud <gray>- Position HUD elements".parse().lie()
        "  <aqua>/$modId help <gray>- View all commands".parse().lie()
        EMPTY_COMPONENT.lie()

        "<hover:<${Mocha.Lavender.argb}>Click to join!><click:url:$discordUrl><gray>Need help? Click to join our Discord!".parse().lie()

        divider.lie()

        if (VisualWords.enabled) return
        "<hover:<green>Click to enable!>A lot of time and effort goes into maintaining Athen. Enable <${Mocha.Mauve.argb}>Visual Words<r> to highlight the amazing people who support its development! <gray>Click on this message to enable!".parse().onClick { ConfigManager.updateConfig("visualWords", true) }.lie()
        divider.lie()
    }

    private fun ping() {
        Beacon.post("ping".api) {
            timeout(connect = 5_000, read = 10_000)
            retries(max = 2, delay = 1000L)
            json(mapOf(
                "uuid" to (Smoothie.client.user.profileId ?: Smoothie.client.player?.uuid ?: Smoothie.client.user.name),
                "mod_version" to modVersion,
                "game_version" to SharedConstants.getCurrentVersion().name()
            ))

            onError {
                LOGGER.error("Failed to send ping: ${it.message}")
            }
        }
    }

    private fun broadcast() {
        Beacon.get("broadcast.txt".data) {
            onSuccess {
                val str = it.trim().takeIf { s -> s.isNotBlank() && s != Dev.lastBroadcast } ?: return@onSuccess

                str.parse().onHover("<${Mocha.Lavender.argb}>Broadcasted message!").modMessage()
                Dev.lastBroadcast = str
            }

            onError {
                "Failed to read broadcast: ${it.message}".devMessage()
            }
        }
    }
}