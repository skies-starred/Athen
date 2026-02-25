package xyz.aerii.athen.modules.impl.render

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.modules.Module

@Load
object RenderOptimiser :  Module(
    "Render optimiser",
    "Cleans up rendering stuff, and maybe optimizes a bit.",
    Category.RENDER
) {
    private val _fog by config.switch("Hide fog", true)
    private val lavaOverlay by config.switch("Hide lava overlay", true)
    private val fireOverlay by config.switch("Hide fire overlay", true)
    private val entityFire = config.switch("Hide fire on entity", true).dependsOn { fireOverlay }.custom("hideEntityFire")

    @JvmStatic
    val fire: Boolean
        get() = react.value && fireOverlay

    @JvmStatic
    val lava: Boolean
        get() = react.value && lavaOverlay

    @JvmStatic
    val fog: Boolean
        get() = react.value && _fog

    init {
        on<WorldRenderEvent.Entity.Pre> {
            renderState.displayFireAnimation = false
        }.runWhen(entityFire.state)
    }
}