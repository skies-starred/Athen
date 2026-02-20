package xyz.aerii.athen.api.skyblock

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster./*? >= 1.21.11 { *//*spider.*//*? }*/CaveSpider
import tech.thatgravyboat.skyblockapi.api.area.slayer.*
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.handlers.Typo.stripped
import java.util.*
import kotlin.math.abs

// FIXME: Doesn't work for tarantula T5.
@Priority
object SlayerAPI {
    private val logged: MutableSet<Entity> = mutableSetOf()
    val slayerBosses: WeakHashMap<Entity, SlayerInfo> = WeakHashMap()
    val SLAYER_NAMES = SLAYER_MOBS.flatMap { it.inGameNames }.toSet()

    private val questFailedRegex = Regex("\\s+SLAYER QUEST FAILED!")
    private val questStartedRegex = Regex("\\s+SLAYER QUEST STARTED!")
    private val questCompletedRegex = Regex("\\s+SLAYER QUEST COMPLETE!")

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
                }

                questFailedRegex.matches(stripped) -> {
                    "SlayerAPI: Quest failed!".devMessage()
                    SlayerEvent.Cleanup(SlayerEvent.CleanupType.QuestFail).post()
                }
            }
        }

        on<EntityEvent.Update.Attach> {
            val entity = infoLineEntity.c() ?: return@on

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

        on<LocationEvent.ServerConnect> {
            "SlayerAPI: Cleaning up.".devMessage()
            SlayerEvent.Cleanup(SlayerEvent.CleanupType.ServerChange).post()
            reset()
        }
    }

    private fun reset() {
        slayerBosses.clear()
        logged.clear()
    }

    private fun String.check(): Boolean {
        if (!startsWith("☠") && !endsWith("❤") && !endsWith("❤ ✯") && !endsWith(" Hits")) return false

        for (name in SLAYER_NAMES) if (contains(name)) return true
        return false
    }

    /**
     * Finds the best matching entity that's closest to this entity.
     * If two entities that are at the same distance is found, it prefers the smallest tick difference.
     * @return best entity
     */
    private fun Entity.c(): Entity? {
        val entities = level().getEntities(this, boundingBox.inflate(0.0, 1.0, 0.0))

        var b: Entity? = null
        var d = Double.MAX_VALUE
        var t = Int.MAX_VALUE

        for (e in entities) {
            if (!e.v()) continue

            val dist = e.distanceToSqr(this)
            if (b != null && dist > d) continue

            val tick = abs(e.tickCount - tickCount)
            if (dist == 0.0 && tick == 0) return e

            if (b == null || dist < d || (dist == d && tick < t)) {
                b = e
                d = dist
                t = tick
            }
        }

        return b
    }

    private fun Entity?.v(): Boolean =
        this != null && this !is ArmorStand && this !is CaveSpider && isAlive && !isInvisible
}