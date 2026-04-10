@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerType
import tech.thatgravyboat.skyblockapi.utils.extentions.serverHealth
import tech.thatgravyboat.skyblockapi.utils.extentions.toRomanNumeral
import xyz.aerii.athen.accessors.attachedStripped
import xyz.aerii.athen.accessors.parent
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.slayer.carry.SlayerCarryTracker.shortName
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderPos
import xyz.aerii.library.api.client
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.abbreviate
import xyz.aerii.library.utils.literal
import xyz.aerii.library.utils.toDuration
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

    private val nameStyle by config.textInput("Name style", "<dark_red>#name_short #tier")
    private val _unused0 by config.textParagraph("Variable: <red>#name_short<r>, <red>#name_long<r>, <red>#tier")

    private val timerStyleExpandable by config.expandable("Timer styles")
    private val timerStyle by config.textInput("Normal", "<aqua>#time").childOf { timerStyleExpandable }
    private val blazeStyle by config.textInput("Blaze", "<aqua>#hits <dark_gray>[<gray>#time<dark_gray>]").childOf { timerStyleExpandable }
    private val laserStyle by config.textInput("Laser", "<aqua>#laser <dark_gray>[<gray>#time<dark_gray>]").childOf { timerStyleExpandable }
    private val _unused1 by config.textParagraph("Variable: <red>#time<r>, <red>#laser").childOf { timerStyleExpandable }

    private val healthStyleExpandable by config.expandable("Health styles")
    private val healthStyle by config.textInput("Normal", "<aqua>#health").childOf { healthStyleExpandable }
    private val hitStyle by config.textInput("Hits", "<aqua>#hits Hits <dark_gray>[<gray>#health<dark_gray>]").childOf { healthStyleExpandable }
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

                for (l in e.attachedStripped) {
                    val a = i.slayer.type == SlayerType.VOIDGLOOM_SERAPH

                    if (hits == null && a && " Hits" in l) {
                        hits = l.substringBefore(" Hits").substringAfterLast(' ').toIntOrNull()
                        continue
                    }

                    if (attached == null && ":" in l && "Spawned by:" !in l) {
                        attached = l
                        if (hits != null || !a) break
                    }
                }

                if (attached != null) i.attached = attached
                i.renderText = i.str(hits)
            }
        }

        on<SlayerEvent.Boss.Spawn> {
            entities[entity] = Info(slayerInfo, slayerInfo.type?.displayName ?: return@on)
        }

        on<SlayerEvent.Boss.Death> {
            val e = entities[entity] ?: return@on
            e.deadSince = 0
            e.renderText = listOf("§c${(entity.tickCount / 20.0).toDuration(secondsDecimals = 1)}".literal())
        }

        on<LocationEvent.Server.Connect> {
            entities.clear()
            hideCache.clear()
        }

        on<WorldRenderEvent.Entity.Pre> {
            if (entities.isEmpty()) return@on

            val entity = entity as? ArmorStand ?: return@on
            if (entity in hideCache) return@on cancel()
            if (entity.parent !in entities) return@on

            cancel()
            hideCache.add(entity)
        }.runWhen(hideOriginal.state)

        on<WorldRenderEvent.Extract> {
            if (entities.isEmpty()) return@on

            for ((e, i) in entities) {
                val l = i.renderText
                val b = e.renderPos.add(0.0, 0.5 + (l.size - 1) * 0.25 / 2, 0.0)

                for (a in l.indices) Render3D.drawString(l[a], b.add(0.0, -a * 0.25, 0.0), depthTest = !i.visible)
            }
        }
    }

    private fun Info.str(hits: Int? = null): List<Component> = buildList {
        add(attached.str(slayer.entity.time(), slayer.type == SlayerType.INFERNO_DEMONLORD))
        add(nameStyle.str0((slayer.type as? SlayerType)?.shortName ?: name, name, slayer.tier?.toRomanNumeral(true) ?: "???"))

        val cH = (slayer.entity as? LivingEntity)?.serverHealth?.abbreviate() ?: "???"
        add(if (hits != null) hitStyle.str3(cH, hits) else healthStyle.str3(cH))
    }

    private fun String.str(t: Double?, blaze: Boolean): Component {
        if (blaze) {
            val hits = substringBeforeLast(" ").trim()
            val time = substringAfterLast(" ").trim()
            return blazeStyle.str1(hits, time)
        }

        val t = when (t) {
            null -> null
            0.0 -> "Soon"
            else -> t.toDuration(secondsDecimals = 1)
        }

        return if (t != null) laserStyle.str2(this, t) else timerStyle.str2(this)
    }

    private fun Entity.time(): Double? {
        val ticks = vehicle?.tickCount ?: return null
        val t = 8.2 - (ticks * 0.05)
        return if (t > 0.0) t else 0.0
    }

    private fun String.str0(short: String, long: String, tier: String): Component = this
        .replace("&", "§")
        .replace("#name_short", short)
        .replace("#name_long", long)
        .replace("#tier", tier)
        .parse(true)

    private fun String.str1(hits: String, time: String): Component = this
        .replace("&", "§")
        .replace("#hits", hits)
        .replace("#time", time)
        .parse(true)

    private fun String.str2(time: String, laser: String = ""): Component = this
        .replace("&", "§")
        .replace("#time", time)
        .replace("#laser", laser)
        .parse(true)

    private fun String.str3(health: String, hits: Int = 0): Component = this
        .replace("&", "§")
        .replace("#health", health)
        .replace("#hits", hits.toString())
        .parse(true)

    private data class Info(
        val slayer: xyz.aerii.athen.api.skyblock.SlayerInfo,
        val name: String,
        var attached: String = "",
        var renderText: List<Component> = emptyList(),
        var deadSince: Int? = null,
        var visible: Boolean = false
    )
}