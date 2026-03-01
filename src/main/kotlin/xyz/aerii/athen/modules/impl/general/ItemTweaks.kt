@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.translate
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
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.colorCoded
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.abbreviate
import xyz.aerii.athen.utils.formatted
import xyz.aerii.athen.utils.isBound
import xyz.aerii.athen.utils.isPressed
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDurationFromMillis
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

    private val showItemStars by config.switch("Item stars as stack size")
    private val starColor by config.colorPicker("Star Color", java.awt.Color(0xFFFF0000.toInt(), true)).dependsOn { showItemStars }

    private val cakeNumbers = config.switch("Cake numbers").custom("cakeNumbers")

    private val tooltipExpandable by config.expandable("Tooltip tweaks")
    private val removeGearScore by config.switch("Remove gear score").childOf { tooltipExpandable }
    private val removeEnchants by config.switch("Remove vanilla enchants").childOf { tooltipExpandable }

    private val showItemAge by config.switch("Show age").childOf { tooltipExpandable }
    private val `showItemAge$style` by config.textInput("Style", "&7Age: &c#age &8(#time)").dependsOn { showItemAge }.childOf { tooltipExpandable }

    private val showItemPrice by config.switch("Show price").childOf { tooltipExpandable }
    private val `showItemPrice$style` by config.textInput("Style", "&fCraft cost: &b#price").dependsOn { showItemPrice }.childOf { tooltipExpandable }
    private val `showItemPrice$abb` by config.dropdown("Numbers", listOf("Abbreviate", "Normal with comma")).dependsOn { showItemPrice }.childOf { tooltipExpandable }

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
                    graphics.pushPop {
                        graphics.translate(slot.x, slot.y + 8)
                        graphics.sizedText("§b${it.component1()}")
                    }
                }
            }
        }.runWhen(cakeNumbers.state)

        on<GuiEvent.Slots.Render.Update> {
            if (!showItemStars || slot.item.isEmpty) return@on

            val starCount = slot.item.getData(DataTypes.STAR_COUNT) ?: return@on
            if (starCount <= 0) return@on

            renders.add { graphics, slotData ->
                graphics.pushPop {
                    graphics.translate(slotData.x, slotData.y)
                    val starText = starCount.toString()
                    val textX = 17 - client.font.width(starText)
                    val textY = 18 - client.font.lineHeight
                    graphics.drawString(client.font, starText, textX, textY, starColor.rgb, true)
                }
            }
        }

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
            if (!showItemPrice) return@on
            if (item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId == null) return@on
            val long = item.getItemValue().price

            val str =
                if (`showItemPrice$abb` == 0) long.abbreviate()
                else long.formatted()

            tooltip.add(str.price().literal())
        }

        on<GuiEvent.Tooltip.Update> {
            if (`showItemHex$keybind`.isBound() && !`showItemHex$keybind`.isPressed()) return@on

            val rgb = item.get(DataComponents.DYED_COLOR)?.rgb ?: return@on
            tooltip.add(1, rgb.hex())
        }.runWhen(showItemHex.state)
    }

    private fun String.price(): String =
        `showItemPrice$style`
            .replace("&", "§")
            .replace("#price", this)

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

    @JvmStatic
    fun renderStarCount(guiGraphics: GuiGraphics, font: Font, stack: ItemStack, x: Int, y: Int) {
        if (!showItemStars || stack.isEmpty) return

        val starCount = stack.getData(DataTypes.STAR_COUNT) ?: return
        if (starCount <= 0) return

        val starText = starCount.toString()
        val textX = x + 17 - font.width(starText)
        val textY = y + 18 - font.lineHeight
        guiGraphics.drawString(font, starText, textX, textY, starColor.rgb, true)
    }
}