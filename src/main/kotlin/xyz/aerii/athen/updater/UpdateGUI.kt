package xyz.aerii.athen.updater

import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.brighten
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

class UpdateGUI(
    private val currentVersion: String,
    private val newVersion: String,
    private val onUpdate: () -> Unit,
    private val onSkip: () -> Unit,
    private val onRemind: () -> Unit
) : Scram("Update GUI [Athen]") {

    private val openProgress = timedValue(0f, 300L, ::easeOutQuad)
    private var updateScale = springValue(1f, 0.25f)
    private var skipScale = springValue(1f, 0.25f)
    private var remindScale = springValue(1f, 0.25f)
    private val panelWidth = 500f
    private val panelHeight = 200f
    private var confirmingSkip = false

    override fun onScramInit() {
        openProgress.value = 1f
    }

    override fun isPauseScreen() = false

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        NVGSpecialRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
            val width = client.window.width
            val height = client.window.height
            val anim = openProgress.value
            val scale = 0.8f + 0.2f * anim

            NVGRenderer.push()
            NVGRenderer.translate(width / 2f, height / 2f)
            NVGRenderer.scale(scale, scale)
            NVGRenderer.translate(-width / 2f, -height / 2f)
            NVGRenderer.globalAlpha(anim)

            drawPanel(width, height)

            NVGRenderer.pop()
        }
    }

    private fun drawPanel(width: Int, height: Int) {
        val panelX = (width - panelWidth) / 2f
        val panelY = (height - panelHeight) / 2f

        NVGRenderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 15f, 4f, 10f)
        NVGRenderer.drawRectangle(panelX, panelY, panelWidth, 60f, Mocha.Base.argb, 10f, 10f, 0f, 0f)
        NVGRenderer.drawRectangle(panelX, panelY + 60f, panelWidth, panelHeight - 60f, Mocha.Surface0.withAlpha(0.04f), 0f, 0f, 10f, 10f)

        NVGRenderer.drawText("Update Available", panelX + 20f, panelY + 18f, 24f, Mocha.Text.argb, NVGRenderer.defaultFont)

        val contentY = panelY + 80f
        NVGRenderer.drawText("Current Version:", panelX + 20f, contentY, 16f, Mocha.Subtext0.argb, NVGRenderer.defaultFont)
        NVGRenderer.drawText(currentVersion, panelX + 180f, contentY, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)

        NVGRenderer.drawText("New Version:", panelX + 20f, contentY + 30f, 16f, Mocha.Subtext0.argb, NVGRenderer.defaultFont)
        NVGRenderer.drawText(newVersion, panelX + 180f, contentY + 30f, 16f, Mocha.Green.argb, NVGRenderer.defaultFont)

        val buttonY = panelY + panelHeight - 60f
        val buttonWidth = 140f
        val buttonHeight = 40f
        val buttonSpacing = 15f

        val updateX = panelX + 20f
        val remindX = panelX + 20f + buttonWidth + buttonSpacing + 5f
        val skipX = panelX + panelWidth - buttonWidth - 20f

        drawButton(updateX, buttonY, buttonWidth, buttonHeight, "Update Now", { updateScale.value }, { updateScale.value = it }, Mocha.Green.argb)
        drawButton(remindX, buttonY, buttonWidth, buttonHeight, "Remind Later", { remindScale.value }, { remindScale.value = it }, Mocha.Peach.argb.brighten(0.9f))
        drawButton(skipX, buttonY, buttonWidth, buttonHeight, if (confirmingSkip) "Confirm?" else "Skip Version", { skipScale.value }, { skipScale.value = it }, Mocha.Red.argb)
    }

    private fun drawButton(x: Float, y: Float, w: Float, h: Float, text: String, getScale: () -> Float, setScale: (Float) -> Unit, color: Int) {
        val isHovered = isAreaHovered(x, y, w, h)

        setScale(if (isHovered) 1.05f else 1f)
        val scale = getScale()

        val centerX = x + w / 2f
        val centerY = y + h / 2f
        val scaledW = w * scale
        val scaledH = h * scale
        val scaledX = centerX - scaledW / 2f
        val scaledY = centerY - scaledH / 2f

        NVGRenderer.drawRectangle(scaledX, scaledY, scaledW, scaledH, color, 8f)

        val textWidth = NVGRenderer.getTextWidth(text, 16f, NVGRenderer.defaultFont)
        NVGRenderer.drawText(text, centerX - textWidth / 2f, centerY - 8f, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return super.onScramMouseClick(mouseX, mouseY, button)

        val width = client.window.width
        val height = client.window.height
        val panelX = (width - panelWidth) / 2f
        val panelY = (height - panelHeight) / 2f

        val buttonY = panelY + panelHeight - 60f
        val buttonWidth = 140f
        val buttonHeight = 40f
        val buttonSpacing = 15f

        val updateX = panelX + 20f
        val remindX = panelX + 20f + buttonWidth + buttonSpacing + 5f
        val skipX = panelX + panelWidth - buttonWidth - 20f

        when {
            isAreaHovered(updateX, buttonY, buttonWidth, buttonHeight) -> {
                onUpdate()
                client.setScreen(null)
                return true
            }
            isAreaHovered(remindX, buttonY, buttonWidth, buttonHeight) -> {
                if (confirmingSkip) {
                    confirmingSkip = false
                    return true
                }

                onRemind()
                "Will remain to update for version $newVersion on next launch".modMessage()
                client.setScreen(null)
                return true
            }
            isAreaHovered(skipX, buttonY, buttonWidth, buttonHeight) -> {
                if (confirmingSkip) {
                    onSkip()
                    "Skipped update for version $newVersion".modMessage()
                    client.setScreen(null)
                } else {
                    confirmingSkip = true
                }

                return true
            }
        }

        return super.onScramMouseClick(mouseX, mouseY, button)
    }
}