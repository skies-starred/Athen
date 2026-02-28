/*
BSD 3-Clause License

Copyright (c) 2025, odtheking

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:Suppress("Unused", "FunctionName")

package xyz.aerii.athen.utils.nvg

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import xyz.aerii.athen.handlers.Resourceful
import xyz.aerii.athen.handlers.Texter
import xyz.aerii.athen.utils.alpha
import xyz.aerii.athen.utils.blue
import xyz.aerii.athen.utils.green
import xyz.aerii.athen.utils.red
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object NVGRenderer {

    private val nvgPaint = NVGPaint.malloc()
    private val nvgColor = NVGColor.malloc()
    private val nvgColor2: NVGColor = NVGColor.malloc()

    private val fontMap = HashMap<Font, NVGFont>()
    private val fontBounds = FloatArray(4)

    private val images = HashMap<Image, NVGImage>()

    private var scissor: Scissor? = null
    private var drawing: Boolean = false
    private var vg = -1L

    val defaultFont = Font("Default", Resourceful.resource("inter.ttf").open())

    init {
        vg = nvgCreate(NVG_ANTIALIAS or NVG_STENCIL_STROKES)
        require(vg != -1L) { "Failed to initialize NanoVG" }
    }

    fun beginFrame(width: Float, height: Float) {
        if (drawing) error("[NVGRenderer] Already drawing, but called beginFrame")

        nvgBeginFrame(vg, width, height, 1f)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        drawing = true
    }

    fun endFrame() {
        if (!drawing) error("[NVGRenderer] Not drawing, but called endFrame")
        nvgEndFrame(vg)

        drawing = false
    }

    fun push() = nvgSave(vg)

    fun pop() = nvgRestore(vg)

    fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    fun rotate(amount: Float) = nvgRotate(vg, amount)

    fun globalAlpha(amount: Float) = nvgGlobalAlpha(vg, amount.coerceIn(0f, 1f))

    fun pushScissor(x: Float, y: Float, w: Float, h: Float) {
        scissor = Scissor(scissor, x, y, w + x, h + y)
        scissor?.applyScissor()
    }

    fun popScissor() {
        nvgResetScissor(vg)
        scissor = scissor?.previous
        scissor?.applyScissor()
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        nvgBeginPath(vg)
        nvgMoveTo(vg, x1, y1)
        nvgLineTo(vg, x2, y2)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun drawRectangle(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun drawRectangle(x: Float, y: Float, w: Float, h: Float, color: Int) {
        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h + .5f)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun drawRectangle(x: Float, y: Float, w: Float, h: Float, color: Int, topRight: Float, topLeft: Float, bottomRight: Float, bottomLeft: Float) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topRight, topLeft, bottomRight, bottomLeft)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun drawHollowRectangle(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun drawHollowRectangle(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, topRight: Float, topLeft: Float, bottomRight: Float, bottomLeft: Float) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topRight, topLeft, bottomRight, bottomLeft)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun drawOutlinedRectangle(x: Float, y: Float, w: Float, h: Float, fillColor: Int, outlineColor: Int, thickness: Float, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        color(fillColor)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)

        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(outlineColor)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun drawOutlinedRectangle(x: Float, y: Float, w: Float, h: Float, fillColor: Int, outlineColor: Int, thickness: Float, topRight: Float, topLeft: Float, bottomRight: Float, bottomLeft: Float) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topRight, topLeft, bottomRight, bottomLeft)
        color(fillColor)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)

        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topRight, topLeft, bottomRight, bottomLeft)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(outlineColor)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun drawGradientRectangle(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, gradient: Gradient, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        gradient(color1, color2, x, y, w, h, gradient)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun drawDropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) {
        nvgRGBA(0, 0, 0, 125, nvgColor)
        nvgRGBA(0, 0, 0, 0, nvgColor2)

        nvgBoxGradient(vg, x - spread, y - spread, width + 2 * spread, height + 2 * spread, radius + spread, blur, nvgColor, nvgColor2, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x - spread - blur, y - spread - blur, width + 2 * spread + 2 * blur, height + 2 * spread + 2 * blur, radius + spread)
        nvgRoundedRect(vg, x, y, width, height, radius)
        nvgPathWinding(vg, NVG_HOLE)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        nvgBeginPath(vg)
        nvgCircle(vg, x, y, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun drawText(text: String, x: Float, y: Float, size: Float, color: Int, font: Font = defaultFont) {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, font.id())
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y + .5f, text)
    }

    fun drawTextWrapped(text: String, x: Float, y: Float, fontSize: Float, maxWidth: Float, font: Font = defaultFont, lineHeight: Float = 1.2f) {
        var cx = x
        var cy = y

        wrapped(text, fontSize, maxWidth, font) { seg, color, width, newLine ->
            if (newLine) {
                cx = x
                cy += fontSize * lineHeight
                return@wrapped
            }

            drawText(seg, cx, cy, fontSize, color or 0xFF000000.toInt(), font)
            cx += width
        }
    }

    fun getWrappedTextHeight(text: String, fontSize: Float, maxWidth: Float, font: Font = defaultFont, lineHeight: Float = 1.2f): Float {
        var lines = 1

        wrapped(text, fontSize, maxWidth, font) { _, _, _, newLine ->
            if (newLine) lines++
        }

        return lines * fontSize * lineHeight
    }

    fun getWrappedTextWidth(text: String, fontSize: Float, maxWidth: Float, font: Font = defaultFont): Float {
        var lineWidth = 0f
        var maxLineWidth = 0f

        wrapped(text, fontSize, maxWidth, font) { _, _, width, newLine ->
            if (newLine) {
                maxLineWidth = maxLineWidth.coerceAtLeast(lineWidth)
                lineWidth = 0f
            } else {
                lineWidth += width
            }
        }

        return maxLineWidth.coerceAtLeast(lineWidth)
    }

    fun getTextWidth(text: String, size: Float, font: Font): Float {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, font.id())
        return nvgTextBounds(vg, 0f, 0f, text, fontBounds)
    }

    fun drawImage(image: Int, textureWidth: Int, textureHeight: Int, subX: Int, subY: Int, subW: Int, subH: Int, x: Float, y: Float, w: Float, h: Float, radius: Float) {
        if (image == -1) return

        val sx = subX.toFloat() / textureWidth
        val sy = subY.toFloat() / textureHeight
        val sw = subW.toFloat() / textureWidth
        val sh = subH.toFloat() / textureHeight

        val iw = w / sw
        val ih = h / sh
        val ix = x - iw * sx
        val iy = y - ih * sy

        nvgImagePattern(vg, ix, iy, iw, ih, 0f, image, 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun drawImage(image: Image, x: Float, y: Float, w: Float, h: Float, radius: Float) {
        nvgImagePattern(vg, x, y, w, h, 0f, image.get(), 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun drawImage(image: Image, x: Float, y: Float, w: Float, h: Float) {
        nvgImagePattern(vg, x, y, w, h, 0f, image.get(), 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h + .5f)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun createImage(resourcePath: String, color: Int = -1): Image {
        val image = images.keys.find { it.identifier == resourcePath } ?: Image(resourcePath)
        if (image.isSVG) {
            images.getOrPut(image) { NVGImage(0, image.svg(color)) }.count++
        } else {
            images.getOrPut(image) { NVGImage(0, image.load()) }.count++
        }
        return image
    }

    fun createImage(textureId: Int, textureWidth: Int, textureHeight: Int): Int =
        nvglCreateImageFromHandle(vg, textureId, textureWidth, textureHeight, NVG_IMAGE_NEAREST or NVG_IMAGE_NODELETE)

    // lowers reference count by 1, if it reaches 0 it gets deleted from mem
    fun deleteImage(image: Image) {
        val nvgImage = images[image] ?: return
        nvgImage.count--
        if (nvgImage.count == 0) {
            nvgDeleteImage(vg, nvgImage.nvg)
            images.remove(image)
        }
    }

    private fun wrapped(text: String, fontSize: Float, maxWidth: Float, font: Font = defaultFont, consumer: (segment: String, color: Int, width: Float, newLine: Boolean) -> Unit) {
        var color = 0xFFFFFF
        var lineWidth = 0f

        val space = " "
        val spaceWidth = getTextWidth(space, fontSize, font)

        var i = 0
        var last = 0

        fun emitChunk(chunk: String) {
            if (chunk.isEmpty()) return

            val words = chunk.split(' ')
            for (idx in words.indices) {
                if (idx > 0) {
                    if (lineWidth + spaceWidth > maxWidth && lineWidth > 0f) {
                        consumer("", 0, 0f, true)
                        lineWidth = 0f
                    }
                    consumer(space, color, spaceWidth, false)
                    lineWidth += spaceWidth
                }

                val word = words[idx]
                if (word.isEmpty()) continue

                val w = getTextWidth(word, fontSize, font)
                if (lineWidth + w > maxWidth && lineWidth > 0f) {
                    consumer("", 0, 0f, true)
                    lineWidth = 0f
                }

                consumer(word, color, w, false)
                lineWidth += w
            }
        }

        while (i < text.length) {
            if (text[i] != '<') {
                i++
                continue
            }

            emitChunk(text.substring(last, i))

            val end = text.indexOf('>', i + 1).takeIf { it != -1 } ?: break
            val tag = text.substring(i + 1, end).trim().lowercase()
            color = if (tag == "r") 0xFFFFFF else Texter.COLORS[tag] ?: color
            last = end + 1
            i = last
        }

        if (last < text.length) emitChunk(text.substring(last))
    }

    private fun Image.get(): Int =
        images[this]?.nvg ?: throw IllegalStateException("Image (${identifier}) doesn't exist")

    private fun Image.load(): Int {
        val w = IntArray(1)
        val h = IntArray(1)
        val channels = IntArray(1)
        val buffer = stbi_load_from_memory(buffer(), w, h, channels, 4) ?: throw NullPointerException("Failed to load image: $identifier")
        return nvgCreateImageRGBA(vg, w[0], h[0], 0, buffer)
    }

    private fun Image.svg(color: Int): Int {
        var vec = stream.use { it.bufferedReader().readText() }

        val hexColor = "#%06X".format(color and 0xFFFFFF)
        vec = vec.replace("currentColor", hexColor)

        val svg = nsvgParse(vec, "px", 96f) ?: throw IllegalStateException("Failed to parse $identifier")

        val width = svg.width().toInt()
        val height = svg.height().toInt()
        val buffer = memAlloc(width * height * 4)

        try {
            val rasterizer = nsvgCreateRasterizer()
            nsvgRasterize(rasterizer, svg, 0f, 0f, 1f, buffer, width, height, width * 4)
            val nvgImage = nvgCreateImageRGBA(vg, width, height, 0, buffer)
            nsvgDeleteRasterizer(rasterizer)
            return nvgImage
        } finally {
            nsvgDelete(svg)
            memFree(buffer)
        }
    }

    private fun Font.id(): Int = fontMap.getOrPut(this) {
        val buffer = buffer()
        NVGFont(nvgCreateFontMem(vg, name, buffer, false), buffer)
    }.id

    private fun color(color: Int) {
        nvgRGBA(color.red.toByte(), color.green.toByte(), color.blue.toByte(), color.alpha.toByte(), nvgColor)
    }

    private fun color(color1: Int, color2: Int) {
        nvgRGBA(color1.red.toByte(), color1.green.toByte(), color1.blue.toByte(), color1.alpha.toByte(), nvgColor)
        nvgRGBA(color2.red.toByte(), color2.green.toByte(), color2.blue.toByte(), color2.alpha.toByte(), nvgColor2)
    }

    private fun gradient(color1: Int, color2: Int, x: Float, y: Float, w: Float, h: Float, direction: Gradient) {
        color(color1, color2)
        when (direction) {
            Gradient.`L->R` -> nvgLinearGradient(vg, x, y, x + w, y, nvgColor, nvgColor2, nvgPaint)
            Gradient.`T->B` -> nvgLinearGradient(vg, x, y, x, y + h, nvgColor, nvgColor2, nvgPaint)
        }
    }

    private class Scissor(val previous: Scissor?, val x: Float, val y: Float, val maxX: Float, val maxY: Float) {
        fun applyScissor() {
            if (previous == null) nvgScissor(vg, x, y, maxX - x, maxY - y)
            else {
                val x = max(x, previous.x)
                val y = max(y, previous.y)
                val width = max(0f, (min(maxX, previous.maxX) - x))
                val height = max(0f, (min(maxY, previous.maxY) - y))
                nvgScissor(vg, x, y, width, height)
            }
        }
    }

    private data class NVGImage(var count: Int, val nvg: Int)
    private data class NVGFont(val id: Int, val buffer: ByteBuffer)
}
