package xyz.aerii.athen.api.kuudra

import net.minecraft.world.entity.monster.Giant
import net.minecraft.world.entity.monster.MagmaCube
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.extentions.getTexture
import tech.thatgravyboat.skyblockapi.utils.extentions.serverMaxHealth
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.kuudra.enums.AbstractSupply
import xyz.aerii.athen.api.kuudra.enums.KuudraPlayer
import xyz.aerii.athen.api.kuudra.enums.KuudraPhase
import xyz.aerii.athen.api.kuudra.enums.KuudraSupply
import xyz.aerii.athen.api.kuudra.enums.KuudraTier
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.KuudraEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.ScoreboardEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.React
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
    private val progressRegex = Regex("^PROGRESS: (?<progress>\\d+)%")
    private val eatRegex = Regex("^(?<user>\\w+) has been eaten by Kuudra!$")
    private val stunRegex = Regex("^\\w+ destroyed one of Kuudra's pods!$")

    private val set = setOf(KuudraPhase.Supply, KuudraPhase.Fuel)
    private val set0 = setOf(KuudraSupply.supply, KuudraSupply.fuel)

    private val _k = Schrodinger(::fn) { !it.isAlive }

    @JvmStatic
    var buildProgress: React<Int> = React(0)
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
    val supplies: MutableSet<AbstractSupply> = mutableSetOf()

    @JvmStatic
    val fuels: MutableSet<AbstractSupply> = mutableSetOf()

    @JvmStatic
    val teammates: MutableSet<KuudraPlayer> = mutableSetOf()

    init {
        on<LocationEvent.ServerConnect> {
            reset()
        }

        on<KuudraEvent.Start> (priority = Int.MIN_VALUE) {
            teammates.clear()
            for (p in McClient.players) teammates += KuudraPlayer(p.profile.name)
        }

        on<TickEvent.Client> {
            if (!inRun) return@on
            if (ticks % 2 != 0) return@on

            val player = client.player ?: return@on
            if (KuudraPhase.Skip.active && player.position().y < 10) phase = KuudraPhase.Kill.start()

            if (phase !in set) return@on
            if (phase == KuudraPhase.Supply) for (s in supplies) s.pos()
            else if (phase == KuudraPhase.Fuel) for (f in fuels) f.pos()

            if (ticks % 10 != 0) return@on
            val players = McLevel.players.takeIf { it.isNotEmpty() } ?: return@on

            if (phase == KuudraPhase.Supply) for (s in supplies) s.nearby = players.any { s.radAABB.contains(it.position()) }
            else if (phase == KuudraPhase.Fuel) for (f in fuels) f.nearby = players.any { f.radAABB.contains(it.position()) }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

        // Elle sometimes does not send the end dialogue if another dialogue sequence is active,
        // that is when she is eaten by Kuudra and the run ends. Love hypixel and their bugs.
        on<EntityEvent.Update.Health> {
            if (!inRun) return@on
            if ((tier?.int ?: 0) < KuudraTier.BURNING.int) return@on
            if (phase != KuudraPhase.DPS) return@on
            if (entity != kuudra) return@on
            if (new <= 2f) phase = KuudraPhase.Kill.start()
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

        on<EntityEvent.Update.Equipment> {
            if (!inRun) return@on
            val phase = phase ?: return@on

            if (phase.ordinal > KuudraPhase.Fuel.ordinal) return@on
            if (phase == KuudraPhase.Build && buildProgress.value < 90) return@on
            val e = entity as? Giant ?: return@on

            if (supplies.any { it.entity == e } || fuels.any { it.entity == e }) return@on
            if (e.mainHandItem?.getTexture() !in set0) return@on

            val s = AbstractSupply(e)
            if (phase == KuudraPhase.Supply) supplies += s
            else fuels += s
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

        on<EntityEvent.Unload> {
            if (!inRun) return@on
            if (phase !in set) return@on
            val e = entity as? Giant ?: return@on

            if (phase == KuudraPhase.Supply) supplies.removeIf { it.entity == e }
            else fuels.removeIf { it.entity == e }
        }.runWhen(SkyBlockIsland.KUUDRA.inIsland)

        on<EntityEvent.Update.Named> {
            if (!inRun) return@on
            val phase = phase ?: return@on

            if (phase.ordinal > KuudraPhase.Build.ordinal) return@on
            val pos = infoLineEntity.blockPosition()
            val n = component.stripped()

            when (phase) {
                KuudraPhase.Supply -> {
                    when (n) {
                        "BRING SUPPLY CHEST HERE" -> {
                            KuudraSupply.at(pos)?.active = false
                        }

                        "✓ SUPPLIES RECEIVED ✓" -> {
                            KuudraSupply.at(pos)?.active = true
                        }
                    }
                }

                KuudraPhase.Build -> {
                    if (n == "PROGRESS: COMPLETE") {
                        KuudraSupply.at(pos, 100)?.built = true
                        return@on
                    }

                    progressRegex.findThenNull(n, "progress") { (progress) ->
                        KuudraSupply.at(pos, progress.toInt())?.built = false
                    } ?: return@on

                    buildRegex.findOrNull(n, "progress", "players") { (p0, p1) ->
                        buildProgress.value = p0.toInt()
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

        on<MessageEvent.Chat.Receive> {
            when {
                stripped == "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!" -> {
                    KuudraEvent.Start.post()
                    phase = KuudraPhase.Supply.start()
                    inRun = true
                }

                stripped == "[NPC] Elle: OMG! Great work collecting my supplies!" -> {
                    phase = KuudraPhase.Build.start()
                }

                stripped == "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!" -> {
                    phase = KuudraPhase.Fuel.start()
                }

                stripped == "[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!" -> {
                    phase = if (tier?.int in KuudraPhase.Skip.tiers) KuudraPhase.Skip.start() else KuudraPhase.Kill.start()
                }

                completeRegex.matches(stripped) -> {
                    KuudraEvent.End.Success.post()
                    KuudraEvent.End.Any.post()
                    inRun = false
                }

                defeatRegex.matches(stripped) -> {
                    KuudraEvent.End.Defeat.post()
                    KuudraEvent.End.Any.post()
                    inRun = false
                }

                else -> {
                    deathRegex.findThenNull(stripped, "username") { (username) ->
                        val n = username.takeIf { it != "You" } ?: Smoothie.playerName ?: return@findThenNull

                        teammates.find { it.name == n }?.deaths++
                    } ?: return@on

                    val tier = tier ?: return@on
                    if (tier.int < KuudraTier.BURNING.int) return@on
                    if (KuudraPhase.Stun.started && KuudraPhase.DPS.started) return@on

                    if (!KuudraPhase.Stun.started) {
                        eatRegex.findThenNull(stripped, "user") { (user) ->
                            if (user == "Elle") return@findThenNull
                            phase = KuudraPhase.Stun.start()
                        } ?: return@on
                    }

                    if (!KuudraPhase.DPS.started) {
                        stunRegex.findThenNull(stripped) {
                            phase = KuudraPhase.DPS.start()
                        } ?: return@on
                    }
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

        buildProgress.value = 0
        buildPlayers = 0

        supplies.clear()
        fuels.clear()
        teammates.clear()
        KuudraSupply.reset()
        KuudraPhase.reset()
    }
}