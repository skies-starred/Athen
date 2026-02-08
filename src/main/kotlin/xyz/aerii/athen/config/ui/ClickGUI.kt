@file:Suppress("ObjectPrivatePropertyName")

package xyz.aerii.athen.config.ui

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.config.ConfigManager
import xyz.aerii.athen.config.ConfigManager.updateConfig
import xyz.aerii.athen.config.ui.elements.FeatureTooltip
import xyz.aerii.athen.config.ui.elements.HelpTooltip
import xyz.aerii.athen.config.ui.panels.Panel
import xyz.aerii.athen.handlers.Notifier.closeIcon
import xyz.aerii.athen.handlers.Roulette
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Scurry.rawX
import xyz.aerii.athen.handlers.Scurry.rawY
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.Image
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.timedValue
import xyz.aerii.athen.utils.url
import kotlin.math.sign

@Priority(-3)
object ClickGUI : Scram("Config [Click UI - Athen]") {
    private val panels = mutableListOf<Panel>()
    private val `anim$open` = timedValue(0f, 300L, ::easeOutQuad)
    private lateinit var searchBar: SearchBar

    lateinit var discordIcon: Image
        private set

    lateinit var featureTooltip: FeatureTooltip
        private set

    private lateinit var helpTooltip: HelpTooltip

    init {
        Roulette.scope.launch {
            Roulette.download.await()
            client.execute {
                closeIcon = NVGRenderer.createImage(Roulette.file("elements/close.svg").path, Mocha.Subtext0.argb)
                discordIcon = NVGRenderer.createImage(Roulette.file("elements/discord.svg").path, Mocha.Text.argb)
            }
        }
    }

    override fun onScramInit() {
        panels.clear()

        ConfigManager.features.entries
            .sortedBy { it.key.ordinal }
            .forEachIndexed { index, (category, features) ->
                val col = index % 7
                val row = index / 7
                val x = if (row > 0) 50f + (col + 1) * 260f else 50f + col * 260f
                val y = 50f + row * 400f

                panels.add(Panel(category, features, x, y, ::updateConfig))
            }

        searchBar = SearchBar { query -> panels.forEach { it.applySearchFilter(query) } }
        featureTooltip = FeatureTooltip()
        helpTooltip = HelpTooltip()
        helpTooltip.initialize(client.window.width)
        `anim$open`.value = 1f
        super.onScramInit()
    }

    override fun onScramClose() {
        ConfigManager.save(true)
        `anim$open`.value = 0f
        super.onScramClose()
    }

    override fun isPauseScreen() = false

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        NVGSpecialRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
            val width = client.window.width
            val height = client.window.height
            val t = `anim$open`.value
            val scale = 0.1f + 0.9f * t

            NVGRenderer.push()
            NVGRenderer.globalAlpha(t)
            NVGRenderer.drawText("${Athen.modName} ${Athen.modVersion}", 4f, height - 18f, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)
            NVGRenderer.pop()

            NVGRenderer.push()
            NVGRenderer.translate(width / 2f, height / 2f)
            NVGRenderer.scale(scale, scale)
            NVGRenderer.translate(-width / 2f, -height / 2f)
            NVGRenderer.globalAlpha(t)

            panels.forEach { it.draw(rawX, rawY) }
            searchBar.draw(width / 2f - 175f, height - 110f, rawX, rawY)
            drawDiscordButton(width / 2f - 55f, height - 60f)
            featureTooltip.draw(rawX, rawY)
            helpTooltip.draw(rawX, rawY)

            NVGRenderer.pop()
        }
    }

    private fun drawDiscordButton(x: Float, y: Float) {
        val buttonWidth = 110f
        val buttonHeight = 40f

        NVGRenderer.drawDropShadow(x, y, buttonWidth, buttonHeight, 10f, 0.75f, 9f)
        NVGRenderer.drawRectangle(x, y, buttonWidth, buttonHeight, Mocha.Base.argb, 9f)
        NVGRenderer.drawHollowRectangle(x, y, buttonWidth, buttonHeight, 3f, Mocha.Mauve.argb, 9f)

        val text = "Join Discord"
        val textX = x + buttonWidth / 2f - 44f
        val textY = y + (buttonHeight - 16f) / 2f

        NVGRenderer.drawText(text, textX, textY, 16f, Mocha.Text.argb, NVGRenderer.defaultFont)
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        if (OmniKeyboard.isShiftKeyPressed) {
            val scroll = vertical.toFloat() * 20f
            val leftmost = panels.minOfOrNull { it.x } ?: 0f
            val rightmost = panels.maxOfOrNull { it.x + 240f } ?: 0f

            if ((scroll > 0 && leftmost < 50f) || (scroll < 0 && rightmost > client.window.width - 50f)) {
                panels.forEach { it.x += scroll }
            }

            return true
        }

        val amount = (vertical.sign * 16).toInt()
        return panels.reversed().any { it.handleScroll(amount) } || super.onScramMouseScroll(mouseX, mouseY, horizontal, vertical)
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0) {
            val width = client.window.width
            val height = client.window.height

            if (isAreaHovered(width / 2f - 55f, height - 60f, 110f, 40f)) {
                Athen.discordUrl.url()
                return true
            }
        }

        if (helpTooltip.mouseClicked(rawX, rawY, button)) return true
        searchBar.mouseClicked(rawX, rawY, button)
        return panels.reversed().any { it.mouseClicked(rawX, rawY, button) } || super.onScramMouseClick(mouseX, mouseY, button)
    }

    override fun onScramMouseRelease(mouseX: Int, mouseY: Int, button: Int): Boolean {
        helpTooltip.mouseReleased(button)
        searchBar.mouseReleased()
        panels.forEach { it.mouseReleased(button) }
        return super.onScramMouseRelease(mouseX, mouseY, button)
    }

    override fun onScramCharType(char: Char, modifiers: Int): Boolean {
        searchBar.keyTyped(char)
        return panels.reversed().any { it.keyTyped(char) } || super.onScramCharType(char, modifiers)
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        searchBar.keyPressed(keyCode)
        return panels.reversed().any { it.keyPressed(keyCode, scanCode) } || super.onScramKeyPress(keyCode, scanCode, modifiers)
    }
}