package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.config.Category
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

@Load
@OnlyIn(skyblock = true)
object CustomScale : Module(
    "Custom scale",
    "Changes the scale for players!",
    Category.RENDER
) {
    private val scales by config.multiSelector("Scale", listOf("Self", "Others", "NPCs", "Nametags", "Shadow"), listOf(0, 1, 2, 3, 4))
    val scale by config.slider("Scale", 1f, 0.1f, 5f, double = true)
    val chibi by config.selector("Chibi style", listOf("None", "Big head", "Small head"), 1)
    val chibiness by config.slider("Chibi factor", 2f, 1f, 5f, double = true)

    init {
        on<WorldRenderEvent.Entity> {
            val r = renderState as? AvatarRenderState ?: return@on
            if (!entity.fn()) return@on

            if (3 in scales) r.nameTagAttachment = r.nameTagAttachment?.scale(scale.toDouble())
            if (4 in scales) r.shadowRadius *= scale
        }
    }

    @JvmStatic
    fun Entity?.fn(): Boolean = when (this) {
        is LocalPlayer -> 0 in scales
        is Player -> if (uuid.version() == 4) 1 in scales else 2 in scales
        else -> false
    }
}
