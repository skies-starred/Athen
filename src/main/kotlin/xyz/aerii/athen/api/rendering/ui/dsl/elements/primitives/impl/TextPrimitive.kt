@file:Suppress("Unused")

package xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.impl

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import xyz.aerii.athen.api.rendering.ui.dsl.elements.primitives.base.impl.IPrimitiveElement
import xyz.aerii.athen.api.rendering.ui.text.vanilla.extensions.extractText
import xyz.aerii.library.api.EMPTY_COMPONENT
import xyz.aerii.library.api.client

open class TextPrimitive : IPrimitiveElement<TextPrimitive>() {
    private var _text: FormattedCharSequence = EMPTY_COMPONENT.visualOrderText
    private var _texts: List<FormattedCharSequence>? = null

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0
    override var color: Int = -1

    override var interact: Boolean = false

    var text: Component = EMPTY_COMPONENT
        set(value) {
            if (field == value) return
            field = value
            _text = value.visualOrderText
            width = 0
            height = 0
            root.layout()
        }

    var texts: List<Component>? = null
        set(value) {
            if (field == value) return
            field = value
            _texts = value?.map { it.visualOrderText }
            width = 0
            height = 0
            root.layout()
        }

    var shadow: Boolean = true
    var center: Boolean = false
    var scale: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            width = 0
            height = 0
            root.layout()
        }

    override fun constrain(parent: IPrimitiveElement<*>) {
        size?.let {
            width = it.width(this, parent)
            height = it.height(this, parent)
        }

        val font = client.font
        if (font != null) {
            if (width == 0) width = (font.width(_text) * scale).toInt()
            if (height == 0) height = (8 * scale).toInt()
        }

        position?.let {
            x = it.x(this, parent)
            y = it.y(this, parent)
        }
    }

    override fun render(graphics: GuiGraphics) {
        if (!visible) return

        val text = _text
        val texts = _texts
        val pose = graphics.pose()

        if (scale == 1f) {
            if (texts != null) graphics.extractText(texts, x, y, shadow, color)
            else graphics.extractText(text, if (center) x + (width / 2) else x, y, shadow, color, center)

            super.render(graphics)
            return
        }

        pose.pushMatrix()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)

        if (texts != null) graphics.extractText(texts, 0, 0, shadow, color)
        else graphics.extractText(text, if (center) (width / 2f / scale).toInt() else 0, 0, shadow, color, center)

        pose.popMatrix()
        super.render(graphics)
    }

    companion object {
        val NONE = TextPrimitive()

        inline fun text(block: TextPrimitive.() -> Unit): TextPrimitive {
            return TextPrimitive().apply(block)
        }
    }
}