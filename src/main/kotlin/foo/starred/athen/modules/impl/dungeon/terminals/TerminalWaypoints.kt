package foo.starred.athen.modules.impl.dungeon.terminals

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.api.dungeon.enums.DungeonClass
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractStyledBox
import foo.starred.athen.api.rendering.level.impl.extensions.impl.extractText
import foo.starred.athen.config.Category
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.markerAABB
import foo.starred.snowbird.api.data.Observable
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB

//? if >= 26.2
//import net.minecraft.world.phys.Vec3

@Load
object TerminalWaypoints : Module(
    "Terminal waypoints",
    "Waypoints with terminal, allows for a ton of customisation!",
    Category.DUNGEONS
) {
    // <editor-fold desc = "Brainfuck">
    private sealed class Node(val positions: List<BlockPos>, val defaultClass: DungeonClass, val configIndex: Int, val section: Int) {
        val aabb1: AABB = positions.first().markerAABB().inflate(0.0001)

        class Terminal(positions: List<BlockPos>, defaultClass: DungeonClass, configIndex: Int, section: Int) : Node(positions, defaultClass, configIndex, section)
        class Lever(positions: List<BlockPos>, defaultClass: DungeonClass, configIndex: Int, section: Int) : Node(positions, defaultClass, configIndex, section)
    }

    private val terminals = buildList {
        add(Node.Terminal(listOf(BlockPos(111, 113, 73), BlockPos(110, 113, 73)), DungeonClass.TANK, 1, 1))
        add(Node.Terminal(listOf(BlockPos(111, 119, 79), BlockPos(110, 119, 79)), DungeonClass.TANK, 2, 1))
        add(Node.Terminal(listOf(BlockPos(89, 112, 92), BlockPos(90, 112, 92)), DungeonClass.MAGE, 3, 1))
        add(Node.Terminal(listOf(BlockPos(89, 122, 101), BlockPos(90, 122, 101)), DungeonClass.MAGE, 4, 1))
        add(Node.Lever(listOf(BlockPos(94, 124, 113), BlockPos(94, 125, 113)), DungeonClass.ARCHER, 5, 1))
        add(Node.Lever(listOf(BlockPos(106, 124, 113), BlockPos(106, 125, 113)), DungeonClass.ARCHER, 6, 1))

        add(Node.Terminal(listOf(BlockPos(68, 109, 121), BlockPos(68, 109, 122)), DungeonClass.TANK, 7, 2))
        add(Node.Terminal(listOf(BlockPos(59, 120, 122), BlockPos(59, 119, 123)), DungeonClass.MAGE, 8, 2))
        add(Node.Terminal(listOf(BlockPos(47, 109, 121), BlockPos(47, 109, 122)), DungeonClass.BERSERK, 9, 2))
        add(Node.Terminal(listOf(BlockPos(39, 108, 143), BlockPos(39, 108, 142)), DungeonClass.ARCHER, 10, 2))
        add(Node.Terminal(listOf(BlockPos(40, 124, 122), BlockPos(40, 124, 123)), DungeonClass.BERSERK, 11, 2))
        add(Node.Lever(listOf(BlockPos(27, 124, 127), BlockPos(27, 125, 127)), DungeonClass.ARCHER, 12, 2))
        add(Node.Lever(listOf(BlockPos(23, 132, 138), BlockPos(23, 133, 138)), DungeonClass.HEALER, 13, 2))

        add(Node.Terminal(listOf(BlockPos(-3, 109, 112), BlockPos(-2, 109, 112)), DungeonClass.TANK, 14, 3))
        add(Node.Terminal(listOf(BlockPos(-3, 119, 93), BlockPos(-2, 119, 93)), DungeonClass.HEALER, 15, 3))
        add(Node.Terminal(listOf(BlockPos(19, 123, 93), BlockPos(18, 123, 93)), DungeonClass.BERSERK, 16, 3))
        add(Node.Terminal(listOf(BlockPos(-3, 109, 77), BlockPos(-2, 109, 77)), DungeonClass.ARCHER, 17, 3))
        add(Node.Lever(listOf(BlockPos(14, 122, 55), BlockPos(14, 123, 55)), DungeonClass.ARCHER, 18, 3))
        add(Node.Lever(listOf(BlockPos(2, 122, 55), BlockPos(2, 123, 55)), DungeonClass.ARCHER, 19, 3))

        add(Node.Terminal(listOf(BlockPos(41, 109, 29), BlockPos(41, 109, 30)), DungeonClass.TANK, 20, 4))
        add(Node.Terminal(listOf(BlockPos(44, 121, 29), BlockPos(44, 121, 30)), DungeonClass.ARCHER, 21, 4))
        add(Node.Terminal(listOf(BlockPos(67, 109, 29), BlockPos(67, 109, 30)), DungeonClass.BERSERK, 22, 4))
        add(Node.Terminal(listOf(BlockPos(72, 115, 48), BlockPos(72, 114, 47)), DungeonClass.HEALER, 23, 4))
        add(Node.Lever(listOf(BlockPos(86, 128, 46), BlockPos(86, 129, 46)), DungeonClass.HEALER, 24, 4))
        add(Node.Lever(listOf(BlockPos(84, 121, 34), BlockPos(84, 122, 34)), DungeonClass.HEALER, 25, 4))
    }

    private val classMapping = mapOf(0 to DungeonClass.HEALER, 1 to DungeonClass.MAGE, 2 to DungeonClass.BERSERK, 3 to DungeonClass.ARCHER, 4 to DungeonClass.TANK)
    private val classOptions = listOf("Healer", "Mage", "Berserk", "Archer", "Tank")

    private val checkClass by config.switch("Check dungeon class")
    private val showText by config.switch("Render text", true)
    private val depthTest by config.switch("Depth test", false)
    private val highlightStyle by config.selector("Highlight style", listOf("Outline", "Filled", "Both"))
    private val terminalColor by config.colorPicker("Terminal color", Catppuccin.Mocha.Blue.argb)
    private val leverColor by config.colorPicker("Lever color", Catppuccin.Mocha.Yellow.argb)

    private val section1 by config.group("Section 1")
    private val section2 by config.group("Section 2")
    private val section3 by config.group("Section 3")
    private val section4 by config.group("Section 4")

    // God forgive me for whatever the fuck this is, the dsl is a love-hate relationship
    private val configValues = terminals.associate { node ->
        val defaultClass = classMapping.entries.firstOrNull { it.value == node.defaultClass }?.key ?: 4
        val sectionNodes = terminals.groupBy { it.section }.getValue(node.section)
        val tS = sectionNodes.filterIsInstance<Node.Terminal>()
        val lS = sectionNodes.filterIsInstance<Node.Lever>()

        val label = when (node) {
            is Node.Terminal -> {
                val index = tS.indexOf(node) + 1
                "S${node.section} Terminal $index"
            }

            is Node.Lever -> {
                val side = if (lS.indexOf(node) == 0) "Right" else "Left"
                "S${node.section} $side lever"
            }
        }

        val section = when (node.section) {
            1 -> section1
            2 -> section2
            3 -> section3
            else -> section4
        }

        val value by section.selector("$label class", classOptions, defaultClass).unique("terminal_${node.configIndex}")
        node.configIndex to { value }
    }
    // </editor-fold>

    private var c: List<Node> = emptyList()
    private var r: Observable<Boolean> = Observable(false)

    init {
        DungeonAPI.P3Phase.onChange {
            r.value = it != 0
            c()
        }

        on<WorldRenderEvent.Extract> {
            val playerClass = DungeonAPI.dungeonClass
            for (t in c) {
                val allowedClass = classMapping[configValues[t.configIndex]?.invoke()] ?: t.defaultClass
                if (checkClass && (playerClass != null && playerClass != allowedClass)) continue

                val color = if (t is Node.Lever) leverColor else terminalColor
                val aabb = t.aabb1

                extractStyledBox(aabb, color, highlightStyle, 2f, depthTest)
                //~ if >= 26.2 't.positions.last().center' -> 'Vec3.atCenterOf(t.positions.last())'
                if (showText) extractText(t.defaultClass.str(), t.positions.last().center, depth = depthTest)
            }
        }.runWhen(r)

        on<LocationEvent.Server.Connect> {
            c = emptyList()
            r.value = false
        }
    }

    private fun c() {
        c = terminals.filter { it.section == DungeonAPI.P3Phase.value }
    }

    private fun DungeonClass.str() = when (this) {
        DungeonClass.MAGE -> "§7[§bMage§7]"
        DungeonClass.ARCHER -> "§7[§6Archer§7]"
        DungeonClass.TANK -> "§7[§aTank§7]"
        DungeonClass.HEALER -> "§7[§dHealer§7]"
        DungeonClass.BERSERK -> "§7[§4Berserk§7]"
    }
}
