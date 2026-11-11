@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.ducks.entity.generic.EntityDuck.Companion.parent
import foo.starred.athen.events.*
import foo.starred.athen.modules.Module
import foo.starred.kontext.abstraction.chat.color.MinecraftColors
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.entity.extension.renderAABB
import foo.starred.parallax.api.primitives.ParallaxBox
import foo.starred.parallax.api.primitives.ParallaxLine
import net.minecraft.world.entity.Entity
import java.util.concurrent.ConcurrentHashMap

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object SlayerHighlights : Module(
    "Slayer highlights",
    "Highlights the slayer bosses.",
    ConfigCategory.SLAYER
) {
    private val regex = Regex("^(?<attunement>[A-Z]+) ♨(\\d+) \\d\\d:\\d\\d$")

    private val fill by config.switch("Filled outline")
    private val expand by config.slider("Expand outline", 0.0, 0.0, 2.0, double = true)

    private val _boss by config.group("Boss highlight")
    private val boss by _boss.switch("Highlight boss")
    private val `boss$tracer` by _boss.switch("Tracer to boss", true)
    private val `boss$mine` by _boss.switch("Only for mine", true)
    private val `boss$color` by _boss.colorPicker("Color", MochaColorScheme.Red.argb)
    private val `boss$width` by _boss.slider("Line width", 2f, 0f, 10f)

    private val _mini by config.group("Miniboss highlight")
    private val mini by _mini.switch("Highlight miniboss", false)
    private val `mini$color` by _mini.colorPicker("Miniboss color", MochaColorScheme.Peach.argb)
    private val `mini$width` by _mini.slider("Miniboss line width", 2f, 0f, 10f)

    private val _demon by config.group("Demon highlight")
    private val demon by _demon.switch("Highlight demon", false)
    private val `demon$color` by _demon.colorPicker("Demon color", MochaColorScheme.Flamingo.argb)
    private val `demon$width` by _demon.slider("Demon line width", 2f, 0f, 10f)

    private val _blaze by config.group("Blaze state colors")
    private val blaze by _blaze.switch("Blaze state colors", true)
    private val `blaze$ashen` by _blaze.colorPicker("Ashen", MinecraftColors.DARK_GRAY.color)
    private val `blaze$auric` by _blaze.colorPicker("Auric", MinecraftColors.GOLD.color)
    private val `blaze$crystal` by _blaze.colorPicker("Crystal", MinecraftColors.AQUA.color)
    private val `blaze$spirit` by _blaze.colorPicker("Spirit", MinecraftColors.WHITE.color)

    private val tracers = ConcurrentHashMap<Entity, Boolean>()
    private val slayers = ConcurrentHashMap<Entity, Int>()
    private val demons = ConcurrentHashMap<Entity, Int>()
    private val minibosses = ConcurrentHashMap<Entity, Int>()

    init {
        on<TickEvent.Client.End> {
            if (ticks % 5 != 0) return@on

            val player = client.player
            if (player != null) {
                for (slayer in tracers.keys) {
                    tracers[slayer] = player.hasLineOfSight(slayer)
                }
            }

            slayers.keys.removeIf { !it.isAlive }
            demons.keys.removeIf { !it.isAlive }
            minibosses.keys.removeIf { !it.isAlive }
        }

        on<EntityEvent.Update.Named> {
            if (!blaze) return@on
            val parent = entity.parent ?: return@on

            val slayer = slayers.any { it.key.id == parent.id }
            val demon = demons.any { it.key.id == parent.id }
            if (!slayer && !demon) return@on

            val (f) = regex.find(stripped)?.destructured ?: return@on
            val c = if (f == "ASHEN") `blaze$ashen` else if (f == "AURIC") `blaze$auric` else if (f == "CRYSTAL") `blaze$crystal` else if (f == "SPIRIT") `blaze$spirit` else `boss$color`

            if (slayer) slayers[parent] = c
            if (demon) demons[parent] = c
        }

        on<SlayerEvent.Boss.Spawn> {
            if (!boss) return@on
            if (`boss$mine` && !slayerInfo.owned) return@on

            slayers[entity] = `boss$color`
            if (`boss$tracer` && slayerInfo.owned) tracers[entity] = false
        }

        on<SlayerEvent.Miniboss.Spawn> {
            if (!mini) return@on

            minibosses[entity] = `mini$color`
        }

        on<SlayerEvent.Demon.Spawn> {
            if (!demon) return@on

            demons[entity] = `demon$color`
        }

        on<SlayerEvent.Boss.Death> {
            slayers -= entity
        }

        on<SlayerEvent.Miniboss.Death> {
            minibosses -= entity
        }

        on<SlayerEvent.Demon.Death> {
            demons -= entity
        }

        on<LocationEvent.Server.Connect> {
            slayers.clear()
            minibosses.clear()
            demons.clear()
        }

        on<WorldRenderEvent.Extract> {
            for ((k, v) in tracers) {
                if (!k.isAlive) continue
                if (!v) continue

                ParallaxLine.tracer(k.renderAABB().center.toVector3f(), `boss$color`, `boss$width`)
            }

            slayers.fn(`boss$width`)
            minibosses.fn(`mini$width`)
            demons.fn(`demon$width`)
        }
    }

    private fun Map<Entity, Int>.fn(width: Float) {
        val map = this
        val inflate = expand
        val fill = fill

        for ((k, v) in map) {
            if (!k.isAlive) continue
            val aabb = k.renderAABB().inflate(inflate, 0.0, inflate)

            if (fill) ParallaxBox.fill(aabb, v) else ParallaxBox.frame(aabb, v, width)
        }
    }
}
