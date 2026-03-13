package xyz.aerii.athen.api.skyblock

import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.api.area.slayer.*
import xyz.aerii.athen.accessors.parent
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.handlers.Typo.stripped
import java.util.*

// FIXME: Doesn't work for tarantula T5.
@Priority
object SlayerAPI {
    private val questFailedRegex = Regex("\\s+SLAYER QUEST FAILED!")
    private val questStartedRegex = Regex("\\s+SLAYER QUEST STARTED!")
    private val questCompletedRegex = Regex("\\s+SLAYER QUEST COMPLETE!")

    private val logged: MutableSet<Entity> = mutableSetOf()
    val slayerBosses: WeakHashMap<Entity, SlayerInfo> = WeakHashMap()
    val slayerNames = SLAYER_MOBS.flatMap { it.inGameNames }.toSet()

    var slayer: SlayerInfo? = null
        private set

    init {
        on<MessageEvent.Chat.Receive> {
            when {
                questStartedRegex.matches(stripped) -> {
                    "SlayerAPI: Quest started!".devMessage()
                    SlayerEvent.Quest.Start.post()
                }

                questCompletedRegex.matches(stripped) -> {
                    "SlayerAPI: Quest completed!".devMessage()
                    SlayerEvent.Quest.End.post()
                    slayer = null
                }

                questFailedRegex.matches(stripped) -> {
                    "SlayerAPI: Quest failed!".devMessage()
                    SlayerEvent.Cleanup(SlayerEvent.CleanupType.QuestFail).post()
                    slayer = null
                }
            }
        }

        on<EntityEvent.Update.Attach> {
            var entity = entity.parent ?: return@on

            val slayerInfo = when {
                component.stripped().check() -> slayerBosses.computeIfAbsent(entity, ::SlayerInfo)
                else -> slayerBosses[entity] ?: return@on
            }

            if (
                slayerInfo.type is SlayerMob &&
                (slayerInfo.type !is SlayerType || slayerInfo.owner != null) &&
                logged.add(entity)
            ) {
                when (slayerInfo.type) {
                    is SlayerType -> {
                        if (slayerInfo.isOwnedByPlayer) slayer = slayerInfo
                        SlayerEvent.Boss.Spawn(entity, slayerInfo).post()
                        "SlayerAPI: Slayer spawned (owner=${slayerInfo.owner}, tier=${slayerInfo.tier}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                    }

                    is SlayerMiniBoss -> {
                        SlayerEvent.Miniboss.Spawn(entity, slayerInfo).post()
                        "SlayerAPI: Miniboss spawned (owner=${slayerInfo.owner}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                    }

                    is SlayerDemon -> {
                        SlayerEvent.Demon.Spawn(entity, slayerInfo).post()
                        "SlayerAPI: Demon spawned (owner=${slayerInfo.owner}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                    }
                }
            }
        }

        on<EntityEvent.Death> {
            val slayerInfo = slayerBosses.remove(entity) ?: return@on
            logged.remove(entity)

            when (slayerInfo.type) {
                is SlayerType -> {
                    if (slayerInfo.isOwnedByPlayer) slayer = null
                    SlayerEvent.Boss.Death(entity, slayerInfo).post()
                    "SlayerAPI: Slayer killed (owner=${slayerInfo.owner}, tier=${slayerInfo.tier}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                }

                is SlayerMiniBoss -> {
                    SlayerEvent.Miniboss.Death(entity, slayerInfo).post()
                    "SlayerAPI: Miniboss killed (owner=${slayerInfo.owner}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                }

                is SlayerDemon -> {
                    SlayerEvent.Demon.Death(entity, slayerInfo).post()
                    "SlayerAPI: Demon killed (owner=${slayerInfo.owner}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                }
            }
        }

        on<LocationEvent.Server.Connect> {
            "SlayerAPI: Cleaning up.".devMessage()
            SlayerEvent.Cleanup(SlayerEvent.CleanupType.ServerChange).post()
            reset()
        }
    }

    private fun reset() {
        slayerBosses.clear()
        logged.clear()
        slayer = null
    }

    private fun String.check(): Boolean {
        if (!startsWith("☠") && !endsWith("❤") && !endsWith("❤ ✯") && !endsWith(" Hits")) return false

        for (name in slayerNames) if (contains(name)) return true
        return false
    }
}