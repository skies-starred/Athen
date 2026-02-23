package xyz.aerii.athen.handlers

import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.render.animations.easeInOutCubic
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.timedValue
import java.util.concurrent.CopyOnWriteArrayList

@Priority
object Toaster {
    private val notifications = CopyOnWriteArrayList<Toast>()

    @JvmStatic
    @JvmOverloads
    fun String.toast(subtitle: String? = null, duration: Long = 2500L, maxWidth: Float = 450f) {
        notifications.add(Toast(this, subtitle, duration, maxWidth))
    }

    init {
        on<GuiEvent.Render.Post> {
            notifications.removeIf { it.eat() }
            if (notifications.isEmpty()) return@on

            NVGSpecialRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
                render()
            }
        }
    }

    private fun render() {
        val width = client.window.width
        var offsetY = 30f

        for (toast in notifications) {
            val toastWidth = toast.calculateWidth()
            val toastHeight = toast.calculateHeight()
            val x = (width - toastWidth) / 2f

            toast.render(x, offsetY, toastWidth, toastHeight)
            offsetY += toastHeight + 18f
        }
    }

    private class Toast(
        val title: String,
        val subtitle: String?,
        val displayDuration: Long,
        val maxWidth: Float
    ) {
        private val fadeIn = timedValue(0f, 300L, ::easeOutQuad)
        private val fadeOut = timedValue(1f, 300L, ::easeInOutCubic)

        private val creationTime = System.currentTimeMillis()
        private var eating = false

        init {
            fadeIn.value = 1f
        }

        fun calculateWidth(): Float {
            val titleWidth = NVGRenderer.getWrappedTextWidth(title, 24f, maxWidth, NVGRenderer.defaultFont)
            val subtitleWidth = subtitle?.let { NVGRenderer.getWrappedTextWidth(it, 19.5f, maxWidth, NVGRenderer.defaultFont) } ?: 0f

            return (titleWidth.coerceAtLeast(subtitleWidth) + 24f * 2)
        }

        fun calculateHeight(): Float {
            val titleHeight = NVGRenderer.getWrappedTextHeight(title, 24f, maxWidth, NVGRenderer.defaultFont)
            val subtitleHeight = subtitle?.let { NVGRenderer.getWrappedTextHeight(it, 19.5f, maxWidth, NVGRenderer.defaultFont) + 6f } ?: 0f

            return 12f * 2 + titleHeight + subtitleHeight
        }

        fun render(x: Float, y: Float, width: Float, height: Float) {
            val elapsed = System.currentTimeMillis() - creationTime

            if (elapsed > displayDuration && !eating) {
                eating = true
                fadeOut.value = 0f
            }

            val alpha = if (eating) fadeOut.value else fadeIn.value

            NVGRenderer.push()
            NVGRenderer.globalAlpha(alpha)

            NVGRenderer.drawDropShadow(x, y, width, height, 12f, 3f, 9f)
            NVGRenderer.drawRectangle(x, y, width, height, Mocha.Base.argb, 9f)

            var contentY = y + 12f

            NVGRenderer.drawTextWrapped(title, x + 24f, contentY, 24f, width - 24f * 2, NVGRenderer.defaultFont)
            subtitle?.let {
                val titleHeight = NVGRenderer.getWrappedTextHeight(title, 24f, width - 24f * 2, NVGRenderer.defaultFont)
                contentY += titleHeight + 6f
                NVGRenderer.drawTextWrapped(it, x + 24f, contentY, 19.5f, width - 24f * 2, NVGRenderer.defaultFont)
            }

            NVGRenderer.pop()
        }

        fun eat(): Boolean = eating && fadeOut.value < 0.001f
    }
}