@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl.dungeon

import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.modules.Module

@Load
@OnlyIn(skyblock = true)
object ItemQuality : Module(
    "Item quality",
    "Shows the quality of dungeon items.",
    Category.DUNGEONS
) {
    private val textStyle by config.textInput("Style", "&7Item Quality: &c#cur&8/&c#max &8(#floor)")
    private val _meow0 by config.textParagraph("Variable: <red>#cur<r>, <red>#max<r>, <red>#floor")

    init {
        on<GuiEvent.Tooltip.Update> {
            val cur = item.getData(DataTypes.DUNGEON_QUALITY) ?: return@on
            val f = item.getData(DataTypes.DUNGEON_TIER) ?: return@on

            tooltip.add(1, str(cur.toString(), f))
        }
    }

    private fun str(cur: String, f: Int): Component = textStyle
        .replace("&", "§")
        .replace("#cur", cur)
        .replace("#max", "50")
        .replace("#floor", "Floor $f")
        .literal()
}