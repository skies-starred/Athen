package xyz.aerii.athen.modules.impl.kuudra

import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.kuudra.KuudraAPI
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object KuudraBreakdown : Module(
    "Kuudra breakdown",
    "Sends a message about what each player did at the end of the run.",
    Category.KUUDRA
) {
    private val supplyRegex = Regex("(?:\\[[^]]*])? ?(?<user>\\w+) recovered one of Elle's supplies! \\(\\d+/\\d+\\)")
    private val fuelRegex = Regex("(?:\\[[^]]*])? ?(?<user>\\w+) recovered a Fuel Cell and charged the Ballista! \\(\\d+%\\)")
    private val stunRegex = Regex("(?<user>\\w+) destroyed one of Kuudra's pods!")
    private val set = mutableSetOf<Player>()

    init {
        on<KuudraEvent.Start> {
            set.clear()
            for (t in KuudraAPI.teammates) set.add(Player(t.name))
        }

        on<MessageEvent.Chat.Receive> {
            if (stripped.isEmpty()) return@on

            if (stripped == "[NPC] Elle: Good job everyone. A hard fought battle come to an end. Let's get out of here before we run into any more trouble!") {
                if (set.isEmpty()) return@on

                "<red>Run breakdown:".parse().modMessage()
                for (p in set) {
                    " • <yellow>${p.name} <gray>- <red>${p.supply} <r>Supplies <gray>| <red>${p.fuel} <r>Fuels <gray>| <red>${p.deaths ?: "???"} <r>Deaths".parse().apply {
                        if (p.stun > 0) onHover("<red>${p.stun} <r>Stuns".parse())
                    }.lie()
                }

                return@on
            }

            supplyRegex.findThenNull(stripped, "user") { (user) ->
                val p = set.find { it.name == user } ?: return@findThenNull
                p.supply++
            } ?: return@on

            fuelRegex.findThenNull(stripped, "user") { (user) ->
                val p = set.find { it.name == user } ?: return@findThenNull
                p.fuel++
            } ?: return@on

            val tier = KuudraAPI.tier?.int ?: return@on
            if (tier < KuudraTier.BURNING.int) return@on

            stunRegex.findThenNull(stripped, "user") { (user) ->
                val p = set.find { it.name == user } ?: return@findThenNull
                p.stun++
            }
        }
    }

    private data class Player(
        val name: String,
        var supply: Int = 0,
        var fuel: Int = 0,
        var stun: Int = 0
    ) {
        val deaths: Int?
            get() = KuudraAPI.teammates.find { it.name == name }?.deaths
    }
}