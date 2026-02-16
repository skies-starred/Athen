package xyz.aerii.athen.events

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.CameraRenderState
import xyz.aerii.athen.events.core.CancellableEvent
import xyz.aerii.athen.events.core.Event

sealed class WorldRenderEvent {
    sealed class Entity {
        data class Pre(
            val renderState: EntityRenderState,
            val poseStack: PoseStack,
            val cameraRenderState: CameraRenderState,
            val entity: net.minecraft.world.entity.Entity?
        ) : CancellableEvent()

        data class Post(
            val renderState: EntityRenderState,
            val poseStack: PoseStack,
            val cameraRenderState: CameraRenderState,
            val entity: net.minecraft.world.entity.Entity?
        ) : Event()
    }

    data object Extract : Event()

    data class Render(val pose: PoseStack, val consumers: MultiBufferSource.BufferSource) : Event()
}
