package xyz.aerii.athen.config.ui.elements

import xyz.aerii.athen.config.ui.elements.base.IBaseUI
import xyz.aerii.athen.utils.nvg.NVGRenderer

class TextParagraphElement(text: String) : IBaseUI(text, "", { _, _ -> }) {
    private val textHeight = NVGRenderer.getWrappedTextHeight(text, 14f, 228f)

    override fun draw(x: Float, y: Float, mouseX: Float, mouseY: Float): Float {
        NVGRenderer.drawTextWrapped(name, x + 6f, y + 8f, 14f, 228f)
        return getHeight()
    }

    override fun getHeight() = textHeight + 16f
}