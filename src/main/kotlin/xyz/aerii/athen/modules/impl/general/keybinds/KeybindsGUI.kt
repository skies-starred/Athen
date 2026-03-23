package xyz.aerii.athen.modules.impl.general.keybinds

import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.handlers.Scram
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.add
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.remove
import xyz.aerii.athen.modules.impl.general.keybinds.Keybinds.update
import xyz.aerii.athen.modules.impl.general.keybinds.ui.*
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import xyz.aerii.athen.utils.render.Render2D.text

object KeybindsGUI : Scram("Keybinds Manager [Athen]") {
    private val entries = mutableListOf<BindingEntry>()
    private val zones = mutableListOf<UIZone>()
    private val categoryBar = CategoryBar(110, 20)
    private val listRenderer = BindingsListRenderer(28, 4, 16, 6)
    private val modal = ModalRenderer(380, 260, 16, 6)

    override fun onScramInit() {
        recreate()
        modal.close()
        categoryBar.cancelCreate()
    }

    override fun onScramClose() = Keybinds.storage.save()

    override fun isPauseScreen() = false

    override fun onScramRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        zones.clear()
        for (e in entries) e.toggleAnim += ((if (e.binding.enabled) 1f else 0f) - e.toggleAnim) * delta * 0.4f

        guiGraphics.drawRectangle(0, 0, width, height, Mocha.Crust.withAlpha(0.6f))

        val px = (width - 576) / 2
        val py = (height - 300) / 2

        categoryBar.draw(guiGraphics, mouseX, mouseY, px, py, 300, modal.open, zones)

        val mainX = px + 116
        guiGraphics.drawRectangle(mainX, py, 460, 300, Mocha.Base.argb)
        guiGraphics.drawOutline(mainX, py, 460, 300, 1, Mocha.Surface0.argb)

        val list = categoryBar.selected?.let { s -> entries.filter { it.binding.category == s } } ?: entries
        listRenderer.draw(guiGraphics, mouseX, mouseY, mainX + 6, py + 6, 448, 260, list, modal.open, zones)
        drawFooter(guiGraphics, mouseX, mouseY, mainX, py)

