package xyz.aerii.athen.modules.impl.slayer

import net.minecraft.world.entity.Entity
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.SlayerEvent
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.render.Render3D
import xyz.aerii.athen.utils.render.renderBoundingBox
import java.awt.Color

@Load
@OnlyIn(skyblock = true)
object SlayerHighlight : Module(
    "Slayer highlights",
    "Highlights the slayer bosses.",
    Category.SLAYER
) {
    private val bossHighlight by config.switch("Highlight boss")
    private val onlyShowForMine by config.switch("Only for mine", true).dependsOn { bossHighlight }
    private val color by config.colorPicker("Color", Color(255, 0, 0, 255)).dependsOn { bossHighlight }
    private val lineWidth by config.slider("Line width", 2f, 0f, 10f).dependsOn { bossHighlight }

    private val minibossHighlight by config.switch("Highlight miniboss", false)
    private val minibossColor by config.colorPicker("Miniboss color", Color(255, 127, 127, 255)).dependsOn { minibossHighlight }
    private val minibossWidth by config.slider("Miniboss line width", 2f, 0f, 10f).dependsOn { minibossHighlight }

    private val demonHighlight by config.switch("Highlight demon", false)
    private val demonColor by config.colorPicker("Demon color", Color(255, 165, 0, 255)).dependsOn { demonHighlight }
    private val demonWidth by config.slider("Demon line width", 2f, 0f, 10f).dependsOn { demonHighlight }

    private val trackedSlayers = mutableListOf<Entity>()
    private val trackedMinibosses = mutableListOf<Entity>()
    private val trackedDemons = mutableListOf<Entity>()

    init {
        on<SlayerEvent.Boss.Spawn> {
            if (!bossHighlight) return@on
            if (onlyShowForMine && !slayerInfo.isOwnedByPlayer) return@on

            trackedSlayers.add(slayerInfo.entity)
        }

        on<SlayerEvent.Miniboss.Spawn> {
            if (!minibossHighlight) return@on

            trackedMinibosses.add(slayerInfo.entity)
        }

        on<SlayerEvent.Demon.Spawn> {
            if (!demonHighlight) return@on

            trackedDemons.add(slayerInfo.entity)
        }

        on<SlayerEvent.Boss.Death> {
            trackedSlayers.remove(slayerInfo.entity)
        }

        on<SlayerEvent.Miniboss.Death> {
            trackedMinibosses.remove(slayerInfo.entity)
        }

        on<SlayerEvent.Demon.Death> {
            trackedDemons.remove(slayerInfo.entity)
        }

        on<LocationEvent.ServerConnect> {
            trackedSlayers.clear()
            trackedMinibosses.clear()
            trackedDemons.clear()
        }

        on<WorldRenderEvent.Extract> {
            trackedSlayers.r(color, lineWidth)
            trackedMinibosses.r(minibossColor, minibossWidth)
            trackedDemons.r(demonColor, demonWidth)
        }
    }

    private fun MutableList<Entity>.r(color: Color, width: Float) {
        var i = 0
        while (i < size) {
            val entity = this[i]

            if (entity.isRemoved) {
                removeAt(i)
                continue
            }

            Render3D.drawBox(entity.renderBoundingBox, color, width)
            i++
        }
    }
}