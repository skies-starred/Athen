@file:Suppress("SameParameterValue", "ObjectPrivatePropertyName", "FunctionName", "ConstPropertyName")

package xyz.aerii.athen.modules.impl.general.keybinds

import com.mojang.blaze3d.platform.InputConstants
import dev.deftu.omnicore.api.client.input.OmniKeyboard
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.handlers.Roulette
import xyz.aerii.athen.handlers.Roulette.scope
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Scurry.isAreaHovered
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.add
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.remove
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.update
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGRenderer
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.render.animations.easeOutQuad
import xyz.aerii.athen.utils.render.animations.springValue
import xyz.aerii.athen.utils.render.animations.timedValue

object KeybindsGUI : Scram("Keybinds Manager [Athen]") {
    private const val PANEL_WIDTH = 800f
    private const val PANEL_HEIGHT = 550f
    private const val ENTRY_HEIGHT = 64f
    private const val FOOTER_HEIGHT = 60f
    private const val HEADER_HEIGHT = 50f

    private val open = timedValue(0.8f, 300L, ::easeOutQuad)
    private val add = springValue(Mocha.Base.argb)
    private var scrollOffset = 0f
    private val entries = mutableListOf<BindingEntry>()

    private lateinit var commandInput: InputField
    private lateinit var keysInput: KeysField
    private lateinit var islandDropdown: IslandDropdown

    private const val str0 = "<yellow>Press Enter<r> to confirm <gray>| <yellow>Press Escape<r> to cancel"
    private const val str1 = "No keybinds configured"
    private const val str2 = "Add Keybind"

    private var width0 = 0f
    private var width1 = 0f
    private var width2 = 0f

    init {
        scope.launch {
            Roulette.download.await()
            width0 = NVGRenderer.getWrappedTextWidth(str0, 14f, 300f)
            width1 = NVGRenderer.getTextWidth(str1, 16f, NVGRenderer.defaultFont)
            width2 = NVGRenderer.getTextWidth(str2, 13f, NVGRenderer.defaultFont)
        }
    }

    override fun onScramInit() {
        r()
        commandInput = InputField("", "Command or message")
        keysInput = KeysField()
        islandDropdown = IslandDropdown()
        open.value = 1f
    }

    override fun onScramClose() = Keybinds.storage.save()

    override fun isPauseScreen() = false

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        NVGSpecialRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
            val width = client.window.width
            val height = client.window.height
            val progress = open.value
            val scale = 0.8f + (progress - 0.8f) * 0.2f / 0.2f
            val alpha = (progress - 0.8f) / 0.2f

            NVGRenderer.push()
            NVGRenderer.translate(width / 2f, height / 2f)
            NVGRenderer.scale(scale.coerceIn(0.8f, 1f), scale.coerceIn(0.8f, 1f))
            NVGRenderer.translate(-width / 2f, -height / 2f)
            NVGRenderer.globalAlpha(alpha.coerceIn(0f, 1f))

            drawPanel(width, height)
            drawTooltip(width, height)

