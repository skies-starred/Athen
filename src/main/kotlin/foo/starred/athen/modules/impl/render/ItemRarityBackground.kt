@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.cascade.graphics.extensions.arc.ring
import foo.starred.cascade.graphics.extensions.circle.circle
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.chat.color.MinecraftColors
import foo.starred.kontext.api.data.Observable.Companion.and
import foo.starred.kontext.api.format.extension.color.alpha
import foo.starred.kontext.api.item.data.rarity.SkyBlockItemRarity
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.SkyBlockRarityKontextItemDataResolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object ItemRarityBackground : Module(
    "Item rarity background",
    "Displays a background for the item that's rendering!",
    ConfigCategory.RENDER
) {
    private val render by config.selector("Render style", listOf("Filled", "Outline",  "Framed", "Circle", "Framed circle"), 2)
    private val mode = config.selector("Render mode", listOf("Everywhere", "Slots"), 1).unique("mode")
    private val hotbar = config.switch("Hotbar", true).unique("hotbar")
    private val fill by config.slider("Fill alpha", 0.5f, 0f, 1f, double = true)

    private val colors by config.group("Colors")
    private val `color$common` by colors.colorPicker("Common color", MinecraftColors.WHITE.color)
    private val `color$uncommon` by colors.colorPicker("Uncommon color", MinecraftColors.GREEN.color)
    private val `color$rare` by colors.colorPicker("Rare color", MinecraftColors.BLUE.color)
    private val `color$epic` by colors.colorPicker("Epic color", MinecraftColors.DARK_PURPLE.color)
    private val `color$leg` by colors.colorPicker("Legendary color", MinecraftColors.GOLD.color)
    private val `color$mythic` by colors.colorPicker("Mythic color", MinecraftColors.PINK.color)
    private val `color$divine` by colors.colorPicker("Divine color", MinecraftColors.AQUA.color)
    private val `color$special` by colors.colorPicker("Special color", MinecraftColors.RED.color)

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
        val a = item.get<SkyBlockRarityKontextItemDataResolver, SkyBlockItemRarity>() ?: return
        val color = a.get()

        val x = x.toFloat()
        val y = y.toFloat()

        when (render) {
            0 -> {
                rectangle(x, y, 16f, 16f, color.alpha(fill))
            }

            1 -> {
                hollowRectangle(x, y, 16f, 16f, 1f, color, inset = true)
            }

            2 -> {
                rectangle(x, y, 16f, 16f, color.alpha(fill))
                hollowRectangle(x, y, 16f, 16f, 1f, color, inset = true)
            }

            3 -> {
                circle(x + 8f, y + 8f, 8f, color.alpha(fill))
            }

            4 -> {
                val x = x + 8f
                val y = y + 8f
                val pose = Matrix3x2f(pose())
                val scissor = scissorStack.peek()

                circle(x, y, 8f, color.alpha(fill), pose, scissor)
                ring(x, y, 7f, 8f, color, pose, scissor)
            }
        }
    }

    private fun SkyBlockItemRarity.get(): Int = when (this) {
        SkyBlockItemRarity.COMMON -> `color$common`
        SkyBlockItemRarity.UNCOMMON -> `color$uncommon`
        SkyBlockItemRarity.RARE -> `color$rare`
        SkyBlockItemRarity.EPIC -> `color$epic`
        SkyBlockItemRarity.LEGENDARY -> `color$leg`
        SkyBlockItemRarity.MYTHIC -> `color$mythic`
        SkyBlockItemRarity.DIVINE -> `color$divine`
        else -> `color$special`
    }
}
