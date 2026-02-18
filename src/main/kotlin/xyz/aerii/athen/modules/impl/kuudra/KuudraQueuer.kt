package xyz.aerii.athen.modules.impl.kuudra

import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.modules.Module

@Load
object KuudraQueuer : Module(
    "Kuudra queuer",
    "Automatically re-queues at the end of each run.",
    Category.KUUDRA
) {
    private val delay by config.slider("Delay", 20, 0, 100, "ticks")

    private val partyRegex = Regex("^Party > (?:\\[[^]]*?] )?\\w{1,16}(?: [ቾ⚒])?: ?(?<message>.+)$")
    private var disableRequeue: Boolean = false

    init {
        on<MessageEvent.Chat> {
            if (PartyAPI.leader?.name != Smoothie.playerName) return@on

            partyRegex.findOrNull(stripped, "message") { (message) ->
                if (message == "!dt") disableRequeue = true
            }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

        on<KuudraEvent.End.Success> {
            if (PartyAPI.leader?.name != Smoothie.playerName) return@on
            if (disableRequeue) {
                disableRequeue = false
                return@on
            }

            Chronos.Tick after delay then {
                "instancerequeue".command()
            }
        }
    }
}