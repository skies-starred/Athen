@file:Suppress("Unused", "ConstPropertyName")

package xyz.aerii.athen.modules.impl.kuudra

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.serialization.Codec
import net.minecraft.network.chat.Component
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.override
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.parse
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

    private var display: List<Component>? = null

    init {
        on<LocationEvent.ServerConnect> {
            display = null
        }.override()

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("times") {
                    then("kuudra") {
                        thenCallback("tier", IntegerArgumentType.integer(1, 5)) {
                            val tier = IntegerArgumentType.getInteger(this, "tier")
                            val splits = KuudraPhase.entries.filter { tier in it.tiers }
                            val pbs = splits.associateWith { PB.get(tier, it) }

                            if (pbs.values.all { it == 0L }) {
                                "<red>No Kuudra splits for tier $tier. Did you have \"Kuudra Splits\" enabled?".parse().modMessage(Typo.PrefixType.ERROR)
                                return@thenCallback
                            }

                            "<yellow>PBs for <red>Kuudra T$tier:".parse().modMessage()

                            for (s in splits) {
                                val pb = pbs[s]?.toDurationFromMillis(secondsDecimals = 1) ?: continue
                                val type = if (s == KuudraPhase.Fuel && tier >= KuudraTier.BURNING.int) "eaten" else null
                                " <dark_gray>• <red>${s.str(type)}<r>: $pb".parse().lie()
                            }

                            val overall = PB.get(tier, null).toDurationFromMillis(secondsDecimals = 1)
                            " <dark_gray>• <red>Overall<r>: $overall".parse().lie()
                        }
                    }
                }
            }
        }

        on<KuudraEvent.End.Success> {
            val tier = KuudraAPI.tier?.int ?: return@on
            val splits = KuudraPhase.entries.filter { tier in it.tiers }

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

                    " <dark_gray>• <red>${s.str()}<r>: $t0 <gray>[$t1]$delta".parse().lie()
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
            if (!_hud.enabled) return@on
            val tier = KuudraAPI.tier?.int ?: return@on
            val splits = KuudraPhase.entries.filter { tier in it.tiers }
            val list = mutableListOf<Component>()

            for (s in splits) {
                val d0 = s.durTime.toDurationFromMillis(secondsDecimals = 1)
                val d1 = (s.durTicks / 20.0).toDuration(secondsDecimals = 1)
                val pb = PB.get(tier, s).toDurationFromMillis(secondsDecimals = 1)

                list += s.style.prs(s.str(), d0, d1, pb)
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

    private fun List<KuudraPhase>.est(tier: Int): Long = sumOf { s ->
        val e = when (estimateType) {
            0 -> PB.get(tier, s)
            1 -> Average.get(tier, s)
            else -> s.est
        }

        if (s.ended) s.durTime
        else if (s.active && s.durTime > e) s.durTime
        else e
    }

    private object PB {
        private val pbs = scribble.mutableMap("pbs", Codec.STRING, Codec.LONG)

        private fun key(tier: Int, split: KuudraPhase?) =
            if (split == null) "$tier.Overall" else "$tier.${split.name}"

        fun get(tier: Int, split: KuudraPhase?): Long {
            val key = key(tier, split)
            return pbs.value[key] ?: 0L
        }

        fun set(tier: Int, split: KuudraPhase?, time: Long) {
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

        fun set(tier: Int, split: KuudraPhase, duration: Long) {
            val key = "$tier.${split.name}"
            val list = history.value.getOrPut(key) { mutableListOf() }.toMutableList()

            list.add(duration)
            if (list.size > 10) list.removeAt(0)
            history.update { this[key] = list }

            val avg = list.sum() / list.size
            averages.update { this[key] = avg }
        }

        fun get(tier: Int, split: KuudraPhase): Long {
            val key = "$tier.${split.name}"
            return averages.value[key] ?: split.est
        }
    }

    private val KuudraPhase.est: Long
        get() {
            val eaten = this == KuudraPhase.Fuel && (KuudraAPI.tier?.int ?: 0) >= KuudraTier.BURNING.int
            return when {
                eaten -> 5_000L
                else -> when (this) {
                    KuudraPhase.Supply -> 34_000L
                    KuudraPhase.Build -> 20_000L
                    KuudraPhase.Fuel -> 15_000L
                    KuudraPhase.Stun -> 1_000L
                    KuudraPhase.DPS -> 5_000L
                    KuudraPhase.Skip -> 5_000L
                    KuudraPhase.Kill -> 4_000L
                }
            }
        }

    private val KuudraPhase.style: String
        get() {
            if (!advanced) return generalStyle

            val eaten = this == KuudraPhase.Fuel && (KuudraAPI.tier?.int ?: 0) >= KuudraTier.BURNING.int
            return when {
                eaten -> eatStyle
                else -> when (this) {
                    KuudraPhase.Supply -> supplyStyle
                    KuudraPhase.Build -> buildStyle
                    KuudraPhase.Fuel -> fuelStyle
                    KuudraPhase.Stun -> stunStyle
                    KuudraPhase.DPS -> dpsStyle
                    KuudraPhase.Skip -> skipStyle
                    KuudraPhase.Kill -> killStyle
                }
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