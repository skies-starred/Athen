@file:Suppress("Unused", "ConstPropertyName")

package xyz.aerii.athen.modules.impl.kuudra

import com.mojang.serialization.Codec
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.override
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDuration
import xyz.aerii.athen.utils.toDurationFromMillis
import kotlin.math.abs

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object KuudraSplits : Module(
    "Kuudra splits",
    "Splits for kuudra, very customisable.",
    Category.KUUDRA
) {
    private val chat by config.switch("Send to chat", true)
    private val _hud = config.hud("Splits display") {
        if (it) return@hud sizedText(hudExample)
        val display = display ?: return@hud null
        sizedText(display)
    }

    private val estimatePace by config.switch("Estimate run pace")
    private val estimateType by config.dropdown("Type", listOf("Use PB", "Use average time", "Use hardcoded values"), 1).dependsOn { estimatePace }
    private val estimateStyle by config.textInput("Style", "<red>Estimate<r>: #time").dependsOn { estimatePace }

    private val styleExpandable by config.expandable("Text style")
    private val advanced by config.switch("Advanced styling").childOf { styleExpandable }
    private val generalStyle by config.textInput("Style", "<red>#name<r>: #time <gray>[#tick]").dependsOn { !advanced }.childOf { styleExpandable }
    private val _unused by config.textParagraph("Variable: <red>#name<r>, <red>#time<r>, <red>#tick<r>, <red>#pb").dependsOn { !advanced }.childOf { styleExpandable }

    private val supplyStyle by config.textInput("Supplies", "<red>Supplies<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val buildStyle by config.textInput("Build", "<red>Build<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val fuelStyle by config.textInput("Fuel", "<red>Fuel<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val eatStyle by config.textInput("Eaten", "<red>Eaten<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val stunStyle by config.textInput("Stun", "<red>Stun<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val dpsStyle by config.textInput("DPS", "<red>DPS<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val skipStyle by config.textInput("Skip", "<red>Skip<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val killStyle by config.textInput("Kill", "<red>Kill<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val overallStyle by config.textInput("Overall", "<dark_red>Overlay<r>: #time <gray>[#tick]").dependsOn { advanced }.childOf { styleExpandable }
    private val _unused0 by config.textParagraph("Variable: <red>#time<r>, <red>#tick<r>, <red>#pb").dependsOn { advanced }.childOf { styleExpandable }

    private val scribble = Scribble("features/kuudraSplits")

    private val eatRegex = Regex("^(?<user>\\w+) has been eaten by Kuudra!$")
    private val stunRegex = Regex("^\\w+ has destroyed one of Kuudra's pods!$")

    private var display: List<Component>? = null

    init {
        on<LocationEvent.ServerConnect> {
            Split.reset()
            display = null
        }.override()

        on<KuudraEvent.End.Success> {
            val tier = KuudraAPI.tier?.int ?: return@on
            val splits = Split.entries.filter { tier in it.tiers }

            val first = splits.firstOrNull { it.started } ?: return@on
            val pBs = splits.associateWith { PB.get(tier, it) }
            val ov = PB.get(tier, null)

            for (s in splits) s.end()
            for (s in splits) PB.set(tier, s, s.durTime)
            for (s in splits) Average.set(tier, s, s.durTime)

            PB.set(tier, null, splits.sumOf { it.durTime })

            if (!chat) return@on
            Chronos.Tick after 2 then {
                "Split breakdown:".modMessage()

                for (s in splits) {
                    val t0 = s.durTime.toDurationFromMillis(secondsDecimals = 1)
                    val t1 = (s.durTicks / 20.0).toDuration(secondsDecimals = 1)
                    val delta = pBs[s].let { if (it != null && it > 0) s.durTime - it else Long.MAX_VALUE }.strD()

                    " <dark_gray>• <red>${s.str}<r>: $t0 <gray>[$t1]$delta".parse().lie()
                }

                val d0 = splits.sumOf { it.durTime }
                val d1 = splits.sumOf { it.durTicks / 20.0 }
                val str = ov.let { if (it > 0) d0 - it else Long.MAX_VALUE }.strD()

                val t0 = d0.toDurationFromMillis(secondsDecimals = 1)
                val t1 = d1.toDuration(secondsDecimals = 1)

                " <dark_gray>• <red>Overall<r>: $t0 <gray>[$t1]$str".parse().lie()
            }
        }
        on<TickEvent.Client> {
            if (!KuudraAPI.inRun) return@on
            val tier = KuudraAPI.tier?.int ?: return@on
            val player = client.player ?: return@on

            if (Split.Skip.active && player.position().y < 10) Split.Kill.start()

            val splits = Split.entries.filter { tier in it.tiers }
            val list = mutableListOf<Component>()

            for (s in splits) {
                val d0 = s.durTime.toDurationFromMillis(secondsDecimals = 1)
                val d1 = (s.durTicks / 20.0).toDuration(secondsDecimals = 1)
                val pb = PB.get(tier, s).toDurationFromMillis(secondsDecimals = 1)

                list += s.style.prs(s.str, d0, d1, pb)
            }

            val d0 = splits.sumOf { it.durTime }.toDurationFromMillis(secondsDecimals = 1)
            val d1 = splits.sumOf { it.durTicks / 20.0 }.toDuration(secondsDecimals = 1)
            val pb = PB.get(tier, null).toDurationFromMillis(secondsDecimals = 1)
            list += (if (advanced) overallStyle else generalStyle).prs("Overall", d0, d1, pb)

            if (!estimatePace || splits.none { it.started }) return@on ::display.set(list)
            val ms = splits.est(tier).takeIf { it != 0L } ?: return@on ::display.set(list)

            list += estimateStyle.prs("Estimate", ms.toDurationFromMillis(secondsDecimals = 1), "", "")
            display = list
        }

        on<MessageEvent.Chat.Receive> {
            if (!KuudraAPI.inRun) return@on
            val tier = KuudraAPI.tier?.int ?: return@on

            when (stripped) {
                "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!" -> {
                    Split.Supply.start()
                }

                "[NPC] Elle: OMG! Great work collecting my supplies!" -> {
                    Split.Build.start()
                }

                "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!" -> {
                    if (tier in Split.Fuel.tiers) return@on Split.Fuel.start()
                    Split.Eaten.start()
                }

                "[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!" -> {
                    if (tier in Split.Skip.tiers) Split.Skip.start()
                    else Split.Kill.start()
                }

                else -> {
                    if (tier < KuudraTier.BURNING.int) return@on
                    if (Split.Stun.started && Split.DPS.started) return@on

                    if (!Split.Stun.started) {
                        eatRegex.findThenNull(stripped, "user") { (user) ->
                            if (user == "Elle") return@findThenNull
                            Split.Stun.start()
                        }
                    }

                    if (!Split.DPS.started) {
                        stunRegex.findThenNull(stripped) {
                            Split.DPS.start()
                        }
                    }
                }
            }
        }
    }

    private fun String.prs(name: String, time: String, tick: String, pb: String): Component = this
        .replace("#name", name)
        .replace("#time", time)
        .replace("#tick", tick)
        .replace("#pb", pb)
        .parse(true)

    private fun Long.strD(): String {
        if (this == Long.MAX_VALUE) return ""

        val f = this < 0
        val color = if (f) "<${Catppuccin.Mocha.Green.argb}>" else "<${Catppuccin.Mocha.Peach.argb}>"
        val abs = abs(this).toDurationFromMillis(secondsDecimals = 1)

        return " $color[${if (f) "-" else "+"}$abs]"
    }

    private fun List<Split>.est(tier: Int): Long = sumOf { s ->
        val e = when (estimateType) {
            0 -> PB.get(tier, s)
            1 -> Average.get(tier, s)
            else -> s.est
        }

        if (s.ended) s.durTime
        else if (s.active && s.durTime > e) s.durTime
        else e
    }

    private enum class Split(val est: Long, val tiers: IntRange = KuudraTier.BASIC.int..KuudraTier.INFERNAL.int) {
        Supply(34_000),
        Build(20_000),
        Fuel(15_000, KuudraTier.BASIC.int..KuudraTier.HOT.int),
        Eaten(5_000, KuudraTier.BURNING.int..KuudraTier.INFERNAL.int),
        Stun(1_000, KuudraTier.BURNING.int..KuudraTier.INFERNAL.int),
        DPS(5_000, KuudraTier.BURNING.int..KuudraTier.INFERNAL.int),
        Skip(5_000, KuudraTier.INFERNAL.int..KuudraTier.INFERNAL.int),
        Kill(4_000);

        var startTick: Int = 0
        var startTime: Long = 0

        var endTick: Int = 0
        var endTime: Long = 0

        val durTime: Long
            get() {
                if (startTime == 0L) return 0
                if (endTime == 0L) return System.currentTimeMillis() - startTime
                return (endTime - startTime).coerceAtLeast(0)
            }

        val durTicks: Int
            get() {
                if (startTick == 0) return 0
                if (endTick == 0) return Chronos.Ticker.tickServer - startTick
                return (endTick - startTick).coerceAtLeast(0)
            }

        val str: String =
            name.lowercase().replaceFirstChar { it.uppercase() }

        val active: Boolean
            get() = started && !ended

        val started: Boolean
            get() = startTime != 0L

        val ended: Boolean
            get() = endTime != 0L

        val style: String
            get() {
                if (!advanced) return generalStyle

                return when (this) {
                    Supply -> supplyStyle
                    Build -> buildStyle
                    Fuel -> fuelStyle
                    Eaten -> eatStyle
                    Stun -> stunStyle
                    DPS -> dpsStyle
                    Skip -> skipStyle
                    Kill -> killStyle
                }
            }

        fun reset() {
            startTime = 0
            startTick = 0
            endTime = 0
            endTick = 0
        }

        fun start() {
            if (started) return

            startTime = System.currentTimeMillis()
            startTick = Chronos.Ticker.tickServer
            entries.filter { it.ordinal < ordinal }.forEach { it.end() }
        }

        fun end() {
            if (ended) return

            endTime = System.currentTimeMillis()
            endTick = Chronos.Ticker.tickServer
        }

        companion object {
            fun reset() = entries.forEach { it.reset() }
        }
    }

    private object PB {
        private val pbs = scribble.mutableMap("pbs", Codec.STRING, Codec.LONG)

        private fun key(tier: Int, split: Split?) =
            if (split == null) "$tier.Overall" else "$tier.${split.name}"

        fun get(tier: Int, split: Split?): Long {
            val key = key(tier, split)
            return pbs.value[key] ?: 0L
        }

        fun set(tier: Int, split: Split?, time: Long) {
            if (time <= 0L) return

            val key = key(tier, split)
            val old = pbs.value[key] ?: Long.MAX_VALUE

            if (time >= old) return

            pbs.update { this[key] = time }
        }
    }

    private object Average {
        private val averages = scribble.mutableMap("averages", Codec.STRING, Codec.LONG)
        private val history = scribble.mutableMap("splitHistory", Codec.STRING, Codec.LONG.listOf(0, 10), mutableMapOf())

        fun set(tier: Int, split: Split, duration: Long) {
            val key = "$tier.${split.name}"
            val list = history.value.getOrPut(key) { mutableListOf() }.toMutableList()

            list.add(duration)
            if (list.size > 10) list.removeAt(0)
            history.update { this[key] = list }

            val avg = list.sum() / list.size
            averages.update { this[key] = avg }
        }

        fun get(tier: Int, split: Split): Long {
            val key = "$tier.${split.name}"
            return averages.value[key] ?: split.est
        }
    }

    private const val hudExample: String =
        "§cSupply§f: 47.4s §7[46.4s]\n" +
        "§cBuild§f: 34.3s §7[31.9s]\n" +
        "§cEaten§f: 6.2s §7[5.7s]\n" +
        "§cStun§f: 0.3s §7[0.3s]\n" +
        "§cDPS§f: 8.2s §7[8.1s]\n" +
        "§cSkip§f: 5.6s §7[5.4s]\n" +
        "§cKill§f: 5.4s §7[5.4s]\n" +
        "§4Overall§f: 1m 40s §7[1m 38s]"
}