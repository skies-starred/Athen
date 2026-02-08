@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText

@Load
object ModSettings : Module(
    "Mod settings",
    "Toggles for a lot of the internal stuff in the mod!",
    Category.GENERAL
) {
    @JvmStatic
    val disableTickCulling by config.switch("Disable tick culling", true)

    private val _tickCullText by config.textParagraph("Disabling this may break slayer features!")

    @JvmStatic
    val commandConfig by config.switch("\'/athen\' opens config")

    init {
        config.hud("Help", outsidePreview = false) {
            sizedText("Use Arrow Keys to move elements\nPress C to center elements.")
        }
    }
}