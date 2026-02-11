/*
 * Heavily inspired by how OdinFabric batches the 3D rendering
 *
 * OdinFabric is under BSD 3-Clause License:
 * BSD 3-Clause License
 *
 * Copyright (c) 2025, odtheking
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Athen is also under BSD 3-Clause License.
 * Read our license at: https://github.com/skies-starred/Athen/blob/master/LICENSE
 */

@file:Suppress("UNUSED", "UnstableApiUsage")

package xyz.aerii.athen.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStack
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStacks
import dev.deftu.omnicore.api.client.render.vertex.OmniBufferBuilder
import dev.deftu.omnicore.api.client.render.vertex.OmniBufferBuilders
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.utils.render.pipelines.StarredPipelines
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class QueuedLine(val x1: Double, val y1: Double, val z1: Double, val x2: Double, val y2: Double, val z2: Double, val color: Color, val width: Float)
private data class QueuedBox(val aabb: AABB, val color: Color, val width: Float)
private data class QueuedFilledBox(val aabb: AABB, val color: Color)
private data class QueuedText(val text: String, val pos: Vec3, val color: Int, val bgColor: Int, val scale: Float, val shadow: Boolean, val depth: Boolean)

private class RenderQueue {
    val linesDepth = mutableListOf<QueuedLine>()
    val linesNoDepth = mutableListOf<QueuedLine>()
    val boxesDepth = mutableListOf<QueuedBox>()
    val boxesNoDepth = mutableListOf<QueuedBox>()
    val filledBoxesDepth = mutableListOf<QueuedFilledBox>()
    val filledBoxesNoDepth = mutableListOf<QueuedFilledBox>()
    val texts = mutableListOf<QueuedText>()

    fun clear() {
        linesDepth.clear()
        linesNoDepth.clear()
        boxesDepth.clear()
        boxesNoDepth.clear()
        filledBoxesDepth.clear()
        filledBoxesNoDepth.clear()
        texts.clear()
    }
}

object Render3D {
    private val queue = RenderQueue()

    private val BOX_EDGES = arrayOf(
        intArrayOf(0, 0, 0, 1, 0, 0), intArrayOf(1, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 0, 0, 1), intArrayOf(0, 0, 1, 0, 0, 0),
        intArrayOf(0, 1, 0, 1, 1, 0), intArrayOf(1, 1, 0, 1, 1, 1),
        intArrayOf(1, 1, 1, 0, 1, 1), intArrayOf(0, 1, 1, 0, 1, 0),
        intArrayOf(0, 0, 0, 0, 1, 0), intArrayOf(1, 0, 0, 1, 1, 0),
        intArrayOf(1, 0, 1, 1, 1, 1), intArrayOf(0, 0, 1, 0, 1, 1)
    )

    init {
        on<WorldRenderEvent.Render> {
            val camera = client.gameRenderer.mainCamera?.position/*? >= 1.21.11 { *//*()*//*? }*/ ?: return@on

            pose.pushPose()
            pose.translate(-camera.x, -camera.y, -camera.z)

            val poseStack = OmniPoseStacks.vanilla(pose)
            flushLines(poseStack)
            flushBoxes(poseStack)
            flushFilledBoxes(poseStack)
            flushTexts(pose, consumers)

            pose.popPose()
            queue.clear()
        }
    }

    // <editor-fold desc = "Internal">
    private inline fun <T> forDepth(
        depthList: List<T>,
        noDepthList: List<T>,
        block: (depth: Boolean, list: List<T>) -> Unit
    ) {
        if (depthList.isNotEmpty()) block(true, depthList)
        if (noDepthList.isNotEmpty()) block(false, noDepthList)
    }

    private fun flushLines(poseStack: OmniPoseStack) {
        forDepth(queue.linesDepth, queue.linesNoDepth) { depth, lines ->
            if (lines.isEmpty()) return@forDepth

            val pipeline = if (depth) StarredPipelines.lines else StarredPipelines.linesNoDepth

            for (line in lines) {
                val buffer = pipeline.createBufferBuilder()

                addLineVertices(
                    buffer,
                    poseStack,
                    line.x1, line.y1, line.z1,
                    line.x2, line.y2, line.z2,
                    line.color
                )

                buffer.buildOrThrow().drawAndClose(pipeline) { setLineWidth(line.width) }
            }
        }
    }

