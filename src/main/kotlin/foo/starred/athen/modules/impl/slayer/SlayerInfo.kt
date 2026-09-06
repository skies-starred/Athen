@file:Suppress("UNUSED")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractText
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.config.Category
import foo.starred.athen.ducks.entity.EntityDuck.Companion.attachedStripped
import foo.starred.athen.ducks.entity.EntityDuck.Companion.parent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.render.renderPos
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.abbreviate
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.toDuration
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import tech.thatgravyboat.skyblockapi.utils.extentions.serverHealth
import tech.thatgravyboat.skyblockapi.utils.extentions.toRomanNumeral
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

    private val hideOriginal = config.switch("Hide original", true).unique("hideOriginal")
    private val showKillTime by config.switch("Show kill time", true)
    private val increase by config.switch("Dynamic text size", true)

    private val nameStyle by config.input("Name style", "<dark_red>#name_short #tier")
    private val _unused0 by config.variables("#name_short", "#name_long", "#tier")

    private val styles0 by config.group("Timer styles")
    private val timerStyle by styles0.input("Normal", "<aqua>#time")
    private val blazeStyle by styles0.input("Blaze", "<aqua>#hits <dark_gray>[<gray>#time<dark_gray>]")
    private val attunementColor by styles0.switch("Use attunement colors", true)
    private val laserStyle by styles0.input("Laser", "<aqua>#laser <dark_gray>[<gray>#time<dark_gray>]")
    private val _unused1 by styles0.variables("#time", "#laser")

    private val styles1 by config.group("Health styles")
    private val healthStyle by styles1.input("Normal", "<aqua>#health")
    private val hitStyle by styles1.input("Hits", "<aqua>#hits Hits <dark_gray>[<gray>#health<dark_gray>]")
    private val _unused2 by styles1.variables("#hits", "#health")

    init {
        on<TickEvent.Client.End> {
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

                val a = i.slayer.type == SlayerBoss.Voidgloom
                for (l in e.attachedStripped) {
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
            entities[entity] = Info(slayerInfo, slayerInfo.type?.display ?: return@on)
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

        on<WorldRenderEvent.Entity> {
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

                for (a in l.indices) extractText(l[a], b.add(0.0, -a * 0.25, 0.0), depth = !i.visible, increase = increase)
            }
        }
    }

    private fun Info.str(hits: Int? = null): List<Component> = buildList {
        add(attached.str(slayer.entity.time(), slayer.type == SlayerBoss.Inferno))
        add(nameStyle.str0((slayer.type as? SlayerBoss)?.short ?: name, name, slayer.tier?.int?.toRomanNumeral(true) ?: "???"))

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
        .replace("#hits", hits.fn())
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

    private fun String.fn(): String {
        if (!attunementColor) return this
        val s = substringBefore(" ")

        return when (s) {
            "ASHEN" -> "<dark_gray>$this"
            "AURIC" -> "<gold>$this"
            "CRYSTAL" -> "<aqua>$this"
            "SPIRIT" -> "<white>$this"
            else -> this
        }
    }

    private data class Info(
        val slayer: foo.starred.athen.api.slayers.data.SlayerInfo,
        val name: String,
        var attached: String = "",
        var renderText: List<Component> = emptyList(),
        var deadSince: Int? = null,
        var visible: Boolean = false
    )
}
