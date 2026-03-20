package xyz.aerii.athen.modules.impl.render.radial.impl

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.Mth
import org.joml.Matrix3x2f
import xyz.aerii.athen.modules.impl.render.radial.base.ISlot
import kotlin.math.PI
import kotlin.math.roundToInt

class SlotsRenderState(
    graphics: GuiGraphics,
    private val x: Int,
    private val y: Int,
    private val num: Int,
    private val inn: Float,
    private val out: Float,
    private val color: Int,
    private val hover: Int,
    private val idx: Int = -1,
    private val sub: List<ISlot> = emptyList(),
    private val idx0: Int = -1,
    private val idx1: Int = -1,
    private val sub0: List<Pair<Int, ISlot>> = emptyList(),
    private val idx2: Int = -1,
) : GuiElementRenderState {
    private val pose = graphics.pose()
    private val rect = graphics.scissorStack.peek()
    private val bounds = run {
        val r = ((out + 2f) / 100f * 130f + sub.size * 21f + 10f).toInt()
        val sr = ScreenRectangle(x - r, y - r, r * 2, r * 2).transformMaxBounds(pose)
        rect?.intersection(sr) ?: sr
    }

    override fun pipeline(): RenderPipeline = RenderPipelines.GUI
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()
    override fun scissorArea(): ScreenRectangle? = rect
    override fun bounds(): ScreenRectangle? = bounds

    override fun buildVertices(vc: VertexConsumer) {
        val step = (PI * 2 / num).toFloat()
        val gap = Math.toRadians(4.0).toFloat()
        val off = (-PI / 2 - step / 2).toFloat()

        for (i in 0 until num) {
            val start = i * step + gap * .5f + off
            val end = (i + 1) * step - gap * .5f + off

            val c0 = Mth.cos(start/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val s0 = Mth.sin(start/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val c1 = Mth.cos(end/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val s1 = Mth.sin(end/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)

            val h = i == idx || i == idx1
            val e = i == idx1 && sub.isNotEmpty()

            val inner = ((inn - if (h && !e) 2f else 0f) / 100f) * 130f
            val outer = ((out + if (h && !e) 2f else 0f) / 100f) * 130f

            val c = if (h) hover else color
            quad(vc, pose, c, x + outer * c0, y + outer * s0, x + inner * c0, y + inner * s0, x + inner * c1, y + inner * s1, x + outer * c1, y + outer * s1)

            if (i != idx1 || sub.isEmpty()) continue

            val base = (out / 100f) * 130f
            for (j in sub.indices) {
                val si = base + 3f + j * 21f
                val so = si + 18f
                val c2 = if (j == idx0) hover else color

                quad(vc, pose, c2, x + so * c0, y + so * s0, x + si * c0, y + si * s0, x + si * c1, y + si * s1, x + so * c1, y + so * s1)
            }
        }

        if (sub0.isEmpty()) return

        val bi = (out / 100f) * 130f + 8f
        val bo = bi + 20f

        for ((idx3, _) in sub0) {
            val start = idx3 * step + gap * .5f + off
            val end = (idx3 + 1) * step - gap * .5f + off

            val c0 = Mth.cos(start/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val s0 = Mth.sin(start/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val c1 = Mth.cos(end/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)
            val s1 = Mth.sin(end/*? >= 1.21.11 {*//*.toDouble()*//*? }*/)

            val h = idx3 == idx2
            val ei = bi - if (h) 2f else 0f
            val eo = bo + if (h) 2f else 0f
            val c = if (h) hover else color

            quad(vc, pose, c, x + eo * c0, y + eo * s0, x + ei * c0, y + ei * s0, x + ei * c1, y + ei * s1, x + eo * c1, y + eo * s1)
        }
    }

    companion object {
        fun centerSlot(cx: Int, cy: Int, num: Int, inn: Float, out: Float, i: Int): Pair<Int, Int> {
            val (c0, s0, c1, s1) = ang(num, i)
            val inner = (inn / 100f) * 130f
            val outer = (out / 100f) * 130f

            return ctr(cx, cy, c0, s0, c1, s1, inner, outer)
        }

        fun centerSub(cx: Int, cy: Int, num: Int, out: Float, i: Int, j: Int): Pair<Int, Int> {
            val (c0, s0, c1, s1) = ang(num, i)
            val base = (out / 100f) * 130f
            val si = base + 3f + j * 21f
            val so = si + 18f

            return ctr(cx, cy, c0, s0, c1, s1, si, so)
        }

        fun centerSub0(cx: Int, cy: Int, num: Int, out: Float, i: Int): Pair<Int, Int> {
            val (c0, s0, c1, s1) = ang(num, i)
            val bi = (out / 100f) * 130f + 8f
            val bo = bi + 20f

            return ctr(cx, cy, c0, s0, c1, s1, bi, bo)
        }

        fun hitSub(mx: Float, my: Float, cx: Int, cy: Int, num: Int, out: Float, i: Int, n: Int): Int {
            val (c0, s0, c1, s1) = ang(num, i)
            val base = (out / 100f) * 130f

            for (j in 0 until n) {
                val si = base + 3f + j * 21f
                val so = si + 18f
                if (hit(mx, my, cx, cy, c0, s0, c1, s1, si, so)) return j
            }

            return -1
        }

        fun hitSub0(mx: Float, my: Float, cx: Int, cy: Int, num: Int, out: Float, pos: List<Int>, bool: Boolean = false): Int {
            val bi = (out / 100f) * 130f + 8f
            val bo = bi + 20f

            for ((i, s) in pos.withIndex()) {
                val (c0, s0, c1, s1) = ang(num, s)
                if (hit(mx, my, cx, cy, c0, s0, c1, s1, bi, bo)) return i
            }

            if (bool && pos.isNotEmpty()) {
                val dx = mx - cx
                val dy = my - cy
                if (dx == 0f && dy == 0f) return -1

                var a = kotlin.math.atan2(dy, dx)
                val step = (PI * 2 / num).toFloat()
                val off = (-PI / 2 - step / 2).toFloat()
                a -= off
                if (a < 0) a += (PI * 2).toFloat()

                val sector = (a / step).toInt()

                val idx = pos.indexOfFirst { it == sector }
                if (idx != -1) return idx
            }

            return -1
        }

        fun tri(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Boolean {
            fun c(ax: Float, ay: Float, bx: Float, by: Float) = (ax - px) * (by - py) - (ay - py) * (bx - px)
            val d1 = c(ax, ay, bx, by)
            val d2 = c(bx, by, cx, cy)
            val d3 = c(cx, cy, ax, ay)
            return !((d1 < 0 || d2 < 0 || d3 < 0) && (d1 > 0 || d2 > 0 || d3 > 0))
        }

        private fun quad(vc: VertexConsumer, pose: Matrix3x2f, c: Int, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            vc.addVertexWith2DPose(pose, x0, y0).setColor(c)
            vc.addVertexWith2DPose(pose, x1, y1).setColor(c)
            vc.addVertexWith2DPose(pose, x2, y2).setColor(c)
            vc.addVertexWith2DPose(pose, x3, y3).setColor(c)
        }

        private fun ang(num: Int, i: Int): FloatArray {
            val step = (PI * 2 / num).toFloat()
            val gap = Math.toRadians(4.0).toFloat()
            val off = (-PI / 2 - step / 2).toFloat()
            val s = i * step + gap * .5f + off
            val e = (i + 1) * step - gap * .5f + off
            return floatArrayOf(Mth.cos(s/*? >= 1.21.11 {*//*.toDouble()*//*? }*/), Mth.sin(s/*? >= 1.21.11 {*//*.toDouble()*//*? }*/), Mth.cos(e/*? >= 1.21.11 {*//*.toDouble()*//*? }*/), Mth.sin(e/*? >= 1.21.11 {*//*.toDouble()*//*? }*/))
        }

        private fun ctr(cx: Int, cy: Int, c0: Float, s0: Float, c1: Float, s1: Float, i: Float, o: Float): Pair<Int, Int> {
            val x = (o * c0 + i * c0 + i * c1 + o * c1) * .25f
            val y = (o * s0 + i * s0 + i * s1 + o * s1) * .25f
            return (cx + x).roundToInt() to (cy + y).roundToInt()
        }

        private fun hit(px: Float, py: Float, cx: Int, cy: Int, c0: Float, s0: Float, c1: Float, s1: Float, i: Float, o: Float): Boolean {
            val x0 = cx + o * c0
            val y0 = cy + o * s0
            val x1 = cx + i * c0
            val y1 = cy + i * s0
            val x2 = cx + i * c1
            val y2 = cy + i * s1
            val x3 = cx + o * c1
            val y3 = cy + o * s1

            return tri(px, py, x0, y0, x1, y1, x2, y2) || tri(px, py, x0, y0, x2, y2, x3, y3)
        }
    }
}