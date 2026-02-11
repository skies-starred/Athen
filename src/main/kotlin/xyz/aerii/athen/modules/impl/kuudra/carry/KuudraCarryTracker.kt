package xyz.aerii.athen.modules.impl.kuudra.carry

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.impl.suggestion.SkyBlockAPICommandSuggestionProvider
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.kuudra.carry.KuudraCarryStateTracker.tracked
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import xyz.aerii.athen.utils.toDuration
import java.awt.Color
import java.util.concurrent.CompletableFuture

@Load
@OnlyIn(skyblock = true)
object KuudraCarryTracker : Module(
    "Kuudra carry tracker",
    "Track kuudra carries and display progress.",
    Category.KUUDRA
) {
    private val announceInParty by config.switch("Announce in party", true)
    private val showStartMessage by config.switch("Show start message", true)

    private val highlightPlayer by config.switch("Highlight player", true)
    private val playerColor by config.colorPicker("Player color", Color(0, 255, 255, 150)).dependsOn { highlightPlayer }
    private val playerLineWidth by config.slider("Player line width", 2f, 0f, 10f).dependsOn { highlightPlayer }

    private val tierMap = mapOf(
        "basic" to KuudraTier.BASIC,
        "hot" to KuudraTier.HOT,
        "burning" to KuudraTier.BURNING,
        "fiery" to KuudraTier.FIERY,
        "infernal" to KuudraTier.INFERNAL,
        "t1" to KuudraTier.BASIC,
        "t2" to KuudraTier.HOT,
        "t3" to KuudraTier.BURNING,
        "t4" to KuudraTier.FIERY,
        "t5" to KuudraTier.INFERNAL
    )

    private val playerSuggestions = object : SkyBlockAPICommandSuggestionProvider() {
        override fun getSuggestions(context: CommandContext<FabricClientCommandSource>, builder: SuggestionsBuilder) =
            CompletableFuture.supplyAsync {
                for (i in McClient.players) suggest(builder, i.profile.name)
                builder.build()
            }
    }

    init {
        config.hudElement("Kuudra Carry display") {
            val onlyInKuudra by switch("Only in Kuudra", true)
            ;

            {
                if (it) return@hudElement sizedText("§f§lKuudra Carries:\n§7> §bExample §8[§7Infernal§8]§f: §b3§f/§b10 §7(5m 30s | 12/hr)")
                if (tracked.isEmpty()) return@hudElement null
                if (onlyInKuudra && !SkyBlockIsland.KUUDRA.inIsland.value) return@hudElement null

                sizedText(buildString {
                    append("§f§lKuudra Carries:")
                    for (i in tracked.values) append("\n${i.str()}")
                })
            }
        }

        on<KuudraEvent.Start> {
            val tier = KuudraAPI.tier ?: return@on

            for (teammate in KuudraAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.tier != tier) continue

                if (showStartMessage) "Kuudra started for <${TextColor.AQUA}>${teammate.name}<${TextColor.GRAY}> [${tier.str}]".parse().modMessage()
            }
        }

        on<KuudraEvent.End> {
            val tier = KuudraAPI.tier ?: return@on

            for (teammate in KuudraAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.tier != tier) continue

                val result = carry.onCompletion()

                "Completed run for <aqua>${teammate.name}".parse().modMessage()
                if (announceInParty) "pc ${teammate.name}: ${result.current}/${result.total}".command()

                if (result.completed) {
                    "<${Mocha.Green.argb}>Completed carries for <${TextColor.AQUA}>${teammate.name} <${TextColor.GRAY}>[${tier.str}] <r>in <${TextColor.YELLOW}>${result.totalTime.toDuration()}"
                        .parse()
                        .modMessage()

                    KuudraCarryStateTracker.add(teammate.name, result.amount, carry.getType())
                    tracked.remove(teammate.name)
                }

                KuudraCarryStateTracker.persist()
            }
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightPlayer) return@on
            if (tracked.isEmpty()) return@on
            if (!KuudraAPI.inRun) return@on

            for (teammate in KuudraAPI.teammates) {
                if (teammate.name !in tracked) continue
                val e = teammate.entity ?: continue
                Render3D.drawBox(e.renderBoundingBox, playerColor, playerLineWidth, false)
            }
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("kcarry") {
                    then("add") {
                        then("player", StringArgumentType.word(), playerSuggestions) {
                            then("amount", IntegerArgumentType.integer(1), listOf("1", "5", "10", "20")) {
                                thenCallback("tier", StringArgumentType.word(), tierMap.keys.toList()) {
                                    val player = StringArgumentType.getString(this, "player")
                                    val amount = IntegerArgumentType.getInteger(this, "amount")
                                    val tierInput = StringArgumentType.getString(this, "tier")

                                    val tier = tierMap[tierInput.lowercase()] ?: return@thenCallback "Invalid tier. Use: basic, hot, burning, fiery, infernal, or t1-t5.".modMessage()

                                    KuudraCarryStateTracker.addCarry(player, amount, tier)
                                }
                            }
                        }
                    }

                    then("remove") {
                        thenCallback("player", StringArgumentType.word(), tracked.keys) {
                            val player = StringArgumentType.getString(this, "player")
                            KuudraCarryStateTracker.removeCarry(player)
                        }
                    }

                    then("list") {
                        callback {
                            KuudraCarryStateTracker.listCarries()
                        }

                        thenCallback("clear") {
                            KuudraCarryStateTracker.clearCarries()
                        }
                    }

                    then("history") {
                        callback {
                            KuudraCarryStateTracker.displayHistory(1)
                        }

                        thenCallback("page", IntegerArgumentType.integer(1), listOf("1", "2", "3", "4", "5")) {
                            val page = IntegerArgumentType.getInteger(this, "page")
                            KuudraCarryStateTracker.displayHistory(page)
                        }
                    }

                    thenCallback("help") {
                        showHelp()
                    }

                    thenCallback("gui") {
                        McClient.setScreen(KuudraCarryGUI)
                    }

                    callback {
                        McClient.setScreen(KuudraCarryGUI)
                    }
                }
            }
        }
    }

    private fun showHelp() {
        val commands = listOf(
            "/${Athen.modId} kcarry" to "Open the kuudra carry tracker GUI",
            "/${Athen.modId} kcarry add <player> <amount> <tier>" to "Add kuudra carries to track",
            "/${Athen.modId} kcarry remove <player>" to "Remove a tracked player",
            "/${Athen.modId} kcarry list" to "List players being tracked",
            "/${Athen.modId} kcarry list clear" to "Clear the active list",
            "/${Athen.modId} kcarry history [page=1]" to "Show tracked history"
        )

        val divider = ("§8§m" + ("-".repeatBreak())).literal()

        divider.lie()
        "§bAthen Kuudra Carry Commands".centeredText().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }
}