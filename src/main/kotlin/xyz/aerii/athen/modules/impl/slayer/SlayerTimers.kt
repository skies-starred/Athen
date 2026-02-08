package xyz.aerii.athen.modules.impl.slayer

import com.mojang.serialization.Codec
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.modMessage
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
    private var killPBs = scribble.mutableMap("kill_pbs", Codec.STRING, Codec.DOUBLE)
    private var questStartTime: Long = 0

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

            val spawnTime = System.currentTimeMillis()
            if (questStartTime <= 0) return@on

            val time = (spawnTime - questStartTime) / 1000.0
            "Slayer spawned in <yellow>${time.toDuration(secondsDecimals = 1)}<r>.".parse().modMessage()
        }

        on<SlayerEvent.Boss.Death> {
            if (!slayerInfo.isOwnedByPlayer) return@on

            val time = entity.tickCount / 20.0
            val key = slayerInfo.str
            val pb = killPBs.value[key]

            val (timeColor, diffText) = when {
                pb == null -> {
                    killPBs.update { this[key] = time }
                    TextColor.YELLOW to "<green> [New PB]"
                }

                time < pb -> {
                    killPBs.update { this[key] = time }
                    TextColor.GREEN to "<green> [-${(pb - time).toDuration(secondsDecimals = 1)}]"
                }

                else -> {
                    val color = if (time < pb * 1.1) Mocha.Peach.argb else TextColor.RED
                    color to "<$color> [+${(time - pb).toDuration(secondsDecimals = 1)}]"
                }
            }

            "Slayer killed in <$timeColor>${time.toDuration(secondsDecimals = 1)} $diffText<r>.".parse().modMessage()
        }
    }

    private fun reset() {
        questStartTime = 0
    }
}