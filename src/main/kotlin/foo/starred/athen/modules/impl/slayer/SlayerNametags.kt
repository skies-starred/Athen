@file:Suppress("UNUSED", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.slayers.data.SlayerInfo
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.ducks.entity.EntityDuck.Companion.attachedStripped
import foo.starred.athen.ducks.entity.EntityDuck.Companion.parent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.SlayerEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.kbus.extensions.runWhen
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.entity.extension.renderVec3
import foo.starred.kontext.api.entity.extension.serverHealth
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.format.extension.number.abbreviate
import foo.starred.kontext.api.format.extension.roman.toRomanNumeral
import foo.starred.kontext.api.format.extension.time.parseDuration
import foo.starred.kontext.api.text.parser.impl.parse
import foo.starred.parallax.api.primitives.ParallaxText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.*

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object SlayerNametags : Module(
    "Slayer nametags",
    "Shows information about the boss in a cleaner format.",
    ConfigCategory.SLAYER
) {
    private val hideOriginal = config.switch("Hide original", true).unique("hideOriginal")
    private val killTime by config.switch("Show kill time", true)
    private val increase by config.switch("Dynamic text size", true)

    private val `styles$name` by config.input("Name style", "<dark_red>#name_short #tier")
    private val `styles$name$unused` by config.variables("#name_short", "#name_long", "#tier")

    private val `styles$timer` by config.group("Timer styles")
    private val `styles$timer$normal` by `styles$timer`.input("Normal", "<aqua>#time")
    private val `styles$timer$blaze` by `styles$timer`.input("Blaze", "<aqua>#hits <dark_gray>[<gray>#time<dark_gray>]")
    private val `styles$timer$blaze$colors` by `styles$timer`.switch("Use attunement colors", true)
    private val `styles$timer$void$laser` by `styles$timer`.input("Laser", "<aqua>#laser <dark_gray>[<gray>#time<dark_gray>]")
    private val `styles$timer$unused` by `styles$timer`.variables("#time", "#laser")

    private val `styles$health` by config.group("Health styles")
    private val `styles$health$normal` by `styles$health`.input("Normal", "<aqua>#health")
    private val `styles$health$hits` by `styles$health`.input("Hits", "<aqua>#hits Hits <dark_gray>[<gray>#health<dark_gray>]")
    private val `styles$health$unused` by `styles$health`.variables("#hits", "#health")

    private val entities: WeakHashMap<Entity, Info> = WeakHashMap()
    private val hidden: MutableSet<Entity> = mutableSetOf()

    init {
        on<TickEvent.Client.End> {
            if (entities.isEmpty()) return@on

            hidden.removeIf {
                !it.isAlive
            }

            val it = entities.entries.iterator()
            while (it.hasNext()) {
                val (entity, info) = it.next()

                val dead = info.deathTicks
                if (dead != null) {
                    info.deathTicks = dead + 1
                    if (dead > 100) it.remove()
                    continue
                }

                if (!entity.isAlive) {
                    it.remove()
                    continue
                }

                info.visible = client.player?.hasLineOfSight(entity) == true

                var hits: Int? = null
                var attached: String? = null
                var effects: MutableList<String>? = null

                val voidgloom = info.slayer.type == SlayerBoss.Voidgloom
                for (line in entity.attachedStripped) {
                    if (line.endsWith("'s SLAYER BOSS")) {
                        continue
                    }

                    val hits0 = line.endsWith(" Hits")
                    if (line.endsWith('❤') || hits0) {
                        effects = effects.effect(line, '\uE01A', "<aqua>\uE01A")
                        effects = effects.effect(line, 'ᛤ', "<dark_purple>ᛤ")
                    }

                    if (hits == null && voidgloom && hits0) {
                        hits = line.substringBefore(" Hits").substringAfterLast(' ').toIntOrNull()
                        continue
                    }

                    if (attached == null && ":" in line) {
                        attached = line
                        if (hits != null || !voidgloom) break
                    }
                }

                if (attached != null) {
                    info.attached = attached
                }

                info.effects = effects
                info.renderable = info.lines(hits)
            }
        }

        on<SlayerEvent.Boss.Spawn> {
            entities[entity] = Info(slayerInfo, slayerInfo.type?.display ?: return@on)
        }

        on<SlayerEvent.Boss.Death> {
            val info = entities[entity] ?: return@on

            info.deathTicks = 0
            info.renderable = listOf("§c${(entity.tickCount / 20.0).parseDuration(decimals = 1)}".literal())
        }

        on<LocationEvent.Server.Connect> {
            entities.clear()
            hidden.clear()
        }

        on<WorldRenderEvent.Entity> {
            if (entities.isEmpty()) return@on

            val entity = entity as? ArmorStand ?: return@on
            if (entity in hidden) return@on cancel()
            if (entity.parent !in entities) return@on

            cancel()
            hidden.add(entity)
        }.runWhen(hideOriginal.state)

        on<WorldRenderEvent.Extract> {
            if (entities.isEmpty()) return@on
            //~ if >= 26.2 'mainCamera' -> 'mainCamera()'
            val camera = client.gameRenderer.mainCamera.position()

            val entities = entities.toMap()
            for ((k, v) in entities) {
                val text = v.renderable
                val spacing = if (increase) 0.25 * camera.distanceTo(k.renderVec3()) / 3f else 0.25
                val position = k.renderVec3().add(0.0, 0.5 + (text.size - 1) * spacing / 2, 0.0)

                for (i in text.indices) {
                    ParallaxText.component(text[i], position.add(0.0, -i * spacing, 0.0), depth = !v.visible, increase = increase)
                }
            }
        }
    }

    private fun Info.lines(hits: Int? = null): List<Component> {
        val health = (slayer.entity as? LivingEntity)?.serverHealth?.abbreviate() ?: "???"
        val special = if (!effects.isNullOrEmpty()) " <dark_gray>| <r>" + effects?.joinToString() else ""

        return buildList {
            add(attached.timer(slayer.entity.time(), slayer.type == SlayerBoss.Inferno))
            add(`styles$name`.fill("#name_short" to ((slayer.type as? SlayerBoss)?.short ?: name), "#name_long" to name, "#tier" to (slayer.tier?.int?.toRomanNumeral() ?: "???")))

            if (hits != null) add(`styles$health$hits`.fill("#health" to health, "#hits" to hits.toString(), extra = special))
            else add(`styles$health$normal`.fill("#health" to health, extra = special))
        }
    }

    private fun String.timer(seconds: Double?, blaze: Boolean): Component {
        if (blaze) {
            return `styles$timer$blaze`.fill("#hits" to substringBeforeLast(" ").trim().tint(), "#time" to substringAfterLast(" ").trim())
        }

        val laser = when (seconds) {
            null -> return `styles$timer$normal`.fill("#time" to this, "#laser" to "")
            0.0 -> "Soon"
            else -> seconds.parseDuration(decimals = 1)
        }

        return `styles$timer$void$laser`.fill("#time" to this, "#laser" to laser)
    }

    private fun String.fill(vararg values: Pair<String, String>, extra: String = ""): Component {
        var text = this

        for ((key, value) in values) {
            text = text.replace(key, value)
        }

        if (extra.isNotEmpty()) {
            text += extra
        }

        return text.parse(true)
    }

    private fun String.tint(): String {
        if (!`styles$timer$blaze$colors`) return this

        return when (substringBefore(" ")) {
            "ASHEN" -> "<dark_gray>$this"
            "AURIC" -> "<gold>$this"
            "CRYSTAL" -> "<aqua>$this"
            "SPIRIT" -> "<white>$this"
            else -> this
        }
    }

    private fun Entity.time(): Double? {
        val ticks = vehicle?.tickCount ?: return null
        return maxOf(0.0, 8.2 - ticks * 0.05)
    }

    private fun MutableList<String>?.effect(line: String, symbol: Char, string: String): MutableList<String>? {
        if (symbol !in line) return this

        return (this ?: mutableListOf()).apply {
            add(string)
        }
    }

    private data class Info(
        val slayer: SlayerInfo,
        val name: String
    ) {
        var effects: List<String>? = null

        var renderable: List<Component> = emptyList()
        var attached: String = ""

        var visible: Boolean = false
        var deathTicks: Int? = null
    }
}
