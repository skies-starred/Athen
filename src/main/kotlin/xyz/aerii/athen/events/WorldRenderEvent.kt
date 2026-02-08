package xyz.aerii.athen.events

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.CameraRenderState
import xyz.aerii.athen.accessors.EntityRenderStateAccessor
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.Event

sealed class WorldRenderEvent {
    sealed class Entity {
        data class Pre(
            val renderState: EntityRenderState,
            val poseStack: PoseStack,
            val cameraRenderState: CameraRenderState
        ) : CancellableEvent() {
            val entity
                get() = (renderState as? EntityRenderStateAccessor)?.`athen$getEntity`()
        }

        data class Post(
            val renderState: EntityRenderState,
            val poseStack: PoseStack,
            val cameraRenderState: CameraRenderState
        ) : Event() {
            val entity
                get() = (renderState as? EntityRenderStateAccessor)?.`athen$getEntity`()
        }
    }

    data object Extract : Event()

    data class Render(val pose: PoseStack, val consumers: MultiBufferSource.BufferSource) : Event()
}
