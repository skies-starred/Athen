@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.kuudra

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.handlers.Smoothie.showTitle
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDurationFromMillis

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object FreshTools : Module(
    "Fresh tools",
    "Fresh notifier and timer for kuudra.",
    Category.KUUDRA
) {
    private val alert by config.switch("Show alert", true)
    private val `alert$message` by config.switch("Alert message", true).dependsOn { alert }
    private val `alert$message$t` by config.textInput("Message", "<red>Fresh tools!").dependsOn { alert && `alert$message` }
    private val `alert$title` by config.switch("Alert title", true).dependsOn { alert }
    private val `alert$title$t` by config.textInput("Title", "<red>Fresh tools!").dependsOn { alert && `alert$title` }

    private val timer = config.hud("Fresh timer") {
        if (it) return@hud sizedText("Fresh: §c6.7s")
        if (time == -1L) return@hud null

        val r = 10_000 - (System.currentTimeMillis() - time)
        if (r <= 0) return@hud fn()

        sizedText("Fresh: §c${r.toDurationFromMillis(secondsDecimals = 1)}")
        null
    }

    private var time: Long = -1

    init {
        on<LocationEvent.ServerConnect> {
            fn()
        }

        on<MessageEvent.Chat> {
            if (!timer.enabled && !alert) return@on
            if (KuudraAPI.phase != KuudraPhase.BUILD) return@on
            if (stripped != "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!") return@on

            time = System.currentTimeMillis()

            if (!alert) return@on
            if (`alert$message`) `alert$message$t`.parse().modMessage()
            if (`alert$title`) `alert$title$t`.parse().showTitle()
        }
    }

    private fun fn(): Pair<Int, Int>? {
        time = -1
        return null
    }
}