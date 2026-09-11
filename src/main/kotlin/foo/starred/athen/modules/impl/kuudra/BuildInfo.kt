@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.kuudra

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.kuudra.KuudraAPI
import foo.starred.athen.api.kuudra.enums.KuudraPhase
import foo.starred.athen.api.kuudra.enums.KuudraSupply
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.sizedText
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.config.Category
import foo.starred.athen.events.KuudraEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.render.fcs
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.alert

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object BuildInfo : Module(
    "Build info",
    "Shows information about the ballista build process in phase 2.",
    Category.KUUDRA
) {
    private val waypoints = config.switch("Unfinished build waypoint", true).unique("waypoints")
    private val color by config.colorPicker("Color", Catppuccin.Mocha.Red.argb)
    private val stun by config.switch("Stun notification", true)
    private val `stun$percent` by config.slider("Notify at", 90, 1, 100, "%")
    private val `stun$message` by config.input("Notification message", "<red>Stun!")

    private val ex0 = listOf("§7Builders: §c3", "§7Progress: §c47%").fcs
    private var sent: Boolean = false

    private val display = Ticking {
        listOf("§7Builders: §c${KuudraAPI.buildPlayers}", "§7Progress: §c${KuudraAPI.buildProgress.value}%").fcs
    }

    private val render: Boolean
        get() = KuudraAPI.inRun && KuudraAPI.phase == KuudraPhase.Build

    init {
        KuudraAPI.buildProgress.onChange {
            if (!stun) return@onChange
            if (sent) return@onChange
            if (it <= `stun$percent`) return@onChange

            val prs = `stun$message`.parse(true)
            prs.alert()
            prs.mod()
            sent = true
        }

        config.hud("Build info") {
            if (it) return@hud sizedText(ex0)
            if (!render) return@hud null

            sizedText(display.value ?: return@hud null)
        }

        on<KuudraEvent.Start> {
            sent = false
        }

        on<WorldRenderEvent.Extract> {
            if (!render) return@on

            for (e in KuudraSupply.every) if (!e.built) extractFrameBox(e.buildAABB, color, depth = false)
        }.runWhen(waypoints.state)
    }
}
