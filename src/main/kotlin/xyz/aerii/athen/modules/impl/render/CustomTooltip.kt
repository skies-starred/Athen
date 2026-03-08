@file:Suppress("ObjectPrivatePropertyName", "ObjectPropertyName", "Unused")

package xyz.aerii.athen.modules.impl.render

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.isBound
import xyz.aerii.athen.utils.isPressed
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import java.awt.Color
import kotlin.math.round

@Load
object CustomTooltip : Module(
    "Custom tooltip",
    "Custom tooltip rendering!",
    Category.RENDER
) {
    private val unused by config.textParagraph("This feature does not break any other mod's tooltip changes. It only changes the rendering.")
    private val customisation by config.expandable("Tooltip customisation")
    private val `scroll$infinite` by config.switch("Infinite scroll").childOf { customisation }
    private val `scroll$horizontal` by config.switch("Horizontal scroll", true).childOf { customisation }
    private val `scroll$horizontal$key` by config.keybind("Horizontal keybind", GLFW.GLFW_KEY_LEFT_SHIFT).dependsOn { `scroll$horizontal` }.childOf { customisation }
    private val `scroll$horizontal$speed` by config.slider("Horizontal scroll speed", 8, 1, 20, "pixels").dependsOn { `scroll$horizontal` }.childOf { customisation }
    private val `scroll$vertical` by config.switch("Vertical scroll", true).childOf { customisation }
    private val `scroll$vertical$speed` by config.slider("Vertical scroll speed", 8, 1, 20, "pixels").dependsOn { `scroll$vertical` }.childOf { customisation }
    private val `scroll$reset` by config.switch("Reset on hover").childOf { customisation }
    private val `scroll$scale` by config.switch("Scale tooltip").childOf { customisation }
    private val `scroll$scale$key` by config.keybind("Scale keybind", GLFW.GLFW_KEY_LEFT_CONTROL).dependsOn { `scroll$scale` }.childOf { customisation }

    private val renderExpandable by config.expandable("Custom rendering")
    private val `header$style` by config.dropdown("Header style", listOf("Combined", "Separated"), 1).childOf { renderExpandable }
    private val `header$centered` by config.switch("Centered header", true).dependsOn { `header$style` == 1 }.childOf { renderExpandable }

    private val border by config.switch("Border", true).childOf { renderExpandable }
    private val `border$width` by config.slider("Border width", 1, 0, 5).dependsOn { border }.childOf { renderExpandable }
    private val `border$rarity` by config.switch("Use rarity color").dependsOn { border }.childOf { renderExpandable }
    private val `border$color` by config.colorPicker("Border color", Color(Catppuccin.Mocha.Sky.argb, true)).dependsOn { border }.childOf { renderExpandable }

    private val background by config.switch("Background", true).childOf { renderExpandable }
    private val `background$color` by config.colorPicker("Background color", Color(Catppuccin.Mocha.Surface0.withAlpha(0.9f), true)).dependsOn { background }.childOf { renderExpandable }

    private val onlyName by config.keybind("Only name toggle", GLFW.GLFW_KEY_LEFT_ALT).childOf { renderExpandable }
    private val `onlyName$unused` by config.textParagraph("Toggling only name mode will hide the actual tooltip and show only the name when it's toggled on.").childOf { renderExpandable }

    val `text$shadow` by config.switch("Text shadows", true).childOf { renderExpandable }

    private var color: Int = `border$color`.rgb
    private var hover: Slot? = null
    private var xo: Double = 0.0
    private var yo: Double = 0.0
    private var scale: Double = 1.0
    private var name: Boolean = false

    init {
        on<GuiEvent.Slots.Hover> {
            hover = slot
            color = slot.item?.getData(DataTypes.RARITY)?.color?.or(0xFF000000.toInt()) ?: `border$color`.rgb
            if (`scroll$reset`) reset()
        }

        on<GuiEvent.Container.Close> {
            hover = null
            color = `border$color`.rgb
            name = false
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (!onlyName.isBound()) return@on
            if (keyEvent.key != onlyName) return@on
            if (hover == null) return@on

            name = !name
            if (name) yo = 0.0
        }

        on<GuiEvent.Input.Mouse.Scroll> {
            if (hover == null) return@on
            if (hover?.item?.isEmpty != false) return@on
            if (name) return@on

            if (`scroll$scale$key`.isBound() && `scroll$scale$key`.isPressed()) {
                scale += amount * 0.1
                scale = scale.coerceIn(0.5, 3.0)
                return@on
            }

            if (`scroll$horizontal$key`.isBound() && `scroll$horizontal$key`.isPressed()) xo += amount * `scroll$horizontal$speed`
            else yo += amount * `scroll$vertical$speed`
        }
    }

    @JvmStatic
    fun render(gg: GuiGraphics, font: Font, components: List<ClientTooltipComponent>, x: Int, y: Int, positioner: ClientTooltipPositioner) {
        if (components.isEmpty()) return

        val components = if (name) components.take(1) else components
        var width = 0
        var height = if (components.size == 1) -2 else 0
        for (c in components) {
            width = maxOf(width, c.getWidth(font))
            height += c.getHeight(font)
        }

        val pos = positioner.positionTooltip(gg.guiWidth(), gg.guiHeight(), x, y, width, height)
        val tx = pos.x()
        val ty = pos.y()
        val w = width + 8
        val bw = `border$width`
        val screenH = gg.guiHeight()

        val pose = gg.pose()
        pose.pushMatrix()
        val s = if (`scroll$scale`) scale.toFloat() else 1f
        pose.translate((tx - 4).toFloat(), (ty - 4).toFloat())
        pose.scale(s, s)
        pose.translate(-(tx - 4).toFloat(), -(ty - 4).toFloat())
        pose.translate(xo.toFloat(), 0f)

        if (`header$style` == 1 && components.size > 1) {
            val header = components[0]
            val headerH = header.getHeight(font) + 6
            val body = components.drop(1).let { if ((it.firstOrNull() as? ClientTextTooltip)?.text?.equals(FormattedCharSequence.EMPTY) == true) it.drop(1) else it }
            val bodyHeight = body.sumOf { it.getHeight(font) }
            val totalH = headerH + 4 + bodyHeight + 8
            val hy = if (totalH < screenH - 40) (ty - 4).coerceIn(20, screenH - 20 - totalH) else 20
            val hx = tx - 4
            val drawTy = hy + 4

            drawBox(gg, hx, hy, w, headerH, bw)
            val text = (header as ClientTextTooltip).text
            val textX = if (`header$centered`) (tx + (w - 8) / 2) - font.width(text) / 2 else tx
            gg.drawString(font, text, textX, drawTy, -1, `text$shadow`)
            header.renderImage(font, tx, drawTy, width, height, gg)

            val bx = tx - 4
            val by = hy + headerH + 4
            val maxBh = minOf(bodyHeight + 6, screenH - 20 - by).coerceAtLeast(0)

            val s = `scroll$vertical$speed`.toDouble()
            yo = round((if (`scroll$infinite`) yo.coerceIn(-bodyHeight.toDouble(), bodyHeight.toDouble()) else yo.coerceIn(-maxOf(0, bodyHeight - (maxBh - 6)).toDouble(), 0.0)) / s) * s

            val scrollY = yo.toInt()
            val bh = if (`scroll$infinite`) (if (scrollY > 0) maxBh - scrollY else bodyHeight + scrollY + 6).coerceIn(0, screenH - 20 - by) else maxBh

            drawBox(gg, bx, by, w, bh, bw)
            drawComponents(gg, font, body, tx, bx, by, w, bh, by + 4 + minOf(0, scrollY), width, bodyHeight)
            drawFades(gg, bx, by, w, bh, scrollY, by + 4 + minOf(0, scrollY) + bodyHeight)
        } else {
            val x0 = tx - 4
            val y0 = if (height + 8 < screenH - 40) (ty - 4).coerceIn(20, screenH - 20 - (height + 8)) else 20
            val drawTy = y0 + 4
            val maxH = minOf(height + 8, screenH - 20 - y0).coerceAtLeast(0)

            val s = `scroll$vertical$speed`.toDouble()
            yo = round((if (`scroll$infinite`) yo.coerceIn(-height.toDouble(), height.toDouble()) else yo.coerceIn(-maxOf(0, height - (maxH - 8)).toDouble(), 0.0)) / s) * s

            val scrollY = yo.toInt()
            val h = if (`scroll$infinite`) (if (scrollY > 0) maxH - scrollY else height + scrollY + 8).coerceIn(0, screenH - 20 - y0) else maxH

            drawBox(gg, x0, y0, w, h, bw)
            drawComponents(gg, font, components, tx, x0, y0, w, h, drawTy + minOf(0, scrollY), width, height, firstGap = 2)
            drawFades(gg, x0, y0, w, h, scrollY, drawTy + minOf(0, scrollY) + height)
        }

        pose.popMatrix()
    }

    private fun drawBox(gg: GuiGraphics, x: Int, y: Int, w: Int, h: Int, bw: Int) {
        if (background) gg.drawRectangle(x, y, w, h, `background$color`.rgb)
        if (border && bw > 0) gg.drawOutline(x, y, w, h, bw, if (`border$rarity`) color else `border$color`.rgb)
    }

    private fun drawComponents(gg: GuiGraphics, font: Font, comps: List<ClientTooltipComponent>, tx: Int, boxX: Int, boxY: Int, boxW: Int, boxH: Int, startY: Int, width: Int, totalHeight: Int, firstGap: Int = 0) {
        gg.enableScissor(boxX, boxY, boxX + boxW, boxY + boxH)

        var drawY = startY
        for ((i, comp) in comps.withIndex()) {
            comp.renderText(gg, font, tx, drawY)
            drawY += comp.getHeight(font) + if (i == 0) firstGap else 0
        }

        drawY = startY
        for ((i, comp) in comps.withIndex()) {
            comp.renderImage(font, tx, drawY, width, totalHeight, gg)
            drawY += comp.getHeight(font) + if (i == 0) firstGap else 0
        }

        gg.disableScissor()
    }

    private fun drawFades(gg: GuiGraphics, x: Int, y: Int, w: Int, h: Int, scrollY: Int, contentBottom: Int) {
        val bg = `background$color`.rgb or 0xFF000000.toInt()
        val bgT = bg and 0x00FFFFFF
        gg.enableScissor(x, y, x + w, y + h)
        if (scrollY < 0) gg.fillGradient(x, y, x + w, y + 18, bg, bgT)
        if (scrollY > 0 || (!`scroll$infinite` && contentBottom > y + h)) gg.fillGradient(x, y + h - 18, x + w, y + h, bgT, bg)
        gg.disableScissor()
    }

    private fun reset() {
        xo = 0.0
        yo = 0.0
        scale = 1.0
    }
}