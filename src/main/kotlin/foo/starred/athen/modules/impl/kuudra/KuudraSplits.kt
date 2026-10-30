@file:Suppress("Unused", "ConstPropertyName")

package foo.starred.athen.modules.impl.kuudra

import com.mojang.serialization.Codec
import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.kuudra.KuudraAPI
import foo.starred.athen.api.kuudra.enums.KuudraPhase
import foo.starred.athen.api.kuudra.enums.KuudraTier
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.KuudraEvent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.format.extension.component.formattedText
import foo.starred.kontext.api.format.extension.time.parseDuration
import foo.starred.kontext.api.scheduling.scheduler.extensions.clientTicks
import foo.starred.kontext.api.storage.impl.generic.extension.mutableMap
import foo.starred.kontext.api.text.parser.impl.parse
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import kotlin.math.abs

@LoadableAthenModule
@AthenModuleScope(islands = [PresetSkyBlockIsland.KUUDRA])
object KuudraSplits : Module(
    "Kuudra splits",
    "Splits for kuudra, very customisable.",
    ConfigCategory.KUUDRA
) {
    private val chat by config.switch("Send to chat", true)
    private val _hud by config.hud("Splits display") {
        val example: List<FormattedCharSequence> =
            """
        §cSupply§f: 47.4s §7[46.4s]
        §cBuild§f: 34.3s §7[31.9s]
        §cEaten§f: 6.2s §7[5.7s]
        §cStun§f: 0.3s §7[0.3s]
        §cDPS§f: 8.2s §7[8.1s]
        §cSkip§f: 5.6s §7[5.4s]
        §cKill§f: 5.4s §7[5.4s]
        §4Overall§f: 1m 40s §7[1m 38s]
        """.trimIndent().lines().formattedText()

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

    private val estimatePace by config.switch("Estimate run pace")
    private val estimateType by config.selector("Type", listOf("PB", "Average time", "Hardcoded"), 1)
    private val estimateStyle by config.input("Style", "<red>Estimate<r>: #time")
    private val styleType by config.selector("Styling type", listOf("General", "Advanced"))

    private val general by config.group("General text style")
    private val generalStyle by general.input("Style", "<red>#name<r>: #time <gray>[#tick]")
    private val _unused by general.variables("#name", "#time", "#tick", "#pb")

    private val advanced by config.group("Advanced text style")
    private val supplyStyle by advanced.input("Supplies", "<red>Supplies<r>: #time <gray>[#tick]")
    private val buildStyle by advanced.input("Build", "<red>Build<r>: #time <gray>[#tick]")
    private val fuelStyle by advanced.input("Fuel", "<red>Fuel<r>: #time <gray>[#tick]")
    private val eatStyle by advanced.input("Eaten", "<red>Eaten<r>: #time <gray>[#tick]")
    private val stunStyle by advanced.input("Stun", "<red>Stun<r>: #time <gray>[#tick]")
    private val dpsStyle by advanced.input("DPS", "<red>DPS<r>: #time <gray>[#tick]")
    private val skipStyle by advanced.input("Skip", "<red>Skip<r>: #time <gray>[#tick]")
    private val killStyle by advanced.input("Kill", "<red>Kill<r>: #time <gray>[#tick]")
    private val overallStyle by advanced.input("Overall", "<dark_red>Overlay<r>: #time <gray>[#tick]")
    private val _unused0 by advanced.variables("#time", "#tick", "#pb")

    private val json = JsonStore("features/kuudraSplits")
    private val display = Ticking {
        if (!KuudraAPI.inRun) return@Ticking null
        val tier = KuudraAPI.tier?.int ?: return@Ticking null
        val splits = KuudraPhase.entries.filter { tier in it.tiers }
        val list = mutableListOf<FormattedCharSequence>()

        for (s in splits) {
            val d0 = (s.durTime / 1000).parseDuration(decimals = 1)
            val d1 = (s.durTicks / 20.0).parseDuration(decimals = 1)
            val pb = (PB.get(tier, s) / 1000).parseDuration(decimals = 1)

            list += s.style.prs(s.str(), d0, d1, pb).visualOrderText
        }

        val d0 = (splits.sumOf { it.durTime } / 1000).parseDuration(decimals = 1)
        val d1 = splits.sumOf { it.durTicks / 20.0 }.parseDuration(decimals = 1)
        val pb = (PB.get(tier, null) / 1000).parseDuration(decimals = 1)
        list += (if (styleType == 1) overallStyle else generalStyle).prs("Overall", d0, d1, pb).visualOrderText

        if (!estimatePace || splits.none { it.started }) return@Ticking list
        val ms = splits.est(tier).takeIf { it != 0L } ?: return@Ticking list

        list += estimateStyle.prs("Estimate", (ms / 1000).parseDuration(decimals = 1), "", "").visualOrderText
        list
    }

    init {
        command {
            "times" / "kuudra" / int("tier", 1, 5) {
                val tier = int("tier")
                val splits = KuudraPhase.entries.filter { tier in it.tiers }
                val pbs = splits.associateWith { PB.get(tier, it) }

                if (pbs.values.all { it == 0L }) {
                    "<red>No Kuudra splits for tier $tier. Did you have \"Kuudra Splits\" enabled?".mod(MessagePrefixType.ERROR)
                    return@int
                }

                "<yellow>PBs for <red>Kuudra T$tier:".mod()

                for (s in splits) {
                    val pb = pbs[s]?.div(1000)?.parseDuration(decimals = 1) ?: continue
                    val type = if (s == KuudraPhase.Fuel && tier >= KuudraTier.BURNING.int) "eaten" else null
                    MinecraftMessages.display(" <dark_gray>• <red>${s.str(type)}<r>: $pb")
                }

                val overall = PB.get(tier, null).div(1000).parseDuration(decimals = 1)
                MinecraftMessages.display(" <dark_gray>• <red>Overall<r>: $overall")
            }
        }

        on<LocationEvent.Server.Connect> {
            display.reset()
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
            Scheduler.schedule(2.clientTicks) {
                "Split breakdown:".mod()

                for (s in splits) {
                    val t0 = (s.durTime / 1000).parseDuration(decimals = 1)
                    val t1 = (s.durTicks / 20.0).parseDuration(decimals = 1)
                    val delta = pBs[s].let { if (it != null && it > 0) s.durTime - it else Long.MAX_VALUE }.strD()

                    MinecraftMessages.display(" <dark_gray>• <red>${s.str()}<r>: $t0 <gray>[$t1]$delta")
                }

                val d0 = splits.sumOf { it.durTime }
                val d1 = splits.sumOf { it.durTicks / 20.0 }
                val str = ov.let { if (it > 0) d0 - it else Long.MAX_VALUE }.strD()

                val t0 = d0.div(1000).parseDuration(decimals = 1)
                val t1 = d1.parseDuration(decimals = 1)

                MinecraftMessages.display(" <dark_gray>• <red>Overall<r>: $t0 <gray>[$t1]$str")
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
        val color = if (f) "<${MochaColorScheme.Green.argb}>" else "<${MochaColorScheme.Peach.argb}>"
        val abs = abs(this).div(1000).parseDuration(decimals = 1)

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
        private val pbs = json.mutableMap("pbs", Codec.STRING, Codec.LONG)

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
        private val averages = json.mutableMap("averages", Codec.STRING, Codec.LONG)
        private val history = json.mutableMap("splitHistory", Codec.STRING, Codec.LONG.listOf(0, 10), mutableMapOf())

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
            if (styleType == 0) return generalStyle

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
}
