@file:Suppress("Unused")

package foo.starred.athen.modules.impl.slayer

import com.mojang.serialization.Codec
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.enums.MessageColors
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.Category
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.athen.utils.command
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.stripped
import foo.starred.snowbird.utils.toDuration

@Load
@OnlyIn(skyblock = true)
object SlayerTimers : Module(
    "Slayer timers",
    "Kill and spawn timers for slayer bosses.",
    Category.SLAYER
) {
    private val style0 by config.input("Spawn style", "Slayer spawned in <yellow>#time<r>.")
    private val _unused0 by config.variables("#time")

    private val style1 by config.input("Kill style", "Slayer#special killed in #color#time <gray>| #pb<r>.")
    private val _unused1 by config.variables("#special", "#color", "#time", "#pb", "#ticks")

    private val group0 by config.group("PB styles", false)
    private val pb0 by group0.input("First PB", "<yellow>#time <green>[New PB]")
    private val pb1 by group0.input("Faster", "<green>#time <dark_green>[-#diff]")
    private val pb2 by group0.input("Slower", "#color#time #color_diff[+#diff]")
    private val _unused2 by group0.variables("#time", "#diff", "#pb", "#color", "#color_diff", "#ticks")

    private val json = JsonStore("features/slayerTimers")
    private val kills = json.mutableMap("kill_pbs", Codec.STRING, Codec.DOUBLE)

    private var start0: Long = 0
    private var start1: Int = 0
    private var bool: Boolean = false

    init {
        on<SlayerEvent.Quest.Start> {
            start0 = System.currentTimeMillis()
        }

        on<SlayerEvent.Quest.End> {
            start0 = 0
        }

        on<SlayerEvent.Reset.Any> {
            reset()
        }

        on<SlayerEvent.Boss.Spawn> {
            if (!slayerInfo.owned) return@on

            val a = slayerInfo.type == SlayerBoss.Tarantula && slayerInfo.tier == SlayerTier.Five
            if (bool && a) return@on ::bool.set(false)
            if (a) bool = true

            start1 = Scheduler.ticks.server
            if (start0 <= 0) return@on

            val time = (System.currentTimeMillis() - start0) / 1000.0
            style0.replace("#time", time.toDuration(secondsDecimals = 1)).mod()
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.owned) return@on

            val time = entity.tickCount / 20.0
            val str0 = time.toDuration(secondsDecimals = 1)
            val time0 = Scheduler.ticks.server - start1
            val str1 = (time0 / 20.0).toDuration(secondsDecimals = 1)
            val key = slayerInfo.string + if (entity.customName?.stripped()?.contains("Conjoined Brood") == true) "_P2" else ""
            val pb = kills.value[key]

            if (pb == null || time < pb) {
                kills.update {
                    this[key] = time
                }
            }

            val (c, s) = when {
                pb == null -> {
                    "<yellow>" to pb0.parse(str1, "", "", "<yellow>", "<green>", "$time0")
                }

                time < pb -> {
                    "<green>" to pb1.parse(str1, (pb - time).toDuration(secondsDecimals = 1), pb.toDuration(secondsDecimals = 1), "<green>", "<dark_green>", "$time0")
                }

                else -> {
                    val a = time < pb * 1.1
                    val c0 = if (a) Mocha.Peach.argb else MessageColors.RED.color
                    val c1 = if (a) Mocha.Pink.argb else Mocha.Red.argb
                    "<$c0>" to pb2.parse(str1, (time - pb).toDuration(secondsDecimals = 1), pb.toDuration(secondsDecimals = 1), "<$c0>", "<$c1>", "$time0")
                }
            }

            val a = slayerInfo.type == SlayerBoss.Tarantula && slayerInfo.tier == SlayerTier.Five
            val p = if (a && bool) " <dark_gray>[P1]<r>" else if (a) " <dark_gray>[P2]<r>" else ""
            val string = style1.replace("#special", p).replace("#color", c).replace("#time", str0).replace("#pb", s).replace("#ticks", "$time0")
            "<hover:<red>$time0 ticks.>$string".mod()
        }

        command {
            "times" / "slayers" {
                val b0 = "<gray>${"-".repeat()}".parse()
                val b1 = "<dark_gray>${"-".repeat()}".parse()

                b0.lie()

                val a = kills.value.entries.groupBy { it.key.substringBeforeLast("_T") }
                var f = true

                for (type in a.keys.sorted()) {
                    if (!f) b1.lie()
                    f = false

                    "<aqua>✦ ${type.str()}".parse().lie()

                    val b = a[type]?.sortedBy { it.key } ?: return@invoke
                    for ((k, d) in b) {
                        val tier = k.substringAfterLast("_")
                        "  • <red>$tier<r>: <green>${d.toDuration(secondsDecimals = 1)}".parse().lie()
                    }
                }

                b0.lie()
            }
        }
    }

    private fun String.str(): String {
        return lowercase().split("_").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    private fun String.parse(time: String, diff: String, pb: String, color: String, color1: String, ticks: String): String {
        return replace("#color_diff", color1)
            .replace("#diff_color", color1)
            .replace("#color1", color1)
            .replace("#color", color)
            .replace("#diff", diff)
            .replace("#time", time)
            .replace("#pb", pb)
            .replace("#ticks", ticks)
    }

    private fun reset() {
        bool = false
        start0 = 0
    }
}
