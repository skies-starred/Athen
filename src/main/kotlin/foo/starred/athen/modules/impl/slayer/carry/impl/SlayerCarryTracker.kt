@file:Suppress("Unused", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer.carry.impl

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.ducks.entity.generic.EntityDuck.Companion.carry
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryHistory
import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryPlayer
import foo.starred.athen.modules.impl.slayer.carry.ui.SlayerCarryGUI
import foo.starred.athen.utils.command
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.chat.command.MinecraftCommands
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.tablist.MinecraftTabList
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.entity.extension.renderAABB
import foo.starred.kontext.api.format.extension.component.formattedText
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.format.extension.time.parseDuration
import foo.starred.kontext.api.network.data.HttpRequest
import foo.starred.kontext.api.scheduling.scheduler.extensions.clientTicks
import foo.starred.kontext.api.storage.impl.generic.extension.mutableList
import foo.starred.kontext.api.text.parser.impl.parse
import foo.starred.parallax.api.primitives.ParallaxBox
import net.minecraft.world.entity.LivingEntity
import kotlin.math.abs
import kotlin.math.round

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object SlayerCarryTracker : Module(
    "Slayer carry tracker",
    "Track slayer carries and display progress.",
    ConfigCategory.SLAYER
) {
    private val `announce$comp$party` by config.switch("Announce in party", true)
    private val `announce$spawn` by config.switch("Show spawn message", true)

    private val _manager by config.button("Open manager") { SlayerCarryGUI.open() }

    private val _webhook by config.group("Discord webhook")
    private val webhook by _webhook.switch("Send to webhook")
    private val `webhook$each` by _webhook.switch("Send on each kill", true)
    private val `webhook$url` by _webhook.input("Webhook URL").description("Requires you to add your own webhook URL!")

    private val _void by config.group("Voidgloom Prices")
    private val `price$void$3` by _void.input("T3 Price (M)", "0.8, 0.65")
    private val `price$void$4` by _void.input("T4 Price (M)", "1.3, 2.3, 2, 1.5")

    private val _blaze by config.group("Inferno Prices")
    private val `price$blaze$2` by _blaze.input("T2 Price (M)", "2, 1.7, 1.2")
    private val `price$blaze$3` by _blaze.input("T3 Price (M)", "3.5, 3, 2.5")
    private val `price$blaze$4` by _blaze.input("T4 Price (M)", "7, 6, 5")

    private val _highlights by config.group("Highlights")
    private val `highlight$boss` = _highlights.switch("Highlight boss", true).unique("highlightBoss")
    private val `highlight$boss$color` by _highlights.colorPicker("Boss color", MochaColorScheme.Red.argb)
    private val `highlight$boss$width` by _highlights.slider("Boss line width", 2f, 0f, 10f)
    private val `highlight$player` = _highlights.switch("Highlight player", true).unique("highlightPlayer")
    private val `highlight$player$color` by _highlights.colorPicker("Player color", MochaColorScheme.Blue.argb)
    private val `highlight$player$width` by _highlights.slider("Player line width", 2f, 0f, 10f)

    private val tradeCompleteRegex = Regex("^Trade completed with (?:\\[.*?] )?(?<player>\\w+)!$")
    private val coinsReceivedRegex = Regex("^ \\+ (?<amount>\\d+\\.?\\d*)M coins$")
    private val deathRegex = Regex("^ ☠ (?<player>\\w+) was killed by (?<killer>.+)\\.$")

    private val json = JsonStore("features/slayerCarryTracker")
    private val history = json.mutableList("history", SlayerCarryHistory.CODEC)
    val tracked = json.mutableList("tracked", SlayerCarryPlayer.CODEC)

    private val display = Ticking(5) {
        if (tracked.value.isEmpty()) return@Ticking null

        buildList {
            add("<bold>Slayer Carries:".parse())
            for (carry in tracked.value) add(carry.toString().parse(true))
        }.map { it.visualOrderText }
    }

    private val hud by config.hud("Slayer carry display") {
        val example = listOf("§f§lSlayer Carries:", "§7> §bExample §8[§7Void T4§8]§f: §b3§f/§b10 §7(12.5s | 28/hr)").formattedText()

        constrain {
            MinecraftFontMeasurer.constrain(example)
        }

        preview {
            MinecraftFontRenderer.extract(graphics, example, 0, 0)
        }

        render {
            MinecraftFontRenderer.extract(graphics, display.value ?: return@render, 0, 0)
        }
    }

    private var trader: String? = null

    init {
        on<MessageEvent.Chat.Receive> {
            tradeCompleteRegex.find(stripped)?.let { match ->
                trader = match.groupValues.get(1)
                return@on
            }

            deathRegex.find(stripped)?.let { match ->
                val (player, killer) = match.destructured

                val p = tracked.value.find { it.name == player }
                if (p?.boss != null) p.reset()

                return@on
            }

            if (trader == null) return@on
            val amount = coinsReceivedRegex.find(stripped)?.groupValues?.get(1) ?: return@on
            val trader = trader?.also { trader = null } ?: return@on
            val i = amount.toDoubleOrNull() ?: return@on

            val list = matches(i)
            if (list.isEmpty()) return@on

            Scheduler.schedule(1.clientTicks) {
                if (list.size == 1) {
                    val (boss, tier, count) = list[0]

                    "<click:command:/athen carry add $trader $count ${boss.short.lowercase()} ${tier.int}>Received payment for <aqua>${count}x <gray>${boss.short} T${tier.int}<r> carries from <aqua>$trader<r>. Click to add!".mod()
                    return@schedule
                }

                val root = "Found multiple matches, add for: ".literal()
                for (i in list.indices) {
                    val (boss, tier, count) = list[i]

                    if (i > 0) root.append(" ".literal())
                    root.append("<click:command:/athen carry add $trader $count ${boss.short.lowercase()} ${tier.int}><aqua>[${count}x ${boss.short} T${tier.int}]".parse())
                }

                root.mod()
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
            if (carry.type == SlayerBoss.Tarantula && carry.tier == SlayerTier.Five && slayerInfo.phase != 2) return@on

            val result = carry.die(entity) ?: return@on
            tracked.update {} // no-op, intended.

            "<hover:<red>${result.ticks} ticks>Killed boss for <aqua>$name<r> in <yellow>${result.time.parseDuration(decimals = 1)} <gray>| <yellow>${(result.ticks / 20.0).parseDuration(decimals = 1)}".mod()

            if (`announce$comp$party`) {
                MinecraftCommands.client("pc $name: ${result.done}/${result.max}")
            }

            if (`webhook$each` && webhook) {
                `webhook$url`.request(HttpRequest.POST) {
                    body(mapOf("content" to "Completed ${result.done}/${result.max} ${carry.type} carries for $name"))
                }
            }

            if (result.last) {
                val time = result.time0.parseDuration()
                "<${MochaColorScheme.Green.argb}>Completed bosses for <aqua>$name <gray>[${type.short}${if (carry.tier == null) " Any" else " T${slayerInfo.tier?.int}"}]<r> in <yellow>$time".mod()

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

            ParallaxBox.frame(e.renderAABB(), `highlight$boss$color`, `highlight$boss$width`)
        }.runWhen(`highlight$boss`.state)

        on<WorldRenderEvent.Extract> {
            for (p in tracked.value) {
                val p = p.entity.value ?: continue
                ParallaxBox.frame(p.renderAABB(), `highlight$player$color`, `highlight$player$width`)
            }
        }.runWhen(`highlight$player`.state)

        on<LocationEvent.Server.Connect> {
            for (t in tracked.value) t.reset()
        }
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

    private fun matches(i: Double): List<Triple<SlayerBoss, SlayerTier, Int>> {
        return SlayerBoss.entries.flatMap { b ->
            val map = when (b) {
                SlayerBoss.Voidgloom -> mapOf(SlayerTier.Three to `price$void$3`, SlayerTier.Four to `price$void$4`)
                SlayerBoss.Inferno -> mapOf(SlayerTier.Two to `price$blaze$2`, SlayerTier.Three to `price$blaze$3`, SlayerTier.Four to `price$blaze$4`)
                else -> return@flatMap emptyList()
            }

            map.flatMap { (k, v) ->
                v.split(',').mapNotNull {
                    val price = it.trim().toDoubleOrNull() ?: return@mapNotNull null
                    if (price <= 0.0) return@mapNotNull null

                    val count = round(i / price).toInt()
                    if (count <= 0) return@mapNotNull null
                    if (abs(i - count * price) >= 0.01) return@mapNotNull null

                    Triple(b, k, count)
                }
            }
        }
    }

    init {
        command {
            "carry" / "add" / word("player").suggests { MinecraftTabList.players.map { it.profile.name } } / int("amount", 1).suggests { listOf("1", "5", "10", "20") } / word("slayerType").suggests { SlayerBoss.SHORTS0 } / word("tier") {
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
                for ((name, type, tier, max, done) in tracked.value) {
                    MinecraftMessages.display(" <dark_gray>- <aqua>$name <gray>[${type.short}${tier?.let { " T${it.int}" } ?: " Any"}] <yellow>$done/$max")
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

                val c = ("<dark_gray>" + MinecraftChat.repeat("-")).parse()
                val green = MochaColorScheme.Green.argb

                MinecraftMessages.display(c)
                "Carry History <gray>(1/$i0)<r>:".mod()

                val e1 = e0.take(10)
                for ((name, type, tier, amount, duration) in e1) {
                    MinecraftMessages.display(" <dark_gray>- <aqua>$name <gray>• <$green>${amount}x ${type.short}${tier?.let { " T${it.int}" } ?: " Any"}s <gray>in <$green>${(duration / 1000.0).parseDuration()}")
                }

                val hover = buildString {
                    val kv = e0.groupBy { "${it.type.short}${it.tier?.let { t -> " T${t.int}" } ?: " Any"}" }
                    for ((i, k) in kv.entries.withIndex()) {
                        if (i > 0) append('\n')
                        append("<$green>${k.key}<r>: <aqua>${k.value.sumOf { it.amount }} <gray>carries across <aqua>${k.value.size} <gray>entries")
                    }
                }

                MinecraftMessages.display(c)
                MinecraftMessages.display("<hover:$hover> <dark_gray>• <r>Total carries: <$green>${e0.sumOf { it.amount }} <gray>across <$green>${e0.size} <gray>entries.".parse(true))
                MinecraftMessages.display(c)
            }

            "carry" / "history" / int("page", 1) {
                val page = int("page")
                val list = history.value.asReversed()

                val c = ("<dark_gray>" + MinecraftChat.repeat("-")).parse()
                val green = MochaColorScheme.Green.argb

                val page0 = maxOf(1, (list.size + 9) / 10)
                if (page > page0) {
                    return@int "<red>Page must be 1-$page0.".mod()
                }

                val start = (page - 1) * 10
                val end = minOf(start + 10, list.size)

                MinecraftMessages.display(c)
                "Carry History <gray>($page/$page0)<r>:".mod()

                for (i in start until end) {
                    val e = list[i]

                    MinecraftMessages.display(" <dark_gray>- <aqua>${e.name} <gray>• <$green>${e.amount}x ${e.type.short}${e.tier?.let { " T${it.int}" } ?: " Any"}s <gray>in <$green>${(e.duration / 1000.0).parseDuration()}")
                }

                val hover = buildString {
                    val kv = list.groupBy { "${it.type.short}${it.tier?.let { t -> " T${t.int}" } ?: " Any"}" }
                    for ((i, k) in kv.entries.withIndex()) {
                        if (i > 0) append('\n')
                        append("<$green>${k.key}<r>: <aqua>${k.value.sumOf { it.amount }} <gray>carries across <aqua>${k.value.size} <gray>entries")
                    }
                }

                MinecraftMessages.display(c)
                MinecraftMessages.display("<hover:$hover> <dark_gray>• <r>Total carries: <$green>${list.sumOf { it.amount }} <gray>across <$green>${list.size} <gray>entries.".parse(true))
                MinecraftMessages.display(c)
            }.suggests { listOf("1", "2", "3", "4", "5") }

            "carry" / "help" {
                help()
            }

            "carry" {
                SlayerCarryGUI.open()
            }
        }
    }

    private fun help() {
        val b = listOf(
            "/athen carry" to "Open the config tracker menu",
            "/athen carry add <player> <amount> <type> <tier>" to ":3",
            "/athen carry remove <player>" to "Removes a tracked player",
            "/athen carry list" to "Lists players being tracked",
            "/athen carry list clear" to "Clears the active list",
            "/athen carry history [page=0]" to "Shows tracked history"
        )

        val c = ("<dark_gray>" + MinecraftChat.repeat("-")).parse()

        MinecraftMessages.display(c)
        MinecraftMessages.display("<aqua>" + MinecraftChat.center("Athen Slayer Carry"))
        MinecraftMessages.display(c)

        for ((cmd, desc) in b) {
            MinecraftMessages.display("  <${MochaColorScheme.Green.argb}>$cmd <dark_gray>- <gray>$desc")
        }

        MinecraftMessages.display(c)
    }
}
