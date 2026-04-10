@file:Suppress("Unused", "ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.kuudra

import net.minecraft.network.chat.Component
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.alert
import xyz.aerii.library.utils.literal

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object KuudraTitles : Module(
    "Kuudra titles",
    "Custom alerts and titles for kuudra!",
    Category.KUUDRA
) {
    private val supplyExpandable by config.expandable("Supplies")
    private val supply = config.hud("Supply titles") {
        if (it) return@hud sizedText(dis0 ?: _dis)
        if (KuudraAPI.phase !in KuudraAPI.set) return@hud null

        val display = display ?: return@hud null
        sizedText(display)
    }.childOf { supplyExpandable }

    private val supplyStyle = config.textInput("Supply text style", "<dark_gray>[<green>#bars<gray>#total <r>- <aqua>#perc%<dark_gray>]").dependsOn { supply.enabled }.childOf { supplyExpandable }.custom("supplyStyle")
    private val `barCharacter$filled` by config.textInput("Filled bar character", "|").dependsOn { supply.enabled }.childOf { supplyExpandable }
    private val `barCharacter$left` by config.textInput("Left bar character", "|").dependsOn { supply.enabled }.childOf { supplyExpandable }
    private val `barCharacter$total` by config.slider("Number", 20, 5, 30, "bars").dependsOn { supply.enabled }.childOf { supplyExpandable }
    private val _unused by config.textParagraph("Variable: <red>#bars<r>, <red>#total<r>, <red>#perc").dependsOn { supply.enabled }.childOf { supplyExpandable }

    private val dropAlert by config.switch("Drop alert", true).childOf { supplyExpandable }
    private val dropMessage by config.textInput("Drop alert message", "<red>Dropped supply!").dependsOn { dropAlert }.childOf { supplyExpandable }
    private val pickupAlert by config.switch("Pick up alert").childOf { supplyExpandable }
    private val pickMessage by config.textInput("Pick up alert message", "<green>Picked up supply!").dependsOn { pickupAlert }.childOf { supplyExpandable }

    private val _dis: Component = "§8[§a|||||||||§f|||||||||§8] §b67%".literal()
    private var dis0: Component? = null
    private var display: Component? = null

    init {
        supplyStyle.state.onChange { dis0 = 20.str() }.also { dis0 = 20.str() }

        on<KuudraEvent.Supply.Progress> {
            if (!supply.enabled) return@on

            display = progress.str()
            cancel()
        }

        on<KuudraEvent.Supply.Pickup> {
            display = null
            if (pickupAlert) pickMessage.parse().alert()
        }

        on<KuudraEvent.Supply.Drop> {
            display = null
            if (dropAlert) dropMessage.parse().alert()
        }
    }

    private fun Int.str(): Component {
        val f = (coerceIn(0, 100) * `barCharacter$total`) / 100

        return supplyStyle.value
            .replace("#perc", toString())
            .replace("#bars", `barCharacter$filled`.repeat(f))
            .replace("#total", `barCharacter$left`.repeat(`barCharacter$total` - f))
            .parse()
    }
}