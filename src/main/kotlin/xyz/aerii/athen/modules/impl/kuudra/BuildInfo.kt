@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.kuudra

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraSupply
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Smoothie.alert
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.render.Render3D
import java.awt.Color

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object BuildInfo : Module(
    "Build info",
    "Shows information about the ballista build process in phase 2.",
    Category.KUUDRA
) {
    private val waypoints = config.switch("Unfinished build waypoint", true).custom("waypoints")
    private val color by config.colorPicker("Color", Color(Catppuccin.Mocha.Red.argb, true)).dependsOn { waypoints.value }
    private val stun by config.switch("Stun notification", true)
    private val `stun$percent` by config.slider("Notify at", 90, 1, 100, "%").dependsOn { stun }
    private val `stun$message` by config.textInput("Notification message", "<red>Stun!").dependsOn { stun }

    private var sent: Boolean = false

    private val render: Boolean
        get() = KuudraAPI.inRun && KuudraAPI.phase == KuudraPhase.Build

    init {
        KuudraAPI.buildProgress.onChange {
            if (!stun) return@onChange
            if (sent) return@onChange
            if (it <= `stun$percent`) return@onChange

            val prs = `stun$message`.parse(true)
            prs.alert()
            prs.modMessage()
            sent = true
        }

        config.hud("Build info") {
            if (it) return@hud sizedText("§7Builders: §c3\n§7Progress: §c47%")
            if (!render) return@hud null

            sizedText("§7Builders: §c${KuudraAPI.buildPlayers}\n§7Progress: §c${KuudraAPI.buildProgress.value}%")
        }

        on<KuudraEvent.Start> {
            sent = false
        }

        on<WorldRenderEvent.Extract> {
            if (!render) return@on

            for (e in KuudraSupply.every) if (!e.built) Render3D.drawFilledBox(e.buildAABB, color, depthTest = false)
        }.runWhen(waypoints.state)
    }
}