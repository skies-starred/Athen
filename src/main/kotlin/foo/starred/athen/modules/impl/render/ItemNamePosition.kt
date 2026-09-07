@file:Suppress("ConstPropertyName")

package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.sizedText
import foo.starred.athen.config.Category
import foo.starred.athen.hud.HUDManager
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.render.fcs
import foo.starred.snowbird.api.client

@Load
object ItemNamePosition : Module(
    "Item name position",
    "Changes the positions of item display names",
    Category.RENDER
) {
    private val ex0 = "§cEpic item".fcs
    private val int by lazy { client.font?.width(ex0) ?: 0 }

    val hud = config.hud("Item name", outsidePreview = false) {
        if (it) sizedText(ex0) else null
    }

    @JvmStatic
    fun x(): Int = ((hud.x + int / 2) * HUDManager.scale).toInt()

    @JvmStatic
    fun y(): Int = (hud.y * HUDManager.scale).toInt()
}