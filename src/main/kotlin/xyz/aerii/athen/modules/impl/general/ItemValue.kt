@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.general

import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.skyblock.PriceAPI.price
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.abbreviate
import xyz.aerii.athen.utils.formatted

@Load
@OnlyIn(skyblock = true)
object ItemValue : Module(
    "Item value",
    "Shows the craft cost, and lowest bin of items.",
    Category.GENERAL
) {
    private val craft by config.switch("Craft cost")
    private val `craft$style` by config.textInput("Craft style", "Craft Cost: <aqua>#price").dependsOn { craft }

    private val lbin by config.switch("Lowest BIN")
    private val `lbin$style` by config.textInput("Lowest BIN style", "Lowest BIN: <aqua>#price").dependsOn { lbin }

    private val bazaar by config.switch("Bazaar")
    private val `bazaar$oneLine` by config.switch("One line").dependsOn { bazaar }
    private val `bazaar$style` by config.textInput("Bazaar style", "Bazaar: <aqua>#buy <gray>| <aqua>#sell").dependsOn { `bazaar$oneLine` && bazaar }
    private val `bazaar$style$buy` by config.textInput("Bazaar buy style", "Bazaar Buy: <aqua>#price").dependsOn { !`bazaar$oneLine` && bazaar }
    private val `bazaar$style$sell` by config.textInput("Bazaar sell style", "Bazaar Sell: <aqua>#price").dependsOn { !`bazaar$oneLine` && bazaar }

    private val number by config.dropdown("Number style", listOf("Abbreviate", "Comma"))

    init {
        on<GuiEvent.Tooltip.Update> {
            if (craft) run {
                if (item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId == null) return@run
                val long = item.getItemValue().price.takeIf { it != 0L } ?: return@run
                tooltip.add(`craft$style`.prs(long))
            }

            val price = item.price() ?: return@on
            if (lbin && price.bazaar == null) run {
                val long = price.lbin?.takeIf { it != 0L } ?: return@run
                tooltip.add(`lbin$style`.prs(long))
            }

            if (bazaar) run {
                val bz = price.bazaar ?: return@run

                val a = bz.buy * item.count
                val b = bz.sell * item.count

                if (`bazaar$oneLine`) {
                    val buy = if (number == 0) a.abbreviate() else a.formatted()
                    val sell = if (number == 0) b.abbreviate() else b.formatted()

                    tooltip.add(`bazaar$style`.replace("#buy", buy).replace("#sell", sell).parse(true))
                    return@run
                }

                tooltip.add(`bazaar$style$buy`.prs(a))
                tooltip.add(`bazaar$style$sell`.prs(b))
            }
        }
    }

    private fun String.prs(a: Number) = this
        .replace("#price", if (number == 0) a.abbreviate() else a.formatted())
        .parse(true)
}