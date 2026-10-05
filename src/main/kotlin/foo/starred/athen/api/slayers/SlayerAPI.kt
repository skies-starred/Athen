package foo.starred.athen.api.slayers

import foo.starred.athen.annotations.Priority
import foo.starred.athen.api.messaging.impl.MessagingAPI.dev
import foo.starred.athen.api.slayers.data.SlayerInfo
import foo.starred.athen.api.slayers.enums.type.base.ISlayerType
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.api.slayers.enums.type.impl.SlayerDemon
import foo.starred.athen.api.slayers.enums.type.impl.SlayerMini
import foo.starred.athen.api.slayers.resolver.base.GenericSlayerBossResolver
import foo.starred.athen.ducks.entity.EntityDuck.Companion.parent
import foo.starred.athen.events.EntityEvent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.events.core.on
import net.minecraft.world.entity.Entity
import java.util.*

@Priority
object SlayerAPI {
    private val failRegex = Regex("\\s+SLAYER QUEST FAILED!")
    private val startRegex = Regex("\\s+SLAYER QUEST STARTED!")
    private val completeRegex = Regex("\\s+SLAYER QUEST COMPLETE!")

    val logged: MutableSet<Int> = mutableSetOf()
    val bosses: WeakHashMap<Entity, SlayerInfo> = WeakHashMap()

    var slayer: SlayerInfo? = null

    init {
        on<MessageEvent.Chat.Receive> {
            when {
                startRegex.matches(stripped) -> {
                    "SlayerAPI: Quest started!".dev()
                    SlayerEvent.Quest.Start.post()
                }

                completeRegex.matches(stripped) -> {
                    "SlayerAPI: Quest completed!".dev()
                    SlayerEvent.Quest.End.post()

                    slayer = null
                }

                failRegex.matches(stripped) -> {
                    "SlayerAPI: Quest failed!".dev()

                    SlayerEvent.Reset.QuestFail.post()
                    SlayerEvent.Reset.Any.post()

                    slayer = null
                }
            }
        }

        on<EntityEvent.Update.Attach> {
            val entity = entity.parent ?: return@on
            val slayerInfo =
                if (stripped.check()) bosses.computeIfAbsent(entity, ::SlayerInfo)
                else bosses[entity] ?: return@on

            if (slayerInfo.type is SlayerBoss && slayerInfo.owner == null) {
                GenericSlayerBossResolver.get(slayerInfo)
                if (slayerInfo.owner == null) return@on
            }

            if (!logged.add(entity.id)) {
                return@on
            }

            when (slayerInfo.type) {
                is SlayerMini -> {
                    SlayerEvent.Miniboss.Spawn(entity, slayerInfo).post()
                    "SlayerAPI: Miniboss spawned (tickAge=${entity.tickCount / 20.0}s)".dev()
                }

                is SlayerDemon -> {
                    SlayerEvent.Demon.Spawn(entity, slayerInfo).post()
                    "SlayerAPI: Demon spawned (tickAge=${entity.tickCount / 20.0}s)".dev()
                }
            }
        }

        on<EntityEvent.Death> {
            val slayerInfo = bosses.remove(entity) ?: bosses.remove(entity.rootVehicle) ?: return@on
            logged.remove(entity.id)

            when (slayerInfo.type) {
                is SlayerBoss -> {
                    if (slayerInfo.owned) slayer = null
                    SlayerEvent.Boss.Death(entity, slayerInfo).post()
                    "SlayerAPI: Slayer killed (owner=${slayerInfo.owner}, phase=${slayerInfo.phase}, tier=${slayerInfo.tier}, tickAge=${entity.tickCount / 20.0}s)".dev()
                }

                is SlayerMini -> {
                    SlayerEvent.Miniboss.Death(entity, slayerInfo).post()
                    "SlayerAPI: Miniboss killed (tickAge=${entity.tickCount / 20.0}s)".dev()
                }

                is SlayerDemon -> {
                    SlayerEvent.Demon.Death(entity, slayerInfo).post()
                    "SlayerAPI: Demon killed (tickAge=${entity.tickCount / 20.0}s)".dev()
                }
            }
        }

        on<TickEvent.Client.End> {
            if (ticks % 5 != 0) return@on

            bosses.entries.removeIf { (key, value) ->
                (!key.isAlive).also { if (it) logged.remove(key.id) }
            }
        }

        on<LocationEvent.Server.Connect> {
            "SlayerAPI: Cleaning up.".dev()

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
        if (!endsWith("❤") && !endsWith("❤ ✯") && !endsWith(" Hits")) return false

        for (name in ISlayerType.Companion.Names.all) {
            if (!contains(name)) continue
            return true
        }

        return false
    }
}
