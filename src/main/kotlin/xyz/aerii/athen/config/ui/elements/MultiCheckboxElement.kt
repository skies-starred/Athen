@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ui.elements.base.IExpandable
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.springValue

class MultiCheckboxElement(
    name: String,
    options: List<String>,
    private var selected: List<Int>,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IExpandable<List<Int>>(name, options, configKey, onUpdate) {
    private val `anim$check` = List(options.size) { i -> springValue(if (selected.contains(i)) 1f else 0f, 0.3f) }

    override fun text() = "${selected.size} selected"

    override fun content(x: Float, y: Float, index: Int, option: String) {
        val checkX = x + 12f
        val checkY = y + 7f
        NVGRenderer.drawRectangle(checkX, checkY, 12f, 12f, Mocha.Surface0.argb, 3f)
        NVGRenderer.drawHollowRectangle(checkX, checkY, 12f, 12f, 1f, Mocha.Text.argb, 3f)

        `anim$check`[index].value = if (selected.contains(index)) 1f else 0f
        val scale = `anim$check`[index].value

        if (scale > 0.01f) {
            val size = 8f * scale
            val offset = (8f - size) / 2f
            NVGRenderer.drawRectangle(checkX + 2f + offset, checkY + 2f + offset, size, size, Mocha.Mauve.argb, 2f * scale)
        }

        drawText(option, x + 30f, y + 7f, 14f)
    }

    override fun click(index: Int) {
        selected = if (selected.contains(index)) selected - index else selected + index
        onUpdate(configKey, selected)
    }
}