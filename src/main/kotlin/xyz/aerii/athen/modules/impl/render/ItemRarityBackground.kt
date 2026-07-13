@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.render

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.rendering.ui.effects.outline.outline
import xyz.aerii.athen.api.rendering.ui.shapes.rectangle.rectangle
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Resourceful
import xyz.aerii.athen.modules.Module
import xyz.aerii.library.handlers.Observable.Companion.and
import xyz.aerii.library.utils.withAlpha
import java.awt.Color

@Load
@OnlyIn(skyblock = true)
object ItemRarityBackground : Module(
    "Item rarity background",
    "Displays a background for the item that's rendering!",
    Category.RENDER
) {
    private val renderStyle by config.dropdown("Render style", listOf("Filled", "Outline",  "Filled outline", "Circle"), 2)
    private val mode = config.dropdown("Render mode", listOf("Everywhere", "Slots"), 1).custom("mode")
    private val hotbar = config.switch("Hotbar", true).dependsOn { mode.value == 1 }.custom("hotbar")
    private val fill by config.slider("Fill alpha", 0.5f, 0f, 1f, showDouble = true).dependsOn { renderStyle != 1 }

    private val colorExpandable by config.expandable("Colors")
    private val `color$common` by config.colorPicker("Common color", Color(SkyBlockRarity.COMMON.color)).childOf { colorExpandable }
    private val `color$uncommon` by config.colorPicker("Uncommon color", Color(SkyBlockRarity.UNCOMMON.color)).childOf { colorExpandable }
    private val `color$rare` by config.colorPicker("Rare color", Color(SkyBlockRarity.RARE.color)).childOf { colorExpandable }
    private val `color$epic` by config.colorPicker("Epic color", Color(SkyBlockRarity.EPIC.color)).childOf { colorExpandable }
    private val `color$leg` by config.colorPicker("Legendary color", Color(SkyBlockRarity.LEGENDARY.color)).childOf { colorExpandable }
    private val `color$mythic` by config.colorPicker("Mythic color", Color(SkyBlockRarity.MYTHIC.color)).childOf { colorExpandable }
    private val `color$divine` by config.colorPicker("Divine color", Color(SkyBlockRarity.DIVINE.color)).childOf { colorExpandable }
    private val `color$special` by config.colorPicker("Special color", Color(SkyBlockRarity.SPECIAL.color)).childOf { colorExpandable }

    private val common = Resourceful.identify("rarity/common")
    private val uncommon = Resourceful.identify("rarity/uncommon")
    private val rare = Resourceful.identify("rarity/rare")
    private val epic = Resourceful.identify("rarity/epic")
    private val leg = Resourceful.identify("rarity/legendary")
    private val mythic = Resourceful.identify("rarity/mythic")
    private val divine = Resourceful.identify("rarity/divine")
    private val special = Resourceful.identify("rarity/special")

    init {
        on<GuiEvent.Items.Render.Pre> {
            graphics.fn(item, x, y)
        }.runWhen(mode.state.map { it == 0 })

        on<GuiEvent.Slots.Render.Pre> {
            graphics.fn(slot.item, slot.x, slot.y)
        }.runWhen(mode.state.map { it == 1 })

        on<GuiEvent.Slots.Render.Hotbar.Pre> {
            graphics.fn(item, x, y)
        }.runWhen(mode.state.map { it == 1 } and hotbar.state)
    }

    private fun GuiGraphics.fn(item: ItemStack, x: Int, y: Int) {
        if (item.isEmpty) return
        val a = item.getData(DataTypes.RARITY) ?: return
        val color = a.get()

        when (renderStyle) {
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
                blitSprite(RenderPipelines.GUI_TEXTURED, a.sprite(), x, y, 16, 16, ARGB.color((fill * 255).toInt(), 255, 255, 255))
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
    }.rgb

    private fun SkyBlockRarity.sprite(): Identifier = when (this) {
        SkyBlockRarity.COMMON -> common
        SkyBlockRarity.UNCOMMON -> uncommon
        SkyBlockRarity.RARE -> rare
        SkyBlockRarity.EPIC -> epic
        SkyBlockRarity.LEGENDARY -> leg
        SkyBlockRarity.MYTHIC -> mythic
        SkyBlockRarity.DIVINE -> divine
        else -> special
    }
}