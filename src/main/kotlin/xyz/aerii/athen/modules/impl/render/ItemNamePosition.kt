@file:Suppress("ConstPropertyName")

package xyz.aerii.athen.modules.impl.render

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.hud.Resolute
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText

@Load
object ItemNamePosition : Module(
    "Item name position",
    "Changes the positions of item display names",
    Category.RENDER
) {
    private const val str = "§cEpic item"
    private val int by lazy { client.font?.width(str) ?: 0 }

    val hud = config.hud("Item name", outsidePreview = false) {
        if (it) sizedText(str) else null
    }

    @JvmStatic
    fun x(): Int = ((hud.x + int / 2) * Resolute.scale).toInt()

    @JvmStatic
    fun y(): Int = (hud.y * Resolute.scale).toInt()
}