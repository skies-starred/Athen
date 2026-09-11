@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.enums.MessageColors
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractFrameBox
import foo.starred.athen.api.slayers.SlayerAPI
import foo.starred.athen.config.Category
import foo.starred.athen.ducks.entity.EntityDuck.Companion.parent
import foo.starred.athen.events.*
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.render.renderBoundingBox
import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import java.util.concurrent.ConcurrentHashMap

@Load
@OnlyIn(skyblock = true)
object SlayerHighlight : Module(
    "Slayer highlights",
    "Highlights the slayer bosses.",
    Category.SLAYER
) {
    private val regex = Regex("^(?<attunement>[A-Z]+) ♨(\\d+) \\d\\d:\\d\\d$")

    private val _boss by config.group("Boss highlight")
    private val boss by _boss.switch("Highlight boss")
    private val `boss$mine` by _boss.switch("Only for mine", true)
    private val `boss$color` by _boss.colorPicker("Color", Catppuccin.Mocha.Red.argb)
    private val `boss$width` by _boss.slider("Line width", 2f, 0f, 10f)

    private val _mini by config.group("Miniboss highlight")
    private val mini by _mini.switch("Highlight miniboss", false)
    private val `mini$color` by _mini.colorPicker("Miniboss color", Catppuccin.Mocha.Peach.argb)
    private val `mini$width` by _mini.slider("Miniboss line width", 2f, 0f, 10f)

    private val _demon by config.group("Demon highlight")
    private val demon by _demon.switch("Highlight demon", false)
    private val `demon$color` by _demon.colorPicker("Demon color", Catppuccin.Mocha.Flamingo.argb)
    private val `demon$width` by _demon.slider("Demon line width", 2f, 0f, 10f)

    private val _blaze by config.group("Blaze state colors")
    private val blaze by _blaze.switch("Blaze state colors", true)
    private val `blaze$ashen` by _blaze.colorPicker("Ashen", MessageColors.DARK_GRAY.color)
    private val `blaze$auric` by _blaze.colorPicker("Auric", MessageColors.GOLD.color)
    private val `blaze$crystal` by _blaze.colorPicker("Crystal", MessageColors.AQUA.color)
    private val `blaze$spirit` by _blaze.colorPicker("Spirit", MessageColors.WHITE.color)

    private val slayers = ConcurrentHashMap<Entity, Int>()
    private val demons = ConcurrentHashMap<Entity, Int>()
    private val minibosses = mutableMapOf<Entity, Int>()

    init {
        on<TickEvent.Client.End> {
            if (ticks % 5 != 0) return@on

            slayers.keys.removeIf { !it.isAlive }
            demons.keys.removeIf { !it.isAlive }
            minibosses.keys.removeIf { !it.isAlive }
        }

        on<EntityEvent.Update.Named> {
            if (!blaze) return@on

            val a = entity in slayers.keys
            val b = entity in demons.keys
            if (!a && !b) return@on

            val e = entity.parent ?: return@on
            val s = SlayerAPI.bosses[e] ?: return@on
            if (!b && s.owner == null) return@on

            val f = regex.findGroup(stripped, "attunement") ?: return@on
            val c = if (f == "ASHEN") `blaze$ashen` else if (f == "AURIC") `blaze$auric` else if (f == "CRYSTAL") `blaze$crystal` else if (f == "SPIRIT") `blaze$spirit` else `boss$color`

            if (a) slayers[e] = c
            if (b) demons[e] = c
        }

        on<SlayerEvent.Boss.Spawn> {
            if (!boss) return@on
            if (`boss$mine` && !slayerInfo.owned) return@on

            slayers[entity] = `boss$color`
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
            slayers.fn(`boss$width`)
            minibosses.fn(`mini$width`)
            demons.fn(`demon$width`)
        }
    }

    private fun Map<Entity, Int>.fn(width: Float) {
        val map = this
        for ((k, v) in map) {
            if (!k.isAlive) continue
            extractFrameBox(k.renderBoundingBox, v, width)
        }
    }
}
