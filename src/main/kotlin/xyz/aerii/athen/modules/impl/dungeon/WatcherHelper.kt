@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.dungeon

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.sounds.SoundEvents
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.ChatEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Smoothie.alert
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.hud.internal.HUDElement
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDuration
import xyz.aerii.athen.utils.toDurationFromMillis
import kotlin.math.abs

@Load
object WatcherHelper : Module(
    "Watcher helper",
    "Shows information about the watcher's speed and movements.",
    Category.DUNGEONS
) {
    private val breakdown by config.switch("Send breakdown", true)
    private val spawnedAll by config.switch("Show alert on all spawned", true)
    private val speak by config.switch("Show alert on speak", true)
    private val move by config.switch("Show alert on move", true)

    private val hud: HUDElement = config.hud("Blood timers") {
        if (it) return@hud if (showTicks) sizedText("Speak: §c23.4s §7(23.2s)\nMove: §c25.6s §7(24.6s)\nTotal: §c57.3s §7(54.2s)") else sizedText("Speak: §c23.4s\nMove: §c25.6s\nTotal: §c57.3s")
        val display = display ?: return@hud null
        sizedText(display)
    }

    private val showTicks by config.switch("Show ticks", true).dependsOn { hud.enabled }

    private val textExpandable by config.expandable("Alert texts")
    private val `text$fast` by config.textInput("Fast", "<red>Vroom!").childOf { textExpandable }
    private val `text$normal` by config.textInput("Normal", "<red>Watcher!").childOf { textExpandable }
    private val `text$slow` by config.textInput("Slow", "<red>Yawn...").childOf { textExpandable }
    private val `text$snail` by config.textInput("Very slow", "<red>Zzz...").childOf { textExpandable }

    private var `blood$start`: Long = 0
    private var `blood$start$t`: Int = 0
    private var `blood$speak`: Long = 0
    private var `blood$speak$t`: Int = 0
    private var `blood$move`: Long = 0
    private var `blood$move$t`: Int = 0

    private var `blood$watcher$x`: Double? = null
    private var `blood$watcher$z`: Double? = null
    private var `blood$watcher$moved`: Boolean = false

    private var `display$speak`: String = "???"
    private var `display$speak$t`: String = "???"
    private var `display$move`: String = "???"
    private var `display$move$t`: String = "???"
    private var `display$total`: String = "???"
    private var `display$total$t`: String = "???"
    private var display: String? = null

    private enum class Shrimp {
        SNAIL,
        SLOW,
        NORMAL,
        FAST;

        companion object {
            fun get(l: Long): Shrimp = when {
                l >= 25_000 -> SNAIL
                l >= 23_000 -> SLOW
                l >= 22_000 -> NORMAL
                else -> FAST
            }
        }
    }

    init {
        DungeonAPI.inBoss.onChange {
            if (!it) return@onChange
            resetStr()
        }

        DungeonAPI.bloodOpened.onChange {
            if (!it) return@onChange

            `blood$start` = System.currentTimeMillis()
            `blood$start$t` = Chronos.Ticker.tickServer
        }

        DungeonAPI.bloodSpawnedAll.onChange {
            if (!it) return@onChange
            if (`blood$start` == 0L) return@onChange
            if (!spawnedAll) return@onChange

            val t = System.currentTimeMillis() - `blood$start`
            val t0 = Chronos.Ticker.tickServer - `blood$start$t`

            val d = t.toDurationFromMillis(secondsDecimals = 1, secondsOnly = true)
            val d0 = (t0 / 20.0).toDuration(secondsDecimals = 1, secondsOnly = true)

            "Watcher took <red>$d <gray>($d0) <r>to spawn all!".parse().modMessage()
        }

        DungeonAPI.bloodKilledAll.onChange {
            if (!it) return@onChange
            if (`blood$start` == 0L) return@onChange

            reset()

            if (!breakdown) return@onChange
            "Watcher time breakdown:".modMessage()
            " <gray>• <r>Speak time: <red>$`display$speak` <gray>| <red>$`display$speak$t`".parse().modMessage()
            " <gray>• <r>Move time: <red>$`display$move` <gray>| <red>$`display$move$t`".parse().modMessage()
            " <gray>• <r>Total time: <red>$`display$total` <gray>| <red>$`display$total$t`".parse().modMessage()
        }

        on<LocationEvent.ServerConnect> {
            reset()
            resetStr()
        }

        on<TickEvent.Client> {
            if (DungeonAPI.bloodKilledAll.value) return@on
            if (!hud.enabled) return@on

            `display$total` = (System.currentTimeMillis() - `blood$start`).toDurationFromMillis(secondsDecimals = 1, secondsOnly = true)
            `display$total$t` = ((Chronos.Ticker.tickServer - `blood$start$t`) / 20.0).toDuration(secondsDecimals = 1, secondsOnly = true)

            display = buildString {
                append("Speak: §c$`display$speak`")
                if (showTicks) append(" §7($`display$speak$t`)")
                append('\n')

                append("Move: §c$`display$move`")
                if (showTicks) append(" §7($`display$move$t`)")
                append('\n')

                append("Total: §c$`display$total`")
                if (showTicks) append(" §7($`display$total$t`)")
            }
        }.runWhen(DungeonAPI.bloodOpened)

        on<ChatEvent> {
            if (message.stripped() != "[BOSS] The Watcher: Let's see how you can handle this.") return@on
            if (`blood$start` == 0L) return@on

            `blood$speak` = System.currentTimeMillis()
            `blood$speak$t` = Chronos.Ticker.tickServer

            val t = `blood$speak` - `blood$start`
            val t0 = `blood$speak$t` - `blood$start$t`

            `display$speak` = t.toDurationFromMillis(secondsDecimals = 1, secondsOnly = true)
            `display$speak$t` = (t0 / 20.0).toDuration(secondsDecimals = 1, secondsOnly = true)

            if (!speak) return@on

            val ty = Shrimp.get(t)

            "Watcher took <red>$`display$speak` <gray>($`display$speak$t`)<r> to speak!".parse().onHover("<red>$t0<white> ticks.".parse()).modMessage()

            when (ty) {
                Shrimp.FAST -> `text$fast`.parse().alert(subTitle = "Took <red>$`display$speak` <r>to speak!".parse(), soundType = SoundEvents.CAT_PURREOW)
                Shrimp.NORMAL -> `text$normal`.parse().alert(subTitle = "Took <red>$`display$speak` <r>to speak!".parse(), soundType = SoundEvents.CAT_PURREOW)
                Shrimp.SLOW -> `text$slow`.parse().alert(subTitle = "Took <red>$`display$speak` <r>to speak!".parse(), soundType = SoundEvents.CAT_PURREOW)
                Shrimp.SNAIL -> `text$snail`.parse().alert(subTitle = "Took <red>$`display$speak` <r>to speak!".parse(), soundType = SoundEvents.CAT_PURREOW)
            }
        }.runWhen(DungeonAPI.bloodOpened)

        onReceive<ClientboundMoveEntityPacket> {
            if (`blood$watcher$moved`) return@onReceive
            if (!hasPosition()) return@onReceive
            if (`blood$speak` == 0L) return@onReceive
            if (System.currentTimeMillis() - `blood$speak` < 2500) return@onReceive

            val l = client.level ?: return@onReceive
            val e = getEntity(l)?.takeIf { it.displayName?.stripped()?.contains("The Watcher") == true } ?: return@onReceive
            val x = `blood$watcher$x` ?: e.x.also { `blood$watcher$x` = it }
            val z = `blood$watcher$z` ?: e.z.also { `blood$watcher$z` = it }

            if (abs(e.x - x) <= 0.05 && abs(e.z - z) <= 0.05) return@onReceive

            `blood$move` = System.currentTimeMillis()
            `blood$move$t` = Chronos.Ticker.tickServer
            `blood$watcher$moved` = true

            val t = `blood$move` - `blood$start`
            val t0 = `blood$move$t` - `blood$start$t`

            `display$move` = t.toDurationFromMillis(secondsDecimals = 1, secondsOnly = true)
            `display$move$t` = (t0 / 20.0).toDuration(secondsDecimals = 1, secondsOnly = true)

            if (!move) return@onReceive

            "Watcher moved at <red>$`display$move` <gray>($`display$move$t`)<r>!".parse().onHover("<red>$t0<white> ticks.".parse()).modMessage()
        }.runWhen(DungeonAPI.bloodOpened)
    }

    private fun reset() {
        `blood$start` = 0
        `blood$start$t` = 0
        `blood$speak` = 0
        `blood$speak$t` = 0
        `blood$move` = 0
        `blood$move$t` = 0

        `blood$watcher$x` = null
        `blood$watcher$z` = null
        `blood$watcher$moved` = false
    }

    private fun resetStr() {
        `display$speak` = "???"
        `display$speak$t` = "???"
        `display$move` = "???"
        `display$move$t` = "???"
        `display$total` = "???"
        `display$total$t` = "???"
        display = null
    }
}