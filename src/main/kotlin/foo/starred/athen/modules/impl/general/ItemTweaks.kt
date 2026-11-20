@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.item.tooltip.impl.ItemTooltipAPI.`watch$tooltip`
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.chat.color.MinecraftColors
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import foo.starred.kontext.api.format.extension.component.colored
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.format.extension.time.parseDuration
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.GenericKontextItemDataResolver
import foo.starred.kontext.api.text.parser.impl.parse
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object ItemTweaks : Module(
    "Item tweaks",
    "Tweaks to the items that are too small to be individual features.",
    ConfigCategory.GENERAL
) {
    private val cakeRegex = Regex("New Year Cake \\(Year (?<year>\\d+)\\)") // https://regex101.com/r/lMIQJm/1
    private val enchants = hashSetOf("Aqua Affinity", "Depth Strider")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault())

    private val showItemStars = config.switch("Item stars as stack size").unique("showItemStars")
    private val starColor by config.colorPicker("Star Color", MinecraftColors.RED.color)

    private val cakeNumbers = config.switch("Cake numbers").unique("cakeNumbers")

    private val tooltip by config.group("Tooltip tweaks")
    private val removeGearScore by tooltip.switch("Remove gear score")
    private val removeEnchants by tooltip.switch("Remove vanilla enchants")

    private val showItemAge by config.switch("Show age")
    private val `showItemAge$style` by config.input("Style", "&7Age: &c#age &8(#time)")

    private val hex by config.group("Hex style")
    private val showItemHex = hex.switch("Show hex color").unique("showItemHex")
    private val `showItemHex$style` by hex.input("Style", "<gray>Color: #hex")
    private val `showItemHex$color` by hex.switch("Color the hex")
    private val `showItemHex$box` by hex.switch("Display color box", true)
    private val `showItemHex$keybind` by hex.keybind("Keybind").`watch$tooltip`()

    init {
        on<GuiEvent.Slots.Render.Any.Post> {
            if (slot.item.item != Items.CAKE) return@on

            val year = cakeRegex.find(slot.item.displayName.stripped())?.groupValues?.get(1) ?: return@on
            MinecraftFontRenderer.extract(graphics, "§b$year", slot.x, slot.y + 8)
        }.runWhen(cakeNumbers.state)

        on<GuiEvent.Tooltip.Update> {
            if (removeGearScore || removeEnchants) {
                val enchantSet = if (removeEnchants) enchants else null

                val it = tooltip.iterator()
                while (it.hasNext()) {
                    val l = it.next()
                    val s = l.stripped()

                    if (
                        (removeGearScore && s.startsWith("Gear Score: ")) ||
                        (enchantSet != null && enchantSet.any(s::startsWith) && "§7" in l.colored())
                    ) {
                        it.remove()
                    }
                }
            }
        }

        on<GuiEvent.Tooltip.Update> {
            if (!showItemAge) return@on
            val time = item.get<GenericKontextItemDataResolver.TIMESTAMP, Long>() ?: return@on
            val instant = Instant.ofEpochMilli(time)
            val timestamp = dateFormatter.format(instant)
            val age = ((Instant.now().toEpochMilli() - time) / 1000).parseDuration(true)

            tooltip.add(1, age.stamp(timestamp).literal())
        }

        on<GuiEvent.Tooltip.Update> {
            if (!GenericInputState.pressed(`showItemHex$keybind`)) return@on

            val rgb = item.get(DataComponents.DYED_COLOR)?.rgb ?: return@on
            tooltip.add(1, rgb.hex())
        }.runWhen(showItemHex.state)

        on<GuiEvent.Items.Render.Post> {
            if (item.isEmpty) return@on
            val stars = item.get<GenericKontextItemDataResolver.STARS, Int>() ?: return@on
            if (stars <= 0) return@on

            val str = stars.toString()
            MinecraftFontRenderer.extract(graphics, str, x + 17 - MinecraftFontMeasurer.width(str), y + 18 - MinecraftFontMeasurer.height, color = starColor)
        }.runWhen(showItemStars.state)
    }

    private fun String.stamp(time: String): String =
        `showItemAge$style`
            .replace("&", "§")
            .replace("#age", this)
            .replace("#time", time)

    private fun Int.hex(): Component =
        `showItemHex$style`
            .replace("&", "§")
            .replace("#hex", "")
            .literal()
            .append("<${if (`showItemHex$color`) this@hex else MinecraftColors.DARK_GRAY.color}>" + String.format("#%06X", this).parse())
            .apply { if (`showItemHex$box`) append("<${this@hex}>⬛".parse()) }
}
