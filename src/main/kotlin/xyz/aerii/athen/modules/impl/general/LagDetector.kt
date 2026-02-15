package xyz.aerii.athen.modules.impl.general

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText

@Load
@OnlyIn(skyblock = true)
object LagDetector : Module(
    "Lag detector",
    "Displays a timer since the last server tick if it was older than the threshold.",
    Category.GENERAL
) {
    private val threshold by config.slider("Threshold", 750, 100, 5000, "ms")
    private var lastTick = 0L

    init {
        config.hud("Lag display") {
            if (it) return@hud sizedText("§c67ms")
            if (lastTick == 0L) return@hud null
            if (Smoothie.player == null) return@hud null

            val t = System.currentTimeMillis() - lastTick
            if (t <= threshold) return@hud null

            sizedText("§c${t}ms")
        }

        on<LocationEvent.ServerConnect> {
            lastTick = 0
        }

        on<TickEvent.Server> {
            lastTick = System.currentTimeMillis()
        }
    }
}