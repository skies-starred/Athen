@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.modules.Module

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

    @JvmStatic
    val upsideDown by config.switch("Upside down", true)

    @JvmStatic
    val priceFetch = config.slider("Price re-fetch", 10, 5, 60, "minutes").custom("priceFetch")

    @JvmStatic
    val oldApi by config.switch("Use old API")

    private val _oldApiText by config.textParagraph("Some features may not work with the old api.")
}