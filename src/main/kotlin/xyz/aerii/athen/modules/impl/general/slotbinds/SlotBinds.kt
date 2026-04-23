package xyz.aerii.athen.modules.impl.general.slotbinds

import com.mojang.serialization.Codec
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.inventory.ClickType
import org.lwjgl.glfw.GLFW
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.PlayerEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.guiClick
import xyz.aerii.athen.utils.render.Render2D.drawLine
import xyz.aerii.athen.utils.render.Render2D.drawOutline
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.client
import xyz.aerii.library.api.pressed
import kotlin.collections.iterator

@Load
object SlotBinds : Module(
    "Slot binds",
    "Bindings for slots!",
    Category.GENERAL
) {
    private val bind by config.keybind("Bind keybind", GLFW.GLFW_KEY_B)
    private val swap by config.keybind("Swap keybind", GLFW.GLFW_KEY_LEFT_SHIFT)
    private val lock = config.switch("Lock bound slots").custom("lock")

    private val scribble = Scribble("features/slotBinds")
    private val fields = scribble.mutableMap("binds", Codec.STRING, Codec.INT)

    private var last: Int? = null

    init {
        on<GuiEvent.Slots.Click> {
            val s = client.screen as? InventoryScreen ?: return@on
            val h = slot?.index ?: return@on
            val a = fields.value
            val hs = h.toString()

            if (lock.value) {
                val b = a.fn(h, hs)
                if (b != null) cancel()
            }

            val b0 = bind.bound && bind.pressed
            val b1 = swap.bound && swap.pressed
            if (!b0 && !b1) return@on

            if (b0) {
                cancel()

                val l = last
                if (l != null) {
                    last = null
                    if (l == h) return@on

                    val b2 = l.fn()
                    val b3 = h.fn()
                    if (b2 == b3) return@on

                    val a = (if (b2) h else l).toString()
                    val b = if (b2) l else h

                    fields.update {
                        entries.removeIf { it.key == a || it.value == b }
                        this[a] = b
                    }

                    return@on
                }

                val a = fields.value
                val b = a.fn(h, hs)

                if (b != null) {
                    fields.update {
                        if (containsKey(hs)) remove(hs)
                        else entries.removeIf { it.value == h }
                    }

                    return@on
                }

                last = h
                return@on
            }

            if (mouseButton != 0) return@on
            val b = a.fn(h, hs) ?: return@on
            val b3 = h.fn()

            if (!b3 && !b.fn()) return@on

            val c = if (b3) b else h
            val d = (if (b3) h else b) - 36

            guiClick(s.menu.containerId, c, d, ClickType.SWAP)
            cancel()
        }

        on<GuiEvent.Slots.Render.Post> {
            val s = client.screen as? InventoryScreen ?: return@on
            val m = s.menu.slots
            if (slot != m.last()) return@on

            for ((a, b) in fields.value) {
                val a = m.getOrNull(a.toInt()) ?: continue
                val b = m.getOrNull(b) ?: continue

                graphics.drawLine(a.x + 8, a.y + 8, b.x + 8, b.y + 8, Catppuccin.Mocha.Mauve.argb, 1)

                graphics.drawOutline(a.x, a.y, 16, 16, 1, Catppuccin.Mocha.Mauve.argb, true)
                graphics.drawOutline(b.x, b.y, 16, 16, 1, Catppuccin.Mocha.Mauve.argb, true)
            }
        }

        on<PlayerEvent.Drop> {
            val h = client.player?.inventory?.selectedSlot?.plus(36) ?: return@on
            val a = fields.value
            val hs = h.toString()

            val b = a[hs] ?: a.entries.find { it.value == h }?.key?.toInt()
            if (b == null) return@on

            cancel()
        }.runWhen(lock.state)
    }

    private fun MutableMap<String, Int>.fn(h: Int, hs: String): Int? {
        return this[hs] ?: entries.find { it.value == h }?.key?.toInt()
    }

    private fun Int.fn(): Boolean {
        return this in 36..44
    }
}