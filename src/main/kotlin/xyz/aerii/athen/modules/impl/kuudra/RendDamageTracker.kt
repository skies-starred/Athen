package xyz.aerii.athen.modules.impl.kuudra

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.abbreviate
import xyz.aerii.athen.utils.toDurationFromMillis

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object RendDamageTracker : Module(
    "Rend damage tracker",
    "Tries to detect how much damage someone did.",
    Category.KUUDRA
) {
    init {
        on<EntityEvent.Update.Health> {
            if (!KuudraAPI.inRun) return@on
            if (KuudraAPI.tier != KuudraTier.INFERNAL) return@on
            if (KuudraAPI.phase != KuudraPhase.Kill) return@on
            if (entity != KuudraAPI.kuudra) return@on
            if (new > 25000) return@on

            val old = old ?: return@on
            val diff = (old - new).takeIf { it > 1666 } ?: return@on
            val player = client.player ?: return@on
            if (player.position().y > 30) return@on
            val damage = (diff * 9600).abbreviate()
            val duration = KuudraPhase.Kill.durTime.toDurationFromMillis(secondsOnly = true, secondsDecimals = 1)

            "Detected <red>$damage<r> damage at <yellow>$duration<r>!".parse().modMessage()
        }
    }
}