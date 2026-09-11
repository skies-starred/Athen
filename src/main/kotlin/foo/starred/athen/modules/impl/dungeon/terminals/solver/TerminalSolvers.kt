@file:Suppress("Unused", "ObjectPropertyName", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.dungeon.terminals.solver

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.Load
import foo.starred.athen.api.dungeon.terminals.TerminalAPI
import foo.starred.athen.api.dungeon.terminals.TerminalType
import foo.starred.athen.config.Category
import foo.starred.athen.events.DungeonEvent
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.PacketEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.mixin.accessors.KeyMappingAccessor
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.dungeon.terminals.solver.impl.MelodySolver
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.ctrl
import foo.starred.snowbird.utils.mouseSX
import foo.starred.snowbird.utils.mouseSY
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

@Load
object TerminalSolvers : Module(
    "Terminal solvers",
    "Shows solutions for F7/M7 terminals in a nice custom gui!",
    Category.DUNGEONS
) {
    private val settings by config.group("Settings")
    val firstClick by settings.slider("First click delay", 350, 150, 1000, "ms")
    val clickDelay by settings.slider("Click delay", 100, 0, 500, "ms")
    private val info by settings.information("Click delay only affects \"Panes\" terminal type.")
    val resync by settings.slider("Resync timeout", 800, 0, 2000, "ms")
    val dropKey by settings.switch("Allow using drop key", true)
    val keybindL by settings.keybind("Keybind left click")
    val keybindR by settings.keybind("Keybind right click")
    val solve by settings.multiSelector("Enabled solvers", listOf("Colors", "Melody", "Name", "Numbers", "Panes", "Rubix"), listOf(0, 1, 2, 3, 4, 5))

    private val rubix by config.group("Rubix")
    val `rubix$left` by rubix.switch("Left click only", true)

    private val melody by config.group("Melody")
    val `melody$prevent` by melody.switch("Prevent wrong clicks")
    val `melody$num` by melody.switch("Number keys")
    val `melody$key0` by melody.keybind("Keybind 1", InputConstants.KEY_1)
    val `melody$key1` by melody.keybind("Keybind 2", InputConstants.KEY_2)
    val `melody$key2` by melody.keybind("Keybind 3", InputConstants.KEY_3)
    val `melody$key3` by melody.keybind("Keybind 4", InputConstants.KEY_4)

    private val gui by config.group("GUI")
    val `ui$scale` by gui.slider("Scale", 1f, 0.1f, 4f, double = true)
    val `ui$roundness` by gui.slider("Roundness", 4f, 0f, 10f, double = true)
    val `ui$borderWidth` by gui.slider("Border width", 1f, 0.5f, 5f, double = true)
    val `ui$blur` by gui.switch("Blur menu", true)
    val `ui$padding` by gui.slider("Padding", 5f, 0f, 20f, double = true)
    val `ui$gap` by gui.slider("Slot gap", 2f, 0f, 10f, double = true)
    val `ui$melodyGap` by gui.slider("Melody gap", 2f, 0f, 10f, double = true)
    val `ui$bg` by gui.colorPicker("Background color", 0xAF0C0C0F)
    val `ui$border` by gui.colorPicker("Border color", 0xDC000000)

    private val slots by config.group("Slots")
    val `ui$slots$fill` by slots.switch("Fill", true)
    val `ui$slots$roundness` by slots.slider("Roundness", 3f, 0f, 10f, double = true)
    val `ui$numbers$showText` by slots.switch("Numbers: Show text", true)

    private val header by config.group("Header")
    val `ui$hideHeader` by header.switch("Hide header", true)
    val `ui$hideTitle` by header.switch("Hide title", true)
    val `ui$titleColor` by header.colorPicker("Title color", 0xE6F0F0F5)
    val `ui$header` by header.colorPicker("Header color", 0xC80A0A0C)

    val clicks by config.sound("Click sound", enabled = false )

    private val colors by config.group("Solver colors")
    val `colors$correct` by colors.colorPicker("Colors: Solution", 0xDC2ECC71)
    val `names$correct` by colors.colorPicker("Names: Solution", 0xDC2ECC71)
    val `panes$correct` by colors.colorPicker("Panes: Solution", 0xDC2ECC71)
    val `numbers$first` by colors.colorPicker("Numbers: 1st", 0xDC2ECC71)
    val `numbers$second` by colors.colorPicker("Numbers: 2nd", 0xDCF1C40F)
    val `numbers$third` by colors.colorPicker("Numbers: 3rd", 0xDCE74C3C)
    val `rubix$positive` by colors.colorPicker("Rubix: Positive", 0xDC3498DB)
    val `rubix$negative` by colors.colorPicker("Rubix: Negative", 0xDCE74C3C)
    val `melody$fill` by colors.colorPicker("Melody: Fill", 0xDC9B59B6)
    val `melody$correct` by colors.colorPicker("Melody: Correct", 0xDC2ECC71)
    val `melody$wrong` by colors.colorPicker("Melody: Wrong", 0xDCE74C3C)
    val `melody$other` by colors.colorPicker("Melody: Other", 0xC818181C)

    val scale: Float
        get() = `ui$scale` * maxOf(1f, 4f / client.window.guiScale.toFloat())

    var last: Long = 0

    init {
        on<PacketEvent.Receive, ClientboundSoundPacket> {
            if (!clicks.enabled) return@on
            if (sound.value() != SoundEvents.EXPERIENCE_ORB_PICKUP) return@on

            it.cancel()
            clicks.play()
        }.runWhen(TerminalAPI.opened)

        on<GuiEvent.Render.Screen.Pre> {
            val term = TerminalAPI.terminal?.impl ?: return@on

            cancel()
            term.main(graphics)
        }.runWhen(TerminalAPI.opened)

        on<GuiEvent.Input.Mouse.Press> {
            val term = TerminalAPI.terminal ?: return@on

            cancel()
            if (System.currentTimeMillis() - TerminalAPI.open >= firstClick) c(mouse = keyEvent.button())
        }.runWhen(TerminalAPI.opened)

        on<GuiEvent.Input.Key.Press> {
            val t = TerminalAPI.terminal ?: return@on
            if (System.currentTimeMillis() - TerminalAPI.open < firstClick) return@on

            when (keyEvent.key) {
                `melody$key0` if t == TerminalType.MELODY -> {
                    MelodySolver.click(1)
                }

                `melody$key1` if t == TerminalType.MELODY -> {
                    MelodySolver.click(2)
                }

                `melody$key2` if t == TerminalType.MELODY -> {
                    MelodySolver.click(3)
                }

                `melody$key3` if t == TerminalType.MELODY -> {
                    MelodySolver.click(4)
                }

                keybindL -> {
                    c(mouse = 0)
                    cancel()
                }

                keybindR -> {
                    c(mouse = 1)
                    cancel()
                }

                (client.options.keyDrop as? KeyMappingAccessor)?.boundKey?.value if (dropKey) -> {
                    c(mouse = if (!ctrl) 0 else 1)
                    cancel()
                }
            }
        }.runWhen(TerminalAPI.opened)

        on<TickEvent.Client.End> {
            val a = TerminalAPI.terminal ?: return@on
            val b = a.impl

            if (resync == 0) return@on
            if (!b.pending) return@on
            if (System.currentTimeMillis() - last <= resync) return@on

            b.pending = false
            b.resync()
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            b.update((client.screen as? AbstractContainerScreen<*>)?.menu?.items?.subList(0, a.slots) ?: return@on)
        }.runWhen(TerminalAPI.opened)

        on<DungeonEvent.Terminal.Update> {
            TerminalAPI.terminal?.impl?.update(items)
        }

        on<DungeonEvent.Terminal.Open> {
            for (a in TerminalType.entries) a.impl.open()
        }

        on<DungeonEvent.Terminal.Close> {
            for (a in TerminalType.entries) a.impl.close()
        }
    }

    private fun c(mouse: Int) {
        val solver = TerminalAPI.terminal?.impl ?: return
        val scale = scale
        val mx = mouseSX / scale
        val my = mouseSY / scale

        val width = client.window.guiScaledWidth.toFloat() / scale
        val height = client.window.guiScaledHeight.toFloat() / scale

        solver.click(mx, my, width, height, mouse)
    }
}
