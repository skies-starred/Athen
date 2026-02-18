@file:Suppress("PrivatePropertyName")

package xyz.aerii.athen.config.ui.panels

import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.config.ui.ClickGUI
import xyz.aerii.athen.config.ui.elements.base.IElement
import xyz.aerii.athen.handlers.Roulette
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.brighten
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.linear
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

class SectionButton(
    val feature: ConfigManager.Feature,
    private val panel: Panel,
    private val onUpdate: (String, Any) -> Unit,
    private val isLast: Boolean
) {
    companion object {
        const val HEIGHT = 32f
        const val CHEVRON_SIZE = 16f
        val ENABLED_COLOR = Mocha.Mauve.withAlpha(0.5f)
        val DISABLED_COLOR = Mocha.Base.withAlpha(0.5f)
        private val chevronIcon = NVGRenderer.createImage(Roulette.file("elements/chevron.svg").path, Mocha.Text.argb)
    }

    private var extended = false
    var visible = true
    var isEnabled = false
        private set

    private val elements = feature.options.sortElements().map { IElement(it, onUpdate) }
    private val `anim$expand` = timedValue(0f, 200L, ::easeOutQuad)
    private val `anim$radius` = timedValue(0f, 200L, ::linear)
    private val `anim$color` = springValue(DISABLED_COLOR, 0.18f)
    private val `anim$chevron` = timedValue(0f, 200L, ::easeOutQuad)

    init {
        isEnabled = ConfigManager.getValue(feature.configKey) as? Boolean ?: false
        `anim$color`.value = if (isEnabled) ENABLED_COLOR else DISABLED_COLOR
    }

    private fun List<ConfigManager.ElementData>.sortElements(): List<ConfigManager.ElementData> {
        if (isEmpty()) return this

        val children = Array(size) { mutableListOf<Int>() }
        val roots = mutableListOf<Int>()

        for ((i, e) in withIndex()) {
            val parentKey = e.parentKey
            if (parentKey == null) {
                roots += i
            } else {
                indexOfFirst { it.key == parentKey }
                    .takeIf { it != -1 }
                    ?.let { children[it] += i }
                    ?: roots.add(i)
            }
        }

        val result = ArrayList<ConfigManager.ElementData>(size)

        fun dfs(i: Int) {
            result += this[i]
            for (c in children[i]) dfs(c)
        }

        for (r in roots) dfs(r)
        return result
    }

    fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        val isHovered = isAreaHovered(x, y, Panel.WIDTH, HEIGHT)

        `anim$color`.value = when {
            isHovered && isEnabled -> ENABLED_COLOR.brighten(0.9f)
            isHovered -> DISABLED_COLOR.brighten(2f)
            isEnabled -> ENABLED_COLOR
            else -> DISABLED_COLOR
        }

        val radiusAnim = `anim$radius`.value
        val bottomRadius = if (isLast || !sections()) 5f * (1f - radiusAnim) else 0f
        NVGRenderer.drawRectangle(x, y, Panel.WIDTH, HEIGHT, `anim$color`.value, 0f, 0f, bottomRadius, bottomRadius)
        NVGRenderer.drawText(feature.name, x + 4f, y + HEIGHT / 2f - 8f, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)

        if (elements.isNotEmpty()) {
            val chevronAnim = `anim$chevron`.value
            NVGRenderer.push()
            NVGRenderer.translate(x + Panel.WIDTH - 16f, y + HEIGHT / 2f)
            NVGRenderer.rotate(Math.toRadians((chevronAnim * 90f).toDouble()).toFloat())
            NVGRenderer.drawImage(chevronIcon, -CHEVRON_SIZE / 2f, -CHEVRON_SIZE / 2f, CHEVRON_SIZE, CHEVRON_SIZE)
            NVGRenderer.pop()
        }

        if (isHovered) ClickGUI.featureTooltip.setText(feature.description)
        else if (feature.description == ClickGUI.featureTooltip.currentText) ClickGUI.featureTooltip.setText("")

        val expand = `anim$expand`.value
        if (extended || expand > 0f) {
            val optionsHeight = height()
            val displayHeight = expand * optionsHeight

            val clipping = expand < 1f
            if (clipping) NVGRenderer.pushScissor(x, y + HEIGHT, Panel.WIDTH, displayHeight)

            var optionY = y + HEIGHT
            for (e in elements) optionY += e.draw(x, optionY, mouseX, mouseY)

            if (clipping) NVGRenderer.popScissor()
        }

        return getHeight()
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val x = panel.x
        var y = panel.y + Panel.HEADER_HEIGHT + panel.scrollOffset
        panel.sections.filter { it.visible }.takeWhile { it != this }.forEach { y += it.getHeight() }

        if (!isAreaHovered(x, y, Panel.WIDTH, HEIGHT)) {
            if (extended) return elements.any { it.mouseClicked(mouseX, mouseY, button) }
            return false
        }

        return when (button) {
            0 -> {
                isEnabled = !isEnabled
                onUpdate(feature.configKey, isEnabled)
                `anim$color`.value = if (isEnabled) ENABLED_COLOR else DISABLED_COLOR
                true
            }

            1 -> {
                if (elements.isEmpty()) return true

                extended = !extended
                val animValue = if (extended) 1f else 0f
                `anim$expand`.value = animValue
                `anim$radius`.value = animValue
                `anim$chevron`.value = animValue
                true
            }

            else -> false
        }
    }

    fun mouseReleased(button: Int) =
        if (extended) for (e in elements) e.mouseReleased(button) else {}

    fun keyTyped(char: Char): Boolean =
        extended && elements.any { it.keyTyped(char) }

    fun keyPressed(keyCode: Int, scanCode: Int): Boolean =
        extended && elements.any { it.keyPressed(keyCode, scanCode) }

    fun matchesSearch(query: String): Boolean =
        feature.name.contains(query, ignoreCase = true) || feature.options.any { it.name.contains(query, ignoreCase = true) }

    fun getHeight(): Float {
        val expandAnim = `anim$expand`.value
        return if (extended || expandAnim > 0f) HEIGHT + expandAnim * height() else HEIGHT
    }

    private fun height() = elements.sumOf { it.getHeight().toDouble() }.toFloat()

    private fun sections(): Boolean = panel.sections.drop(panel.sections.indexOf(this) + 1).any { it.visible }
}