        if (!modal.open) categoryBar.drawTooltip(guiGraphics)
        if (modal.open) modal.draw(guiGraphics, mouseX, mouseY, width, height, zones)
    }

    private fun drawFooter(guiGraphics: GuiGraphics, mx: Int, my: Int, mainX: Int, py: Int) {
        val fy = py + 272
        guiGraphics.drawRectangle(mainX, fy, 460, 1, Mocha.Surface0.argb)

        val btnX = mainX + 170
        val fieldY = fy + 6
        val hovered = !modal.open && mx in btnX until btnX + 120 && my in fieldY until fieldY + 16
        guiGraphics.drawRectangle(btnX, fieldY, 120, 16, if (hovered) Mocha.Surface2.argb else Mocha.Surface1.argb)
        guiGraphics.drawOutline(btnX, fieldY, 120, 16, 1, Mocha.Green.argb)
        guiGraphics.text("+ Create Keybind", btnX + (120 - client.font.width("+ Create Keybind")) / 2, fieldY + (16 - client.font.lineHeight) / 2 + 1, false, Mocha.Green.argb)
        zones.add(UIZone(btnX, fieldY, 120, 16, UIZoneType.BUTTON_CREATE))
    }

    override fun onScramMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (modal.open) {
            clickModal(mouseX, mouseY, button)
            return true
        }

        if (categoryBar.creating) {
            val z = zones.firstOrNull { it.type == UIZoneType.CATEGORY_ADD }
            if (z != null && mouseX in z.x until z.x + z.w && mouseY in z.y until z.y + z.h) {
                if (button == 0) {
                    categoryBar.nameField.focused = true
                    categoryBar.nameField.updateClick(mouseX, z.x)
                }

                return true
            }

            categoryBar.confirmCreate()
            recreate()
            return true
        }

        if (button == 1) {
            val z = zones.lastOrNull { it.type == UIZoneType.CATEGORY_TAB && it.category.isNotEmpty() && mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h }
            categoryBar.deleting = if (z == null || categoryBar.deleting == z.category) null else z.category
            return true
        }

        if (button != 0) return false

        val hit = zones.lastOrNull { mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h } ?: return false
        if (hit.type == UIZoneType.BUTTON_CREATE) {
            modal.open()
            return true
        }

        if (hit.type == UIZoneType.ENTRY_EDIT) {
            entries.firstOrNull { it.index == hit.data }?.let { modal.open(it) }
            return true
        }

        if (hit.type == UIZoneType.ENTRY_DELETE) {
            entries.firstOrNull { it.index == hit.data }?.let { if (it.index.remove()) recreate() }
            return true
        }

        if (hit.type == UIZoneType.ENTRY_TOGGLE) {
            entries.firstOrNull { it.index == hit.data }?.let {
                it.index.update(it.binding.keys, it.binding.command, !it.binding.enabled, it.binding.category, it.condition)
                recreate()
            }

            return true
        }

        if (hit.type == UIZoneType.CATEGORY_TAB) {
            if (categoryBar.deleting != null && hit.category == categoryBar.deleting) {
                Keybinds.removeCategory(hit.category)
                if (categoryBar.selected == hit.category) categoryBar.selected = null
                categoryBar.deleting = null
                recreate()
                return true
            }

            categoryBar.deleting = null
            categoryBar.selected = hit.category.ifEmpty { null }
            return true
        }

        if (hit.type == UIZoneType.CATEGORY_TOGGLE) {
            categoryBar.deleting = null
            Keybinds.categories.value.getOrNull(hit.data)?.let {
                Keybinds.toggleCategory(it.name)
                recreate()
            }

            return true
        }

        if (hit.type == UIZoneType.CATEGORY_ADD) {
            categoryBar.startCreate()
            return true
        }

        return true
    }

    private fun clickModal(mouseX: Int, mouseY: Int, button: Int) {
        if (modal.keysListening) {
            val z = zones.firstOrNull { it.type == UIZoneType.MODAL_KEYS }
            if (z != null && mouseX in z.x until z.x + z.w && mouseY in z.y until z.y + z.h) {
                modal.recorded.add(-(button + 1))
                return
            }

            if (modal.recorded.isNotEmpty()) {
                modal.keysListening = false
                modal.keysBuf = modal.recorded.toMutableList()
            }

            return
        }

        if (modal.categoryOpen) return modal.clickCategory(mouseX, mouseY)
        if (modal.workInOpen) return modal.clickWorkIn(mouseX, mouseY)
        if (modal.islandOpen) return modal.clickIsland(mouseX, mouseY)
        if (modal.floorOpen) return modal.clickFloor(mouseX, mouseY)
        if (modal.classOpen) return modal.clickClass(mouseX, mouseY)
        if (modal.f7PhaseOpen) return modal.clickF7Phase(mouseX, mouseY)
        if (button != 0) return

        val hit = zones.lastOrNull { mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h}
        val pre = modal.cmdField.focused
        modal.cmdField.focused = false

        if (hit == null) return

        if (hit.type == UIZoneType.MODAL_CMD) {
            modal.cmdField.focused = true
            if (pre) modal.cmdField.updateClick(mouseX, hit.x)
            return
        }

        if (hit.type == UIZoneType.MODAL_KEYS) {
            modal.keysListening = true
            modal.recorded.clear()
            return
        }

        if (hit.type == UIZoneType.MODAL_CATEGORY) {
            modal.categoryOpen = !modal.categoryOpen
            return
        }

        if (hit.type == UIZoneType.MODAL_WORK_IN) {
            modal.workInOpen = !modal.workInOpen
            return
        }

        if (hit.type == UIZoneType.MODAL_ISLAND) {
            modal.islandOpen = !modal.islandOpen
            return
        }

        if (hit.type == UIZoneType.MODAL_FLOOR) {
            modal.floorOpen = !modal.floorOpen
            return
        }

        if (hit.type == UIZoneType.MODAL_CLASS) {
            modal.classOpen = !modal.classOpen
            return
        }

        if (hit.type == UIZoneType.MODAL_F7_PHASE) {
            modal.f7PhaseOpen = !modal.f7PhaseOpen
            return
        }

        if (hit.type == UIZoneType.MODAL_SAVE) return saveModal()

        if (hit.type == UIZoneType.MODAL_CANCEL) return modal.close()
    }

    override fun onScramMouseScroll(mouseX: Int, mouseY: Int, horizontal: Double, vertical: Double): Boolean {
        val amount = (vertical * 10).toInt()

        if (modal.open) {
            modal.scroll(amount)
            return true
        }

        if (mouseX < (width - 576) / 2 + 110) {
            categoryBar.handleScroll(amount, 300)
            return true
        }

        if (entries.isEmpty()) return false
        listRenderer.handleScroll(amount)
        return true
    }

    override fun onScramKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (modal.open) {
            if (modal.keysListening) return when (keyCode) {
                GLFW.GLFW_KEY_ENTER -> {
                    if (modal.recorded.isEmpty()) return true
                    modal.keysListening = false
                    modal.keysBuf = modal.recorded.toMutableList()
                    true
                }

                GLFW.GLFW_KEY_ESCAPE -> {
                    modal.keysListening = false
                    modal.recorded.clear()
                    true
                }

                else -> {
                    if (keyCode <= 0) return false
                    modal.recorded.add(keyCode)
                    true
                }
            }

            if (modal.cmdField.focused && modal.cmdField.handleKey(keyCode, modifiers)) return true
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) modal.close()
            return true
        }

        if (categoryBar.creating) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                categoryBar.confirmCreate()
                recreate()
                return true
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                categoryBar.cancelCreate()
                return true
            }

            if (categoryBar.nameField.focused) categoryBar.nameField.handleKey(keyCode, modifiers)
            return true
        }

        return super.onScramKeyPress(keyCode, scanCode, modifiers)
    }

    override fun onScramCharType(char: Char, modifiers: Int): Boolean {
        if (modal.open) {
            if (modal.cmdField.focused && modal.cmdField.handleChar(char)) return true
            return true
        }

        if (categoryBar.creating && categoryBar.nameField.focused) {
            if (categoryBar.nameField.handleChar(char)) return true
            return true
        }

        return super.onScramCharType(char, modifiers)
    }

    private fun saveModal() {
        if (modal.cmdField.value.isEmpty() || modal.keysBuf.isEmpty()) return

        if (modal.entry == null) {
            modal.keysBuf.add(modal.cmdField.value, modal.category, modal.condition)
            recreate()
            modal.close()
            return
        }

        val e = modal.entry!!
        e.index.update(modal.keysBuf, modal.cmdField.value, e.binding.enabled, modal.category, modal.condition)

        recreate()
        modal.close()
    }

    private fun recreate() {
        entries.clear()
        for ((i, b) in Keybinds.bindings.value.withIndex()) entries.add(BindingEntry(i, b))
    }
}