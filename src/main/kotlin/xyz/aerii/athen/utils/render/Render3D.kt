/*
 * Heavily inspired by how OdinFabric batches the 3D rendering.
 * Contains code from OdinFabric as well.
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
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStack
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStacks
import dev.deftu.omnicore.api.client.render.vertex.OmniBufferBuilder
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer./*? >= 1.21.11 {*//*rendertype.RenderTypes*//*? } else {*/RenderType/*? }*/
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.utils.markerAABB
import xyz.aerii.athen.utils.render.pipelines.StarredPipelines
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

//? >= 1.21.11 {
/*import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
*///? }

private data class QueuedLine(val start: Vec3, val end: Vec3, val color: Color, val width: Float)
private data class QueuedBox(val aabb: AABB, val color: Color, val width: Float)
private data class QueuedFilledBox(val aabb: AABB, val color: Color)
private data class QueuedBeaconBeam(val pos: BlockPos, val color: Int)
private data class QueuedText(val text: String, val pos: Vec3, val color: Int, val bgColor: Int, val scale: Float, val shadow: Boolean, val depth: Boolean)
private data class QueuedCircle(val center: Vec3, val radius: Double, val segments: Int, val color: Color, val width: Float, val normal: Vec3)
private data class QueuedFilledCircle(val center: Vec3, val radius: Double, val segments: Int, val color: Color, val normal: Vec3)

private class RenderQueue {
    val linesDepth = mutableListOf<QueuedLine>()
    val linesNoDepth = mutableListOf<QueuedLine>()
    val boxesDepth = mutableListOf<QueuedBox>()
    val boxesNoDepth = mutableListOf<QueuedBox>()
    val filledBoxesDepth = mutableListOf<QueuedFilledBox>()
    val filledBoxesNoDepth = mutableListOf<QueuedFilledBox>()
    val circlesDepth = mutableListOf<QueuedCircle>()
    val circlesNoDepth = mutableListOf<QueuedCircle>()
    val filledCirclesDepth = mutableListOf<QueuedFilledCircle>()
    val filledCirclesNoDepth = mutableListOf<QueuedFilledCircle>()
    val beaconBeams = mutableListOf<QueuedBeaconBeam>()
    val texts = mutableListOf<QueuedText>()

    fun clear() {
        linesDepth.clear()
        linesNoDepth.clear()
        boxesDepth.clear()
        boxesNoDepth.clear()
        filledBoxesDepth.clear()
        filledBoxesNoDepth.clear()
        circlesDepth.clear()
        circlesNoDepth.clear()
        filledCirclesDepth.clear()
        filledCirclesNoDepth.clear()
        beaconBeams.clear()
        texts.clear()
    }
}

object Render3D {
    private val beam = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/beacon_beam.png")
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
            flushCircles(poseStack)
            flushFilledCircles(poseStack)

            flushBeaconBeams(pose, camera, consumers)
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

