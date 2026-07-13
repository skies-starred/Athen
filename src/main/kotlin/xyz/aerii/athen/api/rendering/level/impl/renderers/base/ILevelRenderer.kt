package xyz.aerii.athen.api.rendering.level.impl.renderers.base

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import xyz.aerii.athen.api.rendering.level.impl.data.base.ILevelExtractable
import xyz.aerii.athen.api.rendering.level.impl.queue.data.ExtractedQueue
import kotlin.math.sqrt

interface ILevelRenderer {
    fun render(poseStack: PoseStack, pose: PoseStack.Pose, consumers: MultiBufferSource.BufferSource)

    fun <T : ILevelExtractable> forDepth(list: ExtractedQueue<T>, block: (depth: Boolean, list: List<T>) -> Unit) {
        if (list.depth.isNotEmpty()) block(true, list.depth)
        if (list.depthless.isNotEmpty()) block(false, list.depthless)
    }

    fun PoseStack.Pose.vertex(
        buffer: VertexConsumer,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        width: Float,
        color: Int
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        val length = sqrt(dx * dx + dy * dy + dz * dz)

        val nx = if (length > 0) dx / length else 0f
        val ny = if (length > 0) dy / length else 0f
        val nz = if (length > 0) dz / length else 0f

        buffer.addVertex(this, x1, y1, z1).setColor(color).setNormal(this, nx, ny, nz)/*? >= 1.21.11 {*/.setLineWidth(width)/*? }*/
        buffer.addVertex(this, x2, y2, z2).setColor(color).setNormal(this, nx, ny, nz)/*? >= 1.21.11 {*/.setLineWidth(width)/*? }*/
    }
}