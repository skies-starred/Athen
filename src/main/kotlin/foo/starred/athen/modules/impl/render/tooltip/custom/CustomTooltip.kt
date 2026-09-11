@file:Suppress("ObjectPrivatePropertyName", "ObjectPropertyName", "Unused")

package foo.starred.athen.modules.impl.render.tooltip.custom

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.accessors.hovered
import foo.starred.athen.annotations.Load
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.base.TooltipContext
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.impl.CombinedTooltip
import foo.starred.athen.modules.impl.render.tooltip.custom.renderers.impl.SeparatedTooltip
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.snowbird.api.bound
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.pressed
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData

@Load
object CustomTooltip : Module(
    "Custom tooltip",
    "Custom tooltip rendering!",
    Category.RENDER
) {
    val unused by config.information("This feature does not break any other mod's tooltip changes. It only changes the rendering.")

    val customisation by config.group("Tooltip customisation")
    val `scroll$infinite` by customisation.switch("Infinite scroll")
    val `scroll$horizontal` by customisation.switch("Horizontal scroll", true)
    val `scroll$horizontal$key` by customisation.keybind("Horizontal keybind", InputConstants.KEY_LSHIFT)
    val `scroll$horizontal$speed` by customisation.slider("Horizontal scroll speed", 8, 1, 20, "pixels")
    val `scroll$vertical` by customisation.switch("Vertical scroll", true)
    val `scroll$vertical$speed` by customisation.slider("Vertical scroll speed", 8, 1, 20, "pixels")
    val `scroll$reset` by customisation.switch("Reset on hover")
    val `scroll$scale` by customisation.switch("Scale tooltip")
    val `scroll$scale$key` by customisation.keybind("Scale keybind", InputConstants.KEY_LCONTROL)

    val renderExpandable by config.group("Custom rendering")
    val `tooltip$style` by renderExpandable.selector("Tooltip style", listOf("Combined", "Separated"), 1)
    val `header$centered` by renderExpandable.switch("Centered header", true)

    val border by renderExpandable.switch("Border", true)
    val `border$width` by renderExpandable.slider("Border width", 1, 0, 5)
    val `border$rarity` by renderExpandable.switch("Use rarity color", true)
    val `border$color` by renderExpandable.colorPicker("Border color", Catppuccin.Mocha.Sky.argb)

    val background by renderExpandable.switch("Background", true)
    val `background$color` by renderExpandable.colorPicker("Background color", Catppuccin.Mocha.Surface0.withAlpha(0.9f))

    val onlyName by renderExpandable.keybind("Only name toggle")
    val `onlyName$unused` by renderExpandable.information("Toggling only name mode will hide the actual tooltip and show only the name when it's toggled on.")

    val `text$shadow` by renderExpandable.switch("Text shadows", true)

    var color: Int = `border$color`
    var last: Int = 0
    var xo: Double = 0.0
    var yo: Double = 0.0
    var scale: Double = 1.0
    var name: Boolean = false
    var mss: Double = 0.0
    var msx: Double = 0.0

    init {
        on<GuiEvent.Slots.Input.Hover> {
            color = slot.item.getData(DataTypes.RARITY)?.color?.or(0xFF000000.toInt()) ?: `border$color`
            if (`scroll$reset`) reset()
        }

        on<GuiEvent.Close.Any> {
            color = `border$color`
            name = false
            reset()
        }

        on<GuiEvent.Input.Key.Press> {
            if (!onlyName.bound) return@on
            if (keyEvent.key != onlyName) return@on
            if (last != Scheduler.ticks.client) return@on

            name = !name
            if (name) yo = 0.0
        }

        on<GuiEvent.Input.Mouse.Scroll> {
            if (last != Scheduler.ticks.client) return@on
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
    fun render(graphics: GuiGraphicsExtractor, font: Font, components: List<ClientTooltipComponent>, x: Int, y: Int, positioner: ClientTooltipPositioner) {
        //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
        if (color != `border$color` && (client.screen as? AbstractContainerScreen<*>)?.hovered == null) color = `border$color`

        last = Scheduler.ticks.client
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
