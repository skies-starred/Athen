@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.render

import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.withAlpha
import java.awt.Color

@Load
@OnlyIn(skyblock = true)
object ItemRarityBackground : Module(
    "Item rarity background",
    "Displays a background for the item that's rendering!",
    Category.RENDER
) {
    private val renderStyle by config.dropdown("Render style", listOf("Filled", "Outline",  "Filled outline"), 2)
    private val fillAlpha by config.slider("Fill alpha", 0.5f, 0f, 1f, showDouble = true).dependsOn { renderStyle == 0 || renderStyle == 2 }

    private val colorExpandable by config.expandable("Colors")
    private val `color$common` by config.colorPicker("Common color", Color(SkyBlockRarity.COMMON.color)).childOf { colorExpandable }
    private val `color$uncommon` by config.colorPicker("Uncommon color", Color(SkyBlockRarity.UNCOMMON.color)).childOf { colorExpandable }
    private val `color$rare` by config.colorPicker("Rare color", Color(SkyBlockRarity.RARE.color)).childOf { colorExpandable }
    private val `color$epic` by config.colorPicker("Epic color", Color(SkyBlockRarity.EPIC.color)).childOf { colorExpandable }
    private val `color$leg` by config.colorPicker("Legendary color", Color(SkyBlockRarity.LEGENDARY.color)).childOf { colorExpandable }
    private val `color$mythic` by config.colorPicker("Mythic color", Color(SkyBlockRarity.MYTHIC.color)).childOf { colorExpandable }
    private val `color$divine` by config.colorPicker("Divine color", Color(SkyBlockRarity.DIVINE.color)).childOf { colorExpandable }
    private val `color$special` by config.colorPicker("Special color", Color(SkyBlockRarity.SPECIAL.color)).childOf { colorExpandable }

    init {
        on<GuiEvent.Items.Render.Pre> {
            if (item.isEmpty) return@on
            val color = item.getData(DataTypes.RARITY)?.get() ?: return@on

            when (renderStyle) {
                0 -> {
                    graphics.drawRectangle(x, y, 16, 16, color.withAlpha(fillAlpha))
                }

                1 -> {
                    graphics.drawOutline(x, y, 16, 16, 1, color, true)
                }

                2 -> {
                    graphics.drawRectangle(x, y, 16, 16, color.withAlpha(fillAlpha))
                    graphics.drawOutline(x, y, 16, 16, 1, color, true)
                }
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
}