    private fun tangentsFor(normal: Vec3): Pair<Vec3, Vec3> {
        val n = normal.normalize()
        val arbitrary = if (abs(n.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val u = n.cross(arbitrary).normalize()
        val v = n.cross(u).normalize()
        return u to v
    }

    private fun circlePoints(center: Vec3, radius: Double, segments: Int, normal: Vec3): Array<Vec3> {
        val (u, v) = tangentsFor(normal)
        return Array(segments + 1) { i ->
            val angle = 2.0 * Math.PI * i / segments
            val cos = cos(angle)
            val sin = sin(angle)
            Vec3(
                center.x + radius * (u.x * cos + v.x * sin),
                center.y + radius * (u.y * cos + v.y * sin),
                center.z + radius * (u.z * cos + v.z * sin)
            )
        }
    }

    //? >= 1.21.11 {
    /*private fun flushLines(poseStack: OmniPoseStack) { // does not need posestack
        forDepth(queue.linesDepth, queue.linesNoDepth) { depth, lines ->
            if (lines.isEmpty()) return@forDepth

            for (l in lines) {
                Gizmos.line(l.start, l.end, l.color.rgb, l.width).apply {
                    if (!depth) setAlwaysOnTop()
                }
            }
        }
    }
    *///? } else {
    private fun flushLines(poseStack: OmniPoseStack) {
        forDepth(queue.linesDepth, queue.linesNoDepth) { depth, lines ->
            if (lines.isEmpty()) return@forDepth

            val pipeline = if (depth) StarredPipelines.lines else StarredPipelines.linesNoDepth

            for (line in lines) {
                val buffer = pipeline.createBufferBuilder()

                addLineVertices(
                    buffer,
                    poseStack,
                    line.start.x, line.start.y, line.start.z,
                    line.end.x, line.end.y, line.end.z,
                    line.color
                )

                buffer.buildOrThrow().drawAndClose(pipeline) { setLineWidth(line.width) }
            }
        }
    }
    //? }

    //? >= 1.21.11 {
    /*private fun flushBoxes(poseStack: OmniPoseStack) { // does not need posestack
        forDepth(queue.boxesDepth, queue.boxesNoDepth) { depth, boxes ->
            if (boxes.isEmpty()) return@forDepth

            for (b in boxes) {
                Gizmos.cuboid(b.aabb, GizmoStyle.stroke(b.color.rgb, b.width)).apply {
                    if (!depth) setAlwaysOnTop()
                }
            }
        }
    }
    *///? } else {
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
    //? }

    //? >= 1.21.11 {
    /*private fun flushFilledBoxes(poseStack: OmniPoseStack) { // does not need posestack
        forDepth(queue.filledBoxesDepth, queue.filledBoxesNoDepth) { depth, boxes ->
            if (boxes.isEmpty()) return@forDepth

            for (b in boxes) {
                Gizmos.cuboid(b.aabb, GizmoStyle.fill(b.color.rgb)).apply {
                    if (!depth) setAlwaysOnTop()
                }
            }
        }
    }
    *///? } else {
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
    //? }

    //? >= 1.21.11 {
    /*private fun flushCircles(poseStack: OmniPoseStack) {
        forDepth(queue.circlesDepth, queue.circlesNoDepth) { depth, circles ->
            if (circles.isEmpty()) return@forDepth

            for (c in circles) {
                Gizmos.circle(c.center, c.radius.toFloat(), GizmoStyle.stroke(c.color.rgb, c.width))
            }
        }
    }
    *///? } else {
    private fun flushCircles(poseStack: OmniPoseStack) {
        forDepth(queue.circlesDepth, queue.circlesNoDepth) { depth, circles ->
            if (circles.isEmpty()) return@forDepth

            val pipeline = if (depth) StarredPipelines.lines else StarredPipelines.linesNoDepth

            for (circle in circles) {
                val buffer = pipeline.createBufferBuilder()
                val pts = circlePoints(circle.center, circle.radius, circle.segments, circle.normal)

                for (i in 0 until circle.segments) {
                    addLineVertices(
                        buffer, poseStack,
                        pts[i].x, pts[i].y, pts[i].z,
                        pts[i + 1].x, pts[i + 1].y, pts[i + 1].z,
                        circle.color
                    )
                }

                buffer.buildOrThrow().drawAndClose(pipeline) { setLineWidth(circle.width) }
            }
        }
    }
    //? }

    //? >= 1.21.11 {
    /*private fun flushFilledCircles(poseStack: OmniPoseStack) {
        forDepth(queue.filledCirclesDepth, queue.filledCirclesNoDepth) { depth, circles ->
            if (circles.isEmpty()) return@forDepth

            for (c in circles) {
                Gizmos.circle(c.center, c.radius.toFloat(), GizmoStyle.fill(c.color.rgb))
            }
        }
    }
    *///? } else {
    private fun flushFilledCircles(poseStack: OmniPoseStack) {
        forDepth(queue.filledCirclesDepth, queue.filledCirclesNoDepth) { depth, circles ->
            if (circles.isEmpty()) return@forDepth

            val pipeline = if (depth) StarredPipelines.triangleStrip else StarredPipelines.triangleStripNoDepth
            val buffer = pipeline.createBufferBuilder()

            for (circle in circles) {
                val pts = circlePoints(circle.center, circle.radius, circle.segments, circle.normal)
                val n = circle.segments

                val indices = buildList {
                    var lo = 0
                    var hi = n - 1
                    while (lo <= hi) {
                        add(lo++)
                        if (lo <= hi) add(hi--)
                    }
                }

                val first = pts[indices[0]]
                buffer.vertex(poseStack, first.x, first.y, first.z).color(circle.color).next()
                buffer.vertex(poseStack, first.x, first.y, first.z).color(circle.color).next()

                for (idx in indices) {
                    val p = pts[idx]
                    buffer.vertex(poseStack, p.x, p.y, p.z).color(circle.color).next()
                }

                val last = pts[indices.last()]
                buffer.vertex(poseStack, last.x, last.y, last.z).color(circle.color).next()
                buffer.vertex(poseStack, last.x, last.y, last.z).color(circle.color).next()
            }

            buffer.buildOrThrow().drawAndClose(pipeline)
        }
    }
    //? }

    /**
     * @see net.minecraft.client.renderer.blockentity.BeaconRenderer
     */
    private fun flushBeaconBeams(pose: PoseStack, camera: Vec3, consumers: MultiBufferSource.BufferSource) {
        if (queue.beaconBeams.isEmpty()) return

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

        for (beacon in queue.beaconBeams) {
            val pos = beacon.pos
            val hdx = pos.x + 0.5
            val hdz = pos.z + 0.5
            val radiusScale = if (scoping) 1f else maxOf(1f, sqrt(hdx * hdx + hdz * hdz).toFloat() / 96f)
            val beamRadius = 0.2f * radiusScale
            val glowRadius = 0.25f * radiusScale

            pose.pushPose()
            pose.translate(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)

            pose.pushPose()
            pose.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25f - 45f))
            renderBeaconPart(pose.last(), consumers.getBuffer(opaqueType), beacon.color, 0f, beamRadius, beamRadius, 0f, -beamRadius, 0f, 0f, -beamRadius, 320 * (0.5f / beamRadius) + s, s)
            pose.popPose()

            renderBeaconPart(pose.last(), consumers.getBuffer(translucentType), ARGB.color(32, beacon.color), -glowRadius, -glowRadius, glowRadius, -glowRadius, -glowRadius, glowRadius, glowRadius, glowRadius, 320f + s, s)

            pose.popPose()
        }

        consumers.endBatch(opaqueType)
        consumers.endBatch(translucentType)
    }

