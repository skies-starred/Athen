@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import com.google.gson.JsonParser
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.network.protocol.game.ServerboundChatPacket
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.websocket.SocketPacket
import xyz.aerii.athen.api.websocket.base.IWebSocket
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.InternalEvent
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.library.api.center
import xyz.aerii.library.api.client
import xyz.aerii.library.api.lie
import xyz.aerii.library.api.name
import xyz.aerii.library.api.repeat
import xyz.aerii.library.handlers.Observable
import xyz.aerii.library.handlers.parser.parse

@Load
object IRC : Module(
    "IRC",
    "Enables the IRC by default on launch if the module is enabled.",
    Category.GENERAL,
    true
), IWebSocket {
    private val _unused by config.textParagraph("Run <red>\"/athen irc help\" <r>to view all commands!")
    private val help by config.switch("Help message", true)
    private val format0 by config.textInput("Message format", "<#A6E3A1>#name <dark_gray>➤ <white>#message")
    private val discord by config.switch("Discord IRC", true)
    private val format1 by config.textInput("Discord format", "<#A6E3A1>#name <dark_gray>➤ <white>#message").dependsOn { discord }

    private val ob: Observable<Boolean> = Observable(false)
    private var cc: String = "general"

    init {
        on<PacketEvent.Send, ServerboundChatPacket> {
            if (message.startsWith('/')) return@on
            send(message)
            it.cancel()
        }.runWhen(ob)

        on<InternalEvent.WebSocket.Message> {
            if (id !in SocketPacket.IRC.ClientBound.all) return@on

            when (id) {
                SocketPacket.IRC.ClientBound.Join.id -> {
                    if (channel == null) return@on

                    cc = channel
                    "<gray>Joined channel <aqua>#$channel".parse().modMessage()
                    if (help) "<gray>Need help? Run <red>\"/athen irc help\"<r>!".parse().modMessage()
                }

                SocketPacket.IRC.ClientBound.Left.id -> {
                    if (channel == null) return@on
                    if (cc == channel) cc = "general"

                    "<gray>Left channel <aqua>#$channel".parse().modMessage()
                }

                SocketPacket.IRC.ClientBound.Chat.id -> {
                    if (channel == null) return@on
                    if (name == null) return@on
                    if (body == null) return@on
                    if (name == client.user.name) return@on
                    if (name == "[Discord]") return@on

                    "<dark_gray>[<aqua>#$channel<dark_gray>]".format0(name, body).parse().modMessage()
                }

                SocketPacket.IRC.ClientBound.Discord.id -> {
                    if (!discord) return@on
                    if (name == null) return@on
                    if (body == null) return@on

                    "<dark_gray>[<aqua>Discord<dark_gray>]".format1(name, body).parse().modMessage()
                }

                SocketPacket.IRC.ClientBound.Error.id -> {
                    "<red>IRC error: <gray>$body".parse().modMessage(Typo.PrefixType.ERROR)
                }

                SocketPacket.IRC.ClientBound.Warn.id -> {
                    "<yellow>IRC: <gray>$body".parse().modMessage(Typo.PrefixType.ERROR)
                }

                SocketPacket.IRC.ClientBound.List.id -> {
                    if (body == null) return@on

                    val ch = runCatching {
                        JsonParser.parseString(body).asJsonArray.map {
                            val arr = it.asJsonArray
                            "${arr[0].asString} (${arr[1].asInt})"
                        }.sortedWith(compareBy({ if (it.startsWith("general ")) 0 else 1 }, { it }))
                    }.getOrNull() ?: return@on

                    if (ch.isEmpty()) "<gray>No active channels.".parse().modMessage()
                    else "<gray>Active channels: <aqua>${ch.joinToString("<dark_gray>, <aqua>") { ch -> "#$ch" }}".parse().modMessage()
                }
            }
        }

        on<CommandRegistration> {
            event.register("airc") {
                thenCallback("message", StringArgumentType.greedyString()) {
                    send(StringArgumentType.getString(this@thenCallback, "message"))
                }

                thenCallback("toggle") {
                    val b = !ob.value
                    ob.value = b
                    "Send all messages to IRC <gray>➤ ${if (b) "<green>Enabled" else "<red>Disabled"}".parse().modMessage()
                }
            }

            event.register(Athen.modId) {
                then("irc") {
                    then("create") {
                        then("channel", StringArgumentType.string()) {
                            callback {
                                if (!auth) return@callback er0()
                                create(StringArgumentType.getString(this, "channel"))
                            }

                            thenCallback("pin", StringArgumentType.string()) {
                                if (!auth) return@thenCallback er0()
                                create(StringArgumentType.getString(this, "channel"), StringArgumentType.getString(this, "pin"))
                            }
                        }
                    }

                    then("join") {
                        then("channel", StringArgumentType.string()) {
                            callback {
                                if (!auth) return@callback er0()
                                join(StringArgumentType.getString(this, "channel"))
                            }

                            thenCallback("pin", StringArgumentType.string()) {
                                if (!auth) return@thenCallback er0()
                                join(StringArgumentType.getString(this, "channel"), StringArgumentType.getString(this, "pin"))
                            }
                        }
                    }

                    then("pin") {
                        thenCallback("pin", StringArgumentType.string()) {
                            if (!auth) return@thenCallback er0()
                            pin(StringArgumentType.getString(this, "pin"))
                        }
                    }

                    thenCallback("leave") {
                        if (!auth) return@thenCallback er0()
                        leave()
                    }

                    then("chat") {
                        thenCallback("message", StringArgumentType.greedyString()) {
                            if (!auth) return@thenCallback er0()
                            send(StringArgumentType.getString(this, "message"))
                        }
                    }

                    thenCallback("list") {
                        if (!auth) return@thenCallback er0()
                        list()
                    }

                    thenCallback("help") {
                        help()
                    }
                }
            }
        }
    }

    private fun create(channel: String, pin: String? = null) {
        `socket$send`(SocketPacket.IRC.ServerBound.Create.id, "c" to channel, "p" to pin)
    }

    private fun pin(pin: String) {
        `socket$send`(SocketPacket.IRC.ServerBound.Pin.id, "p" to pin)
    }

    private fun join(channel: String, pin: String? = null) {
        `socket$send`(SocketPacket.IRC.ServerBound.Join.id, "c" to channel, "p" to pin)
    }

    private fun leave() {
        `socket$send`(SocketPacket.IRC.ServerBound.Leave.id)
    }

    private fun list() {
        `socket$send`(SocketPacket.IRC.ServerBound.List.id)
    }

    private fun send(body: String) {
        if ("@everyone" in body) return "Please don't ping everyone...".modMessage()
        if ("@here" in body) return "Please don't ping every online member...".modMessage()

        `socket$send`(SocketPacket.IRC.ServerBound.Chat.id, "b" to body)
        "<dark_gray>[<aqua>#$cc<dark_gray>]".format0(name, body).parse(true).modMessage()
    }

    private fun String.format0(n: String, b: String): String {
        return "$this " + format0.replace("#name", n).replace("#message", b)
    }

    private fun String.format1(n: String, b: String): String {
        return "$this " + format1.replace("#name", n).replace("#message", b)
    }

    private fun help() {
        val a = ("<dark_gray>" + ("-".repeat())).parse()
        val b = Athen.modId
        val c = Catppuccin.Mocha.Green.argb

        a.lie()
        ("<red>" + ("Athen IRC".center())).parse().lie()
        a.lie()

        " <dark_gray>- <$c>/$b irc create [channel] [pin <gray>- optional<$c>]".parse().lie()
        " <dark_gray>- <$c>/$b irc join [channel] [pin <gray>- optional<$c>]".parse().lie()
        " <dark_gray>- <$c>/$b irc leave <gray>- leave channel".parse().lie()
        " <dark_gray>- <$c>/$b irc pin [pin] <gray>- sets a pin".parse().lie()
        " <dark_gray>- <$c>/$b irc chat [message]".parse().lie()
        " <dark_gray>- <$c>/$b irc list <gray>- list channels".parse().lie()

        a.lie()

        " <dark_gray>- <$c>/airc [message] <gray>- send message alias".parse().lie()
        " <dark_gray>- <$c>/airc toggle <gray>- send all messages to irc".parse().lie()

        a.lie()
    }

    private fun er0() {
        "Not connected to IRC! Use <yellow>/${Athen.modId} ws connect".parse().modMessage(Typo.PrefixType.ERROR)
    }
}