package xyz.aerii.athen.modules.impl.slayer

import com.mojang.serialization.Codec
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.toDuration

@Load
@OnlyIn(skyblock = true)
object SlayerTimers : Module(
    "Slayer timers",
    "Kill and spawn timers for slayer bosses.",
    Category.SLAYER
) {
    private val scribble = Scribble("features/slayerTimers")
    private val killPBs = scribble.mutableMap("kill_pbs", Codec.STRING, Codec.DOUBLE)
    private var questStartTime: Long = 0
    private var startTick: Int = 0

    init {
        on<SlayerEvent.Quest.Start> {
            questStartTime = System.currentTimeMillis()
        }

        on<SlayerEvent.Quest.End> {
            questStartTime = 0
        }

        on<SlayerEvent.Cleanup> {
            reset()
        }

        on<LocationEvent.ServerConnect> {
            reset()
        }

        on<SlayerEvent.Boss.Spawn> {
            if (!slayerInfo.isOwnedByPlayer) return@on

            startTick = Chronos.Ticker.tickServer
            val spawnTime = System.currentTimeMillis()
            if (questStartTime <= 0) return@on

            val time = (spawnTime - questStartTime) / 1000.0
            "Slayer spawned in <yellow>${time.toDuration(secondsDecimals = 1)}<r>.".parse().modMessage()
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.isOwnedByPlayer) return@on

            val time = entity.tickCount / 20.0
            val str0 = time.toDuration(secondsDecimals = 1)
            val time0 = Chronos.Ticker.tickServer - startTick
            val str1 = (time0 / 20.0).toDuration(secondsDecimals = 1)
            val key = slayerInfo.str
            val pb = killPBs.value[key]

            val str = when {
                pb == null -> {
                    killPBs.update { this[key] = time }
                    "<yellow>$str0 <gray>| <yellow>$str1 <green>[New PB]"
                }

                time < pb -> {
                    killPBs.update { this[key] = time }
                    "<green>$str0 <gray>| <green>$str1 <dark_green>[-${(pb - time).toDuration(secondsDecimals = 1)}]"
                }

                else -> {
                    val a = time < pb * 1.1
                    val color = if (a) Mocha.Peach.argb else TextColor.RED
                    val tc = if (a) Mocha.Pink.argb else Mocha.Red.argb
                    "<$color>$str0 <gray>| <$color>$str1 <$tc>[+${(time - pb).toDuration(secondsDecimals = 1)}]"
                }
            }

            "Slayer killed in $str<r>.".parse().onHover("<red>$time0 ticks.".parse()).modMessage()
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("times") {
                    thenCallback("slayers") {
                        val b0 = "<gray>${"-".repeatBreak()}".parse()
                        val b1 = "<dark_gray>${"-".repeatBreak()}".parse()

                        b0.lie()

                        val a = killPBs.value.entries.groupBy { it.key.substringBeforeLast("_T") }
                        var f = true

                        for (type in a.keys.sorted()) {
                            if (!f) b1.lie()
                            f = false

                            "<aqua>✦ ${type.str()}".parse().lie()

                            val b = a[type]?.sortedBy { it.key } ?: return@thenCallback
                            for ((k, d) in b) {
                                val tier = k.substringAfterLast("_")
                                "  • <red>$tier<r>: <green>${d.toDuration(secondsDecimals = 1)}".parse().lie()
                            }
                        }

                        b0.lie()
                    }
                }
            }
        }
    }

    private fun String.str(): String =
        lowercase().split("_").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    private fun reset() {
        questStartTime = 0
    }
}