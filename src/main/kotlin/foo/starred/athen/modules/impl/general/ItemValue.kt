@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.skyblock.PriceAPI.price
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.id
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.abbreviate
import foo.starred.snowbird.utils.formatted
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue

@Load
@OnlyIn(skyblock = true)
object ItemValue : Module(
    "Item value",
    "Shows the craft cost, and lowest bin of items.",
    Category.GENERAL
) {
    private val number by config.selector("Number style", listOf("Abbreviate", "Comma"))

    private val auctions by config.group("Auction value", false)
    private val craft by auctions.switch("Craft cost")
    private val `craft$style` by auctions.input("Craft style", "Craft Cost: <aqua>#price")

    private val lbin by auctions.switch("Lowest BIN")
    private val `lbin$style` by auctions.input("Lowest BIN style", "Lowest BIN: <aqua>#price")
    private val variables by auctions.variables("#price")

    private val bazaars by config.group("Bazaar value", false)
    private val bazaar by bazaars.switch("Bazaar")
    private val `bazaar$oneLine` by bazaars.switch("One line")
    private val `bazaar$style` by bazaars.input("Bazaar style", "Bazaar: <aqua>#buy <gray>| <aqua>#sell #individual")
    private val `bazaar$style$buy` by bazaars.input("Bazaar buy style", "Bazaar Buy: <aqua>#price #individual")
    private val `bazaar$style$sell` by bazaars.input("Bazaar sell style", "Bazaar Sell: <aqua>#price #individual")
    private val `bazaar$style$individual` by bazaars.input("Individual count style", "<gray>[#count@#price]")
    private val variables0 by bazaars.variables("#buy", "#sell", "#individual", "#price", "#count")

    init {
        on<GuiEvent.Tooltip.Update> {
            val price = item.price()
            val bool = price?.bazaar == null

            if (craft && bool) run {
                if (item.id() == null) return@run
                val long = item.getItemValue().price.takeIf { it != 0L } ?: return@run
                tooltip.add(`craft$style`.prs(long))
            }

            if (price == null) return@on
            if (lbin && bool) run {
                val long = price.auction?.lbin?.takeIf { it != 0L } ?: return@run
                tooltip.add(`lbin$style`.prs(long))
            }

            if (bazaar) run {
                val bz = price.bazaar ?: return@run

                val a = bz.buy * item.count
                val b = bz.sell * item.count

                val c = if (number == 0) a.abbreviate() else a.formatted()
                val d = if (number == 0) b.abbreviate() else b.formatted()

                val e = fn(bz.buy, item.count)
                val f = fn(bz.sell, item.count)

                if (`bazaar$oneLine`) {
                    tooltip.add(`bazaar$style`.replace("#buy", c).replace("#sell", d).replace("#individual", "$e | $f").parse(true))
                    return@run
                }

                tooltip.add(`bazaar$style$buy`.replace("#price", c).replace("#individual", e).parse(true))
                tooltip.add(`bazaar$style$sell`.replace("#price", d).replace("#individual", f).parse(true))
            }
        }
    }

    private fun String.prs(a: Number): Component = this
        .replace("#price", if (number == 0) a.abbreviate() else a.formatted())
        .parse(true)

    private fun fn(price: Number, count: Int): String = `bazaar$style$individual`
        .replace("#count", count.toString())
        .replace("#price", if (number == 0) price.abbreviate() else price.formatted())
}
