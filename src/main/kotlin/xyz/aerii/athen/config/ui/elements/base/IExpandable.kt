@file:Suppress("PropertyName")

package xyz.aerii.athen.config.ui.elements.base

import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

abstract class IExpandable<T>(
    name: String,
    protected val options: List<String>,
    configKey: String,
    onUpdate: (String, Any) -> Unit
) : IBaseUI(name, configKey, onUpdate) {
    protected var expanded = false
    protected val `anim$expand` = timedValue(0f, 200L, ::easeOutQuad)
    protected val `anim$button` = springValue(Mocha.Base.argb, 0.2f)
    protected val `anim$optionHover` = options.map { springValue(Mocha.Base.argb, 0.15f) }
    protected val optionHeight = 26f

    abstract fun text(): String
    abstract fun click(index: Int)
    abstract fun content(x: Float, y: Float, index: Int, option: String)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        lastX = x
        lastY = y

        drawText(name, x + 6f, y + 8f)

        val buttonText = text()
        val buttonW = textWidth(buttonText) + 12f
        val buttonX = x + width - 20f - textWidth(buttonText)
        val isButtonHovered = isAreaHovered(buttonX, y + 8f, buttonW, 20f)

        `anim$button`.value = if (isButtonHovered) Mocha.Surface1.argb else Mocha.Base.argb
        drawBox(buttonX, y + 8f, buttonW, 20f, `anim$button`.value, Mocha.Surface0.argb)
        drawText(buttonText, buttonX + 6f, y + 10f)

        val targetHeight = options.size * optionHeight - 1f
        val containerHeight = `anim$expand`.value

        if (expanded || containerHeight > 0f) {
            if (containerHeight < targetHeight) NVGRenderer.pushScissor(x, y + 32f, width, containerHeight)
            NVGRenderer.drawRectangle(x + 6f, y + 32f, width - 12f, targetHeight, Mocha.Base.argb, 5f)

            for ((i, option) in options.withIndex()) {
                val optionY = y + 32f + i * optionHeight
                val isOptionHovered = isAreaHovered(x + 6f, optionY, width - 12f, optionHeight)

                `anim$optionHover`[i].value = if (isOptionHovered) Mocha.Surface1.argb else Mocha.Base.argb

                val (tl, tr, bl, br) = radii(i, options.size)
                NVGRenderer.drawRectangle(x + 6f, optionY, width - 12f, optionHeight, `anim$optionHover`[i].value, tl, tr, bl, br)
                content(x, optionY, i, option)
            }

            if (containerHeight < targetHeight) NVGRenderer.popScissor()
        }

        val displayHeight = if (expanded || containerHeight > 0f) containerHeight + 6f else 0f
        return 32f + displayHeight
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        val buttonText = text()
        val buttonW = textWidth(buttonText) + 12f
        val buttonX = lastX + width - 20f - textWidth(buttonText)

        if (isAreaHovered(buttonX, lastY + 8f, buttonW, 20f)) {
            toggleExpanded()
            return true
        }

        if (expanded) {
            for (i in options.indices) {
                val optionY = lastY + 32f + i * optionHeight
                if (isAreaHovered(lastX + 6f, optionY, width - 12f, optionHeight)) {
                    click(i)
                    return true
                }
            }
        }

        return false
    }

    override fun getHeight(): Float {
        val displayHeight = if (expanded || `anim$expand`.value > 0.001f) `anim$expand`.value + 6f else 0f
        return 32f + displayHeight
    }

    protected fun toggleExpanded() {
        expanded = !expanded
        `anim$expand`.value = if (expanded) options.size * optionHeight - 1f else 0f
    }

    protected fun collapse() {
        expanded = false
        `anim$expand`.value = 0f
    }
}
