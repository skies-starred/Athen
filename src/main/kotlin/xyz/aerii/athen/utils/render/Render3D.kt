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

@file:Suppress("UNUSED", "UnstableApiUsage", "FunctionName")

package xyz.aerii.athen.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer./*? >= 1.21.11 {*//*rendertype.RenderTypes*//*? } else {*/RenderType/*? }*/
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import xyz.aerii.athen.events.WorldRenderEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.utils.markerAABB
import xyz.aerii.athen.utils.render.pipelines.StarredRenderTypes
import xyz.aerii.library.api.client
import xyz.aerii.library.utils.literal
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class QueuedLine(val start: Vector3f, val end: Vector3f, val color: Int, val width: Float)
private data class QueuedBox(val aabb: AABB, val color: Int, val width: Float)
private data class QueuedBeaconBeam(val pos: BlockPos, val color: Int)
private data class QueuedText(val text: Component, val pos: Vec3, val color: Int, val bgColor: Int, val scale: Float, val shadow: Boolean, val depth: Boolean)
private data class QueuedCircle(val center: Vec3, val radius: Double, val segments: Int, val color: Int, val width: Float, val normal: Vec3)

private object RenderQueue {
    val linesDepth = mutableListOf<QueuedLine>()
    val linesNoDepth = mutableListOf<QueuedLine>()
    val boxesDepth = mutableListOf<QueuedBox>()
    val boxesNoDepth = mutableListOf<QueuedBox>()
    val filledBoxesDepth = mutableListOf<QueuedBox>()
    val filledBoxesNoDepth = mutableListOf<QueuedBox>()
    val circlesDepth = mutableListOf<QueuedCircle>()
    val circlesNoDepth = mutableListOf<QueuedCircle>()
    val filledCirclesDepth = mutableListOf<QueuedCircle>()
    val filledCirclesNoDepth = mutableListOf<QueuedCircle>()
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

            val last = pose.last()
            last.lines(consumers)
            last.boxes(consumers)
            last.boxes0(consumers)
            last.circles(consumers)
            last.circles0(consumers)

            pose.beacons(camera, consumers)
            pose.texts(consumers)

            pose.popPose()
            RenderQueue.clear()
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

