@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.slayer

import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerMiniBoss
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Smoothie.alert
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Toaster.toast
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module

@Load
@OnlyIn(skyblock = true)
object MinibossAlert : Module(
    "Miniboss alert",
    "Shows an alert for you when a miniboss spawns nearby.",
    Category.SLAYER
) {
    private val sendMessage by config.switch("Send message", true)
    private val vanillaMessage by config.switch("Use mc message").dependsOn { sendMessage }
    private val showTitle by config.switch("Show title", true)
    private val vanillaTitle by config.switch("Use mc title").dependsOn { showTitle }
    private val maxDistance by config.slider("Maximum distance", 10, 1, 15)
    private val alertText by config.textInput("Alert text", "<aqua>Miniboss spawned!")
    private val bigBoiText by config.textInput("Big boi text", "<red>Big boi spawned!")
    private val _unused0 by config.textParagraph("The same text will be used for both title and message.\n<gray>Big boi = Big miniboss")

    init {
        on<SlayerEvent.Miniboss.Spawn> {
            if (entity.tickCount >= 20) return@on
            val player = client.player ?: return@on
            val slayerMiniBoss = (slayerInfo.type as? SlayerMiniBoss).takeIf { entity.distanceTo(player) < maxDistance } ?: return@on
            val text = (if (slayerMiniBoss.isBigBoy) bigBoiText else alertText)

            if (showTitle) if (vanillaTitle) text.parse().alert() else text.toast()
            if (sendMessage) if (vanillaMessage) text.parse().modMessage() else text.notify()
        }
    }
}