    private fun renderBeaconPart(pose: PoseStack.Pose, consumer: VertexConsumer, color: Int, x1: Float, z1: Float, x2: Float, z2: Float, x3: Float, z3: Float, x4: Float, z4: Float, minV: Float, maxV: Float) {
        renderBeaconQuad(pose, consumer, color, x1, z1, x2, z2, minV, maxV)
        renderBeaconQuad(pose, consumer, color, x4, z4, x3, z3, minV, maxV)
        renderBeaconQuad(pose, consumer, color, x2, z2, x4, z4, minV, maxV)
        renderBeaconQuad(pose, consumer, color, x3, z3, x1, z1, minV, maxV)
    }

    private fun renderBeaconQuad(pose: PoseStack.Pose, consumer: VertexConsumer, color: Int, minX: Float, minZ: Float, maxX: Float, maxZ: Float, minV: Float, maxV: Float) {
        addBeaconVertex(pose, consumer, color, 320, minX, minZ, 1f, minV)
        addBeaconVertex(pose, consumer, color, 0, minX, minZ, 1f, maxV)
        addBeaconVertex(pose, consumer, color, 0, maxX, maxZ, 0f, maxV)
        addBeaconVertex(pose, consumer, color, 320, maxX, maxZ, 0f, minV)
    }

    private fun addBeaconVertex(pose: PoseStack.Pose, consumer: VertexConsumer, color: Int, y: Int, x: Float, z: Float, u: Float, v: Float) {
        consumer.addVertex(pose, x, y.toFloat(), z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f)
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
        shadow: Boolean = true,
        increase: Boolean = false
    ) {
        var scale = scale
        if (increase) {
            val p = client.gameRenderer.mainCamera.position/*? >= 1.21.11 { *//*()*//*? }*/
            scale *= p.distanceTo(pos).toFloat() / 3f
        }

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
    fun drawWaypoint(
        pos: BlockPos,
        color: Color,
        aabb: AABB = pos.markerAABB(),
        depthTest: Boolean = false
    ) {
        drawFilledBox(aabb, color, depthTest)
        drawBeaconBeam(pos, color.rgb)
    }

    @JvmStatic
    @JvmOverloads
    fun drawWaypoint(
        pos: BlockPos,
        color: Color,
        lineWidth: Float,
        aabb: AABB = pos.markerAABB(),
        depthTest: Boolean = false
    ) {
        drawBox(aabb, color, lineWidth, depthTest)
        drawBeaconBeam(pos, color.rgb)
    }

    @JvmStatic
    fun drawBeaconBeam(pos: BlockPos, color: Int) {
        queue.beaconBeams.add(QueuedBeaconBeam(pos, color))
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
        lineQueue.add(QueuedLine(from, to, color, lineWidth))
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
            lineQueue.add(QueuedLine(from, to, color, lineWidth))
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

    @JvmStatic
    @JvmOverloads
    fun drawCircle(
        center: Vec3,
        radius: Double,
        color: Color,
        segments: Int = 64,
        normal: Vec3 = Vec3(0.0, 1.0, 0.0),
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        val circleQueue = if (depthTest) queue.circlesDepth else queue.circlesNoDepth
        circleQueue.add(QueuedCircle(center, radius, segments, color, lineWidth, normal))
    }

    @JvmStatic
    @JvmOverloads
    fun drawFilledCircle(
        center: Vec3,
        radius: Double,
        color: Color,
        segments: Int = 64,
        normal: Vec3 = Vec3(0.0, 1.0, 0.0),
        depthTest: Boolean = true
    ) {
        val filledQueue = if (depthTest) queue.filledCirclesDepth else queue.filledCirclesNoDepth
        filledQueue.add(QueuedFilledCircle(center, radius, segments, color, normal))
    }

    @JvmStatic
    @JvmOverloads
    fun drawStyledCircle(
        center: Vec3,
        radius: Double,
        color: Color,
        style: CircleStyle = CircleStyle.BOTH,
        segments: Int = 64,
        normal: Vec3 = Vec3(0.0, 1.0, 0.0),
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        when (style) {
            CircleStyle.FILLED -> drawFilledCircle(center, radius, color, segments, normal, depthTest)
            CircleStyle.OUTLINED -> drawCircle(center, radius, color, segments, normal, lineWidth, depthTest)
            CircleStyle.BOTH -> {
                val filledColor = Color(color.red, color.green, color.blue, (color.alpha * 0.5f).toInt())
                drawFilledCircle(center, radius, filledColor, segments, normal, depthTest)
                drawCircle(center, radius, color, segments, normal, lineWidth, depthTest)
            }
        }
    }

    enum class BoxStyle {
        FILLED,
        OUTLINED,
        BOTH
    }

    enum class CircleStyle {
        FILLED,
        OUTLINED,
        BOTH
    }
}