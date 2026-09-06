package foo.starred.athen.events

import com.mojang.blaze3d.vertex.PoseStack
import foo.starred.athen.events.core.CancellableEvent
import foo.starred.athen.events.core.Event
//~ if >= 26.2 'MultiBufferSource' -> 'SubmitNodeCollector'
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState

sealed class WorldRenderEvent {
    class Entity(
        val renderState: EntityRenderState,
        val poseStack: PoseStack,
        val cameraRenderState: CameraRenderState,
        val entity: net.minecraft.world.entity.Entity?
    ) : CancellableEvent()

    data object Extract : Event()

    data class Render(
        val pose: PoseStack,
        //~ if >= 26.2 'MultiBufferSource.BufferSource' -> 'SubmitNodeCollector'
        val consumers: MultiBufferSource.BufferSource
    ) : Event()
}
