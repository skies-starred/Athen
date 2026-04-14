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
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Ticking
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryStateTracker.bossToPlayer
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryStateTracker.tracked
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.fcs
import xyz.aerii.athen.utils.render.renderBoundingBox
import xyz.aerii.library.api.*
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.handlers.time.client
import xyz.aerii.library.utils.literal
import xyz.aerii.library.utils.toDuration
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

    private val ex0 = listOf("§f§lSlayer Carries:", "§7> §bExample §8[§7Void T4§8]§f: §b3§f/§b10 §7(12.5s | 28/hr)").fcs
    private val display = Ticking {
        if (tracked.isEmpty()) return@Ticking null

        buildString {
            append("§f§lSlayer Carries:")
            for (t in tracked.values) append("\n${t.str()}")
        }.split("\n").fcs
    }

    init {
        config.hud("Slayer carry display") {
            if (it) return@hud sizedText(ex0)
            sizedText(display.value ?: return@hud null)
        }

        on<TickEvent.Client.End> {
            if (ticks % 40 != 0) return@on
            for (t in tracked.values) t.clean()
        }

        on<MessageEvent.Chat.Receive> {
            tradeCompleteRegex.findThenNull(stripped, "player") { (player) ->
                recentTradeWith = player
            } ?: return@on

            coinsReceivedRegex.findThenNull(stripped, "amount") { (amountStr) ->
                val recent = recentTradeWith ?: return@findThenNull
                val amount = amountStr.toDoubleOrNull() ?: return@findThenNull
                val matches = getPrices().flatMap { (slayerType, tiers) ->
                    tiers.flatMap { (tier, priceStr) ->
                        priceStr.split(',')
                            .mapNotNull { it.trim().toDoubleOrNull() }
                            .filter { it > 0.0 }
                            .mapNotNull { price ->
                                val count = round(amount / price).toInt()
                                if (count > 0 && abs(amount - count * price) < 0.01) CarryMatch(slayerType, tier, count, price)
                                else null
                            }
                    }
                }.takeIf { it.isNotEmpty() } ?: return@findThenNull

                Chronos.schedule(1.client) {
                    when {
                        matches.size == 1 -> {
                            val match = matches.first()
                            "Received payment for <aqua>${match.count}x <gray>${match.type.shortName} T${match.tier}<r> carries from <aqua>$recent<r>. "
                                .parse()
                                .append("Click to add".literal().onClick { add(recent, match.count, match.type.shortName.lowercase(), match.tier.toString()) })
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
                                        .onClick { add(recent, m.count, m.type.shortName.lowercase(), m.tier.toString()) }
                                )
                            }

                            msg.modMessage()
                        }
                    }
                }
            } ?: return@on

            deathRegex.findThenNull(stripped, "player", "killer") { (player, killer) ->
                val p = tracked[player]
                if (p?.entity != null) p.reset()
            }
        }

        on<SlayerEvent.Boss.Spawn> {
            if (tracked.isEmpty()) return@on

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
            if (tracked.isEmpty()) return@on

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
                bossToPlayer.keys.removeIf {
                    Render3D.drawBox(it.renderBoundingBox, bossColor, bossLineWidth)
                    it.isRemoved
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

        on<LocationEvent.Server.Connect> {
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
                                        val slayerType = StringArgumentType.getString(this, "slayerType").lowercase()
                                        val tier = StringArgumentType.getString(this, "tier")
                                        add(player, amount, slayerType, tier)
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
                        SlayerCarryGUI.open()
                    }

                    callback {
                        SlayerCarryGUI.open()
                    }
                }
            }
        }
    }

    private fun add(player: String, amount: Int, slayerType: String, tier: String) {
        val tier = if (tier.equals("any", true)) -1
        else tier.toIntOrNull() ?: return "Invalid tier.".modMessage()

        val slayerType = slayerTypeMap[slayerType] ?: return "Invalid slayer type. Use: zombie, spider, wolf, void, or blaze.".modMessage()

        if (tier != -1) {
            val maxTier = slayerMaxTier[slayerType] ?: return "Unknown slayer tier limits.".modMessage()
            if (tier !in 1..maxTier) return "§cTier must be 1-$maxTier or any for $slayerType.".modMessage()
        }

        SlayerCarryStateTracker.addCarry(player, amount, slayerType, tier)
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

        val divider = ("§8§m" + ("-".repeat())).literal()

        divider.lie()
        "§bAthen Carry Commands".center().lie()
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