    private fun Vec3.tangents(): Pair<Vec3, Vec3> {
        val n = normalize()
        val arbitrary = if (abs(n.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val u = n.cross(arbitrary).normalize()
        val v = n.cross(u).normalize()
        return u to v
    }

    private fun Vec3.points( radius: Double, segments: Int, normal: Vec3): Array<Vector3f> {
        val (u, v) = normal.tangents()
        return Array(segments + 1) { i ->
            val angle = 2.0 * Math.PI * i / segments
            val cos = cos(angle)
            val sin = sin(angle)
            Vector3f(
                (x + radius * (u.x * cos + v.x * sin)).toFloat(),
                (y + radius * (u.y * cos + v.y * sin)).toFloat(),
                (z + radius * (u.z * cos + v.z * sin)).toFloat()
            )
        }
    }

    private fun PoseStack.Pose.lines(consumers: MultiBufferSource.BufferSource) {
        forDepth(RenderQueue.linesDepth, RenderQueue.linesNoDepth) { depth, lines ->
            if (lines.isEmpty()) return@forDepth
            val renderType = if (depth) StarredRenderTypes.LINES else StarredRenderTypes.LINES_DEPTHLESS
            val buffer = consumers.getBuffer(renderType)

            for (line in lines) {
                vertex(
                    buffer,
                    line.start.x,
                    line.start.y,
                    line.start.z,
                    line.end.x,
                    line.end.y,
                    line.end.z,
                    line.width,
                    line.color
                )
            }
        }
    }

    private fun PoseStack.Pose.boxes(consumers: MultiBufferSource.BufferSource) {
        forDepth(RenderQueue.boxesDepth, RenderQueue.boxesNoDepth) { depth, boxes ->
            if (boxes.isEmpty()) return@forDepth
            val renderType = if (depth) StarredRenderTypes.LINES else StarredRenderTypes.LINES_DEPTHLESS
            val buffer = consumers.getBuffer(renderType)

            for (box in boxes) {
                val aabb = box.aabb
                val x0 = aabb.minX.toFloat()
                val x1 = aabb.maxX.toFloat()
                val y0 = aabb.minY.toFloat()
                val y1 = aabb.maxY.toFloat()
                val z0 = aabb.minZ.toFloat()
                val z1 = aabb.maxZ.toFloat()

                for (edge in BOX_EDGES) {
                    vertex(
                        buffer,
                        if (edge[0] == 0) x0 else x1,
                        if (edge[1] == 0) y0 else y1,
                        if (edge[2] == 0) z0 else z1,
                        if (edge[3] == 0) x0 else x1,
                        if (edge[4] == 0) y0 else y1,
                        if (edge[5] == 0) z0 else z1,
                        box.width,
                        box.color
                    )
                }
            }
        }
    }

    private fun PoseStack.Pose.boxes0(consumers: MultiBufferSource.BufferSource) {
        forDepth(RenderQueue.filledBoxesDepth, RenderQueue.filledBoxesNoDepth) { depth, boxes ->
            if (boxes.isEmpty()) return@forDepth
            val type = if (depth) StarredRenderTypes.DEBUG_FILLED else StarredRenderTypes.DEBUG_FILLED_DEPTHLESS
            val buffer = consumers.getBuffer(type)

            fun buff(x: Float, y: Float, z: Float, color: Int) {
                buffer.addVertex(this, x, y, z).setColor(color)
            }

            for (box in boxes) {
                val aabb = box.aabb
                val color = box.color

                val x1 = aabb.minX.toFloat()
                val x2 = aabb.maxX.toFloat()
                val y1 = aabb.minY.toFloat()
                val y2 = aabb.maxY.toFloat()
                val z1 = aabb.minZ.toFloat()
                val z2 = aabb.maxZ.toFloat()

                buff(x1, y1, z1, color)
                buff(x1, y1, z2, color)
                buff(x1, y2, z2, color)
                buff(x1, y2, z1, color)

                buff(x2, y1, z2, color)
                buff(x2, y1, z1, color)
                buff(x2, y2, z1, color)
                buff(x2, y2, z2, color)

                buff(x1, y1, z1, color)
                buff(x1, y2, z1, color)
                buff(x2, y2, z1, color)
                buff(x2, y1, z1, color)

                buff(x2, y1, z2, color)
                buff(x2, y2, z2, color)
                buff(x1, y2, z2, color)
                buff(x1, y1, z2, color)

                buff(x1, y1, z1, color)
                buff(x2, y1, z1, color)
                buff(x2, y1, z2, color)
                buff(x1, y1, z2, color)

                buff(x1, y2, z2, color)
                buff(x2, y2, z2, color)
                buff(x2, y2, z1, color)
                buff(x1, y2, z1, color)
            }
        }
    }

    private fun PoseStack.Pose.circles(consumers: MultiBufferSource.BufferSource) {
        forDepth(RenderQueue.circlesDepth, RenderQueue.circlesNoDepth) { depth, circles ->
            if (circles.isEmpty()) return@forDepth
            val type = if (depth) StarredRenderTypes.LINES else StarredRenderTypes.LINES_DEPTHLESS
            val buffer = consumers.getBuffer(type)

            for (circle in circles) {
                val pts = circle.center.points(circle.radius, circle.segments, circle.normal)

                for (i in 0 until circle.segments) {
                    vertex(
                        buffer,
                        pts[i].x,
                        pts[i].y,
                        pts[i].z,
                        pts[i + 1].x,
                        pts[i + 1].y,
                        pts[i + 1].z,
                        circle.width,
                        circle.color
                    )
                }
            }
        }
    }

    private fun PoseStack.Pose.circles0(consumers: MultiBufferSource.BufferSource) {
        forDepth(RenderQueue.filledCirclesDepth, RenderQueue.filledCirclesNoDepth) { depth, circles ->
            if (circles.isEmpty()) return@forDepth

            val type = if (depth) StarredRenderTypes.TRIANGLE_FAN else StarredRenderTypes.TRIANGLE_FAN_DEPTHLESS
            val buffer = consumers.getBuffer(type)

            for (circle in circles) {
                val (u, v) = circle.normal.tangents()
                val center = circle.center

                buffer.addVertex(this, center.x.toFloat(), center.y.toFloat(), center.z.toFloat()).setColor(circle.color)

                val segments = circle.segments
                val radius = circle.radius

                for (i in 0..segments) {
                    val angle = 2.0 * Math.PI * i / segments
                    val cos = cos(angle)
                    val sin = sin(angle)

                    val x = center.x + radius * (u.x * cos + v.x * sin)
                    val y = center.y + radius * (u.y * cos + v.y * sin)
                    val z = center.z + radius * (u.z * cos + v.z * sin)

                    buffer.addVertex(this, x.toFloat(), y.toFloat(), z.toFloat()).setColor(circle.color)
                }
            }
        }
    }

    /**
     * @see net.minecraft.client.renderer.blockentity.BeaconRenderer
     */
    private fun PoseStack.beacons(camera: Vec3, consumers: MultiBufferSource.BufferSource) {
        if (RenderQueue.beaconBeams.isEmpty()) return
        val last = last()

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

        for (beacon in RenderQueue.beaconBeams) {
            val pos = beacon.pos
            val hdx = pos.x + 0.5
            val hdz = pos.z + 0.5
            val radiusScale = if (scoping) 1f else maxOf(1f, sqrt(hdx * hdx + hdz * hdz).toFloat() / 96f)
            val beamRadius = 0.2f * radiusScale
            val glowRadius = 0.25f * radiusScale

            pushPose()
            translate(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)

            pushPose()
            mulPose(Axis.YP.rotationDegrees(animationTime * 2.25f - 45f))
            last.`beacon$part`(consumers.getBuffer(opaqueType), beacon.color, 0f, beamRadius, beamRadius, 0f, -beamRadius, 0f, 0f, -beamRadius, 320 * (0.5f / beamRadius) + s, s)
            popPose()

            last.`beacon$part`(consumers.getBuffer(translucentType), ARGB.color(32, beacon.color), -glowRadius, -glowRadius, glowRadius, -glowRadius, -glowRadius, glowRadius, glowRadius, glowRadius, 320f + s, s)

            popPose()
        }

        consumers.endBatch(opaqueType)
        consumers.endBatch(translucentType)
    }

    private fun PoseStack.Pose.`beacon$part`(consumer: VertexConsumer, color: Int, x1: Float, z1: Float, x2: Float, z2: Float, x3: Float, z3: Float, x4: Float, z4: Float, minV: Float, maxV: Float) {
        `beacon$quad`(consumer, color, x1, z1, x2, z2, minV, maxV)
        `beacon$quad`(consumer, color, x4, z4, x3, z3, minV, maxV)
        `beacon$quad`(consumer, color, x2, z2, x4, z4, minV, maxV)
        `beacon$quad`(consumer, color, x3, z3, x1, z1, minV, maxV)
    }

    private fun PoseStack.Pose.`beacon$quad`(consumer: VertexConsumer, color: Int, minX: Float, minZ: Float, maxX: Float, maxZ: Float, minV: Float, maxV: Float) {
        `beacon$vertex`(consumer, color, 320, minX, minZ, 1f, minV)
        `beacon$vertex`(consumer, color, 0, minX, minZ, 1f, maxV)
        `beacon$vertex`(consumer, color, 0, maxX, maxZ, 0f, maxV)
        `beacon$vertex`(consumer, color, 320, maxX, maxZ, 0f, minV)
    }

    private fun PoseStack.Pose.`beacon$vertex`(consumer: VertexConsumer, color: Int, y: Int, x: Float, z: Float, u: Float, v: Float) {
        consumer
            .addVertex(this, x, y.toFloat(), z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(this, 0f, 1f, 0f)
    }

    private fun PoseStack.texts(consumers: MultiBufferSource.BufferSource) {
        val cam = client.gameRenderer.mainCamera

        for (text in RenderQueue.texts) {
            pushPose()

            val pose = last().pose()
            val scale = text.scale * 0.025f

            pose.translate(text.pos.x.toFloat(), text.pos.y.toFloat(), text.pos.z.toFloat())
                .rotate(cam.rotation())
                .scale(scale, -scale, scale)

            client.font.drawInBatch(
                text.text,
                -client.font.width(text.text) / 2f,
                0f,
                text.color,
                text.shadow,
                pose,
                consumers,
                if (text.depth) Font.DisplayMode.NORMAL else Font.DisplayMode.SEE_THROUGH,
                text.bgColor,
                LightTexture.FULL_BRIGHT
            )

            popPose()
        }
    }

    private fun PoseStack.Pose.vertex(
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

        buffer.addVertex(this, x1, y1, z1).setColor(color).setNormal(this, nx, ny, nz)/*? >= 1.21.11 {*//*.setLineWidth(width)*//*? }*/
        buffer.addVertex(this, x2, y2, z2).setColor(color).setNormal(this, nx, ny, nz)/*? >= 1.21.11 {*//*.setLineWidth(width)*//*? }*/
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

        RenderQueue.texts.add(QueuedText(text.literal(), pos, color, backgroundColor, scale, shadow, depthTest))
    }

    @JvmStatic
    @JvmOverloads
    fun drawString(
        text: Component,
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

        RenderQueue.texts.add(QueuedText(text, pos, color, backgroundColor, scale, shadow, depthTest))
    }

    @JvmStatic
    @JvmOverloads
    fun drawBox(
        aabb: AABB,
        color: Color,
        lineWidth: Float = 2f,
        depthTest: Boolean = true
    ) {
        val boxQueue = if (depthTest) RenderQueue.boxesDepth else RenderQueue.boxesNoDepth
        boxQueue.add(QueuedBox(aabb, color.rgb, lineWidth))
    }

    @JvmStatic
    @JvmOverloads
    fun drawFilledBox(
        aabb: AABB,
        color: Color,
        depthTest: Boolean = true
    ) {
        val filledQueue = if (depthTest) RenderQueue.filledBoxesDepth else RenderQueue.filledBoxesNoDepth
        filledQueue.add(QueuedBox(aabb, color.rgb, 1f))
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
        RenderQueue.beaconBeams.add(QueuedBeaconBeam(pos, color))
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
        val lineQueue = if (depthTest) RenderQueue.linesDepth else RenderQueue.linesNoDepth
        lineQueue.add(QueuedLine(Vector3f(from.x.toFloat(), from.y.toFloat(), from.z.toFloat()), Vector3f(to.x.toFloat(), to.y.toFloat(), to.z.toFloat()), color.rgb, lineWidth))
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
        val lineQueue = if (depthTest) RenderQueue.linesDepth else RenderQueue.linesNoDepth

        for (i in 0 until points.size - 1) {
            val from = points[i]
            val to = points[i + 1]
            lineQueue.add(QueuedLine(Vector3f(from.x.toFloat(), from.y.toFloat(), from.z.toFloat()), Vector3f(to.x.toFloat(), to.y.toFloat(), to.z.toFloat()), color.rgb, lineWidth))
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
        val circleQueue = if (depthTest) RenderQueue.circlesDepth else RenderQueue.circlesNoDepth
        circleQueue.add(QueuedCircle(center, radius, segments, color.rgb, lineWidth, normal))
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
        val filledQueue = if (depthTest) RenderQueue.filledCirclesDepth else RenderQueue.filledCirclesNoDepth
        filledQueue.add(QueuedCircle(center, radius, segments, color.rgb, 1f, normal))
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