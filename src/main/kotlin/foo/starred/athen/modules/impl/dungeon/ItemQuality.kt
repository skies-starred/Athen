@file:Suppress("UNUSED")

package foo.starred.athen.modules.impl.dungeon

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import foo.starred.kontext.api.text.parser.impl.parse
import net.minecraft.network.chat.Component

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object ItemQuality : Module(
    "Item quality",
    "Shows the quality of dungeon items.",
    ConfigCategory.DUNGEONS
) {
    private val style by config.input("Style", "<gray>Item Quality: <red>#current<dark_gray>/<red>#max <dark_gray>(#floor)")
    private val _meow0 by config.variables("#current", "#max", "#floor")

    init {
        on<GuiEvent.Tooltip.Update> {
            val quality = item.get<GenericKontextItemDataResolver.QUALITY, Int>() ?: return@on
            val tier = item.get<GenericKontextItemDataResolver.TIER, Int>() ?: return@on

            tooltip.add(1, str(quality.toString(), tier))
        }
    }

    private fun str(quality: String, tier: Int): Component = style
        .replace("#current", quality)
        .replace("#max", "50")
        .replace("#floor", "Floor $tier")
        .parse()
}
