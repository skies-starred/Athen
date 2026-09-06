package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.config.Category
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.client
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.util.Mth

@Load
object PlayerSpin : Module(
    "Player spin",
    "Spins your player model around... nicely",
    Category.RENDER
) {
    private val head by config.switch("Rotate head")
    private val head0 by config.selector("Head rotation", listOf("Clockwise", "Anti-clockwise"))
    private val head1 by config.slider("Head speed", 1f, 0f, 10f, double = true)

    private val body by config.switch("Rotate body")
    private val body0 by config.selector("Body rotation", listOf("Clockwise", "Anti-clockwise"))
    private val body1 by config.slider("Body speed", 1f, 0f, 10f, double = true)

    init {
        on<WorldRenderEvent.Entity> {
            val r = renderState as? AvatarRenderState ?: return@on
            val e = entity as? LocalPlayer ?: return@on

            val a = head
            val b = body
            val c = head1
            val d = body1

            val f = (e.tickCount + client.deltaTracker.getGameTimeDeltaPartialTick(true)) / 20f
            val g = if (a) Mth.wrapDegrees(f * c * 360f * if (head0 == 0) 1f else -1f) else 0f
            val h = if (b) Mth.wrapDegrees(f * d * 360f * if (body0 == 0) 1f else -1f) else 0f

            if (a) r.yRot = g - h
            if (b) r.bodyRot = h
        }
    }
}
