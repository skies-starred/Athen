@file:Suppress("LocalVariableName")

package foo.starred.athen.api.slayers.resolver.base

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.messaging.impl.MessagingAPI.dev
import foo.starred.athen.api.slayers.SlayerAPI
import foo.starred.athen.api.slayers.data.SlayerInfo
import foo.starred.athen.api.slayers.enums.tier.SlayerTier
import foo.starred.athen.api.slayers.enums.type.base.ISlayerType
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.events.PacketEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.events.core.on
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.mainThread
import foo.starred.snowbird.utils.stripped
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
//~ if >= 26.2 'EntityType' -> 'EntityTypes'
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import kotlin.math.acos
import kotlin.text.get

@Load
object GenericSlayerBossResolver {
    private val spawnRegex = Regex("SLAYER BOSS! The (?<type>.+?) (?<tier>[IVXLCDM]+) spawned!")
    //~ if >= 26.2 'EntityType' -> 'EntityTypes'
    private val set = setOf(EntityType.ZOMBIE, EntityType.ENDERMAN, EntityType.SPIDER, EntityType.BLAZE, EntityType.WOLF)

    private var last: Int = 0

    init {
        on<PacketEvent.Receive, ClientboundAddEntityPacket> {
            if (type !in set) return@on

            last = id
        }

        on<PacketEvent.Receive, ClientboundSystemChatPacket> {
            if (overlay) return@on
            val text = content.stripped().takeIf { it.startsWith("SLAYER BOSS! The ") } ?: return@on
            val match = spawnRegex.find(text) ?: return@on

            val type0 = ISlayerType.Companion.Names.map[match.groups["type"]?.value ?: ""]
            val tier0 = SlayerTier.find(match.groups["tier"]?.value ?: "")
            val last = last

            mainThread {
                val entity = level?.getEntity(last) ?: return@mainThread

                spawn(SlayerAPI.bosses.computeIfAbsent(entity, ::SlayerInfo).apply {
                    type = type0
                    tier = tier0
                    owner = user.name
                })
            }
        }

        on<TickEvent.Client.End> {
            if (ticks % 10 != 0) return@on

            for (info in SlayerAPI.bosses.values) {
                if (info.type !is SlayerBoss) continue
                if (info.owner != null) continue
                if (!info.entity.isAlive) continue
                if (info.entity.tickCount > 20) continue

                get(info)
            }
        }
    }

    fun get(info: SlayerInfo) {
        if (info.type !is SlayerBoss) return
        if (info.owner != null) return
        if (info.phase == 2 && info.entity.tickCount > 15) return

        val living = info.entity as? LivingEntity ?: return
        val looked = living.looking() ?: return

        info.owner = looked.gameProfile.name()
        spawn(info)
    }

    fun spawn(info: SlayerInfo) {
        if (info.owned) {
            SlayerAPI.slayer = info
        }

        if (SlayerAPI.logged.add(info.entity.id)) {
            SlayerEvent.Boss.Spawn(info.entity, info).post()
            "SlayerAPI: Slayer spawned (owner=${info.owner}, phase=${info.phase}, tier=${info.tier}, tickAge=${info.entity.tickCount / 20.0}s)".dev()
        }
    }

    private fun LivingEntity.looking(distance: Double = 15.0): Player? {
        val level = client.level ?: return null
        val player = client.player ?: return null
        val distance = distance * distance
        val active = SlayerAPI.bosses.values.filter { it.entity.isAlive && (it.type != SlayerBoss.Tarantula || it.phase != 1) }.mapNotNull { it.owner }
        val players = level.players().filter { it != player && it.uuid.version() == 4 && it.gameProfile.name() !in active && it.distanceToSqr(this) <= distance }.takeIf { it.isNotEmpty() } ?: return null

        return players.minByOrNull {
            val distance = it.distanceTo(this)
            val angle = angle(it)

            distance + (angle / 90.0) * 2.0
        }
    }

    private fun LivingEntity.angle(player: Player): Double {
        val vec3 = player.eyePosition.subtract(eyePosition)
        val distance = vec3.length().takeIf { it >= 0.001 } ?: return 0.0

        return Math.toDegrees(acos(getViewVector(1.0f).normalize().dot(vec3.scale(1.0 / distance)).coerceIn(-1.0, 1.0)))
    }
}
