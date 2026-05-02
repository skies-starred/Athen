package xyz.aerii.athen.modules.impl.render

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.modules.Module

@Load
object RenderTweaks : Module(
    "Render tweaks",
    "Tweaks Minecraft's rendering!",
    Category.RENDER
) {
    private val _nametag by config.switch("Show own nametag", true)

    @JvmStatic
    val nametag: Boolean
        get() = enabled && _nametag
}