    private fun flushBoxes(poseStack: OmniPoseStack) {
        forDepth(queue.boxesDepth, queue.boxesNoDepth) { depth, boxes ->
            if (boxes.isEmpty()) return@forDepth
            val pipeline = if (depth) StarredPipelines.lines else StarredPipelines.linesNoDepth

            for (box in boxes) {
                val buffer = pipeline.createBufferBuilder()

                val aabb = box.aabb
                val x0 = aabb.minX
                val x1 = aabb.maxX
                val y0 = aabb.minY
                val y1 = aabb.maxY
                val z0 = aabb.minZ
                val z1 = aabb.maxZ

                for (edge in BOX_EDGES) {
                    addLineVertices(
                        buffer,
                        poseStack,
                        if (edge[0] == 0) x0 else x1,
                        if (edge[1] == 0) y0 else y1,
                        if (edge[2] == 0) z0 else z1,
                        if (edge[3] == 0) x0 else x1,
                        if (edge[4] == 0) y0 else y1,
                        if (edge[5] == 0) z0 else z1,
                        box.color
                    )
                }

                buffer.buildOrThrow().drawAndClose(pipeline) { setLineWidth(box.width) }
            }
        }
    }

    private fun flushFilledBoxes(poseStack: OmniPoseStack) {
        forDepth(queue.filledBoxesDepth, queue.filledBoxesNoDepth) { depth, boxes ->
            if (boxes.isEmpty()) return@forDepth

            val pipeline = if (depth) StarredPipelines.triangleStrip else StarredPipelines.triangleStripNoDepth
            val buffer = pipeline.createBufferBuilder()

            for (box in boxes) {
                val aabb = box.aabb
                val x0 = aabb.minX
                val x1 = aabb.maxX
                val y0 = aabb.minY
                val y1 = aabb.maxY
                val z0 = aabb.minZ
                val z1 = aabb.maxZ

                buffer.vertex(poseStack, x0, y0, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y0, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y0, z0).color(box.color).next()

                buffer.vertex(poseStack, x0, y0, z1).color(box.color).next()
                buffer.vertex(poseStack, x0, y1, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y1, z1).color(box.color).next()

                buffer.vertex(poseStack, x0, y1, z1).color(box.color).next()

                buffer.vertex(poseStack, x0, y0, z1).color(box.color).next()
                buffer.vertex(poseStack, x1, y1, z1).color(box.color).next()
                buffer.vertex(poseStack, x1, y0, z1).color(box.color).next()

                buffer.vertex(poseStack, x1, y0, z1).color(box.color).next()

                buffer.vertex(poseStack, x1, y0, z0).color(box.color).next()
                buffer.vertex(poseStack, x1, y1, z1).color(box.color).next()
                buffer.vertex(poseStack, x1, y1, z0).color(box.color).next()

                buffer.vertex(poseStack, x1, y1, z0).color(box.color).next()

                buffer.vertex(poseStack, x1, y0, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y1, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y0, z0).color(box.color).next()

                buffer.vertex(poseStack, x0, y0, z0).color(box.color).next()

                buffer.vertex(poseStack, x1, y0, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y0, z1).color(box.color).next()
                buffer.vertex(poseStack, x1, y0, z1).color(box.color).next()

                buffer.vertex(poseStack, x1, y0, z1).color(box.color).next()

                buffer.vertex(poseStack, x0, y1, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y1, z0).color(box.color).next()
                buffer.vertex(poseStack, x0, y1, z1).color(box.color).next()
                buffer.vertex(poseStack, x1, y1, z0).color(box.color).next()
                buffer.vertex(poseStack, x1, y1, z1).color(box.color).next()

                buffer.vertex(poseStack, x1, y1, z1).color(box.color).next()
                buffer.vertex(poseStack, x1, y1, z1).color(box.color).next()
            }

            buffer.buildOrThrow().drawAndClose(pipeline)
        }
    }

    private fun flushTexts(pose: PoseStack, consumers: MultiBufferSource.BufferSource) {
        val cam = client.gameRenderer.mainCamera

        for (text in queue.texts) {
            pose.pushPose()

            val scale = text.scale * 0.025f
            pose.translate(text.pos.x, text.pos.y, text.pos.z)
            pose.mulPose(cam.rotation())
            pose.scale(scale, -scale, scale)

            val matrix = pose.last().pose()

            client.font.drawInBatch(
                text.text,
                -client.font.width(text.text) / 2f,
                0f,
                text.color,
                text.shadow,
                matrix,
                consumers,
                if (text.depth) Font.DisplayMode.NORMAL else Font.DisplayMode.SEE_THROUGH,
                text.bgColor,
                LightTexture.FULL_BRIGHT
            )

            pose.popPose()
        }
    }

