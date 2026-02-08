package xyz.aerii.athen.handlers

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Smoothie.play
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.Image
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.render.animations.easeInOutCubic
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.linear
import xyz.aerii.athen.utils.render.animations.timedValue
import kotlin.math.min

@Priority
object Notifier {
    private val notifications = mutableListOf<Notification>()
    lateinit var closeIcon: Image

    @JvmStatic
    @JvmOverloads
    fun String.notify(
        header: String = "Notification",
        duration: Int = 3000,
        playSound: Boolean = true,
        soundType: SoundEvent = SoundEvents.NOTE_BLOCK_CHIME.value()
    ) {
        notifications.add(Notification(header, this, duration))
        if (playSound) soundType.play()
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
        val height = client.window.height

        var currentY = height - 15f

        for (n in notifications.asReversed()) {
            val notificationHeight = n.calculateHeight()
            val yPos = currentY - notificationHeight

            n.render(width.toFloat(), yPos)
            currentY = yPos - 10f
        }
    }

    private class Notification(
        val header: String,
        val message: String,
        val duration: Int
    ) {
        private val slideIn = timedValue(1f, 300L, ::easeOutQuad)
        private val slideOut = timedValue(0f, 200L, ::easeInOutCubic)
        private val fadeOut = timedValue(1f, 200L, ::linear)

        private val creationTime = System.currentTimeMillis()
        private var eating = false

        init {
            slideIn.value = 0f
        }

        fun calculateHeight(): Float {
            val contentPadding = 12f
            val headerHeight = if (header.isNotEmpty()) 20f else 0f
            val headerSpacing = if (header.isNotEmpty()) 6f else 0f

            val maxTextWidth = 350f - contentPadding * 2
            val messageHeight = NVGRenderer.getWrappedTextHeight(message, 14f, maxTextWidth)

            return contentPadding * 2 + headerHeight + headerSpacing + messageHeight
        }

        fun render(screenWidth: Float, y: Float) {
            val elapsed = System.currentTimeMillis() - creationTime

            if (elapsed > duration && !eating) {
                eating = true
                slideOut.value = 1f
                fadeOut.value = 0f
            }

            val contentPadding = 12f
            val notificationHeight = calculateHeight()

            val slideInProgress = 1f - slideIn.value
            val slideOutProgress = slideOut.value
            val slideOffset = if (eating) slideOutProgress * (350f + 15f * 2) else (1f - slideInProgress) * (350f + 15f * 2)
            val alpha = fadeOut.value
            val x = screenWidth - 15f - 350f + slideOffset

            NVGRenderer.push()
            NVGRenderer.globalAlpha(alpha)

            NVGRenderer.drawDropShadow(x, y, 350f, notificationHeight, 12f, 3f, 8f)
            NVGRenderer.drawOutlinedRectangle(x, y, 350f, notificationHeight, Mocha.Base.argb, Mocha.Mauve.argb, 1f, 8f, 8f, 0f, 0f)
            NVGRenderer.drawImage(closeIcon, x + 350f - contentPadding - 16, y + contentPadding, 16f, 16f)

            var currentY = y + contentPadding
            if (header.isNotEmpty()) {
                NVGRenderer.drawText(header, x + contentPadding, currentY, 18f, Mocha.Mauve.argb, NVGRenderer.defaultFont)
                currentY += 20f + 6f
            }

            val maxTextWidth = 350f - contentPadding * 2
            NVGRenderer.drawTextWrapped(message, x + contentPadding, currentY, 14f, maxTextWidth, NVGRenderer.defaultFont)

            val progressBarHeight = 3f
            val progressBarY = y + notificationHeight - progressBarHeight
            val progress = min(1f, elapsed.toFloat() / duration)

            if (!eating && progress < 1f) NVGRenderer.drawRectangle(x, progressBarY, 350f * progress, progressBarHeight, Mocha.Mauve.argb, 0f, 0f, 8f, 8f)
            NVGRenderer.pop()
        }

        fun eat(): Boolean {
            val slideOutDone = slideOut.value > 0.999f
            val fadeOutDone = fadeOut.value < 0.001f
            return eating && slideOutDone && fadeOutDone
        }
    }
}