@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.enums.MessageColors
import foo.starred.athen.api.rendering.ui.effects.outline.outline
import foo.starred.athen.api.rendering.ui.shapes.rectangle.rectangle
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.cascade.graphics.extensions.arc.ring
import foo.starred.cascade.graphics.extensions.circle.circle
import foo.starred.snowbird.api.data.Observable.Companion.and
import foo.starred.snowbird.utils.withAlpha
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData

@Load
@OnlyIn(skyblock = true)
object ItemRarityBackground : Module(
    "Item rarity background",
    "Displays a background for the item that's rendering!",
    Category.RENDER
) {
    private val render by config.selector("Render style", listOf("Filled", "Outline",  "Framed", "Circle", "Framed circle"), 2)
    private val mode = config.selector("Render mode", listOf("Everywhere", "Slots"), 1).unique("mode")
    private val hotbar = config.switch("Hotbar", true).unique("hotbar")
    private val fill by config.slider("Fill alpha", 0.5f, 0f, 1f, double = true)

    private val colors by config.group("Colors")
    private val `color$common` by colors.colorPicker("Common color", MessageColors.WHITE.color)
    private val `color$uncommon` by colors.colorPicker("Uncommon color", MessageColors.GREEN.color)
    private val `color$rare` by colors.colorPicker("Rare color", MessageColors.BLUE.color)
    private val `color$epic` by colors.colorPicker("Epic color", MessageColors.DARK_PURPLE.color)
    private val `color$leg` by colors.colorPicker("Legendary color", MessageColors.GOLD.color)
    private val `color$mythic` by colors.colorPicker("Mythic color", MessageColors.PINK.color)
    private val `color$divine` by colors.colorPicker("Divine color", MessageColors.AQUA.color)
    private val `color$special` by colors.colorPicker("Special color", MessageColors.RED.color)

    init {
        on<GuiEvent.Items.Render.Pre> {
            graphics.fn(item, x, y)
        }.runWhen(mode.state.map { it == 0 })

        on<GuiEvent.Slots.Render.Any.Pre> {
            graphics.fn(slot.item, slot.x, slot.y)
        }.runWhen(mode.state.map { it == 1 })

        on<GuiEvent.Slots.Render.Hotbar.Pre> {
            graphics.fn(item, x, y)
        }.runWhen(mode.state.map { it == 1 } and hotbar.state)
    }

    private fun GuiGraphicsExtractor.fn(item: ItemStack, x: Int, y: Int) {
        if (item.isEmpty) return
        val a = item.getData(DataTypes.RARITY) ?: return
        val color = a.get()

        when (render) {
            0 -> {
                rectangle(x, y, 16, 16, color.withAlpha(fill))
            }

            1 -> {
                outline(x, y, 16, 16, 1, color, true)
            }

            2 -> {
                rectangle(x, y, 16, 16, color.withAlpha(fill))
                outline(x, y, 16, 16, 1, color, true)
            }

            3 -> {
                circle(x + 8f, y + 8f, 8f, color.withAlpha(fill))
            }

            4 -> {
                val x = x + 8f
                val y = y + 8f
                val pose = Matrix3x2f(pose())
                val scissor = scissorStack.peek()

                circle(x, y, 8f, color.withAlpha(fill), pose, scissor)
                ring(x, y, 7f, 8f, color, pose, scissor)
            }
        }
    }

    private fun SkyBlockRarity.get(): Int = when (this) {
        SkyBlockRarity.COMMON -> `color$common`
        SkyBlockRarity.UNCOMMON -> `color$uncommon`
        SkyBlockRarity.RARE -> `color$rare`
        SkyBlockRarity.EPIC -> `color$epic`
        SkyBlockRarity.LEGENDARY -> `color$leg`
        SkyBlockRarity.MYTHIC -> `color$mythic`
        SkyBlockRarity.DIVINE -> `color$divine`
        else -> `color$special`
    }
}
