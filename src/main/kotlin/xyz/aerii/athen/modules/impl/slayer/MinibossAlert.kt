@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.slayer

import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerMiniBoss
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.library.api.client
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.alert

@Load
@OnlyIn(skyblock = true)
object MinibossAlert : Module(
    "Miniboss alert",
    "Shows an alert for you when a miniboss spawns nearby.",
    Category.SLAYER
) {
    private val detection = config.dropdown("Detection type", listOf("Chat based", "Event based"), 1).custom("detection")
    private val _unused by config.textParagraph("Chat based detection type only works for your minibosses. Event based detection type works for all minibosses near you.")
    private val sendMessage by config.switch("Send message", true)
    private val vanillaMessage by config.switch("Use mc message").dependsOn { sendMessage }
    private val showTitle by config.switch("Show title", true)
    private val maxDistance by config.slider("Maximum distance", 10, 1, 15, "blocks")
    private val alertText by config.textInput("Alert text", "<aqua>Miniboss spawned!")
    private val bigBoiText by config.textInput("Big boi text", "<red>Big boi spawned!")
    private val _unused0 by config.textParagraph("The same text will be used for both title and message.\n<gray>Big boi = Big miniboss")

    private val bigBoys = SlayerMiniBoss.entries.filter { it.isBigBoy }.map { it.displayName }
    private val regex = Regex("^SLAYER MINI-BOSS (?<name>.+?) has spawned!$")

    init {
        on<MessageEvent.Chat.Receive> {
            val name = regex.findGroup(stripped, "name") ?: return@on
            val text = if (name in bigBoys) bigBoiText else alertText

            if (showTitle) text.parse().alert()
            if (sendMessage) if (vanillaMessage) text.parse().modMessage() else text.notify()
        }.runWhen(detection.state.map { it == 0 })

        on<SlayerEvent.Miniboss.Spawn> {
            if (entity.tickCount >= 20) return@on
            val player = client.player ?: return@on
            val slayerMiniBoss = (slayerInfo.type as? SlayerMiniBoss).takeIf { entity.distanceTo(player) < maxDistance } ?: return@on
            val text = (if (slayerMiniBoss.isBigBoy) bigBoiText else alertText)

            if (showTitle) text.parse().alert()
            if (sendMessage) if (vanillaMessage) text.parse().modMessage() else text.notify()
        }.runWhen(detection.state.map { it == 1 })
    }
}