@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.slayer

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.handlers.Smoothie.alert
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render2D.sizedText
import xyz.aerii.athen.utils.toDurationFromMillis

@Load
@OnlyIn(skyblock = true)
object CocoonAlert : Module(
    "Cocoon alert",
    "Alerts you when you cocoon your slayer boss!",
    Category.SLAYER
) {
    private val alert by config.switch("Show alert", true)
    private val `alert$message` by config.textInput("Alert message", "<red>Boss cocooned!").dependsOn { alert }
    private val `alert$sound` by config.sound("Alert sound").dependsOn { alert }

    private val timer = config.hud("Cocoon timer") {
        if (it) return@hud sizedText("Cocoon: §c4.6s")
        if (time == 0L) return@hud null

        val t = time - System.currentTimeMillis()
        if (t <= 0L) {
            time = 0
            return@hud null
        }

        sizedText("Cocoon: §c${t.toDurationFromMillis(secondsDecimals = 1)}")
    }

    private var time: Long = 0

    init {
        on<MessageEvent.Chat.Receive> {
            if (stripped != "YOU COCOONED YOUR SLAYER BOSS") return@on

            if (alert) `alert$message`.parse().alert(soundType = `alert$sound`.sound)
            if (timer.enabled) time = System.currentTimeMillis() + 6000
        }
    }
}