            NVGRenderer.pop()
        }
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        val coords = PanelCoords.calculate(client.window.width, client.window.height)

        if (char(button, coords)) return true
        if (button != 0) return false
        if (coords.`dropdown$click`()) return true
        if (coords.`footer$click`()) return true
        if (coords.`entry$click`()) return true

        return super.onScramMouseClick(mouseX, mouseY, button)
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        val amount = (vertical * 20).toFloat()
        val coords = PanelCoords.calculate(client.window.width, client.window.height)

        if (coords.`dropdown$scroll`(amount)) return true
        if (entries.isEmpty()) return false

        val contentHeight = entries.size * ENTRY_HEIGHT
        val listHeight = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20f
        val maxScroll = -(contentHeight - listHeight).coerceAtLeast(0f)

        scrollOffset = (scrollOffset + amount).coerceIn(maxScroll, 0f)
        return true
    }

    override fun onScramCharType(char: Char, modifiers: Int): Boolean {
        entries.firstOrNull { it.commandInput.focused }?.let {
            it.commandInput.insertChar(char)
            it.index.update(it.binding.keys, it.commandInput.value, it.islandDropdown.selectedIsland)
            return true
        }

        if (commandInput.focused) {
            commandInput.insertChar(char)
            return true
        }

        return super.onScramCharType(char, modifiers)
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (key(keyCode)) return true
        if (`entry$key`(keyCode)) return true
        if (`footer$key`(keyCode)) return true

        return super.onScramKeyPress(keyCode, scanCode, modifiers)
    }

    private fun drawPanel(width: Int, height: Int) {
        val coords = PanelCoords.calculate(width, height)

        NVGRenderer.drawDropShadow(coords.panelX, coords.panelY, PANEL_WIDTH, PANEL_HEIGHT, 15f, 4f, 10f)
        NVGRenderer.drawRectangle(coords.panelX, coords.panelY, PANEL_WIDTH, HEADER_HEIGHT, Mocha.Base.argb, 10f, 10f, 0f, 0f)
        NVGRenderer.drawRectangle(coords.panelX, coords.panelY + HEADER_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT - HEADER_HEIGHT, Mocha.Surface0.withAlpha(0.04f), 0f, 0f, 10f, 10f)
        NVGRenderer.drawText("Keybinds Manager", coords.panelX + 20f, coords.panelY + 14f, 24f, Mocha.Text.argb, NVGRenderer.defaultFont)

        drawBindingsList(coords)
        drawFooter(coords)
    }

    private fun drawTooltip(width: Int, height: Int) {
        if (!keysInput.listening) return

        val coords = PanelCoords.calculate(width, height)
        val boxWidth = width0 + 20f
        val boxX = coords.panelX + (PANEL_WIDTH - boxWidth) / 2f
        val boxY = coords.panelY + PANEL_HEIGHT + 10f

        NVGRenderer.drawOutlinedRectangle(boxX, boxY, boxWidth, 24f, Mocha.Base.withAlpha(0.8f), Mocha.Surface0.argb, 1f, 5f)
        NVGRenderer.drawTextWrapped(str0, boxX + 10f, boxY + 5f, 14f, 300f)
    }

    private fun drawBindingsList(coords: PanelCoords) {
        val listY = coords.panelY + HEADER_HEIGHT + 10f
        val listHeight = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20f

        if (entries.isEmpty()) {
            NVGRenderer.drawText(str1, coords.panelX + (PANEL_WIDTH - width1) / 2f, listY + listHeight / 2f - 8f, 16f, Mocha.Subtext0.argb, NVGRenderer.defaultFont)
            return
        }

        val contentHeight = entries.size * ENTRY_HEIGHT
        val maxScroll = -(contentHeight - listHeight).coerceAtLeast(0f)
        scrollOffset = scrollOffset.coerceIn(maxScroll, 0f)

        NVGRenderer.pushScissor(coords.panelX + 10f, listY, PANEL_WIDTH - 20f, listHeight)

        var currentY = listY + scrollOffset
        for (e in entries) {
            e.draw(coords.panelX + 10f, currentY, PANEL_WIDTH - 20f)
            currentY += ENTRY_HEIGHT
        }

        NVGRenderer.popScissor()
    }

    private fun drawFooter(coords: PanelCoords) {
        NVGRenderer.drawRectangle(coords.panelX, coords.footerY, PANEL_WIDTH, 1f, Mocha.Surface0.argb, 0f)

        commandInput.draw(coords.commandX, coords.footerY + 12f, coords.commandW, 36f)
        keysInput.draw(coords.keysX, coords.footerY + 12f, coords.keysW, 36f)
        islandDropdown.draw(coords.islandX, coords.footerY + 12f, coords.islandW, 36f)
        drawAddButton(coords.addButtonX, coords.footerY + 12f, coords.buttonW, 36f)
    }

    private fun drawAddButton(x: Float, y: Float, width: Float, height: Float) {
        val isHovered = isAreaHovered(x, y, width, height)
        val canAdd = commandInput.value.isNotEmpty() && keysInput.keys.isNotEmpty()

        add.value = when {
            !canAdd && isHovered -> Mocha.Red.argb
            canAdd && isHovered -> Mocha.Green.argb
            else -> Mocha.Base.argb
        }

        NVGRenderer.drawRectangle(x, y, width, height, add.value, 5f)
        NVGRenderer.drawHollowRectangle(x, y, width, height, 1f, Mocha.Surface0.argb, 5f)
        NVGRenderer.drawText(str2, x + (width - width2) / 2f, y + (height - 13f) / 2f, 13f, Mocha.Text.argb, NVGRenderer.defaultFont)
    }

    private fun char(button: Int, coords: PanelCoords): Boolean {
        if (!keysInput.listening) return false

        if (isAreaHovered(coords.keysX, coords.footerY + 12f, coords.keysW, 36f)) {
            keysInput.recordedKeys.add(-(button + 1))
            return true
        }

        if (keysInput.recordedKeys.isNotEmpty()) {
            keysInput.listening = false
            keysInput.keys = keysInput.recordedKeys.toMutableList()
        }
        return true
    }

    private fun key(keyCode: Int): Boolean {
        if (!keysInput.listening) return false

        return when (keyCode) {
            GLFW.GLFW_KEY_ENTER -> {
                if (keysInput.recordedKeys.isNotEmpty()) {
                    keysInput.listening = false
                    keysInput.keys = keysInput.recordedKeys.toMutableList()
                }
                true
            }

            GLFW.GLFW_KEY_ESCAPE -> {
                keysInput.listening = false
                keysInput.recordedKeys.clear()
                true
            }

            else -> if (keyCode > 0) {
                keysInput.recordedKeys.add(keyCode)
                true
            } else false
        }
    }

    private fun PanelCoords.`dropdown$click`(): Boolean {
        for (e in entries) {
            if (!e.islandDropdown.isOpen) continue
            val entryY = entries.indexOf(e) * ENTRY_HEIGHT + panelY + HEADER_HEIGHT + 10f + scrollOffset
            val islandX = panelX + 148f
            if (e.islandDropdown.handleClick(islandX, entryY + 14f, 140f, 28f)) {
                e.index.update(e.binding.keys, e.commandInput.value, e.islandDropdown.selectedIsland)
                return true
            }
        }

        return islandDropdown.handleClick(islandX, footerY + 12f, islandW, 36f)
    }

    private fun PanelCoords.`dropdown$scroll`( amount: Float): Boolean {
        if (islandDropdown.handleScroll(islandX, footerY + 12f, islandW, 36f, amount)) {
            return true
        }

        for ((i, e) in entries.withIndex()) if (e.islandDropdown.handleScroll(panelX + 148f, panelY + HEADER_HEIGHT + 10f + scrollOffset + i * ENTRY_HEIGHT + 14f, 140f, 28f, amount)) return true

        return false
    }

    private fun PanelCoords.`footer$click`(): Boolean {
        commandInput.focused = false

        when {
            isAreaHovered(commandX, footerY + 12f, commandW, 36f) -> {
                commandInput.focused = true
                return true
            }
            isAreaHovered(keysX, footerY + 12f, keysW, 36f) -> {
                keysInput.listening = true
                keysInput.recordedKeys.clear()
                return true
            }
            isAreaHovered(addButtonX, footerY + 12f, buttonW, 36f) -> {
                addBinding()
                return true
            }
        }

        return false
    }

    private fun PanelCoords.`entry$click`(): Boolean {
        val listY = panelY + HEADER_HEIGHT + 10f

        for ((i, e) in entries.withIndex()) {
            val entryY = listY + scrollOffset + i * ENTRY_HEIGHT
            val islandX = panelX + 148f
            val deleteX = panelX + PANEL_WIDTH - 48f
            val inputX = islandX + 148f
            val inputW = PANEL_WIDTH - 360f

            when {
                e.islandDropdown.handleClick(islandX, entryY + 14f, 140f, 28f) -> {
                    e.index.update(e.binding.keys, e.commandInput.value, e.islandDropdown.selectedIsland)
                    return true
                }

                isAreaHovered(deleteX, entryY + 14f, 28f, 28f) -> {
                    if (e.index.remove()) r()
                    return true
                }

                isAreaHovered(inputX, entryY + 14f, inputW, 28f) -> {
                    e.commandInput.focused = true
                    return true
                }
            }
        }

        return false
    }

    private fun `entry$key`(keyCode: Int): Boolean {
        entries.firstOrNull { it.commandInput.focused }?.let { entry ->
            return when (keyCode) {
                GLFW.GLFW_KEY_BACKSPACE -> {
                    entry.commandInput.deleteChar()
                    entry.index.update(entry.binding.keys, entry.commandInput.value, entry.islandDropdown.selectedIsland)
                    true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    entry.commandInput.focused = false
                    true
                }
                GLFW.GLFW_KEY_V -> if (OmniKeyboard.isCtrlKeyPressed) {
                    entry.commandInput.paste(client.keyboardHandler.clipboard)
                    entry.index.update(entry.binding.keys, entry.commandInput.value, entry.islandDropdown.selectedIsland)
                    true
                } else false
                else -> false
            }
        }
        return false
    }

    private fun `footer$key`(keyCode: Int): Boolean {
        if (!commandInput.focused) return false

        return when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                commandInput.deleteChar()
                true
            }

            GLFW.GLFW_KEY_ESCAPE -> {
                commandInput.focused = false
                true
            }

            GLFW.GLFW_KEY_V -> if (OmniKeyboard.isCtrlKeyPressed) {
                commandInput.paste(client.keyboardHandler.clipboard)
                true
            } else false

            else -> false
        }
    }

    private fun addBinding() {
        if (commandInput.value.isEmpty() || keysInput.keys.isEmpty()) return
        if (!keysInput.keys.add(commandInput.value, islandDropdown.selectedIsland)) return

        commandInput.value = ""
        commandInput.caret = 0
        keysInput.keys.clear()
        islandDropdown.selectedIsland = null
        r()
    }

    private fun r() {
        entries.clear()
        for ((i, b) in Keybinds.bindings.value.withIndex()) entries.add(BindingEntry(i, b))
    }

    private data class PanelCoords(
        val panelX: Float,
        val panelY: Float,
        val footerY: Float,
        val commandX: Float,
        val commandW: Float,
        val keysX: Float,
        val keysW: Float,
        val islandX: Float,
        val islandW: Float,
        val addButtonX: Float,
        val buttonW: Float
    ) {
        companion object {
            fun calculate(width: Int, height: Int): PanelCoords {
                val panelX = (width - PANEL_WIDTH) / 2f
                val panelY = (height - PANEL_HEIGHT) / 2f
                val footerY = panelY + PANEL_HEIGHT - FOOTER_HEIGHT

                val commandW = PANEL_WIDTH * 0.40f
                val keysW = PANEL_WIDTH * 0.20f
                val islandW = PANEL_WIDTH * 0.20f
                val buttonW = PANEL_WIDTH - commandW - keysW - islandW - 40f

                return PanelCoords(
                    panelX = panelX,
                    panelY = panelY,
                    footerY = footerY,
                    commandX = panelX + 15f,
                    commandW = commandW,
                    keysX = panelX + 20f + commandW,
                    keysW = keysW,
                    islandX = panelX + 25f + commandW + keysW,
                    islandW = islandW,
                    addButtonX = panelX + 30f + commandW + keysW + islandW,
                    buttonW = buttonW
                )
            }
        }
    }

    private data class InputField(
        var value: String,
        val placeholder: String,
        var focused: Boolean = false,
        var caret: Int = value.length
    ) {
        private val bgAnim = springValue(Mocha.Base.argb, 0.2f)
        private val borderAnim = springValue(Mocha.Surface0.argb, 0.25f)

        fun insertChar(char: Char) {
            value = value.substring(0, caret) + char + value.substring(caret)
            caret++
        }

        fun deleteChar() {
            if (caret > 0) {
                value = value.substring(0, caret - 1) + value.substring(caret)
                caret--
            }
        }

        fun paste(text: String) {
            value = value.substring(0, caret) + text + value.substring(caret)
            caret += text.length
        }

        fun draw(x: Float, y: Float, width: Float, height: Float) {
            val isHovered = isAreaHovered(x, y, width, height)

            bgAnim.value = when {
                focused -> Mocha.Surface2.argb
                isHovered -> Mocha.Surface0.argb
                else -> Mocha.Base.argb
            }

            borderAnim.value = if (focused) Mocha.Mauve.argb else Mocha.Surface0.argb

            NVGRenderer.drawRectangle(x, y, width, height, bgAnim.value, 5f)
            NVGRenderer.drawHollowRectangle(x, y, width, height, 1f, borderAnim.value, 5f)

            NVGRenderer.pushScissor(x + 4f, y, width - 8f, height)

            val displayText = if (value.isEmpty() && !focused) placeholder else value
            val textColor = if (value.isEmpty() && !focused) Mocha.Subtext0.argb else Mocha.Text.argb
            val fontSize = if (height > 30f) 14f else 13f

            NVGRenderer.drawText(displayText, x + 8f, y + (height - fontSize) / 2f, fontSize, textColor, NVGRenderer.defaultFont)

            if (focused && System.currentTimeMillis() % 1000 < 500) {
                val caretOffset = NVGRenderer.getTextWidth(displayText.take(caret), fontSize, NVGRenderer.defaultFont)
                NVGRenderer.drawLine(x + 8f + caretOffset, y + 6f, x + 8f + caretOffset, y + height - 6f, 1.5f, Mocha.Text.argb)
            }

            NVGRenderer.popScissor()
        }
    }

    private data class KeysField(
        var keys: MutableList<Int> = mutableListOf(),
        var listening: Boolean = false,
        val recordedKeys: MutableSet<Int> = mutableSetOf()
    ) {
        private val bgAnim = springValue(Mocha.Base.argb, 0.2f)
        private val borderAnim = springValue(Mocha.Surface0.argb, 0.25f)

        fun draw(x: Float, y: Float, width: Float, height: Float) {
            val isHovered = isAreaHovered(x, y, width, height)

            bgAnim.value = when {
                listening -> Mocha.Peach.withAlpha(0.3f)
                isHovered -> Mocha.Surface0.argb
                else -> Mocha.Base.argb
            }

            borderAnim.value = if (listening) Mocha.Peach.argb else Mocha.Surface0.argb

            NVGRenderer.drawRectangle(x, y, width, height, bgAnim.value, 5f)
            NVGRenderer.drawHollowRectangle(x, y, width, height, 1f, borderAnim.value, 5f)

            val displayText = when {
                listening -> if (recordedKeys.isEmpty()) "Press keys..." else recordedKeys.toList().str()
                keys.isEmpty() -> "Click to bind"
                else -> keys.str()
            }

            val textW = NVGRenderer.getTextWidth(displayText, 13f, NVGRenderer.defaultFont)
            NVGRenderer.drawText(displayText, x + (width - textW) / 2f, y + (height - 13f) / 2f, 13f, Mocha.Text.argb, NVGRenderer.defaultFont)
        }
    }

    private data class IslandDropdown(
        var selectedIsland: SkyBlockIsland? = null,
        var isOpen: Boolean = false,
        private var scrollOffset: Float = 0f
    ) {
        private val bgAnim = springValue(Mocha.Base.argb, 0.2f)
        private val borderAnim = springValue(Mocha.Surface0.argb, 0.25f)

        fun draw(x: Float, y: Float, width: Float, height: Float) {
            val isHovered = isAreaHovered(x, y, width, height)

            bgAnim.value = if (isHovered) Mocha.Surface0.argb else Mocha.Base.argb
            borderAnim.value = if (isOpen) Mocha.Mauve.argb else Mocha.Surface0.argb

            NVGRenderer.drawRectangle(x, y, width, height, bgAnim.value, 5f)
            NVGRenderer.drawHollowRectangle(x, y, width, height, 1f, borderAnim.value, 5f)

            val displayText = selectedIsland?.displayName ?: "All Islands"
            val textW = NVGRenderer.getTextWidth(displayText, 13f, NVGRenderer.defaultFont)
            NVGRenderer.drawText(displayText, x + (width - textW) / 2f, y + (height - 13f) / 2f, 13f, Mocha.Text.argb, NVGRenderer.defaultFont)

            drawArrow(x + width - 15f, y + height / 2f, 6f, isOpen)

            if (isOpen) drawMenu(x, y + height + 2f, width)
        }

        private fun drawArrow(x: Float, y: Float, size: Float, down: Boolean) {
            val offset = if (down) size / 2f else -size / 2f
            NVGRenderer.drawLine(x - size, y - offset, x, y + offset, 1.5f, Mocha.Text.argb)
            NVGRenderer.drawLine(x, y + offset, x + size, y - offset, 1.5f, Mocha.Text.argb)
        }

        private fun drawMenu(x: Float, y: Float, width: Float) {
            val islands = listOf(null) + SkyBlockIsland.entries
            val itemHeight = 28f
            val maxHeight = 200f
            val contentHeight = islands.size * itemHeight
            val menuHeight = contentHeight.coerceAtMost(maxHeight)

            NVGRenderer.drawDropShadow(x, y, width, menuHeight, 8f, 2f, 6f)
            NVGRenderer.drawRectangle(x, y, width, menuHeight, Mocha.Base.argb, 5f)
            NVGRenderer.drawHollowRectangle(x, y, width, menuHeight, 1f, Mocha.Surface0.argb, 5f)

            NVGRenderer.pushScissor(x, y, width, menuHeight)

            var currentY = y + scrollOffset
            islands.forEachIndexed { index, island ->
                if (currentY + itemHeight >= y && currentY <= y + menuHeight) {
                    val isHovered = isAreaHovered(x, currentY, width, itemHeight)

                    if (isHovered) {
                        val topRadius = if (currentY <= y || index == 0) 5f else 0f
                        val bottomRadius = if (currentY + itemHeight >= y + menuHeight || index == islands.size - 1) 5f else 0f
                        NVGRenderer.drawRectangle(x, currentY, width, itemHeight, Mocha.Surface0.argb, topRadius, topRadius, bottomRadius, bottomRadius)
                    }

                    val text = island?.displayName ?: "All Islands"
                    val color = if (island == selectedIsland) Mocha.Mauve.argb else Mocha.Text.argb
                    NVGRenderer.drawText(text, x + 10f, currentY + 7f, 13f, color, NVGRenderer.defaultFont)
                }
                currentY += itemHeight
            }

            NVGRenderer.popScissor()
        }

        fun handleClick(x: Float, y: Float, width: Float, height: Float): Boolean {
            if (isOpen) {
                val islands = listOf(null) + SkyBlockIsland.entries
                val menuHeight = (islands.size * 28f).coerceAtMost(200f)
                val menuY = y + height + 2f

                if (isAreaHovered(x, menuY, width, menuHeight)) {
                    var currentY = menuY + scrollOffset
                    islands.forEach { island ->
                        if (isAreaHovered(x, currentY, width, 28f)) {
                            selectedIsland = island
                            isOpen = false
                            return true
                        }
                        currentY += 28f
                    }
                } else if (!isAreaHovered(x, y, width, height)) {
                    isOpen = false
                    return true
                }
            }

            if (isAreaHovered(x, y, width, height)) {
                isOpen = !isOpen
                return true
            }

            return false
        }

        fun handleScroll(x: Float, y: Float, width: Float, height: Float, amount: Float): Boolean {
            if (!isOpen) return false

            val islands = listOf(null) + SkyBlockIsland.entries
            val contentHeight = islands.size * 28f
            val menuHeight = contentHeight.coerceAtMost(200f)
            val menuY = y + height + 2f

            if (isAreaHovered(x, menuY, width, menuHeight)) {
                val maxScroll = -(contentHeight - menuHeight).coerceAtLeast(0f)
                scrollOffset = (scrollOffset + amount).coerceIn(maxScroll, 0f)
                return true
            }

            return false
        }
    }

    private class BindingEntry(
        val index: Int,
        val binding: Keybinds.KeybindEntry
    ) {
        private val deleteAnim = springValue(Mocha.Surface0.argb, 0.2f)
        private val deleteScaleAnim = springValue(1f, 0.25f)

        var commandInput = InputField(binding.command, "Command")
        var islandDropdown = IslandDropdown(binding.island)

        fun draw(x: Float, y: Float, width: Float) {
            NVGRenderer.drawRectangle(x, y, width, 56f, Mocha.Base.argb, 6f)
            NVGRenderer.drawHollowRectangle(x, y, width, 56f, 1f, Mocha.Surface0.argb, 6f)

            drawKeys(x + 10f, y + 14f)
            islandDropdown.draw(x + 138f, y + 14f, 140f, 28f)
            commandInput.draw(x + 286f, y + 14f, width - 340f, 28f)
            drawDelete(x + width - 38f, y + 14f)
        }

        private fun drawKeys(x: Float, y: Float) {
            val keysText = binding.keys.str()
            val width = 120f

            NVGRenderer.drawRectangle(x, y, width, 28f, Mocha.Surface0.argb, 4f)
            NVGRenderer.drawHollowRectangle(x, y, width, 28f, 1f, Mocha.Crust.argb, 4f)

            val textW = NVGRenderer.getTextWidth(keysText, 13f, NVGRenderer.defaultFont)
            NVGRenderer.drawText(keysText, x + (width - textW) / 2f, y + 7f, 13f, Mocha.Text.argb, NVGRenderer.defaultFont)
        }

        private fun drawDelete(x: Float, y: Float) {
            val size = 28f
            val isHovered = isAreaHovered(x, y, size, size)

            deleteAnim.value = if (isHovered) Mocha.Red.argb else Mocha.Surface0.argb
            deleteScaleAnim.value = if (isHovered) 1.08f else 1f

            val centerX = x + size / 2f
            val centerY = y + size / 2f
            val scaledSize = size * deleteScaleAnim.value
            val scaledX = centerX - scaledSize / 2f
            val scaledY = centerY - scaledSize / 2f

            NVGRenderer.drawRectangle(scaledX, scaledY, scaledSize, scaledSize, deleteAnim.value, 4f)

            val textW = NVGRenderer.getTextWidth("×", 18f, NVGRenderer.defaultFont)
            NVGRenderer.drawText("×", centerX - textW / 2f, centerY - 9f, 18f, Mocha.Text.argb, NVGRenderer.defaultFont)
        }
    }

    private fun List<Int>.str(): String =
        if (isEmpty()) "None" else joinToString(" + ") { it.str() }

    private fun Int.str(): String = when (this) {
        -1 -> "LMB"
        -2 -> "RMB"
        -3 -> "MMB"
        in Int.MIN_VALUE..-4 -> "M${-this - 1}"
        else -> InputConstants.Type.KEYSYM.getOrCreate(this).displayName.string.let {
            if (it.length == 1) it.uppercase() else it
        }
    }
}