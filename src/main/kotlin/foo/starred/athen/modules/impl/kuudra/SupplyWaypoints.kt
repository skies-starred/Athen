@file:Suppress("Unused")

package foo.starred.athen.modules.impl.kuudra

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.kuudra.KuudraAPI
import foo.starred.athen.api.kuudra.enums.KuudraPhase
import foo.starred.athen.api.kuudra.enums.KuudraSupply
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractBeam
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFilledBox
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.config.Category
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.markerAABB
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.toDurationFromMillis
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object SupplyWaypoints : Module(
    "Supply waypoints",
    "Waypoints for supplies, pickup and drop-off spots.",
    Category.KUUDRA
) {
    private val dropOff by config.switch("Drop off", true)
    private val dropOffColor by config.colorPicker("Drop off color", Catppuccin.Mocha.Green.argb)

    private val pickup by config.switch("Pick up", true)
    private val pickupColor by config.colorPicker("Pick up color", Catppuccin.Mocha.Teal.argb)

    private val fuel by config.switch("Fuel", true)
    private val fuelColor by config.colorPicker("Fuel color", Catppuccin.Mocha.Blue.argb)

    private val changeColor by config.switch("Detect player proximity", true)
    private val playerColor by config.colorPicker("Nearby color", Catppuccin.Mocha.Peach.argb)

    private val customMessages = config.switch("Custom supply messages", true)
    private val textStyle by config.input("Supply text style", "<gray>➤ <red>#user <r>recovered a supply in <red>#time <gray>(#cur/#max)")
    private val _unused by config.variables("#user", "#time", "#cur", "#max")

    private val supplyRegex = Regex("(?:\\[[^]]*] )?(?<user>\\w+) recovered one of Elle's supplies! \\((?<cur>\\d+)/(?<max>\\d+)\\)")

    init {
        on<MessageEvent.Chat.Intercept> {
            if (KuudraAPI.phase != KuudraPhase.Supply) return@on

            supplyRegex.findOrNull(stripped, "user", "cur", "max") { (user, cur, max) ->
                cancel()

                textStyle
                    .replace("#user", user)
                    .replace("#time", KuudraPhase.Supply.durTime.toDurationFromMillis(secondsDecimals = 1))
                    .replace("#cur", cur)
                    .replace("#max", max)
                    .parse(true)
                    .lie()
            }
        }.runWhen(customMessages.state)

        on<WorldRenderEvent.Extract> {
            if (!KuudraAPI.inRun) return@on
            if (!dropOff && !pickup && !fuel) return@on
            val phase = KuudraAPI.phase ?: return@on
            if (phase != KuudraPhase.Supply && phase != KuudraPhase.Fuel) return@on

            when (phase) {
                KuudraPhase.Supply if (dropOff || pickup) -> {
                    if (dropOff) {
                        for (b in KuudraSupply.every) if (!b.active) extractFilledBox(b.buildAABB, dropOffColor, false)
                    }

                    if (pickup) {
                        for (s in KuudraAPI.supplies) {
                            val color = if (changeColor && s.nearby) playerColor else pickupColor
                            extractFrameBox(s.blockPos.markerAABB(), color, depth = false)
                            extractBeam(s.blockPos, color)
                        }
                    }
                }

                KuudraPhase.Fuel if fuel -> {
                    for (s in KuudraAPI.fuels) {
                        val color = if (changeColor && s.nearby) playerColor else fuelColor
                        extractFrameBox(s.blockPos.markerAABB(), color, depth = false)
                        extractBeam(s.blockPos, color)
                    }
                }

                else -> {}
            }
        }
    }
}
