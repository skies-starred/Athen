package xyz.aerii.athen.modules.impl.slayer.carry

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerType
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.impl.suggestion.SkyBlockAPICommandSuggestionProvider
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.*
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryStateTracker.bossToPlayer
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryStateTracker.tracked
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import xyz.aerii.athen.utils.toDuration
import java.awt.Color
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
import kotlin.math.round

@Load
@OnlyIn(skyblock = true)
object SlayerCarryTracker : Module(
    "Slayer carry tracker",
    "Track slayer carries and display progress.",
    Category.SLAYER
) {
    private data class CarryMatch(val type: SlayerType, val tier: Int, val count: Int, val price: Double)

    private val announceInParty by config.switch("Announce in party", true)
    private val showSpawnMessage by config.switch("Show spawn message", true)
    private val useCustomMessages by config.switch("Use custom messages")

    private val voidExpanded by config.expandable("Voidgloom Prices")
    private val voidT3Price by config.textInput("T3 Price (M)", "0.8, 0.65").childOf { voidExpanded }
    private val voidT4Price by config.textInput("T4 Price (M)", "1.3, 2.3, 2, 1.5").childOf { voidExpanded }

    private val blazeExpanded by config.expandable("Inferno Prices")
    private val blazeT2Price by config.textInput("T2 Price (M)", "2, 1.7, 1.2").childOf { blazeExpanded }
    private val blazeT3Price by config.textInput("T3 Price (M)", "3.5, 3, 2.5").childOf { blazeExpanded }
    private val blazeT4Price by config.textInput("T4 Price (M)", "7, 6, 5").childOf { blazeExpanded }

    private val highlightBoss by config.switch("Highlight boss", true)
    private val bossColor by config.colorPicker("Boss color", Color(255, 0, 0, 150)).dependsOn { highlightBoss }
    private val bossLineWidth by config.slider("Boss line width", 2f, 0f, 10f).dependsOn { highlightBoss }

    private val highlightPlayer by config.switch("Highlight player", true)
    private val playerColor by config.colorPicker("Player color", Color(0, 255, 255, 150)).dependsOn { highlightPlayer }
    private val playerLineWidth by config.slider("Player line width", 2f, 0f, 10f).dependsOn { highlightPlayer }

    private val tradeCompleteRegex = Regex("^Trade completed with (?:\\[.*?] )?(?<player>\\w+)!$")
    private val coinsReceivedRegex = Regex("^ \\+ (?<amount>\\d+\\.?\\d*)M coins$")
    private val deathRegex = Regex("^ ☠ (?<player>\\w+) was killed by (?<killer>.+)\\.$")
    private var recentTradeWith: String? = null

    val slayerTypeMap = mapOf(
        SlayerType.REVENANT_HORROR to listOf("zombie", "rev", "revenant"),
        SlayerType.TARANTULA_BROODFATHER to listOf("spider", "tara", "tarantula"),
        SlayerType.SVEN_PACKMASTER to listOf("wolf", "sven"),
        SlayerType.VOIDGLOOM_SERAPH to listOf("enderman", "voidgloom", "eman", "void"),
        SlayerType.INFERNO_DEMONLORD to listOf("blaze", "inferno")
    ).flatMap { (type, aliases) ->
        aliases.map { it.lowercase() to type }
    }.toMap()

    private val slayerMaxTier = mapOf(
        SlayerType.REVENANT_HORROR to 5,
        SlayerType.TARANTULA_BROODFATHER to 5,
        SlayerType.SVEN_PACKMASTER to 4,
        SlayerType.VOIDGLOOM_SERAPH to 4,
        SlayerType.INFERNO_DEMONLORD to 4
    )

    private val playerSuggestions = object : SkyBlockAPICommandSuggestionProvider() {
        override fun getSuggestions(context: CommandContext<FabricClientCommandSource>, builder: SuggestionsBuilder) =
            CompletableFuture.supplyAsync {
                McClient.players.forEach { suggest(builder, it.profile.name) }
                builder.build()
            }
    }

    init {
        config.hud("Slayer Carry display") {
            if (it) return@hud sizedText("§f§lSlayer Carries:\n§7> §bExample §8[§7Void T4§8]§f: §b3§f/§b10 §7(12.5s | 28/hr)")
            if (tracked.isEmpty()) return@hud null

            sizedText(buildString {
                append("§f§lSlayer Carries:")
                for (t in tracked.values) append("\n${t.str()}")
            })
        }

        Chronos.Tick every 40 repeat {
            if (!react.value) return@repeat
            for (t in tracked.values) t.clean()
        }

        on<ChatEvent> {
            if (actionBar) return@on
            val text = message.stripped()

            tradeCompleteRegex.findThenNull(text, "player") { (player) ->
                recentTradeWith = player
            } ?: return@on

            coinsReceivedRegex.findThenNull(text, "amount") { (amountStr) ->
                val recent = recentTradeWith ?: return@findThenNull
                val amount = amountStr.toDoubleOrNull() ?: return@findThenNull
                val matches = getPrices().flatMap { (slayerType, tiers) ->
                    tiers.mapNotNull { (tier, priceStr) ->
                        val price = priceStr.toDoubleOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                        val count = round(amount / price).toInt()
                        if (count > 0 && abs(amount - count * price) < 0.01) CarryMatch(slayerType, tier, count, price) else null
                    }
                }.takeIf { it.isNotEmpty() } ?: return@findThenNull

                when {
                    matches.size == 1 -> {
                        val match = matches.first()
                        "Received payment for <aqua>${match.count}x <gray>${match.type.shortName} T${match.tier}<r> carries from <aqua>$recent<r>. "
                            .parse()
                            .append("Click to add".literal().onClick { "/${Athen.modId} carry add $recent ${match.count} ${match.type.name.lowercase()} ${match.tier}".command() })
                            .modMessage()
                    }

                    else -> {
                        val msg = "Found multiple matches, add for: ".literal()
                        for ((i, m) in matches.withIndex()) {
                            if (i > 0) msg.append(" ".literal())
                            msg.append(
                                "[${m.count}x ${m.type.shortName} T${m.tier}]"
                                    .literal()
                                    .withColor(TextColor.AQUA)
                                    .onClick { "/${Athen.modId} carry add $recent ${m.count} ${m.type.name.lowercase()} ${m.tier}".command() }
                            )
                        }

                        msg.modMessage()
                    }
                }
            } ?: return@on

            deathRegex.findThenNull(text, "player", "killer") { (player, killer) ->
                val p = tracked[player]
                if (p?.entity != null) p.reset()
            }
        }

        on<SlayerEvent.Boss.Spawn> {
            val owner = slayerInfo.owner ?: return@on
            val slayerType = slayerInfo.type as? SlayerType ?: return@on
            val carry = tracked[owner] ?: return@on
            if (carry.slayerType != slayerType) return@on
            if (carry.tier != -1 && carry.tier != slayerInfo.tier) return@on
            if (!carry.onSpawn(entity)) return@on

            bossToPlayer[entity] = owner
            if (showSpawnMessage) {
                "Boss spawned for <aqua>$owner <gray>[${slayerType.shortName}${if (carry.tier == -1) " Any" else " T${slayerInfo.tier}"}]".also { if (useCustomMessages) it.notify() else it.parse().modMessage() }
            }
        }

        on<SlayerEvent.Boss.Death> {
            val slayerType = slayerInfo.type as? SlayerType ?: return@on
            val player = bossToPlayer.remove(entity) ?: return@on
            val carry = tracked[player] ?: return@on
            if (carry.slayerType != slayerType) return@on
            if (carry.tier != -1 && carry.tier != slayerInfo.tier) return@on

            val result = carry.onKill() ?: return@on

            "Killed boss for <aqua>$player<r> in <yellow>${result.killTime.toDuration(secondsDecimals = 1)} <gray>| <yellow>${(result.killTicks / 20.0).toDuration(secondsDecimals = 1)}".parse().onHover("<red>${result.killTicks} ticks.".parse()).modMessage()
            if (announceInParty) "pc $player: ${result.current}/${result.total}".command()
            if (result.completed) {
                "<${Mocha.Green.argb}>Completed bosses for <aqua>$player <gray>[${slayerType.shortName}${if (carry.tier == -1) " Any" else " T${slayerInfo.tier}"}]<r> in <yellow>${result.totalTime.toDuration()}".parse().modMessage()

                SlayerCarryStateTracker.add(player, result.amount, carry.getType())
                tracked.remove(player)
            }

            SlayerCarryStateTracker.persist()
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightBoss && !highlightPlayer) return@on
            if (tracked.isEmpty()) return@on

            if (highlightBoss) {
                bossToPlayer.keys.removeIf { it.isRemoved }
                bossToPlayer.keys.forEach { entity ->
                    Render3D.drawBox(entity.renderBoundingBox, bossColor, bossLineWidth)
                }
            }

            if (highlightPlayer) {
                client.level?.players()?.forEach { player ->
                    if (player.name.string in tracked) {
                        Render3D.drawBox(player.renderBoundingBox, playerColor, playerLineWidth)
                    }
                }
            }
        }

        on<LocationEvent.ServerConnect> {
            for (t in tracked.values) t.reset()
            bossToPlayer.clear()
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("carry") {
                    then("add") {
                        then("player", StringArgumentType.word(), playerSuggestions) {
                            then("amount", IntegerArgumentType.integer(1), listOf("1", "5", "10", "20")) {
                                then("slayerType", StringArgumentType.word(), listOf("void", "blaze", "rev", "tara", "sven")) {
                                    thenCallback("tier", StringArgumentType.word(), listOf("any", "1", "2", "3", "4", "5")) {
                                        val player = StringArgumentType.getString(this, "player")
                                        val amount = IntegerArgumentType.getInteger(this, "amount")
                                        val slayerTypeInput = StringArgumentType.getString(this, "slayerType").lowercase()
                                        val tierInput = StringArgumentType.getString(this, "tier")

                                        val tier = if (tierInput.equals("any", true)) -1
                                        else tierInput.toIntOrNull()
                                            ?: return@thenCallback "Invalid tier.".modMessage()

                                        val slayerType = slayerTypeMap[slayerTypeInput]
                                            ?: return@thenCallback "Invalid slayer type. Use: zombie, spider, wolf, void, or blaze.".modMessage()

                                        if (tier != -1) {
                                            val maxTier = slayerMaxTier[slayerType] ?: return@thenCallback "Unknown slayer tier limits.".modMessage()
                                            if (tier !in 1..maxTier) return@thenCallback "§cTier must be 1-$maxTier or any for $slayerTypeInput.".modMessage()
                                        }

                                        SlayerCarryStateTracker.addCarry(player, amount, slayerType, tier)
                                    }
                                }
                            }
                        }
                    }

                    then("remove") {
                        thenCallback("player", StringArgumentType.word(), tracked.keys) {
                            val player = StringArgumentType.getString(this, "player")
                            SlayerCarryStateTracker.removeCarry(player)
                        }
                    }

                    then("list") {
                        callback {
                            SlayerCarryStateTracker.listCarries()
                        }

                        thenCallback("clear") {
                            SlayerCarryStateTracker.clearCarries()
                        }
                    }

                    then("history") {
                        callback {
                            SlayerCarryStateTracker.displayHistory(1)
                        }

                        thenCallback("page", IntegerArgumentType.integer(1), listOf("1", "2", "3", "4", "5")) {
                            val page = IntegerArgumentType.getInteger(this, "page")
                            SlayerCarryStateTracker.displayHistory(page)
                        }
                    }

                    thenCallback("help") {
                        showHelp()
                    }

                    thenCallback("gui") {
                        McClient.setScreen(SlayerCarryGUI)
                    }

                    callback {
                        McClient.setScreen(SlayerCarryGUI)
                    }
                }
            }
        }
    }

    private fun getPrices(): Map<SlayerType, Map<Int, String>> {
        return mapOf(
            SlayerType.VOIDGLOOM_SERAPH to mapOf(3 to voidT3Price, 4 to voidT4Price),
            SlayerType.INFERNO_DEMONLORD to mapOf(2 to blazeT2Price, 3 to blazeT3Price, 4 to blazeT4Price)
        )
    }

    private fun showHelp() {
        val commands = listOf(
            "/${Athen.modId} carry" to "Open the config tracker menu",
            "/${Athen.modId} carry add <player> <amount> <type> <tier>" to ":3",
            "/${Athen.modId} carry remove <player>" to "Removes a tracked player",
            "/${Athen.modId} carry list" to "Lists players being tracked",
            "/${Athen.modId} carry list clear" to "Clears the active list",
            "/${Athen.modId} carry history [page=0]" to "Shows tracked history"
        )

        val divider = ("§8§m" + ("-".repeatBreak())).literal()

        divider.lie()
        "§bAthen Carry Commands".centeredText().lie()
        divider.lie()

        for ((c, d) in commands) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        divider.lie()
    }

    val SlayerType.shortName: String
        get() = when (this) {
            SlayerType.REVENANT_HORROR -> "Rev"
            SlayerType.SVEN_PACKMASTER -> "Sven"
            SlayerType.INFERNO_DEMONLORD -> "Blaze"
            SlayerType.TARANTULA_BROODFATHER -> "Tara"
            SlayerType.RIFTSTALKER_BLOODFIEND -> "Vamp"
            SlayerType.VOIDGLOOM_SERAPH -> "Void"
        }
}