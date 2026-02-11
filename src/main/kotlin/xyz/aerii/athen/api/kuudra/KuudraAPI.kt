package xyz.aerii.athen.api.kuudra

import net.minecraft.world.entity.monster.MagmaCube
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.serverMaxHealth
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.kuudra.enums.KuudraPlayer
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.events.ChatEvent
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.ScoreboardEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Schrodinger
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.stripped

@Priority
object KuudraAPI {
    private val deathRegex = Regex("^ ☠ (?:(?<username>\\w+)|You were) .+(?: and became a ghost)?\\.$")
    private val tierRegex = Regex(" ⏣ Kuudra's Hollow \\(T(?<t>\\d+)\\)")
    private val completeRegex = Regex("\\s+KUUDRA DOWN!")

    @JvmStatic
    val kuudra: MagmaCube? by Schrodinger(::fn) { !it.isAlive }

    @JvmStatic
    var inRun: Boolean = false

    @JvmStatic
    val teammates: MutableSet<KuudraPlayer> = mutableSetOf()

    @JvmStatic
    var tier: KuudraTier? = null

    init {
        on<LocationEvent.ServerConnect> {
            reset()
        }

        on<KuudraEvent.Start> {
            teammates.clear()
            for (p in McClient.players) teammates.add(KuudraPlayer(p.profile.name))
        }

        on<ScoreboardEvent.Update> {
            if (tier != null) return@on

            tierRegex.anyMatch(added, "t") { (t) ->
                tier = KuudraTier.get(t.toInt())
            }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

        on<ChatEvent> {
            if (actionBar) return@on
            val message = message.stripped()

            when {
                message == "[NPC] Elle: Head over to the main platform, I will join you when I get a bite!" -> {
                    KuudraEvent.Start.post()
                    inRun = true
                }

                completeRegex.matches(message) -> {
                    KuudraEvent.End.post()
                    inRun = false
                }

                else -> {
                    val m = deathRegex.find(message) ?: return@on
                    val n = m.groups["username"]?.value ?: Smoothie.playerName ?: return@on

                    teammates.find { it.name == n }?.deaths++
                }
            }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)
    }

    private fun fn(): MagmaCube? =
        client.level?.entitiesForRendering()?.find { it is MagmaCube && it.size == 30 && it.serverMaxHealth == 100_000f } as? MagmaCube

    private fun reset() {
        tier = null
        inRun = false
        teammates.clear()
    }
}