@file:Suppress("Unused", "ObjectPropertyName", "ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.dungeon.terminals.solver

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.dungeon.terminals.TerminalAPI
import xyz.aerii.athen.api.dungeon.terminals.TerminalType
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.DungeonEvent
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Scurry
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Smoothie.play
import xyz.aerii.athen.mixin.accessors.KeyMappingAccessor
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.impl.*
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.nvg.NVGSpecialRenderer
import xyz.aerii.athen.utils.url
import java.awt.Color

@Load
object TerminalSolver : Module(
    "Terminal solver",
    "Shows solutions for F7/M7 terminals in a nice custom gui!",
    Category.DUNGEONS
) {
    private val settingsExpandable by config.expandable("Settings")
    val fcDelay by config.slider("First click delay", 350, 150, 1000, "ms").childOf { settingsExpandable }
    val dropKey by config.switch("Allow using drop key", true).childOf { settingsExpandable }
    val keybindL by config.keybind("Keybind left click").childOf { settingsExpandable }
    val keybindR by config.keybind("Keybind right click").childOf { settingsExpandable }

    private val guiExpandable by config.expandable("GUI")
    val `ui$scale` by config.slider("Scale", 1f, 0.1f, 2f, showDouble = true).childOf { guiExpandable }
    val `ui$roundness` by config.slider("Roundness", 0f, 0f, 10f, showDouble = true).childOf { guiExpandable }
    val `ui$bg` by config.colorPicker("Background", Color(0, 0, 0, 150)).childOf { guiExpandable }
    val `ui$border` by config.colorPicker("Border", Color(Mocha.Mauve.rgba)).childOf { guiExpandable }
    val `ui$header` by config.colorPicker("Header", Color(20, 20, 20, 200)).childOf { guiExpandable }
    val `ui$slots$fill` by config.switch("Slots: Fill").childOf { guiExpandable }
    val `ui$slots$roundness` by config.slider("Slots: Roundness", 0f, 0f, 10f, showDouble = true).childOf { guiExpandable }
    val `ui$numbers$showText` by config.switch("Numbers: Text", true).childOf { guiExpandable }

    private val soundExpandable by config.expandable("Sounds")
    val `sound$enabled` by config.switch("Enable sounds").childOf { soundExpandable }
    private val clickSound = config.textInput("Sound", "block.note_block.pling").childOf { soundExpandable }.custom($$"sound$click")
    private val _unused by config.button("Play") { `sound$click`?.play(`sound$volume`, `sound$pitch`) }.childOf { soundExpandable }
    private val _unused0 by config.button("Open sounds list") { "https://www.digminecraft.com/lists/sound_list_pc.php".url() }.childOf { soundExpandable }
    val `sound$pitch` by config.slider("Pitch", 1f, 0f, 1f, showDouble = true).childOf { soundExpandable }
    val `sound$volume` by config.slider("Volume", 1f, 0f, 1f, showDouble = true).childOf { soundExpandable }
    var `sound$click`: SoundEvent? = null

    private val colorExpandable by config.expandable("Colors")
    val `colors$correct` by config.colorPicker("Colors: Solution", Color(0, 255, 0, 180)).childOf { colorExpandable }
    val `names$correct` by config.colorPicker("Names: Solution", Color(0, 255, 0, 180)).childOf { colorExpandable }
    val `panes$correct` by config.colorPicker("Panes: Solution", Color(0, 255, 0, 180)).childOf { colorExpandable }
    val `numbers$first` by config.colorPicker("Numbers: 1st", Color(0, 255, 0, 180)).childOf { colorExpandable }
    val `numbers$second` by config.colorPicker("Numbers: 2nd", Color(0, 200, 0, 180)).childOf { colorExpandable }
    val `numbers$third` by config.colorPicker("Numbers: 3rd", Color(0, 150, 0, 180)).childOf { colorExpandable }
    val `rubix$positive` by config.colorPicker("Rubix: Positive", Color(0, 114, 255, 180)).childOf { colorExpandable }
    val `rubix$negative` by config.colorPicker("Rubix: Negative", Color(205, 0, 0, 180)).childOf { colorExpandable }
    val `melody$fill` by config.colorPicker("Melody: Fill", Color(Mocha.Mauve.rgba)).childOf { colorExpandable }
    val `melody$correct` by config.colorPicker("Melody: Correct", Color(0, 255, 0, 180)).childOf { colorExpandable }
    val `melody$wrong` by config.colorPicker("Melody: Wrong", Color(205, 0, 0, 180)).childOf { colorExpandable }
    val `melody$other` by config.colorPicker("Melody: Other", Color(Mocha.Base.rgba)).childOf { colorExpandable }

    private val solvers = mapOf(
        TerminalType.NUMBERS to NumbersSolver,
        TerminalType.PANES to PanesSolver,
        TerminalType.NAME to NameSolver,
        TerminalType.COLORS to ColorsSolver,
        TerminalType.RUBIX to RubixSolver,
        TerminalType.MELODY to MelodySolver
    )

    init {
        `sound$click` = clickSound.value.prs()

        clickSound.state.onChange {
            `sound$click` = it.prs()
        }

        on<GuiEvent.Container.Render.Pre> {
            val term = TerminalAPI.currentTerminal ?: return@on
            val solver = solvers[term] ?: return@on

            cancel()
            NVGSpecialRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) { solver.main() }
        }.runWhen(TerminalAPI.terminalOpen)

        on<GuiEvent.Input.Mouse.Press> {
            val term = TerminalAPI.currentTerminal ?: return@on
            cancel()
            if (System.currentTimeMillis() - TerminalAPI.openTime >= fcDelay) c(mouse = keyEvent.button())
        }.runWhen(TerminalAPI.terminalOpen)

        on<GuiEvent.Input.Key.Press> {
            TerminalAPI.currentTerminal ?: return@on
            if (System.currentTimeMillis() - TerminalAPI.openTime < fcDelay) return@on

            when (keyEvent.key) {
                keybindL -> {
                    c(mouse = 0)
                    cancel()
                }

                keybindR -> {
                    c(mouse = 1)
                    cancel()
                }

                (client.options?.keyDrop as? KeyMappingAccessor)?.boundKey?.value if (dropKey) -> {
                    c(mouse = if (!OmniKeyboard.isCtrlKeyPressed) 0 else 1)
                    cancel()
                }
            }
        }.runWhen(TerminalAPI.terminalOpen)

        on<DungeonEvent.Terminal.Update> {
            TerminalAPI.currentTerminal?.let { solvers[it] }?.update(slot, item)
        }

        on<DungeonEvent.Terminal.Open> {
            for (s in solvers.values) s.onOpen()
        }

        on<DungeonEvent.Terminal.Close> {
            for (s in solvers.values) s.onClose()
        }
    }

    private fun c(mouse: Int) {
        val solver = solvers[TerminalAPI.currentTerminal] ?: return
        val uiScale = 3f * `ui$scale`
        val mx = Scurry.rawX / uiScale
        val my = Scurry.rawY / uiScale

        val width = client.window.width / uiScale
        val height = client.window.height / uiScale

        solver.click(mx, my, width, height, mouse)
    }

    private fun String.prs(): SoundEvent? {
        val p = ResourceLocation.tryParse(this) ?: return null
        return SoundEvent.createVariableRangeEvent(p)
    }
}