package xyz.aerii.athen.api.kuudra

import net.minecraft.world.entity.monster.MagmaCube
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.serverMaxHealth
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraPlayer
import xyz.aerii.athen.api.kuudra.enums.KuudraSupply
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.events.ChatEvent
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.ScoreboardEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Schrodinger
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.stripped

@Priority
object KuudraAPI {
    private val deathRegex = Regex("^ ☠ (?:(?<username>\\w+)|You were) .+(?: and became a ghost)?\\.$")
    private val tierRegex = Regex(" ⏣ Kuudra's Hollow \\(T(?<t>\\d+)\\)")
    private val completeRegex = Regex("^\\s+KUUDRA DOWN!")
    private val defeatRegex = Regex("^\\s+DEFEAT")
    private val buildRegex = Regex("Building Progress (?<progress>\\d+)% \\((?<players>\\d) Players Helping\\)")
    private val progressRegex = Regex("^PROGRESS: \\d+%")

    private val _k = Schrodinger(::fn) { !it.isAlive }

    @JvmStatic
    var buildProgress: Int = 0
        private set

    @JvmStatic
    var buildPlayers: Int = 0
        private set

    @JvmStatic
    var inRun: Boolean = false
        private set

    @JvmStatic
    var tier: KuudraTier? = null
        private set

    @JvmStatic
    var phase: KuudraPhase? = null
        private set

    @JvmStatic
    val kuudra: MagmaCube? by _k

    @JvmStatic
    val teammates: MutableSet<KuudraPlayer> = mutableSetOf()

    init {
        on<LocationEvent.ServerConnect> {
            reset()
        }

        on<KuudraEvent.Start> (priority = Int.MIN_VALUE) {
            teammates.clear()
            for (p in McClient.players) teammates.add(KuudraPlayer(p.profile.name))
        }

        on<EntityEvent.NameChange> {
            if (!inRun) return@on
            val phase = phase ?: return@on

            if (phase.int > 2) return@on
            val pos = infoLineEntity.blockPosition()
            val n = component.stripped()

            when (phase) {
                KuudraPhase.SUPPLIES -> {
                    when (n) {
                        "BRING SUPPLY CHEST HERE" -> {
                            KuudraSupply.at(pos)?.active = false
                        }

                        "✓ SUPPLIES RECEIVED ✓" -> {
                            KuudraSupply.at(pos)?.active = true
                        }
                    }
                }

                KuudraPhase.BUILD -> {
                    if (n == "PROGRESS: COMPLETE") {
                        KuudraSupply.at(pos)?.built = true
                        return@on
                    }

                    progressRegex.findThenNull(n) {
                        KuudraSupply.at(pos)?.built = false
                    } ?: return@on

                    buildRegex.findOrNull(n, "progress", "players") { (p0, p1) ->
                        buildProgress = p0.toInt()
                        buildPlayers = p1.toInt()
                    }
                }

                else -> {}
            }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

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
                message == "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!" -> {
                    KuudraEvent.Start.post()
                    phase = KuudraPhase.SUPPLIES
                    inRun = true
                }

                message == "[NPC] Elle: OMG! Great work collecting my supplies!" -> {
                    phase = KuudraPhase.BUILD
                }

                message == "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!" -> {
                    phase = KuudraPhase.FUEL
                }

                message == "[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!" -> {
                    if (tier == KuudraTier.INFERNAL) phase = KuudraPhase.LAIR
                }

                completeRegex.matches(message) -> {
                    KuudraEvent.End.Success.post()
                    KuudraEvent.End.Any.post()
                    inRun = false
                }

                defeatRegex.matches(message) -> {
                    KuudraEvent.End.Defeat.post()
                    KuudraEvent.End.Any.post()
                    inRun = false
                }

                else -> {
                    val m = deathRegex.find(message) ?: return@on
                    val n = m.groups["username"]?.value?.takeIf { it != "You" } ?: Smoothie.playerName ?: return@on

                    teammates.find { it.name == n }?.deaths++
                }
            }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)
    }

    private fun fn(): MagmaCube? =
        client.level?.entitiesForRendering()?.find { it is MagmaCube && it.size == 30 && it.serverMaxHealth == 100_000f } as? MagmaCube

    private fun reset() {
        tier = null
        _k._v = null
        phase = null
        inRun = false

        buildProgress = 0
        buildPlayers = 0

        teammates.clear()
        KuudraSupply.reset()
    }
}