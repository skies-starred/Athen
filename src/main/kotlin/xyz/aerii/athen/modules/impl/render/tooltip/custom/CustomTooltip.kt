@file:Suppress("ObjectPrivatePropertyName", "ObjectPropertyName", "Unused")

package xyz.aerii.athen.modules.impl.render.tooltip.custom

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.accessors.hovered
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.base.TooltipContext
import xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.impl.CombinedTooltip
import xyz.aerii.athen.modules.impl.render.tooltip.custom.renderers.impl.SeparatedTooltip
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.client
import xyz.aerii.library.api.pressed
import java.awt.Color

@Load
object CustomTooltip : Module(
    "Custom tooltip",
    "Custom tooltip rendering!",
    Category.RENDER
) {
    val unused by config.textParagraph("This feature does not break any other mod's tooltip changes. It only changes the rendering.")
    val customisation by config.expandable("Tooltip customisation")
    val `scroll$infinite` by config.switch("Infinite scroll").childOf { customisation }
    val `scroll$horizontal` by config.switch("Horizontal scroll", true).childOf { customisation }
    val `scroll$horizontal$key` by config.keybind("Horizontal keybind", GLFW.GLFW_KEY_LEFT_SHIFT).dependsOn { `scroll$horizontal` }.childOf { customisation }
    val `scroll$horizontal$speed` by config.slider("Horizontal scroll speed", 8, 1, 20, "pixels").dependsOn { `scroll$horizontal` }.childOf { customisation }
    val `scroll$vertical` by config.switch("Vertical scroll", true).childOf { customisation }
    val `scroll$vertical$speed` by config.slider("Vertical scroll speed", 8, 1, 20, "pixels").dependsOn { `scroll$vertical` }.childOf { customisation }
    val `scroll$reset` by config.switch("Reset on hover").childOf { customisation }
    val `scroll$scale` by config.switch("Scale tooltip").childOf { customisation }
    val `scroll$scale$key` by config.keybind("Scale keybind", GLFW.GLFW_KEY_LEFT_CONTROL).dependsOn { `scroll$scale` }.childOf { customisation }

    val renderExpandable by config.expandable("Custom rendering")
    val `tooltip$style` by config.dropdown("Tooltip style", listOf("Combined", "Separated"), 1).childOf { renderExpandable }
    val `header$centered` by config.switch("Centered header", true).dependsOn { `tooltip$style` == 1 }.childOf { renderExpandable }

    val border by config.switch("Border", true).childOf { renderExpandable }
    val `border$width` by config.slider("Border width", 1, 0, 5).dependsOn { border }.childOf { renderExpandable }
    val `border$rarity` by config.switch("Use rarity color", true).dependsOn { border }.childOf { renderExpandable }
    val `border$color` by config.colorPicker("Border color", Color(Catppuccin.Mocha.Sky.argb, true)).dependsOn { border }.childOf { renderExpandable }

    val background by config.switch("Background", true).childOf { renderExpandable }
    val `background$color` by config.colorPicker("Background color", Color(Catppuccin.Mocha.Surface0.withAlpha(0.9f), true)).dependsOn { background }.childOf { renderExpandable }

    val onlyName by config.keybind("Only name toggle").childOf { renderExpandable }
    val `onlyName$unused` by config.textParagraph("Toggling only name mode will hide the actual tooltip and show only the name when it's toggled on.").childOf { renderExpandable }

    val `text$shadow` by config.switch("Text shadows", true).childOf { renderExpandable }

    var color: Int = `border$color`.rgb
    var last: Int = 0
    var xo: Double = 0.0
    var yo: Double = 0.0
    var scale: Double = 1.0
    var name: Boolean = false
    var mss: Double = 0.0
    var msx: Double = 0.0

    init {
        on<GuiEvent.Slots.Hover> {
            color = slot.item?.getData(DataTypes.RARITY)?.color?.or(0xFF000000.toInt()) ?: `border$color`.rgb
            if (`scroll$reset`) reset()
        }

        on<GuiEvent.Close.Any> {
            color = `border$color`.rgb
            name = false
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (!onlyName.bound) return@on
            if (keyEvent.key != onlyName) return@on
            if (last != Chronos.ticks.client) return@on

            name = !name
            if (name) yo = 0.0
        }

        on<GuiEvent.Input.Mouse.Scroll> {
            if (last != Chronos.ticks.client) return@on
            if (name) return@on

            if (`scroll$scale` && `scroll$scale$key`.bound && `scroll$scale$key`.pressed) {
                scale += amount * 0.1
                scale = scale.coerceIn(0.5, 3.0)
                return@on
            }

            if (`scroll$horizontal` && `scroll$horizontal$key`.bound && `scroll$horizontal$key`.pressed) {
                xo += amount * `scroll$horizontal$speed`
                return@on
            }

            if (`scroll$vertical`) {
                val n = (yo + amount * `scroll$vertical$speed`).coerceIn(mss, msx)
                yo = if (yo != 0.0 && (n > 0) != (yo > 0)) 0.0 else n
            }
        }
    }

    @JvmStatic
    fun render(graphics: GuiGraphics, font: Font, components: List<ClientTooltipComponent>, x: Int, y: Int, positioner: ClientTooltipPositioner) {
        if (color != `border$color`.rgb && (client.screen as? AbstractContainerScreen<*>)?.hovered == null) color = `border$color`.rgb

        last = Chronos.ticks.client
        val components = if (name) components.take(1) else components
        val cs = components.size == 1

        var width = 0
        var height = if (cs) -2 else 0

        for (c in components) {
            width = maxOf(width, c.getWidth(font))
            height += c.getHeight(font)
        }

        val pos = positioner.positionTooltip(graphics.guiWidth(), graphics.guiHeight(), x, y, width, height)
        val context = TooltipContext(graphics, font, components, pos.x(), pos.y(), width, height, graphics.guiHeight())

        val tx = pos.x()
        val ty = pos.y()
        val pose = graphics.pose()

        pose.pushMatrix()
        val s = if (`scroll$scale`) scale.toFloat() else 1f
        pose.translate((tx - 4).toFloat(), (ty - 4).toFloat())
        pose.scale(s, s)
        pose.translate(-(tx - 4).toFloat(), -(ty - 4).toFloat())
        pose.translate(xo.toFloat(), 0f)

        when (`tooltip$style`) {
            0 -> CombinedTooltip
            1 -> if (cs) CombinedTooltip else SeparatedTooltip
            else -> null
        }?.r(context)

        pose.popMatrix()
    }

    fun scroll(content: Int, visible: Int): Int {
        mss = if (`scroll$infinite`) -content.toDouble() else -maxOf(0, content - visible).toDouble()
        msx = if (`scroll$infinite`) content.toDouble() else 0.0
        yo = yo.coerceIn(mss, msx)

        return yo.toInt()
    }

    private fun reset() {
        xo = 0.0
        yo = 0.0
        scale = 1.0

        mss = 0.0
        msx = 0.0
    }
}