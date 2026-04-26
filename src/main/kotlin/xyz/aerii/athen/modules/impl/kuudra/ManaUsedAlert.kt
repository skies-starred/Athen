package xyz.aerii.athen.modules.impl.kuudra

import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.library.api.client
import xyz.aerii.library.api.command
import xyz.aerii.library.handlers.parser.parse

@Load
@OnlyIn(islands = [SkyBlockIsland.KUUDRA])
object ManaUsedAlert : Module(
    "Mana used alert",
    "Alerts the party when you mana dump!",
    Category.KUUDRA
) {
    private val ignore0 by config.switch("Ignore if 0 players", true)
    private val regex = Regex("^Used Extreme Focus! \\((?<int>\\d+) Mana\\)$")

    init {
        on<MessageEvent.Chat.Receive> {
            val p = client.player ?: return@on
            val i0 = regex.findGroup(stripped, "int")?.toIntOrNull() ?: return@on
            var i1 = 0

            for (a in McLevel.players) {
                if (a == p) continue
                if (a.uuid.version() != 4) continue
                if (p.distanceToSqr(a) > 25) continue

                i1++
            }

            if (i1 == 0 && ignore0) return@on

            "pc $i0 mana used on $i1 players!".command()
            "<red>$i0 <r>mana used on <red>$i1 <r>players!".parse(true).modMessage()
        }
    }
}