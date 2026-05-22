package xyz.aerii.athen.modules.impl.render.highlight.ui.renderers

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.registries.BuiltInRegistries
import xyz.aerii.athen.api.rendering.ui.effects.outline.outline
import xyz.aerii.athen.api.rendering.ui.shapes.rectangle.rectangle
import xyz.aerii.athen.api.rendering.ui.text.vanilla.extensions.extractText
import xyz.aerii.athen.modules.impl.render.highlight.ui.data.HighlightEntry
import xyz.aerii.athen.modules.impl.render.highlight.ui.data.UIZoneType
import xyz.aerii.athen.ui.IZoneType
import xyz.aerii.athen.ui.InputField
import xyz.aerii.athen.ui.UIZone
import xyz.aerii.athen.ui.base.AbstractModalRenderer
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.library.api.client
import xyz.aerii.library.utils.hovered

class ModalRenderer(
    mw: Int,
    mh: Int,
    fh: Int,
    padding: Int
) : AbstractModalRenderer<HighlightEntry>(mw, mh, fh, padding) {
    override val create = "Create Highlight"
    override val edit = "Edit Highlight"
    override val zone0: IZoneType = UIZoneType.MODAL_SAVE
    override val zone1: IZoneType = UIZoneType.MODAL_CANCEL

    val nameField = InputField("Name or entity type")
    val colorField = InputField("Hex color (e.g. ff0000)")
    val maxHpField = InputField("-1 for any")
    var typed = false

    private var list = listOf<String>()
    private var y1 = 0
    private var x1 = 0
    private var w1 = 0
    var scroll0 = 0

    public override val dropdown: Boolean
        get() = typed && nameField.focused && list.isNotEmpty()

    override fun fields(graphics: GuiGraphics, mx: Int, my: Int, x0: Int, y0: Int, cy: Int, fw: Int, zones: MutableList<UIZone>) {
        var cy = cy

        graphics.extractText(if (typed) "Entity Type" else "Name", x0 + padding, cy, false, Mocha.Subtext0.argb)
        cy += client.font.lineHeight + 2
        nameField.draw(graphics, x0 + padding, cy, fw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, if (typed) UIZoneType.MODAL_TYPE else UIZoneType.MODAL_NAME)) }

        if (typed) {
            x1 = x0 + padding
            y1 = cy + 16
            w1 = fw
            fn()
        }

        cy += fh + 8

        val hw = fw / 2 - 4
        graphics.extractText("Color", x0 + padding, cy, false, Mocha.Subtext0.argb)
        graphics.extractText("Max HP", x0 + padding + hw + 8, cy, false, Mocha.Subtext0.argb)
        cy += client.font.lineHeight + 2

        val preview = colorField.value.removePrefix("#")
        val parsed = preview.toIntOrNull(16)
        val colorX = x0 + padding

        if (parsed != null) {
            graphics.rectangle(colorX, cy + (fh - 14) / 2, 14, 14, parsed or 0xFF000000.toInt())
            graphics.outline(colorX, cy + (fh - 14) / 2, 14, 14, 1, Mocha.Overlay0.argb)
        } else {
            graphics.rectangle(colorX, cy + (fh - 14) / 2, 14, 14, Mocha.Surface0.argb)
            graphics.outline(colorX, cy + (fh - 14) / 2, 14, 14, 1, Mocha.Overlay0.argb)
        }

        colorField.draw(graphics, colorX + 18, cy, hw - 18) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, UIZoneType.MODAL_COLOR)) }
        maxHpField.draw(graphics, x0 + padding + hw + 8, cy, hw) { zx, zy, zw, zh -> zones.add(UIZone(zx, zy, zw, zh, UIZoneType.MODAL_MAX_HP)) }
    }

    override fun overlays(graphics: GuiGraphics, mx: Int, my: Int, x0: Int, y0: Int, fw: Int, zones: MutableList<UIZone>) {
        if (!dropdown) return
        val h = (list.size * 14).coerceAtMost(84)

        graphics.rectangle(x1, y1, w1, h, Mocha.Base.argb)
        graphics.outline(x1, y1, w1, h, 1, Mocha.Mauve.argb)
        graphics.enableScissor(x1, y1, x1 + w1, y1 + h)

        var cy = y1 + scroll0
        for ((i, s) in list.withIndex()) {
            if (cy + 14 > y1 && cy < y1 + h) {
                if (hovered(x1, cy, w1, cy + 14, true)) graphics.rectangle(x1, cy, w1, 14, Mocha.Surface1.argb)
                graphics.extractText(s, x1 + 4, cy + (14 - client.font.lineHeight) / 2 + 1, false, Mocha.Text.argb)
                zones.add(UIZone(x1, cy, w1, 14, UIZoneType.MODAL_SUGGESTION, i))
            }

            cy += 14
        }

        graphics.disableScissor()
    }

    private fun fn() {
        val a = nameField.value.trim().lowercase()
        list = if (a.isEmpty()) emptyList() else BuiltInRegistries.ENTITY_TYPE.keySet().map { it.toString() }.filter { a in it }.sorted().take(30)

        val h0 = list.size * 14
        val h1 = h0.coerceAtMost(84)
        scroll0 = scroll0.coerceIn(-maxOf(0, h0 - h1), 0)
    }

    fun click(mouseX: Int, mouseY: Int) {
        val h = (list.size * 14).coerceAtMost(84)

        if (mouseX !in x1 until x1 + w1) return
        if (mouseY !in y1 until y1 + h) return

        var cy = y1 + scroll0
        for (s in list) {
            if (mouseY !in cy until cy + 14) {
                cy += 14
                continue
            }

            nameField.value = s
            nameField.cursor = s.length
            nameField.selectionStart = -1
            scroll0 = 0
            return
        }
    }

    fun scroll(amount: Int) {
        val h0 = list.size * 14
        val h1 = h0.coerceAtMost(84)
        scroll0 = (scroll0 + amount).coerceIn(-maxOf(0, h0 - h1), 0)
    }

    fun open(isTyped: Boolean) {
        entry = null
        typed = isTyped
        reset()
        open = true
    }

    fun open(e: HighlightEntry) {
        entry = e
        typed = e.typed
        open = true

        nameField.reset(true)
        nameField.value = e.name
        nameField.cursor = e.name.length
        nameField.focused = true

        colorField.reset(true)
        colorField.value = e.color.toHexString()
        colorField.cursor = colorField.value.length

        maxHpField.reset(true)
        maxHpField.value = if (e.max == -1) "" else e.max.toString()
        maxHpField.cursor = maxHpField.value.length

        scroll0 = 0
    }

    override fun onClose() {
        nameField.focused = false
        colorField.focused = false
        maxHpField.focused = false
        scroll0 = 0
    }

    private fun reset() {
        nameField.reset(true)
        nameField.focused = true
        colorField.reset(true)
        colorField.value = "ff0000"
        colorField.cursor = 6
        maxHpField.reset(true)
        scroll0 = 0
    }

    private fun Int.toHexString(): String = Integer.toHexString(this).padStart(6, '0')
}