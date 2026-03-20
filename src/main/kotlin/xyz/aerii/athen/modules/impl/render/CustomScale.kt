package xyz.aerii.athen.modules.impl.render

import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.modules.Module

@Load
@OnlyIn(skyblock = true)
object CustomScale : Module(
    "Custom scale",
    "Changes the scale for players!",
    Category.RENDER
) {
    private val self by config.switch("Scale self", true)
    private val others by config.switch("Scale others", true)
    private val npc by config.switch("Scale NPCs", true)
    private val nametags by config.switch("Scale nametags", true)
    private val shadow by config.switch("Scale shadow", true)
    val scale by config.slider("Scale", 1f, 0.1f, 5f, showDouble = true)

    init {
        on<WorldRenderEvent.Entity.Pre> {
            val r = renderState as? AvatarRenderState ?: return@on
            if (!entity.fn()) return@on

            if (nametags) r.nameTagAttachment = r.nameTagAttachment?.scale(scale.toDouble())
            if (shadow) r.shadowRadius *= scale
        }
    }

    @JvmStatic
    fun Entity?.fn(): Boolean = when (this) {
        is LocalPlayer -> self
        is Player -> if (uuid.version() == 4) others else npc
        else -> false
    }
}