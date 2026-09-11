@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.items.ItemAPI.`watch$tooltip`
import foo.starred.athen.api.messaging.enums.MessageColors
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.extractText
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.bound
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.pressed
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.colorCoded
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.stripped
import foo.starred.snowbird.utils.toDurationFromMillis
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant

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

    private val showItemStars = config.switch("Item stars as stack size").unique("showItemStars")
    private val starColor by config.colorPicker("Star Color", MessageColors.RED.color)

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

            cakeRegex.findOrNull(slot.item.displayName.stripped(), "year") {
                graphics.extractText("§b${it.component1()}", slot.x, slot.y + 8)
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
            val timestamp = dateFormatter.format(instant.toJavaInstant())
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
            graphics.extractText(str, x + 17 - client.font.width(str), y + 18 - client.font.lineHeight, color = starColor)
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
            .append("<${if (`showItemHex$color`) this@hex else MessageColors.DARK_GRAY.color}>" + String.format("#%06X", this).parse())
            .apply { if (`showItemHex$box`) append("<${this@hex}>⬛".parse()) }
}
