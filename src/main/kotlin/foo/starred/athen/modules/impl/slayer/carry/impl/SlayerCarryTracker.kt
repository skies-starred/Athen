@file:Suppress("Unused", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer.carry.impl

import foo.starred.athen.Athen
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.sizedText
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.Category
import foo.starred.athen.ducks.entity.EntityDuck.Companion.carry
import foo.starred.athen.events.*
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryHistory
import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryPlayer
import foo.starred.athen.modules.impl.slayer.carry.ui.SlayerCarryGUI
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.athen.utils.command
import foo.starred.athen.utils.render.fcs
import foo.starred.athen.utils.render.renderBoundingBox
import foo.starred.snowbird.api.*
import foo.starred.snowbird.api.network.data.HttpRequest
import foo.starred.snowbird.api.scheduling.scheduler.extensions.clientTicks
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.toDuration
import net.minecraft.world.entity.LivingEntity
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import java.awt.Color
import kotlin.math.abs
import kotlin.math.round

@Load
@OnlyIn(skyblock = true)
object SlayerCarryTracker : Module(
    "Slayer carry tracker",
    "Track slayer carries and display progress.",
    Category.SLAYER
) {
    private val `announce$comp$party` by config.switch("Announce in party", true)
    private val `announce$spawn` by config.switch("Show spawn message", true)

    @Suppress("unused")
    private val _manager by config.button("Open manager") { SlayerCarryGUI.open() }

    private val _webhook by config.group("Discord webhook")
    private val webhook by _webhook.switch("Send to webhook")
    private val `webhook$each` by _webhook.switch("Send on each kill", true)
    private val `webhook$url` by _webhook.input("Webhook URL")
    private val `webhook$url$desc` by _webhook.information("Requires you to add your own webhook URL!")

    private val _void by config.group("Voidgloom Prices")
    private val `price$void$3` by _void.input("T3 Price (M)", "0.8, 0.65")
    private val `price$void$4` by _void.input("T4 Price (M)", "1.3, 2.3, 2, 1.5")

    private val _blaze by config.group("Inferno Prices")
    private val `price$blaze$2` by _blaze.input("T2 Price (M)", "2, 1.7, 1.2")
    private val `price$blaze$3` by _blaze.input("T3 Price (M)", "3.5, 3, 2.5")
    private val `price$blaze$4` by _blaze.input("T4 Price (M)", "7, 6, 5")

    private val _highlights by config.group("Highlights")
    private val `highlight$boss` = _highlights.switch("Highlight boss", true).unique("highlightBoss")
    private val `highlight$boss$color` by _highlights.colorPicker("Boss color", Color(255, 0, 0, 150))
    private val `highlight$boss$width` by _highlights.slider("Boss line width", 2f, 0f, 10f)
    private val `highlight$player` = _highlights.switch("Highlight player", true).unique("highlightPlayer")
    private val `highlight$player$color` by _highlights.colorPicker("Player color", Color(0, 255, 255, 150))
    private val `highlight$player$width` by _highlights.slider("Player line width", 2f, 0f, 10f)

    private val tradeCompleteRegex = Regex("^Trade completed with (?:\\[.*?] )?(?<player>\\w+)!$")
    private val coinsReceivedRegex = Regex("^ \\+ (?<amount>\\d+\\.?\\d*)M coins$")
    private val deathRegex = Regex("^ ☠ (?<player>\\w+) was killed by (?<killer>.+)\\.$")

    private val json = JsonStore("features/slayerCarryTracker")
    val tracked = json.mutableList("tracked", SlayerCarryPlayer.CODEC)
    private val history = json.mutableList("history", SlayerCarryHistory.CODEC)

    private val ex0 = listOf("§f§lSlayer Carries:", "§7> §bExample §8[§7Void T4§8]§f: §b3§f/§b10 §7(12.5s | 28/hr)").fcs
    private val display = Ticking(5) {
        if (tracked.value.isEmpty()) return@Ticking null

        buildList {
            add("<bold>Slayer Carries:".parse())
            for (carry in tracked.value) add(carry.toString().parse(true))
        }.map { it.visualOrderText }
    }

    private val hud = config.hud("Slayer carry display") {
        if (it) return@hud sizedText(ex0)
        sizedText(display.value ?: return@hud null)
    }

    private var trader: String? = null

    init {
        command {
            "carry" / "add" / word("player").suggests { McClient.players.map { it.profile.name } } / int("amount", 1).suggests { listOf("1", "5", "10", "20") } / word("slayerType").suggests { SlayerBoss.SHORTS0 } / word("tier") {
                val name = string("player")
                val amount = int("amount")
                val slayerType = string("slayerType").lowercase()
                val t0 = string("tier")

                if (tracked.value.any { it.name.equals(name, true) }) return@word "<red>$name is already being tracked.".mod()

                val type = SlayerBoss.entries.find { it.short.lowercase() == slayerType } ?: return@word "Invalid slayer type.".mod()
                val i0 = if (t0.equals("any", true)) null else t0.toIntOrNull() ?: return@word "Invalid tier.".mod()

                if (i0 != null) {
                    val i1 = type.max.int
                    if (i0 !in 1..i1) return@word "<red>Tier must be 1-$i1 or any for $type.".mod()
                }

                val tier = SlayerTier.entries.find { it.int == i0 }
                tracked.update { add(SlayerCarryPlayer(name, type, tier, amount)) }
                "<green>Now tracking <aqua>$name <gray>[${type.short}${tier?.let { " T${it.int}" } ?: " Any"}] x$amount!".mod()
            }.suggests { listOf("any", "1", "2", "3", "4", "5") }

            "carry" / "remove" / word("player") {
                val name = string("player")
                val bool = tracked.value.any { it.name.equals(name, true) }

                if (!bool) {
                    return@word "<red>$name is not being tracked.".mod()
                }

                tracked.update { removeIf { it.name.equals(name, true) } }
                "<green>Removed <aqua>$name <green>from tracking.".mod()
            }.suggests { tracked.value.map { it.name } }

            "carry" / "list" {
                if (tracked.value.isEmpty()) {
                    return@invoke "<red>No active carries.".mod()
                }

                "Currently tracking:".mod()
                for (k in tracked.value) {
                    " <dark_gray>- <aqua>${k.name} <gray>[${k.type.short}${k.tier?.let { " T${it.int}" } ?: " Any"}] <yellow>${k.done}/${k.max}".parse().lie()
                }
            }

            "carry" / "list" / "clear" {
                tracked.update { clear() }
                "<green>Cleared all tracked carries.".mod()
            }

            "carry" / "history" {
                if (history.value.isEmpty()) {
                    return@invoke "<red>No carry history.".mod()
                }

                val e0 = history.value.asReversed()
                val i0 = maxOf(1, (e0.size + 9) / 10)

                val c = ("<dark_gray>" + ("-".repeat())).parse()
                val green = Mocha.Green.argb

                c.lie()
                "Carry History <gray>(1/$i0)<r>:".mod()

                val e1 = e0.take(10)
                for (e in e1) {
                    " <dark_gray>- <aqua>${e.name} <gray>• <$green>${e.amount}x ${e.type.short}${e.tier?.let { " T${it.int}" } ?: " Any"}s <gray>in <$green>${(e.duration / 1000.0).toDuration()}".parse().lie()
                }

                val hover = buildString {
                    val kv = e0.groupBy { "${it.type.short}${it.tier?.let { t -> " T${t.int}" } ?: " Any"}" }
                    for ((i, k) in kv.entries.withIndex()) {
                        if (i > 0) append('\n')
                        append("<$green>${k.key}<r>: <aqua>${k.value.sumOf { it.amount }} <gray>carries across <aqua>${k.value.size} <gray>entries")
                    }
                }

                c.lie()
                "<hover:$hover> <dark_gray>• <r>Total carries: <$green>${e0.sumOf { it.amount }} <gray>across <$green>${e0.size} <gray>entries.".parse(true).lie()
                c.lie()
            }

            "carry" / "history" / int("page", 1) {
                val page = int("page")
                val list = history.value.asReversed()

                val c = ("<dark_gray>" + ("-".repeat())).parse()
                val green = Mocha.Green.argb

                val page0 = maxOf(1, (list.size + 9) / 10)
                if (page > page0) {
                    return@int "<red>Page must be 1-$page0.".mod()
                }

                val start = (page - 1) * 10
                val end = minOf(start + 10, list.size)

                c.lie()
                "Carry History <gray>($page/$page0)<r>:".mod()

                for (i in start until end) {
                    val e = list[i]

                    " <dark_gray>- <aqua>${e.name} <gray>• <$green>${e.amount}x ${e.type.short}${e.tier?.let { " T${it.int}" } ?: " Any"}s <gray>in <$green>${(e.duration / 1000.0).toDuration()}".parse().lie()
                }

                val hover = buildString {
                    val kv = list.groupBy { "${it.type.short}${it.tier?.let { t -> " T${t.int}" } ?: " Any"}" }
                    for ((i, k) in kv.entries.withIndex()) {
                        if (i > 0) append('\n')
                        append("<$green>${k.key}<r>: <aqua>${k.value.sumOf { it.amount }} <gray>carries across <aqua>${k.value.size} <gray>entries")
                    }
                }

                c.lie()
                "<hover:$hover> <dark_gray>• <r>Total carries: <$green>${list.sumOf { it.amount }} <gray>across <$green>${list.size} <gray>entries.".parse(true).lie()
                c.lie()
            }.suggests { listOf("1", "2", "3", "4", "5") }

            "carry" / "help" {
                help()
            }

            "carry" {
                SlayerCarryGUI.open()
            }
        }

        on<MessageEvent.Chat.Receive> {
            tradeCompleteRegex.findThenNull(stripped, "player") { (player) ->
                trader = player
            } ?: return@on

            coinsReceivedRegex.findThenNull(stripped, "amount") { (a) ->
                val last = trader?.also { trader = null } ?: return@findThenNull
                val a0 = a.toDoubleOrNull() ?: return@findThenNull

                val list = mutableListOf<Triple<SlayerBoss, SlayerTier, Int>>()
                for (b in SlayerBoss.entries) {
                    val c = b.price ?: continue

                    for ((k, v) in c) {
                        for (e in v.split(',')) {
                            val price = e.trim().toDoubleOrNull() ?: continue
                            if (price <= 0.0) continue

                            val count = round(a0 / price).toInt()
                            if (count <= 0) continue

                            if (abs(a0 - count * price) < 0.01) list.add(Triple(b, k, count))
                        }
                    }
                }

                if (list.isEmpty()) return@findThenNull

                Scheduler.schedule(1.clientTicks) {
                    if (list.size == 1) {
                        val (boss, tier, count) = list[0]

                        "<click:command:/athen carry add $last $count ${boss.short.lowercase()} ${tier.int}>Received payment for <aqua>${count}x <gray>${boss.short} T${tier.int}<r> carries from <aqua>$last<r>. Click to add!".mod()
                        return@schedule
                    }

                    val root = "Found multiple matches, add for: ".literal()

                    for (i in list.indices) {
                        val (boss, tier, count) = list[i]

                        if (i > 0) {
                            root.append(" ".literal())
                        }

                        root.append("<click:command:/athen carry add $last $count ${boss.short.lowercase()} ${tier.int}><aqua>[${count}x ${boss.short} T${tier.int}]".parse())
                    }

                    root.mod()
                }
            } ?: return@on

            deathRegex.findThenNull(stripped, "player", "killer") { (player, killer) ->
                val p = tracked.value.find { it.name == player } ?: return@findThenNull
                if (p.boss != null) p.reset()
            }
        }

        on<SlayerEvent.Boss.Spawn> {
            val list = tracked.value
            if (list.isEmpty()) return@on

            val owner = slayerInfo.owner ?: return@on
            val type = slayerInfo.type as? SlayerBoss ?: return@on
            val carry = list.find { it.name == owner } ?: return@on
            if (carry.type != type) return@on
            if (carry.tier != null && carry.tier != slayerInfo.tier) return@on
            if (!carry.spawn(entity)) return@on

            entity.carry = carry

            if (!`announce$spawn`) return@on
            "Boss spawned for <aqua>$owner <gray>[${type.short}${if (carry.tier == null) " Any" else " T${slayerInfo.tier?.int}"}]".mod()
        }

        on<SlayerEvent.Boss.Death> {
            val list = tracked.value
            if (list.isEmpty()) return@on

            val type = slayerInfo.type as? SlayerBoss ?: return@on
            val name = entity.carry?.name ?: return@on
            val carry = list.find { it.name == name } ?: return@on
            if (carry.type != type) return@on
            if (carry.tier != null && carry.tier != slayerInfo.tier) return@on

            val result = carry.die(entity) ?: return@on
            tracked.update {} // no-op, intended.

            "<hover:<red>${result.ticks} ticks>Killed boss for <aqua>$name<r> in <yellow>${result.time.toDuration(secondsDecimals = 1)} <gray>| <yellow>${(result.ticks / 20.0).toDuration(secondsDecimals = 1)}".mod()

            if (`announce$comp$party`) {
                "pc $name: ${result.done}/${result.max}".command(false)
            }

            if (`webhook$each` && webhook) {
                `webhook$url`.request(HttpRequest.POST) {
                    body(mapOf("content" to "Completed ${result.done}/${result.max} ${carry.type} carries for $name"))
                }
            }

            if (result.last) {
                val time = result.time0.toDuration()
                "<${Mocha.Green.argb}>Completed bosses for <aqua>$name <gray>[${type.short}${if (carry.tier == null) " Any" else " T${slayerInfo.tier?.int}"}]<r> in <yellow>$time".mod()

                if (webhook) {
                    `webhook$url`.request(HttpRequest.POST) {
                        body(mapOf("content" to "Completed ${result.done}x ${carry.type} carries for $name ($time)"))
                    }
                }

                tracked.update { removeIf { it.name == name } }
                history.update { add(SlayerCarryHistory(name, carry.type, carry.tier, carry.max, carry.last - carry.first)) }
            }
        }

        on<WorldRenderEvent.Entity> {
            val e = entity as? LivingEntity ?: return@on
            if (e.carry == null) return@on

            extractFrameBox(e.renderBoundingBox, `highlight$boss$color`.rgb, `highlight$boss$width`)
        }.runWhen(`highlight$boss`.state)

        on<WorldRenderEvent.Extract> {
            for (p in tracked.value) {
                val p = p.entity.value ?: continue
                extractFrameBox(p.renderBoundingBox, `highlight$player$color`.rgb, `highlight$player$width`)
            }
        }.runWhen(`highlight$player`.state)

        on<LocationEvent.Server.Connect> {
            for (t in tracked.value) t.reset()
        }
    }

    private fun help() {
        val a = Athen.modId
        val b = listOf(
            "/$a carry" to "Open the config tracker menu",
            "/$a carry add <player> <amount> <type> <tier>" to ":3",
            "/$a carry remove <player>" to "Removes a tracked player",
            "/$a carry list" to "Lists players being tracked",
            "/$a carry list clear" to "Clears the active list",
            "/$a carry history [page=0]" to "Shows tracked history"
        )

        val c = ("<dark_gray>" + ("-".repeat())).parse()

        c.lie()
        ("<aqua>" + ("Athen Slayer Carry".center())).parse().lie()
        c.lie()

        for ((c, d) in b) "  <${Mocha.Green.argb}>$c <dark_gray>- <gray>$d".parse().lie()

        c.lie()
    }

    fun update(index: Int, name: String, type: SlayerBoss, tier: SlayerTier?, max: Int, done: Int) {
        val old = tracked.value.getOrNull(index) ?: return
        tracked.update { set(index, SlayerCarryPlayer(name, type, tier, max, done, old.first, old.last)) }
    }

    fun remove(index: Int) {
        if (index !in tracked.value.indices) return
        tracked.update { removeAt(index) }
    }

    fun complete(index: Int) {
        val carry = tracked.value.getOrNull(index) ?: return
        val duration = if (carry.first > 0 && carry.last > 0) carry.last - carry.first else 0L
        history.update { add(SlayerCarryHistory(carry.name, carry.type, carry.tier, carry.max, duration)) }
        tracked.update { removeAt(index) }
    }

    private val SlayerBoss.price: Map<SlayerTier, String>?
        get() {
            if (this == SlayerBoss.Voidgloom) return mapOf(SlayerTier.Three to `price$void$3`, SlayerTier.Four to `price$void$4`)
            if (this == SlayerBoss.Inferno) return mapOf(SlayerTier.Two to `price$blaze$2`, SlayerTier.Three to `price$blaze$3`, SlayerTier.Four to `price$blaze$4`)
            return null
        }
}
