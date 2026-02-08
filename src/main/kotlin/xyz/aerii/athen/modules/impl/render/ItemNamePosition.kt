@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.modules.impl.render

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.override
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText

@Load
object ItemNamePosition : Module(
    "Item name position",
    "Changes the positions of item display names",
    Category.RENDER
) {
    private const val str = "§cEpic item"
    private var int = 0

    val hud = config.hud("Item name") {
        if (it) sizedText(str) else null
    }

    init {
        on<TickEvent.Client> {
            int = client.font.width(str)

            if (hud.x == 20f && hud.y == 20f) {
                hud.x = client.window.guiScaledWidth / 2f
                hud.y = client.window.guiScaledHeight - 59f
            }
        }.override().once()
    }

    @JvmStatic
    fun x(): Int = hud.scaledX.toInt() + int / 2

    @JvmStatic
    fun y(): Int = hud.scaledY.toInt()
}