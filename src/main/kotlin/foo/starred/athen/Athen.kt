@file:Suppress("ConstPropertyName")

package foo.starred.athen

import com.google.gson.Gson
import foo.starred.athen.annotations.loader.AthenAnnotationLoader
import foo.starred.athen.api.messaging.impl.MessagingAPI.dev
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.AthenAPI
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.module.irc.ConfigIrcUI
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.InternalEvent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.core.on
import foo.starred.kbus.impl.KBus
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.api.EMPTY_COMPONENT
import foo.starred.kontext.api.scheduling.scheduler.extensions.clientTicks
import foo.starred.kontext.api.storage.impl.generic.extension.boolean
import foo.starred.kontext.api.storage.impl.generic.extension.string
import foo.starred.kontext.api.text.parser.impl.parse
import foo.starred.updater.logic.source.impl.ModrinthUpdateSource
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

object Athen : ClientModInitializer {
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(Data.name)

    @JvmField
    val JSON: JsonStore = JsonStore("main/config")

    @JvmField
    val GSON: Gson = Gson()

    @JvmField
    val SCOPE: CoroutineScope = CoroutineScope(Dispatchers.Default + CoroutineName(Data.name))

    @JvmField
    val BUS: KBus = KBus()

    override fun onInitializeClient() {
        InternalEvent.Mod.Loading.Start.post()
        AthenAnnotationLoader.load()

        ModrinthUpdateSource("athen").init(Data.Version.current)

        on<LocationEvent.Server.Connect> {
            Scheduler.schedule(20.clientTicks, ::install)
            Scheduler.schedule(60.clientTicks, ::broadcast)
            Scheduler.repeat(1.hours, action = ::broadcast)
        }.once()

        InternalEvent.Mod.Loading.End.post()
    }

    private fun install() {
        if (Data.Version.last.isEmpty() || Data.Version.last == "0.3.7") {
            MinecraftGui.screen(ConfigIrcUI)
        }

        if (Data.Version.last == Data.Version.current) return
        Data.Version.last = Data.Version.current

        val divider = ("<dark_gray><strikethrough>" + MinecraftChat.repeat("-")).parse()

        MinecraftMessages.display(divider)
        MinecraftMessages.display("<${MochaColorScheme.Lavender.argb}>" + MinecraftChat.center("Athen"))
        MinecraftMessages.display(divider)
        MinecraftMessages.display("<gray>You are now on Athen <dark_gray>(v${Data.Version.current})<gray>!")
        MinecraftMessages.display(EMPTY_COMPONENT)
        MinecraftMessages.display("<gray>Quick Start:")
        MinecraftMessages.display("  <aqua>/athen config <gray>- Open configuration menu")
        MinecraftMessages.display("  <aqua>/athen hud <gray>- Position HUD elements")
        MinecraftMessages.display("  <aqua>/athen help <gray>- View all commands")
        MinecraftMessages.display(EMPTY_COMPONENT)

        MinecraftMessages.display("<hover:<${MochaColorScheme.Lavender.argb}>Click to join!><click:url:${Data.discord}><gray>Need help? Click to join our Discord!")

        MinecraftMessages.display(divider)
        MinecraftMessages.display("<gray><hover:<green>Click to open page!><click:url:https://patreon.com/starredskies>Want to help support the development for mods like Athen? Click here to open the Patreon :3")
        MinecraftMessages.display(divider)
        MinecraftMessages.display("<click:command:/athen toggle feature irc><dark_red><bold>Do you want to enable/disable the Athen IRC? Click here!")
        MinecraftMessages.display(divider)
    }

    private fun broadcast() {
        AthenAPI.data("broadcast.txt").request {
            success<String> { string ->
                val parsed = string.trim().takeIf { it.isNotBlank() && it != Data.broadcast } ?: return@success

                "<hover:<${MochaColorScheme.Lavender.argb}>Broadcasted message!>$parsed".mod()
                Data.broadcast = parsed
            }

            error {
                "Failed to read broadcast: ${it.message}".dev()
            }
        }
    }

    object Data {
        const val discord: String = "https://discord.gg/DB5S3DjQVa"
        const val name: String = /*$ mod_name*/"Athen"
        const val id: String = /*$ mod_id*/"athen"

        var broadcast: String by JSON.string("broadcast")
        var debug: Boolean by JSON.boolean("debug")

        object Version {
            val current: String = /*$ mod_version*/"0.4.2"

            var last: String by JSON.string($$"version$last")
        }
    }
}
