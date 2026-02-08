@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.translate
import tech.thatgravyboat.skyblockapi.utils.extentions.format
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Texter.colorCoded
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.abbreviate
import xyz.aerii.athen.utils.formatted
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

    private val cakeNumbers = config.switch("Cake numbers").custom("cakeNumbers")

    private val tooltipExpandable by config.expandable("Tooltip tweaks")
    private val removeGearScore by config.switch("Remove gear score").childOf { tooltipExpandable }
    private val removeEnchants by config.switch("Remove vanilla enchants").childOf { tooltipExpandable }
    private val showItemAge by config.switch("Show age").childOf { tooltipExpandable }
    private val `showItemAge$style` by config.textInput("Style", "&7Age: &c#age &8(#time)").dependsOn { showItemAge }.childOf { tooltipExpandable }
    private val showItemPrice by config.switch("Show price").childOf { tooltipExpandable }
    private val `showItemPrice$style` by config.textInput("Style", "&fCraft cost: &b#price").dependsOn { showItemPrice }.childOf { tooltipExpandable }
    private val `showItemPrice$abb` by config.dropdown("Numbers", listOf("Abbreviate", "Normal with comma")).dependsOn { showItemPrice }.childOf { tooltipExpandable }

    init {
        on<GuiEvent.Slots.Render.Post> {
            if (slot.item.item != Items.CAKE) return@on

            cakeRegex.findOrNull(slot.item.displayName.stripped(), "year") {
                graphics.pushPop {
                    graphics.translate(slot.x, slot.y + 8)
                    graphics.sizedText("§b${it.component1()}")
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
            if (!showItemPrice) return@on
            if (item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId == null) return@on
            val long = item.getItemValue().price

            val str =
                if (`showItemPrice$abb` == 0) long.abbreviate()
                else long.formatted()

            tooltip.add(str.price().literal())
        }
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
}