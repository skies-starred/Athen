package xyz.aerii.athen.api.rendering.level.impl.renderers.impl

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer./*? >= 1.21.11 {*//*rendertype.RenderTypes*//*? } else {*/RenderType/*? }*/
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import xyz.aerii.athen.api.rendering.level.impl.renderers.base.ILevelRenderer
import xyz.aerii.athen.api.rendering.level.impl.queue.impl.LevelQueueImpl
import xyz.aerii.athen.api.rendering.level.internal.annotations.impl.LevelRenderer
import xyz.aerii.library.api.client
import kotlin.math.sqrt

//? if >= 26.1 {
/*import net.minecraft.util.LightCoordsUtil
*///? } else {
import net.minecraft.client.renderer.LightTexture
//? }

@LevelRenderer
object BeamRenderer : ILevelRenderer {
    private val beam = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/beacon_beam.png")

    override fun render(poseStack: PoseStack, pose: PoseStack.Pose, consumers: MultiBufferSource.BufferSource) {
        fn(poseStack, consumers)
    }

    private fun fn(poseStack: PoseStack, consumers: MultiBufferSource.BufferSource) {
        if (LevelQueueImpl.beams.isEmpty()) return

        val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val animationTime = ((client.level?.gameTime ?: 0L) % 40).toFloat() + partialTick
        val scoping = client.player?.isScoping ?: false
        val s = -1f + Mth.frac(-animationTime * 0.2f - Mth.floor(-animationTime * 0.1f).toFloat())

        //? if >= 1.21.11 {
        /*val opaqueType = RenderTypes.beaconBeam(beam, false)
        val translucentType = RenderTypes.beaconBeam(beam, true)
        *///? } else {
        val opaqueType = RenderType.beaconBeam(beam, false)
        val translucentType = RenderType.beaconBeam(beam, true)
        //? }

        for (beacon in LevelQueueImpl.beams) {
            val pos = beacon.pos
            val hdx = pos.x + 0.5
            val hdz = pos.z + 0.5
            val radiusScale = if (scoping) 1f else maxOf(1f, sqrt(hdx * hdx + hdz * hdz).toFloat() / 96f)
            val beamRadius = 0.2f * radiusScale
            val glowRadius = 0.25f * radiusScale

            poseStack.pushPose()
            poseStack.translate(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
            val pose = poseStack.last()

            poseStack.pushPose()
            poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25f - 45f))
            pose.part(consumers.getBuffer(opaqueType), beacon.color, 0f, beamRadius, beamRadius, 0f, -beamRadius, 0f, 0f, -beamRadius, 320 * (0.5f / beamRadius) + s, s)
            poseStack.popPose()

            pose.part(consumers.getBuffer(translucentType), ARGB.color(32, beacon.color), -glowRadius, -glowRadius, glowRadius, -glowRadius, -glowRadius, glowRadius, glowRadius, glowRadius, 320f + s, s)

            poseStack.popPose()
        }

        consumers.endBatch(opaqueType)
        consumers.endBatch(translucentType)
    }

    private fun PoseStack.Pose.part(consumer: VertexConsumer, color: Int, x1: Float, z1: Float, x2: Float, z2: Float, x3: Float, z3: Float, x4: Float, z4: Float, minV: Float, maxV: Float) {
        quad(consumer, color, x1, z1, x2, z2, minV, maxV)
        quad(consumer, color, x4, z4, x3, z3, minV, maxV)
        quad(consumer, color, x2, z2, x4, z4, minV, maxV)
        quad(consumer, color, x3, z3, x1, z1, minV, maxV)
    }

    private fun PoseStack.Pose.quad(consumer: VertexConsumer, color: Int, minX: Float, minZ: Float, maxX: Float, maxZ: Float, minV: Float, maxV: Float) {
        vertex(consumer, color, 320, minX, minZ, 1f, minV)
        vertex(consumer, color, 0, minX, minZ, 1f, maxV)
        vertex(consumer, color, 0, maxX, maxZ, 0f, maxV)
        vertex(consumer, color, 320, maxX, maxZ, 0f, minV)
    }

    private fun PoseStack.Pose.vertex(consumer: VertexConsumer, color: Int, y: Int, x: Float, z: Float, u: Float, v: Float) {
        consumer
            .addVertex(this, x, y.toFloat(), z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            //~ if >= 26.1 'LightTexture' -> 'LightCoordsUtil'
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(this, 0f, 1f, 0f)
    }
}