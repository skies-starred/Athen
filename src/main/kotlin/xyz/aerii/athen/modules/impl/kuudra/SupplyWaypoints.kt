package xyz.aerii.athen.modules.impl.kuudra

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraSupply
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render3D
import java.awt.Color

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object SupplyWaypoints : Module(
    "Supply waypoints",
    "Waypoints for supplies, pickup and drop-off spots.",
    Category.KUUDRA
) {
    private val dropOff by config.switch("Drop off", true)
    private val dropOffColor by config.colorPicker("Drop off color", Color(Catppuccin.Mocha.Green.argb, true)).dependsOn { dropOff }

    private val render: Boolean
        get() = KuudraAPI.inRun && KuudraAPI.phase == KuudraPhase.SUPPLIES

    init {
        on<WorldRenderEvent.Extract> {
            if (!render) return@on
            if (!dropOff) return@on

            for (b in KuudraSupply.every) if (!b.active) Render3D.drawFilledBox(b.buildAABB, dropOffColor, false)
        }
    }
}