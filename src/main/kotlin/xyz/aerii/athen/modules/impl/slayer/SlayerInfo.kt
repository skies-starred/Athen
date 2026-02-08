@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerType
import tech.thatgravyboat.skyblockapi.helpers.getAttachedTo
import tech.thatgravyboat.skyblockapi.helpers.getStrippedAttachedLines
import tech.thatgravyboat.skyblockapi.utils.extentions.serverHealth
import tech.thatgravyboat.skyblockapi.utils.extentions.toRomanNumeral
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryTracker.shortName
import xyz.aerii.athen.utils.abbreviate
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderPos
import xyz.aerii.athen.utils.toDuration
import java.util.*

@Load
@OnlyIn(skyblock = true)
object SlayerInfo : Module(
    "Slayer info",
    "Shows information about the boss, in a nicer way.",
    Category.SLAYER
) {
    private val entities: WeakHashMap<Entity, Info> = WeakHashMap()
    private val hideCache: MutableSet<Entity> = mutableSetOf()

    private val hideOriginal = config.switch("Hide original", true).custom("hideOriginal")
    private val showKillTime by config.switch("Show kill time", true)

    private val nameStyle by config.textInput("Name style", "&4#name_short #tier")
    private val _unused0 by config.textParagraph("Variable: <red>#name_short<r>, <red>#name_long<r>, <red>#tier")

    private val timerStyleExpandable by config.expandable("Timer styles")
    private val timerStyle by config.textInput("Normal", "&b#time").childOf { timerStyleExpandable }
    private val laserStyle by config.textInput("Laser", "&b#laser &8[&7#time&8]").childOf { timerStyleExpandable }
    private val _unused1 by config.textParagraph("Variable: <red>#time<r>, <red>#laser").childOf { timerStyleExpandable }

    private val healthStyleExpandable by config.expandable("Health styles")
    private val healthStyle by config.textInput("Normal", "&b#health").childOf { healthStyleExpandable }
    private val hitStyle by config.textInput("Hits", "&b#hits Hits &8[&7#health&8]").childOf { healthStyleExpandable }
    private val _unused2 by config.textParagraph("Variable: <red>#hits<r>, <red>#health").childOf { healthStyleExpandable }

    init {
        on<TickEvent.Client> {
            if (entities.isEmpty()) return@on

            hideCache.removeIf { !it.isAlive }

            val it = entities.entries.iterator()
            while (it.hasNext()) {
                val (e, i) = it.next()

                val d = i.deadSince
                if (d != null) {
                    i.deadSince = d + 1
                    if (d > 100) it.remove()
                    continue
                }

                if (!e.isAlive) {
                    it.remove()
                    continue
                }

                i.visible = client.player?.hasLineOfSight(e) == true

                var hits: Int? = null
                var attached: String? = null

                for (l in e.getStrippedAttachedLines()) {
                    if (hits == null && i.slayer.type == SlayerType.VOIDGLOOM_SERAPH && " Hits" in l) {
                        hits = l.substringBefore(" Hits").substringAfterLast(' ').toIntOrNull()
                        continue
                    }

                    if (attached == null && ":" in l && "Spawned by:" !in l) {
                        attached = l
                        if (hits != null) break
                    }
                }

                if (attached != null) i.attached = attached
                i.renderText = i.str(hits).split('\n')
            }
        }

        on<SlayerEvent.Boss.Spawn> {
            entities[entity] = Info(slayerInfo, slayerInfo.type?.displayName ?: return@on)
        }

        on<SlayerEvent.Boss.Death> {
            val e = entities[entity] ?: return@on
            e.deadSince = 0
            e.renderText = listOf("§c${(entity.tickCount / 20.0).toDuration(secondsDecimals = 1)}")
        }

        on<LocationEvent.ServerConnect> {
            entities.clear()
            hideCache.clear()
        }

        on<WorldRenderEvent.Entity.Pre> {
            val entity = entity as? ArmorStand ?: return@on
            if (entity in hideCache) return@on cancel()
            if (entity.getAttachedTo() !in entities) return@on

            cancel()
            hideCache.add(entity)
        }.runWhen(hideOriginal.state)

        on<WorldRenderEvent.Extract> {
            for ((e, i) in entities) {
                val l = i.renderText
                val b = e.renderPos.add(0.0, 0.5 + (l.size - 1) * 0.25 / 2, 0.0)

                for (a in l.indices) Render3D.drawString(l[a], b.add(0.0, -a * 0.25, 0.0), depthTest = !i.visible)
            }
        }
    }

    private fun Info.str(hits: Int? = null): String = buildString {
        append(attached.str(slayer.entity.time()) + '\n')
        append(nameStyle.str0((slayer.type as? SlayerType)?.shortName ?: name, name, slayer.tier?.toRomanNumeral(true) ?: "???") + '\n')

        val cH = (slayer.entity as? LivingEntity)?.serverHealth?.abbreviate() ?: "???"
        append(if (hits != null) hitStyle.str2(cH, hits) else healthStyle.str2(cH))
    }

    private fun String.str(t: Double?): String {
        val t = when (t) {
            null -> null
            0.0 -> "Soon"
            else -> t.toDuration(secondsDecimals = 1)
        }

        return if (t != null) laserStyle.str1(this, t) else timerStyle.str1(this)
    }

    private fun Entity.time(): Double? {
        val ticks = vehicle?.tickCount ?: return null
        val t = 8.2 - (ticks * 0.05)
        return if (t > 0.0) t else 0.0
    }

    private fun String.str0(short: String, long: String, tier: String): String = this
        .replace("&", "§")
        .replace("#name_short", short)
        .replace("#name_long", long)
        .replace("#tier", tier)

    private fun String.str1(time: String, laser: String = ""): String = this
        .replace("&", "§")
        .replace("#time", time)
        .replace("#laser", laser)

    private fun String.str2(health: String, hits: Int = 0): String = this
        .replace("&", "§")
        .replace("#health", health)
        .replace("#hits", hits.toString())

    private data class Info(
        val slayer: xyz.aerii.athen.api.skyblock.SlayerInfo,
        val name: String,
        var attached: String = "",
        var renderText: List<String> = listOf(""),
        var deadSince: Int? = null,
        var visible: Boolean = false
    )
}