package foo.starred.athen.modules.impl.render.radial.utils

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import kotlin.math.*

class RadialRenderState(
    graphics: GuiGraphicsExtractor,
    private val x: Int,
    private val y: Int,
    private val num: Int,
    private val sub: List<RadialSlot> = emptyList(),
    private val sub0: List<Pair<Int, RadialSlot>> = emptyList(),
    private val i0: Int = RadialMenu.i0,
    private val i1: Int = RadialMenu.i1,
    private val i2: Int = RadialMenu.i2,
    private val i3: Int = -1
) : GuiElementRenderState {
    private val thickness = RadialMenu.thickness

    private val pose = Matrix3x2f(graphics.pose())
    private val rect = graphics.scissorStack.peek()
    private val bounds = run {
        val r = (RadialMenu.radius2 + sub.size * (3f + thickness) + 10f).toInt()
        val sr = ScreenRectangle(x - r, y - r, r * 2, r * 2).transformMaxBounds(pose)
        rect?.intersection(sr) ?: sr
    }

    override fun pipeline(): RenderPipeline {
        return RenderPipelines.GUI
    }

    override fun textureSetup(): TextureSetup {
        return TextureSetup.noTexture()
    }

    override fun scissorArea(): ScreenRectangle? {
        return rect
    }

    override fun bounds(): ScreenRectangle {
        return bounds
    }

    override fun buildVertices(vc: VertexConsumer) {
        if (num == 0) return

        val step = TAU / num
        val gap = if (num == 1) 0f else GAP

        val r01 = RadialMenu.radius1
        val r02 = RadialMenu.radius2

        val hover = RadialMenu.`color$hover`
        val color = RadialMenu.`color$normal`

        for (i in 0 until num) {
            val angle0 = i * step + ORIGIN
            val angle1 = (i + 1) * step + ORIGIN

            val b0 = i == i0 || i == i2
            val b1 = i == i2 && sub.isNotEmpty()

            val r0 = r01 - if (b0 && !b1) 2f else 0f
            val r1 = r02 + if (b0 && !b1) 2f else 0f
            val color0 = if (b0) hover else color

            arc(vc, pose, x.toFloat(), y.toFloat(), r0, r1, angle0, angle1, color0, gap)

            if (i != i2) continue
            if (sub.isEmpty()) continue

            for (j in sub.indices) {
                val si = r02 + 3f + j * (3f + thickness)
                val so = si + thickness
                val c2 = if (j == i1) hover else color

                arc(vc, pose, x.toFloat(), y.toFloat(), si, so, angle0, angle1, c2, gap)
            }
        }

        if (sub0.isNotEmpty()) {
            val bi = r02 + 8f
            val bo = bi + thickness + 2f

            for ((i, _) in sub0) {
                val angle2 = i * step + ORIGIN
                val angle3 = (i + 1) * step + ORIGIN

                val b = i == i3
                val i0 = bi - if (b) 2f else 0f
                val i1 = bo + if (b) 2f else 0f
                val c0 = if (b) hover else color

                arc(vc, pose, x.toFloat(), y.toFloat(), i0, i1, angle2, angle3, c0, gap)
            }
        }

        disc(vc, pose, x.toFloat(), y.toFloat(), color)
    }

    companion object {
        private const val ORIGIN = -PI / 2.0
        private const val TAU = PI * 2.0
        private const val GAP = 4f

        fun anchor(x: Int, y: Int, num: Int, radius0: Float, radius1: Float, i: Int): Pair<Int, Int> {
            val mid = (radius0 + radius1) / 2f
            val angle = angle(num, i)
            return (x + cos(angle) * mid).roundToInt() to (y + sin(angle) * mid).roundToInt()
        }

        fun nested(x: Int, y: Int, num: Int, radius: Float, i0: Int, i1: Int, subThickness: Float = 18f): Pair<Int, Int> {
            val si = radius + 3f + i1 * (3f + subThickness)
            val so = si + subThickness
            val mid = (si + so) / 2f
            val angle = angle(num, i0)
            return (x + cos(angle) * mid).roundToInt() to (y + sin(angle) * mid).roundToInt()
        }

        fun ring(x: Int, y: Int, i0: Int, radius: Float, i1: Int, subThickness: Float = 18f): Pair<Int, Int> {
            val bi = radius + 8f
            val bo = bi + subThickness + 2f
            val mid = (bi + bo) / 2f
            val angle = angle(i0, i1)
            return (x + cos(angle) * mid).roundToInt() to (y + sin(angle) * mid).roundToInt()
        }

        fun hit(x0: Float, y0: Float, x1: Int, y1: Int, num: Int, radius0: Float, radius1: Float, direction: Boolean = false): Int {
            if (num == 0) return -1

            val x2 = x0 - x1
            val y2 = y0 - y1
            val dist = hypot(x2.toDouble(), y2.toDouble())

            if (dist <= radius0 && !direction) return -1

            val step = TAU / num
            var angle = atan2(y2.toDouble(), x2.toDouble()) - ORIGIN
            if (angle < 0) angle += TAU

            val sector = (angle / step).toInt().coerceIn(0, num - 1)

            if (dist <= radius1) return sector
            if (direction) return sector

            return -1
        }

        fun hitNested(x0: Float, y0: Float, x1: Int, y1: Int, num: Int, radius: Float, i0: Int, i2: Int, direction: Boolean = false, subThickness: Float = 18f): Int {
            val x2 = x0 - x1
            val y2 = y0 - y1
            val dist = hypot(x2.toDouble(), y2.toDouble())

            val step = TAU / num
            val gap = (if (num == 1) 0f else GAP) / dist
            val angle1 = i0 * step + gap * 0.5 + ORIGIN
            val angle2 = (i0 + 1) * step - gap * 0.5 + ORIGIN

            var angle = atan2(y2.toDouble(), x2.toDouble())
            if (angle < ORIGIN) angle += TAU

            val b0 = angle in angle1..angle2 || angle + TAU in angle1..angle2
            if (!b0) return -1

            for (j in 0 until i2) {
                val si = radius + 3f + j * (3f + subThickness)
                val so = si + subThickness
                if (dist in si..so) return j
            }

            if (direction && i2 > 0 && dist > radius) return (i2 - 1).coerceAtMost(((dist - radius - 3f) / (3f + subThickness)).toInt().coerceIn(0, i2 - 1))

            return -1
        }

        fun hitRing(x0: Float, y0: Float, x1: Int, y1: Int, num: Int, radius: Float, pos: List<Int>, direction: Boolean = false, subThickness: Float = 18f): Int {
            val x2 = x0 - x1
            val y2 = y0 - y1
            val dist = hypot(x2.toDouble(), y2.toDouble())

            val bi = radius + 8f
            val bo = bi + subThickness + 2f

            val step = TAU / num
            val gap = (if (num == 1) 0f else GAP) / dist

            for ((i, v) in pos.withIndex()) {
                val angle0 = normalize(v * step + gap * 0.5 + ORIGIN)
                val angle1 = normalize((v + 1) * step - gap * 0.5 + ORIGIN)
                val angle2 = normalize(atan2(y2.toDouble(), x2.toDouble()))

                if ((angle0 <= angle1 && angle2 in angle0..angle1 || angle0 > angle1 && (angle2 >= angle0 || angle2 <= angle1)) && dist in bi..bo) {
                    return i
                }
            }

            if (direction && pos.isNotEmpty()) {
                var angle = atan2(y2.toDouble(), x2.toDouble()) - ORIGIN
                if (angle < 0) angle += TAU

                val i0 = (angle / step).toInt()
                val i = pos.indexOfFirst { it == i0 }
                if (i != -1) return i
            }

            return -1
        }

        private fun angle(i0: Int, i1: Int): Double {
            val step = TAU / i0
            return ORIGIN + step * 0.5 + i1 * step
        }

        private fun normalize(a: Double): Double {
            var r = a % TAU
            if (r < 0) r += TAU
            return r
        }

        private fun arc(vc: VertexConsumer, pose: Matrix3x2f, x: Float, y: Float, radius0: Float, radius1: Float, angle0: Double, angle1: Double, color: Int, gapPx: Float = GAP) {
            val i0 = if (radius0 > 0) asin((gapPx / 2.0 / radius0).coerceIn(-1.0, 1.0)) else 0.0
            val i1 = if (radius1 > 0) asin((gapPx / 2.0 / radius1).coerceIn(-1.0, 1.0)) else 0.0

            val start0 = angle0 + i0
            val end0 = angle1 - i0
            val start1 = angle0 + i1
            val end1 = angle1 - i1

            if (end0 <= start0) return
            if (end1 <= start1) return

            val steps = ceil((end1 - start1) * radius1 / 2.0).toInt().coerceAtLeast(1)
            val step0 = (end0 - start0) / steps
            val step1 = (end1 - start1) / steps

            repeat(steps) { i ->
                val a00 = start0 + i * step0
                val a01 = a00 + step0
                val a10 = start1 + i * step1
                val a11 = a10 + step1

                val c00 = cos(a00).toFloat()
                val s00 = sin(a00).toFloat()
                val c01 = cos(a01).toFloat()
                val s01 = sin(a01).toFloat()
                val c10 = cos(a10).toFloat()
                val s10 = sin(a10).toFloat()
                val c11 = cos(a11).toFloat()
                val s11 = sin(a11).toFloat()

                vc.addVertexWith2DPose(pose, x + c10 * radius1, y + s10 * radius1).setColor(color)
                vc.addVertexWith2DPose(pose, x + c00 * radius0, y + s00 * radius0).setColor(color)
                vc.addVertexWith2DPose(pose, x + c01 * radius0, y + s01 * radius0).setColor(color)
                vc.addVertexWith2DPose(pose, x + c11 * radius1, y + s11 * radius1).setColor(color)
            }
        }

        private fun disc(vc: VertexConsumer, pose: Matrix3x2f, x: Float, y: Float, color: Int) {
            arc(vc, pose, x, y, 0f, 15f, 0.0, TAU, color, 0f)
        }
    }
}
