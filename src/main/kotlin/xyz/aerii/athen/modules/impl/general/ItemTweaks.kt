@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.extentions.format
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Itemizer.`watch$tooltip`
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.text
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.client
import xyz.aerii.library.api.pressed
import xyz.aerii.library.utils.colorCoded
import xyz.aerii.library.utils.literal
import xyz.aerii.library.utils.stripped
import xyz.aerii.library.utils.toDurationFromMillis
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Load
@OnlyIn(skyblock = true)
object ItemTweaks : Module(
    "Item tweaks",
    "Tweaks to the items that are too small to be individual features.",
    Category.GENERAL
) {
    private val cakeRegex = Regex("New Year Cake \\(Year (?<year>\\d+)\\)") // https://regex101.com/r/lMIQJm/1
    private val enchants = hashSetOf("Aqua Affinity", "Depth Strider")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault())

    private val showItemStars = config.switch("Item stars as stack size").custom("showItemStars")
    private val starColor by config.colorPicker("Star Color", Color.RED).dependsOn { showItemStars.value }

    private val cakeNumbers = config.switch("Cake numbers").custom("cakeNumbers")

    private val tooltipExpandable by config.expandable("Tooltip tweaks")
    private val removeGearScore by config.switch("Remove gear score").childOf { tooltipExpandable }
    private val removeEnchants by config.switch("Remove vanilla enchants").childOf { tooltipExpandable }

    private val showItemAge by config.switch("Show age").childOf { tooltipExpandable }
    private val `showItemAge$style` by config.textInput("Style", "&7Age: &c#age &8(#time)").dependsOn { showItemAge }.childOf { tooltipExpandable }

    private val showItemHex = config.switch("Show hex color").childOf { tooltipExpandable }.custom("showItemHex")
    private val `showItemHex$style` by config.textInput("Style", "&7Color: #hex").dependsOn { showItemHex.value }.childOf { tooltipExpandable }
    private val `showItemHex$color` by config.switch("Color the hex").dependsOn { showItemHex.value }.childOf { tooltipExpandable }
    private val `showItemHex$box` by config.switch("Display color box", true).dependsOn { showItemHex.value }.childOf { tooltipExpandable }
    private val `showItemHex$keybind` by config.keybind("Keybind").`watch$tooltip`().dependsOn { showItemHex.value }.childOf { tooltipExpandable }

    init {
        on<GuiEvent.Slots.Render.Update> {
            if (slot.item.item != Items.CAKE) return@on

            cakeRegex.findOrNull(slot.item.displayName.stripped(), "year") {
                renders.add { graphics, slot ->
                    graphics.text("§b${it.component1()}", slot.x, slot.y + 8)
                }
            }
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
                        (enchantSet != null && enchantSet.any(s::startsWith) && "§7" in l.colorCoded())
                    ) {
                        it.remove()
                    }
                }
            }
        }

        on<GuiEvent.Tooltip.Update> {
            if (!showItemAge) return@on
            val instant = item.getData(DataTypes.TIMESTAMP) ?: return@on
            val timestamp = dateFormatter.format(instant)
            val age = (Instant.now().toEpochMilli() - instant.toEpochMilliseconds()).toDurationFromMillis(true)

            tooltip.add(1, age.stamp(timestamp).literal())
        }

        on<GuiEvent.Tooltip.Update> {
            if (`showItemHex$keybind`.bound && !`showItemHex$keybind`.pressed) return@on

            val rgb = item.get(DataComponents.DYED_COLOR)?.rgb ?: return@on
            tooltip.add(1, rgb.hex())
        }.runWhen(showItemHex.state)

        on<GuiEvent.Items.Render.Post> {
            if (item.isEmpty) return@on
            val stars = item.getData(DataTypes.STAR_COUNT) ?: return@on
            if (stars <= 0) return@on

            val str = stars.toString()
            graphics.text(str, x + 17 - client.font.width(str), y + 18 - client.font.lineHeight, color = starColor.rgb)
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
            .append(String.format("#%06X", this).literal { color = if (`showItemHex$color`) this@hex else TextColor.DARK_GRAY })
            .apply { if (`showItemHex$box`) append("⬛".literal { color = this@hex }) }
}