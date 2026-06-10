@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.dungeon.carry

import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import xyz.aerii.athen.api.rendering.ui.text.vanilla.extensions.sizedText
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.config.ConfigBuilder
import xyz.aerii.athen.events.DungeonEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Ticking
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.utils.DiscordWebhook
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.dungeon.carry.DungeonCarryStateTracker.tracked
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.fcs
import xyz.aerii.athen.utils.render.renderBoundingBox
import xyz.aerii.library.api.center
import xyz.aerii.library.api.command
import xyz.aerii.library.api.lie
import xyz.aerii.library.api.repeat
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.kommand.ICommand
import xyz.aerii.library.utils.literal
import xyz.aerii.library.utils.toDuration
import java.awt.Color

@Load
@OnlyIn(skyblock = true)
object DungeonCarryTracker : Module(
    "Dungeon carry tracker",
    "Track dungeon carries and display progress.",
    Category.DUNGEONS
), ICommand {
    private val announceInParty by config.switch("Announce in party", true)
    private val showStartMessage by config.switch("Show start message", true)

    private val webhookExpanded by config.expandable("Discord Webhook")
    private val webhookUrl by config.textInput("Webhook URL", "", "https://discord.com/api/webhooks/...").childOf { webhookExpanded }

    private val highlightPlayer by config.switch("Highlight player", true)
    private val playerColor by config.colorPicker("Player color", Color(0, 255, 255, 150)).dependsOn { highlightPlayer }
    private val playerLineWidth by config.slider("Player line width", 2f, 0f, 10f).dependsOn { highlightPlayer }

    private val ex0 = listOf("§f§lDungeon Carries:", "§7> §bExample §8[§7M7§8]§f: §b3§f/§b10 §7(5m 30s | 12/hr)").fcs
    private val hud: ConfigBuilder.HUDElementBuilder = config.hud("Dungeon carry display") {
        if (it) return@hud sizedText(ex0)
        sizedText(display.value ?: return@hud null)
    }

    private val `hud$dungeon` by config.switch("Only in dungeons", true).dependsOn { hud.enabled }

    private val floorMap = mapOf(
        "e" to DungeonFloor.E,
        "f1" to DungeonFloor.F1,
        "f2" to DungeonFloor.F2,
        "f3" to DungeonFloor.F3,
        "f4" to DungeonFloor.F4,
        "f5" to DungeonFloor.F5,
        "f6" to DungeonFloor.F6,
        "f7" to DungeonFloor.F7,
        "m1" to DungeonFloor.M1,
        "m2" to DungeonFloor.M2,
        "m3" to DungeonFloor.M3,
        "m4" to DungeonFloor.M4,
        "m5" to DungeonFloor.M5,
        "m6" to DungeonFloor.M6,
        "m7" to DungeonFloor.M7
    )

    private val display = Ticking {
        if (tracked.isEmpty()) return@Ticking null
        if (`hud$dungeon` && !SkyBlockIsland.THE_CATACOMBS.inIsland.value) return@Ticking null

        buildString {
            append("§f§lDungeon Carries:")
            for (i in tracked.values) append("\n${i.str()}")
        }.split("\n").fcs
    }

    init {
        command(Athen.modId) {
            "dcarry" / "add" / word("player").suggests { McClient.players.map { it.profile.name } } / int("amount", 1).suggests { listOf("1", "5", "10", "20") } / word("floor") {
                val player = string("player")
                val floor = string("floor")
                val amount = int("amount")

                val floor0 = floorMap[floor.lowercase()] ?: return@word "Invalid floor. Use: e, f1-f7, m1-m7.".modMessage()

                DungeonCarryStateTracker.addCarry(player, amount, floor0)
            }.suggests { floorMap.keys }

            "dcarry" / "remove" / word("player") {
                DungeonCarryStateTracker.removeCarry(string("player"))
            }.suggests { tracked.keys }

            "dcarry" / "list" {
                DungeonCarryStateTracker.listCarries()
            }

            "dcarry" / "list" / "clear" {
                DungeonCarryStateTracker.clearCarries()
            }

            "dcarry" / "history" {
                DungeonCarryStateTracker.displayHistory(1)
            }

            "dcarry" / "history" / int("page", 1) {
                DungeonCarryStateTracker.displayHistory(int("page"))
            }.suggests { listOf("1", "2", "3", "4", "5") }

            "dcarry" / "help" {
                showHelp()
            }

            "dcarry" / "gui" {
                DungeonCarryGUI.open()
            }

            "dcarry" {
                DungeonCarryGUI.open()
            }
        }

        on<DungeonEvent.Start> {
            val floor = DungeonAPI.floor.value ?: return@on

            for (teammate in DungeonAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.floor != floor) continue

                if (showStartMessage) "Dungeon started for <${TextColor.AQUA}>${teammate.name}<${TextColor.GRAY}> [${floor.name}]".parse().modMessage()
            }
        }

        on<DungeonEvent.End> {
            val floor = DungeonAPI.floor.value ?: return@on

            for (teammate in DungeonAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.floor != floor) continue

                val result = carry.onCompletion()

                "Completed run for <aqua>${teammate.name}".parse().modMessage()
                if (announceInParty) "pc ${teammate.name}: ${result.current}/${result.total}".command()

                if (result.completed) {
                    "<${Mocha.Green.argb}>Completed carries for <${TextColor.AQUA}>${teammate.name} <${TextColor.GRAY}>[${floor.name}] <r>in <${TextColor.YELLOW}>${result.totalTime.toDuration()}"
                        .parse()
                        .modMessage()

                    val t = result.totalTime.toInt()
                    val timeStr = if (t >= 60) "${t / 60}m ${t % 60}s" else "${t}s"
                    DiscordWebhook.send(webhookUrl, "Completed ${result.amount}x ${floor.name} carries for ${teammate.name} ($timeStr)")

                    DungeonCarryStateTracker.add(teammate.name, result.amount, carry.getType())
                    tracked.remove(teammate.name)
                }

                DungeonCarryStateTracker.persist()
            }
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightPlayer) return@on
            if (tracked.isEmpty()) return@on

            for (teammate in DungeonAPI.teammates) {
                if (teammate.name !in tracked) continue
                val e = teammate.entity ?: continue
                extractFrameBox(e.renderBoundingBox, playerColor.rgb, playerLineWidth, false)
            }
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)
    }

    private fun showHelp() {
        val commands = listOf(
            "/${Athen.modId} dcarry" to "Open the dungeon carry tracker GUI",
            "/${Athen.modId} dcarry add <player> <amount> <floor>" to "Add dungeon carries to track",
            "/${Athen.modId} dcarry remove <player>" to "Remove a tracked player",
            "/${Athen.modId} dcarry list" to "List players being tracked",
            "/${Athen.modId} dcarry list clear" to "Clear the active list",
            "/${Athen.modId} dcarry history [page=1]" to "Show tracked history"
        )

        val divider = ("§8§m" + ("-".repeat())).literal()

        divider.lie()
        "§bAthen Dungeon Carry Commands".center().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }
}