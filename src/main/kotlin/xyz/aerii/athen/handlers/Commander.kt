@file:Suppress("ObjectPropertyName")

package xyz.aerii.athen.handlers

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.ui.ClickGUI
import xyz.aerii.athen.events.ChatEvent
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Toaster.toast
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.hud.internal.HUDEditor
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.athen.modules.impl.Dev.clickUiHelperCollapsed
import xyz.aerii.athen.modules.impl.Dev.clickUiHelperHidden
import xyz.aerii.athen.modules.impl.Dev.debug
import xyz.aerii.athen.modules.impl.ModSettings
import xyz.aerii.athen.modules.impl.general.keybinds.KeybindsGUI
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.updater.ModUpdater

@Load
object Commander {
    private object StateTracker {
        var `warning$updater$sentOnce` = false
        var `warning$clickUiHelper$sentOnce` = false
    }

    private val commands = listOf(
        "/${Athen.modId} config" to "Open the configuration menu",
        "/${Athen.modId} hud" to "Open the HUD editor",
        "/${Athen.modId} simulate terminals" to "Terminal simulator",
        "/${Athen.modId} carry help" to "Info about slayer carry commands",
        "/${Athen.modId} dcarry help" to "Info about dungeon carry commands",
        "/${Athen.modId} kcarry help" to "Info about kuudra carry commands",
        "/${Athen.modId} toggle help" to "Toggle the UI helper tooltip",
        "/${Athen.modId} clear chat" to "Clear the chat history",
        "/${Athen.modId} stats <name>" to "View stats for any player",
        "/${Athen.modId} times slayers" to "Shows the slayer kill times",
        "/${Athen.modId} update [stream]" to "Install updates (release/beta/alpha)",
        "/${Athen.modId} checkupdate [stream]" to "Check for available updates"
    )

    init {
        on<CommandRegistration> {
            event.register(Athen.modId) {
                callback {
                    if (ModSettings.commandConfig) {
                        McClient.setScreen(ClickGUI)
                        "Opened the config! <gray>(use /athen help to view commands)".parse().modMessage()
                    } else {
                        showHelp()
                    }
                }

                then("config") {
                    callback {
                        McClient.setScreen(ClickGUI)
                    }

                    thenCallback("keybinds") {
                        McClient.setScreen(KeybindsGUI)
                    }
                }

                thenCallback("hud") {
                    McClient.setScreen(HUDEditor)
                }

                thenCallback("keybinds") {
                    McClient.setScreen(KeybindsGUI)
                }

                thenCallback("help") {
                    showHelp()
                }

                then("toggle") {
                    thenCallback("dev") {
                        debug = !debug
                        val stateColor = if (debug) "green" else "red"

                        "Debug mode is now: <$stateColor>${if (debug) "Enabled" else "Disabled"}<r>.".parse().modMessage()
                    }

                    thenCallback("help") {
                        clickUiHelperCollapsed = !clickUiHelperCollapsed
                        if (clickUiHelperCollapsed) "Click UI helper has been <yellow>collapsed<r> and moved to the <aqua>top right corner<r>. Left-click it to expand. Use <yellow>/${Athen.modId} hide help<r> to permanently hide (not recommended).".parse().modMessage()
                        else "Click UI helper has been <green>expanded<r>.".parse().modMessage()
                    }
                }

                then("hide") {
                    thenCallback("help") {
                        if (!StateTracker.`warning$clickUiHelper$sentOnce` && !clickUiHelperHidden) {
                            StateTracker.`warning$clickUiHelper$sentOnce` = true

                            "<red>THIS IS NOT RECOMMENDED! Run this command again to confirm!".parse().modMessage(Typo.PrefixType.ERROR)
                            return@thenCallback
                        }

                        clickUiHelperHidden = !clickUiHelperHidden
                        "Click UI helper is now: <${if (clickUiHelperHidden) "red" else "green"}>${if (clickUiHelperHidden) "Hidden" else "Visible"}<r>.".parse().modMessage()
                        StateTracker.`warning$clickUiHelper$sentOnce` = false
                    }

                    thenCallback("updater") {
                        ModUpdater.checkForUpdate().thenAccept { update ->
                            if (!update.isUpdateAvailable) return@thenAccept "No update available to hide.".modMessage(Typo.PrefixType.ERROR)

                            val newVersion = update.update.versionName

                            if (!StateTracker.`warning$updater$sentOnce`) {
                                StateTracker.`warning$updater$sentOnce` = true

                                "<red>THIS IS NOT RECOMMENDED! Run this command again to confirm!".parse().modMessage(Typo.PrefixType.ERROR)
                                return@thenAccept
                            }

                            ModUpdater.trulySkip = newVersion
                            "Update skip is now set for version <red>$newVersion<r>.".parse().modMessage()
                            StateTracker.`warning$updater$sentOnce` = false
                        }.exceptionally { e ->
                            Athen.LOGGER.error("Failed to fetch update version", e)
                            "Failed to fetch update version.".modMessage(Typo.PrefixType.ERROR)
                            null
                        }
                    }
                }


                then("simulate") {
                    then("chat") {
                        then("actionbar", BoolArgumentType.bool()) {
                            thenCallback("message", StringArgumentType.greedyString()) {
                                val actionBar = BoolArgumentType.getBool(this, "actionbar")
                                val message = StringArgumentType.getString(this, "message")
                                ChatEvent(message.literal(), actionBar).post()
                                "<gray>Simulated (actionBar=$actionBar): <red>$message".parse().modMessage()
                            }
                        }
                    }

                    then("toast") {
                        thenCallback("message", StringArgumentType.greedyString()) {
                            StringArgumentType.getString(this, "message").toast()
                        }
                    }

                    then("notification") {
                        thenCallback("message", StringArgumentType.greedyString()) {
                            StringArgumentType.getString(this, "message").notify()
                        }
                    }
                }

                then("clear") {
                    thenCallback("chat") {
                        client.gui.chat.clearMessages(false)
                    }

                    thenCallback("broadcast") {
                        Dev.lastBroadcast = ""
                    }
                }

                then("checkupdate") {
                    callback {
                        ModUpdater.checkAndNotify()
                    }

                    thenCallback("stream", StringArgumentType.string()) {
                        ModUpdater.checkAndNotify(StringArgumentType.getString(this, "stream"))
                    }
                }

                then("update") {
                    callback {
                        ModUpdater.installUpdate()
                    }

                    thenCallback("stream", StringArgumentType.string()) {
                        ModUpdater.installUpdate(StringArgumentType.getString(this, "stream"))
                    }
                }
            }
        }
    }

    private fun showHelp() {
        val divider = ("§8§m" + ("-".repeatBreak())).literal()

        divider.lie()
        "§bAthen Commands".centeredText().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }
}