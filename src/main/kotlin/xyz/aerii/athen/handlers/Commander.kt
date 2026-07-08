@file:Suppress("ObjectPropertyName", "Unused")

package xyz.aerii.athen.handlers

import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.config.ui.ClickGUI
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.hud.HUDEditor
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.athen.modules.impl.Dev.clickUiHelperCollapsed
import xyz.aerii.athen.modules.impl.Dev.clickUiHelperHidden
import xyz.aerii.athen.modules.impl.Dev.debug
import xyz.aerii.athen.modules.impl.ModSettings
import xyz.aerii.athen.modules.impl.general.keybinds.ui.KeybindsGUI
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.updater.ModUpdater
import xyz.aerii.library.api.center
import xyz.aerii.library.api.client
import xyz.aerii.library.api.lie
import xyz.aerii.library.api.repeat
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.kommand.ICommand
import xyz.aerii.library.utils.formatted
import xyz.aerii.library.utils.literal

@Load
object Commander : ICommand {
    private val r = Regex("(?<!^)([A-Z])")

    object StateTracker {
        var `warning$updater$sentOnce` = false
        var `warning$clickUiHelper$sentOnce` = false
        var `warning$kat$sentOnce` = false
    }

    private val commands = listOf(
        "/${Athen.modId} config" to "Open the configuration menu",
        "/${Athen.modId} hud" to "Open the HUD editor",
        "/${Athen.modId} simulate terminals" to "Terminal simulator",
        "/${Athen.modId} radial help" to "Info about radial menu",
        "/${Athen.modId} visuals help" to "Info about visual words replacement",
        "/${Athen.modId} carry help" to "Info about slayer carry commands",
        "/${Athen.modId} dcarry help" to "Info about dungeon carry commands",
        "/${Athen.modId} kcarry help" to "Info about kuudra carry commands",
        "/${Athen.modId} clear chat" to "Clear the chat history",
        "/${Athen.modId} stats <name>" to "View stats for any player",
        "/${Athen.modId} times slayers" to "Shows the slayer kill times",
        "/${Athen.modId} times kuudra <tier>" to "Shows the kuudra pbs",
        "/${Athen.modId} toggle feature <featureKey>" to "Toggles the specified feature!",
        "/${Athen.modId} irc help" to "View all IRC commands"
    )

    init {
        command(Athen.modId) {
            executes {
                if (!ModSettings.commandConfig) return@executes showHelp()

                ClickGUI.open()
                "Opened the config! <gray>(use /athen help to view commands)".parse().modMessage()
            }

            "help" {
                showHelp()
            }

            "config" {
                ClickGUI.open()
            }

            "hud" {
                HUDEditor.open()
            }

            "keybinds" {
                KeybindsGUI.open()
            }

            "calc" / greedyString("operation") {
                val string = string("operation")
                val result = Calculator.calc(string).formatted()
                "<gray>$string = <green>$result".parse().modMessage(Typo.PrefixType.SUCCESS)
            }

            "toggle" / "dev" {
                debug = !debug
                val a = if (debug) "<green>Enabled" else "<red>Disabled"

                "Debug mode is now: $a<r>.".parse().modMessage()
            }

            "toggle" / "help" {
                clickUiHelperCollapsed = !clickUiHelperCollapsed
                if (clickUiHelperCollapsed) "Click UI helper has been <yellow>collapsed<r> and moved to the <aqua>top right corner<r>. Left-click it to expand. Use <yellow>/${Athen.modId} hide help<r> to permanently hide (not recommended).".parse().modMessage()
                else "Click UI helper has been <green>expanded<r>.".parse().modMessage()
            }

            "toggle" / "feature" / string("key") {
                val key = string("key")

                val b = ConfigManager.getValue(key) as? Boolean ?: return@string "Not a valid feature!".modMessage()
                ConfigManager.updateConfig(key, !b)

                val s = key.replace(r, " $1").lowercase().replaceFirstChar { it.uppercase() }
                "<${Mocha.Mauve.argb}>$s <gray>➤ ${if (b) "<red>Disabled" else "<green>Enabled"}".parse().modMessage()
            }

            "hide" / "help" {
                if (!StateTracker.`warning$clickUiHelper$sentOnce` && !clickUiHelperHidden) {
                    StateTracker.`warning$clickUiHelper$sentOnce` = true

                    "<red>THIS IS NOT RECOMMENDED! Run this command again to confirm!".parse().modMessage(Typo.PrefixType.ERROR)
                    return@invoke
                }

                clickUiHelperHidden = !clickUiHelperHidden
                "Click UI helper is now: <${if (clickUiHelperHidden) "red" else "green"}>${if (clickUiHelperHidden) "Hidden" else "Visible"}<r>.".parse().modMessage()
                StateTracker.`warning$clickUiHelper$sentOnce` = false
            }

            "hide" / "updater" {
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

            "simulate" / "chat" / bool("actionbar") / greedyString("message") {
                val actionBar = bool("actionbar")
                val message = string("message")

                if (actionBar) MessageEvent.ActionBar(message.literal()).post()
                else MessageEvent.Chat.Receive(message.literal()).post()

                "<gray>Simulated (actionBar=$actionBar): <red>$message".parse().modMessage()
            }

            "simulate" / "notification" / greedyString("message") {
                string("message").notify()
            }

            "clear" / "chat" {
                client.gui.chat.clearMessages(false)
            }

            "clear" / "broadcast" {
                Dev.lastBroadcast = ""
            }

            "checkupdate" {
                ModUpdater.checkAndNotify(silent = false)
            }

            "checkupdate" / string("stream") {
                ModUpdater.checkAndNotify(string("stream"), false)
            }

            "update" {
                ModUpdater.installUpdate()
            }

            "update" / string("stream") {
                ModUpdater.installUpdate(string("stream"))
            }
        }
    }

    private fun showHelp() {
        val divider = ("§8§m" + ("-".repeat())).literal()

        divider.lie()
        "§bAthen Commands".center().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }
}