    private fun addLineVertices(
        buffer: OmniBufferBuilder,
        poseStack: OmniPoseStack,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        color: Color
    ) {
        val dx = (x2 - x1).toFloat()
        val dy = (y2 - y1).toFloat()
        val dz = (z2 - z1).toFloat()
        val length = sqrt(dx * dx + dy * dy + dz * dz)

        val nx = if (length > 0) dx / length else 0f
        val ny = if (length > 0) dy / length else 0f
        val nz = if (length > 0) dz / length else 0f

        buffer.vertex(poseStack, x1, y1, z1).color(color).normal(poseStack, nx, ny, nz).next()
        buffer.vertex(poseStack, x2, y2, z2).color(color).normal(poseStack, nx, ny, nz).next()
    }
    // </editor-fold>

    @JvmStatic
    @JvmOverloads
    fun drawString(
        text: String,
        pos: Vec3,
        color: Int = -1,
        backgroundColor: Int = 0,
        scale: Float = 1f,
        depthTest: Boolean = true,
        shadow: Boolean = true
    ) {
        queue.texts.add(QueuedText(text, pos, color, backgroundColor, scale, shadow, depthTest))
    }

    @JvmStatic
    @JvmOverloads
    fun drawBox(
        aabb: AABB,
        color: Color,
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        val boxQueue = if (depthTest) queue.boxesDepth else queue.boxesNoDepth
        boxQueue.add(QueuedBox(aabb, color, lineWidth))
    }

    @JvmStatic
    @JvmOverloads
    fun drawFilledBox(
        aabb: AABB,
        color: Color,
        depthTest: Boolean = true
    ) {
        val filledQueue = if (depthTest) queue.filledBoxesDepth else queue.filledBoxesNoDepth
        filledQueue.add(QueuedFilledBox(aabb, color))
    }

    @JvmStatic
    @JvmOverloads
    fun drawLine(
        from: Vec3,
        to: Vec3,
        color: Color,
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        val lineQueue = if (depthTest) queue.linesDepth else queue.linesNoDepth
        lineQueue.add(QueuedLine(from.x, from.y, from.z, to.x, to.y, to.z, color, lineWidth))
    }

    @JvmStatic
    @JvmOverloads
    fun drawLines(
        points: List<Vec3>,
        color: Color,
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        if (points.size < 2) return
        val lineQueue = if (depthTest) queue.linesDepth else queue.linesNoDepth

        for (i in 0 until points.size - 1) {
            val from = points[i]
            val to = points[i + 1]
            lineQueue.add(QueuedLine(from.x, from.y, from.z, to.x, to.y, to.z, color, lineWidth))
        }
    }

    @JvmStatic
    @JvmOverloads
    fun drawCylinder(
        center: Vec3,
        radius: Float,
        height: Float,
        color: Color,
        segments: Int = 32,
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        val lineQueue = if (depthTest) queue.linesDepth else queue.linesNoDepth
        val angleStep = 2.0 * Math.PI / segments

        for (i in 0 until segments) {
            val angle1 = i * angleStep
            val angle2 = (i + 1) * angleStep

            val x1 = radius * cos(angle1)
            val z1 = radius * sin(angle1)
            val x2 = radius * cos(angle2)
            val z2 = radius * sin(angle2)

            lineQueue.add(QueuedLine(center.x + x1, center.y + height, center.z + z1, center.x + x2, center.y + height, center.z + z2, color, lineWidth))
            lineQueue.add(QueuedLine(center.x + x1, center.y, center.z + z1, center.x + x2, center.y, center.z + z2, color, lineWidth))
            lineQueue.add(QueuedLine(center.x + x1, center.y, center.z + z1, center.x + x1, center.y + height, center.z + z1, color, lineWidth))
        }
    }

    @JvmStatic
    @JvmOverloads
    fun drawStyledBox(
        aabb: AABB,
        color: Color,
        style: BoxStyle = BoxStyle.BOTH,
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        when (style) {
            BoxStyle.FILLED -> drawFilledBox(aabb, color, depthTest)
            BoxStyle.OUTLINED -> drawBox(aabb, color, lineWidth, depthTest)
            BoxStyle.BOTH -> {
                val filledColor = Color(color.red, color.green, color.blue, (color.alpha * 0.5f).toInt())
                drawFilledBox(aabb, filledColor, depthTest)
                drawBox(aabb, color, lineWidth, depthTest)
            }
        }
    }

    enum class BoxStyle {
        FILLED,
        OUTLINED,
        BOTH
    }
}