package xyz.aerii.athen.modules.impl.dungeon.carry

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.impl.suggestion.SkyBlockAPICommandSuggestionProvider
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.DungeonEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.dungeon.carry.DungeonCarryStateTracker.tracked
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import xyz.aerii.athen.utils.toDuration
import java.awt.Color
import java.util.concurrent.CompletableFuture

@Load
@OnlyIn(skyblock = true)
object DungeonCarryTracker : Module(
    "Dungeon carry tracker",
    "Track dungeon carries and display progress.",
    Category.DUNGEONS
) {
    private val announceInParty by config.switch("Announce in party", true)
    private val showStartMessage by config.switch("Show start message", true)

    private val highlightPlayer by config.switch("Highlight player", true)
    private val playerColor by config.colorPicker("Player color", Color(0, 255, 255, 150)).dependsOn { highlightPlayer }
    private val playerLineWidth by config.slider("Player line width", 2f, 0f, 10f).dependsOn { highlightPlayer }

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

    private val playerSuggestions = object : SkyBlockAPICommandSuggestionProvider() {
        override fun getSuggestions(context: CommandContext<FabricClientCommandSource>, builder: SuggestionsBuilder) =
            CompletableFuture.supplyAsync {
                for (i in McClient.players) suggest(builder, i.profile.name)
                builder.build()
            }
    }

    init {
        config.hudElement("Dungeon Carry display") {
            val onlyInDungeon by switch("Only in dungeons", true)
            ;

            {
                if (it) return@hudElement sizedText("§f§lDungeon Carries:\n§7> §bExample §8[§7M7§8]§f: §b3§f/§b10 §7(5m 30s | 12/hr)")
                if (tracked.isEmpty()) return@hudElement null
                if (onlyInDungeon && !SkyBlockIsland.THE_CATACOMBS.inIsland.value) return@hudElement null

                sizedText(buildString {
                    append("§f§lDungeon Carries:")
                    for (i in tracked.values) append("\n${i.str()}")
                })
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
                Render3D.drawBox(e.renderBoundingBox, playerColor, playerLineWidth, false)
            }
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("dcarry") {
                    then("add") {
                        then("player", StringArgumentType.word(), playerSuggestions) {
                            then("amount", IntegerArgumentType.integer(1), listOf("1", "5", "10", "20")) {
                                thenCallback("floor", StringArgumentType.word(), floorMap.keys.toList()) {
                                    val player = StringArgumentType.getString(this, "player")
                                    val amount = IntegerArgumentType.getInteger(this, "amount")
                                    val floorInput = StringArgumentType.getString(this, "floor")

                                    val floor = floorMap[floorInput.lowercase()] ?: return@thenCallback "Invalid floor. Use: e, f1-f7, m1-m7.".modMessage()

                                    DungeonCarryStateTracker.addCarry(player, amount, floor)
                                }
                            }
                        }
                    }

                    then("remove") {
                        thenCallback("player", StringArgumentType.word(), tracked.keys) {
                            val player = StringArgumentType.getString(this, "player")
                            DungeonCarryStateTracker.removeCarry(player)
                        }
                    }

                    then("list") {
                        callback {
                            DungeonCarryStateTracker.listCarries()
                        }

                        thenCallback("clear") {
                            DungeonCarryStateTracker.clearCarries()
                        }
                    }

                    then("history") {
                        callback {
                            DungeonCarryStateTracker.displayHistory(1)
                        }

                        thenCallback("page", IntegerArgumentType.integer(1), listOf("1", "2", "3", "4", "5")) {
                            val page = IntegerArgumentType.getInteger(this, "page")
                            DungeonCarryStateTracker.displayHistory(page)
                        }
                    }

                    thenCallback("help") {
                        showHelp()
                    }

                    thenCallback("gui") {
                        McClient.setScreen(DungeonCarryGUI)
                    }

                    callback {
                        McClient.setScreen(DungeonCarryGUI)
                    }
                }
            }
        }
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

        val divider = ("§8§m" + ("-".repeatBreak())).literal()

        divider.lie()
        "§bAthen Dungeon Carry Commands".centeredText().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }
}