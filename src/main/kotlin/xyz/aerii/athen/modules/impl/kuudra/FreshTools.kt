@file:Suppress("ObjectPrivatePropertyName", "Unused")

package xyz.aerii.athen.modules.impl.kuudra

import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraSupply
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Smoothie.showTitle
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.parse
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
    private val notify by config.switch("Notify party", true)
    private val `notify$message` by config.textInput("Notify message", "FRESH [#buildPerc]").dependsOn { notify }
    private val `notify$unused` by config.textParagraph("Variable: <red>#buildPerc").dependsOn { notify }
    private val `notify$checkParty` by config.switch("Check party", true).dependsOn { notify }

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

        on<MessageEvent.Chat.Receive> {
            if (!timer.enabled && !alert) return@on
            if (KuudraAPI.phase != KuudraPhase.Build) return@on
            if (stripped != "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!") return@on

            time = System.currentTimeMillis()

            if (!alert && !notify) return@on
            if (alert && `alert$message`) `alert$message$t`.parse().modMessage()
            if (alert && `alert$title`) `alert$title$t`.parse().showTitle()
            if (notify && (!`notify$checkParty` || PartyAPI.inParty)) fn0()
        }
    }

    private fun fn(): Pair<Int, Int>? {
        time = -1
        return null
    }

    private fun fn0() {
        val vector = client.player?.blockPosition() ?: return
        val int = KuudraSupply.every.minByOrNull { it.buildPos.distSqr(vector) }?.progress ?: return
        val a = `notify$message`.replace("#buildPerc", "$int%")
        "pc $a".command()
    }
}