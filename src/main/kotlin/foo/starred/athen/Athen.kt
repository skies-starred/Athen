@file:Suppress("ConstPropertyName")

package foo.starred.athen

import com.google.gson.Gson
import foo.starred.athen.annotations.AnnotationLoader
import foo.starred.athen.api.messaging.impl.MessagingAPI.dev
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.core.on
import foo.starred.athen.modules.impl.Dev
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.athen.utils.data
import foo.starred.snowbird.api.EMPTY_COMPONENT
import foo.starred.snowbird.api.center
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.api.scheduling.scheduler.extensions.clientTicks
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.literal
import foo.starred.updater.logic.source.impl.ModrinthUpdateSource
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.time.Duration.Companion.hours

object Athen : ClientModInitializer {
    const val modVersion: String = /*$ mod_version*/"0.3.2"
    const val modId: String = /*$ mod_id*/"athen"
    const val modName: String = /*$ mod_name*/"Athen"
    const val discordUrl: String = "https://discord.gg/DB5S3DjQVa"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(Athen::class.java)

    @JvmField
    val GSON: Gson = Gson()

    @JvmField
    val SCOPE: CoroutineScope = CoroutineScope(Dispatchers.Default + CoroutineName(modName))

    override fun onInitializeClient() {
        AnnotationLoader.load()
        ModrinthUpdateSource("athen").init(modVersion)

        on<LocationEvent.Server.Connect> {
            Scheduler.schedule(20.clientTicks) { li() }
            Scheduler.schedule(60.clientTicks) { broadcast() }
            Scheduler.repeat(1.hours) { broadcast() }
        }.once()
    }

    private fun li() {
        if (Dev.lastVersion == modVersion) return
        Dev.lastVersion = modVersion

        val divider = ("§8§m" + "-".repeat()).literal()

        divider.lie()
        "§d§l$modName".center().lie()
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
        "<hover:<green>Click to open page!><click:url:https://patreon.com/starredskies>Want to help support the development for mods like Athen? Click here to open the Patreon :3".parse().lie()
        divider.lie()
    }

    private fun broadcast() {
        "broadcast.txt".data.request {
            success<String> {
                val str = it.trim().takeIf { s -> s.isNotBlank() && s != Dev.lastBroadcast } ?: return@success

                "<hover:<${Mocha.Lavender.argb}>Broadcasted message!>$str".mod()
                Dev.lastBroadcast = str
            }

            error {
                "Failed to read broadcast: ${it.message}".dev()
            }
        }
    }
}
