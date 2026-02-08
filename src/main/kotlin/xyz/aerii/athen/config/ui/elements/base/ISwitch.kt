@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements.base

import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.brighten
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue

class ISwitch(initialValue: Boolean, private val enabledColor: Int = Mocha.Green.argb.brighten(0.8f)) {
    private val `anim$thumb` = springValue(if (initialValue) 26f else 2f, 0.25f)
    private val `anim$track` = springValue(if (initialValue) enabledColor else Mocha.Base.argb, 0.25f)
    private val `anim$scale` = springValue(1f, 0.3f)

    fun draw(x: Float, y: Float, enabled: Boolean): Boolean {
        val hovered = isAreaHovered(x, y, 40f, 16f)

        `anim$thumb`.value = if (enabled) 26f else 2f
        `anim$track`.value = if (enabled) enabledColor else Mocha.Base.argb
        `anim$scale`.value = if (hovered) 1.05f else 1f

        val scale = `anim$scale`.value
        val trackW = 40f * scale
        val trackH = 16f * scale

        val centerX = x + 20f
        val centerY = y + 8f
        val trackX = centerX - trackW / 2f
        val trackY = centerY - trackH / 2f

        val travel = trackW - 12f
        val thumbBase = `anim$thumb`.value / 26f
        val thumbX = trackX + thumbBase * travel - if (enabled) 2f else 0f
        val thumbY = trackY + (trackH - 12f) / 2f

        NVGRenderer.drawRectangle(trackX, trackY, trackW, trackH, `anim$track`.value, 5f)
        NVGRenderer.drawRectangle(thumbX, thumbY, 12f, 12f, Mocha.Text.argb, 5f)

        return hovered
    }

    fun isHovered(x: Float, y: Float) = isAreaHovered(x, y, 40f, 16f)
}
