package xyz.aerii.athen.api.slayers

import net.minecraft.world.entity.Entity
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.slayers.data.SlayerInfo
import xyz.aerii.athen.api.slayers.enums.type.base.ISlayerType
import xyz.aerii.athen.api.slayers.enums.type.impl.SlayerBoss
import xyz.aerii.athen.api.slayers.enums.type.impl.SlayerDemon
import xyz.aerii.athen.api.slayers.enums.type.impl.SlayerMini
import xyz.aerii.athen.ducks.entity.parent
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Typo.devMessage
import java.util.*

@Priority
object SlayerAPI {
    private val failRegex = Regex("\\s+SLAYER QUEST FAILED!")
    private val startRegex = Regex("\\s+SLAYER QUEST STARTED!")
    private val completeRegex = Regex("\\s+SLAYER QUEST COMPLETE!")

    private val logged: MutableSet<Int> = mutableSetOf()

    val bosses: WeakHashMap<Entity, SlayerInfo> = WeakHashMap()
    var slayer: SlayerInfo? = null
        private set

    init {
        on<MessageEvent.Chat.Receive> {
            when {
                startRegex.matches(stripped) -> {
                    "SlayerAPI: Quest started!".devMessage()
                    SlayerEvent.Quest.Start.post()
                }

                completeRegex.matches(stripped) -> {
                    "SlayerAPI: Quest completed!".devMessage()
                    SlayerEvent.Quest.End.post()
                    slayer = null
                }

                failRegex.matches(stripped) -> {
                    "SlayerAPI: Quest failed!".devMessage()

                    SlayerEvent.Reset.QuestFail.post()
                    SlayerEvent.Reset.Any.post()

                    slayer = null
                }
            }
        }

        on<EntityEvent.Update.Attach> {
            if (!logged.add(entity.id)) return@on

            val entity = entity.parent ?: return@on
            val slayerInfo =
                if (stripped.check()) bosses.computeIfAbsent(entity, ::SlayerInfo)
                else bosses[entity] ?: return@on

            if (
                slayerInfo.type is ISlayerType &&
                (slayerInfo.type !is SlayerBoss || slayerInfo.owner != null)
            ) {
                when (slayerInfo.type) {
                    is SlayerBoss -> {
                        if (slayerInfo.owned) slayer = slayerInfo
                        SlayerEvent.Boss.Spawn(entity, slayerInfo).post()
                        "SlayerAPI: Slayer spawned (owner=${slayerInfo.owner}, tier=${slayerInfo.tier}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                    }

                    is SlayerMini -> {
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
            val slayerInfo = bosses.remove(entity) ?: return@on
            logged.remove(entity.id)

            when (slayerInfo.type) {
                is SlayerBoss -> {
                    if (slayerInfo.owned) slayer = null
                    SlayerEvent.Boss.Death(entity, slayerInfo).post()
                    "SlayerAPI: Slayer killed (owner=${slayerInfo.owner}, tier=${slayerInfo.tier}, tickAge=${entity.tickCount / 20.0}s)".devMessage()
                }

                is SlayerMini -> {
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

            SlayerEvent.Reset.ServerChange.post()
            SlayerEvent.Reset.Any.post()

            reset()
        }
    }

    private fun reset() {
        bosses.clear()
        logged.clear()
        slayer = null
    }

    private fun String.check(): Boolean {
        if (!startsWith("☠") && !endsWith("❤") && !endsWith("❤ ✯") && !endsWith(" Hits")) return false

        for (name in ISlayerType.Companion.Names.all) if (contains(name)) return true
        return false
    }
}