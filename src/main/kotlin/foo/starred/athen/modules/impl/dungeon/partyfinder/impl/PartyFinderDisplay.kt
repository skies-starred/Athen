@file:Suppress("Unused", "ObjectPropertyName")

package foo.starred.athen.modules.impl.dungeon.partyfinder.impl

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.location.SkyBlockIsland
import foo.starred.athen.api.profile.ProfileAPI
import foo.starred.athen.api.profile.data.PlayerProfileStats
import foo.starred.athen.api.rendering.ui.shapes.rectangle.rectangle
import foo.starred.athen.api.rendering.ui.text.vanilla.extensions.extractText
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.PacketEvent
import foo.starred.athen.events.core.runWhen
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.dungeon.partyfinder.data.PartyFinderSlotData
import foo.starred.athen.modules.impl.dungeon.partyfinder.enums.PartyFinderClassType
import foo.starred.athen.modules.impl.dungeon.partyfinder.enums.PartyFinderSlotStatus
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.contains
import foo.starred.athen.utils.lore
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.mainThread
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.alpha
import foo.starred.snowbird.utils.formatted
import foo.starred.snowbird.utils.stripped
import foo.starred.snowbird.utils.toMS
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanNumeral
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroups
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours

@Load
@OnlyIn(islands = [SkyBlockIsland.DUNGEON_HUB])
object PartyFinderDisplay : Module(
    "Party finder display",
    "Displays stats of party finder groups in the menu.",
    Category.DUNGEONS
) {
    private val stats = config.switch("Show stats").unique("showStats")
    private val stack = config.switch("Party stack size", true).unique("stackSize")

    private val highlight = config.switch("Highlight parties").unique("highlight")
    val `color$allowed` by config.colorPicker("Joinable color", 0x55FF55)
    val `color$maybe` by config.colorPicker("Dupe color", 0xFFFF00)
    val `color$blocked` by config.colorPicker("Blocked color", 0xFF0000)
    val `color$vc` by config.colorPicker("VC color", 0x7300FF)
    val `color$perm` by config.colorPicker("Perm color", 0x00FFFF)
    val `color$carry` by config.colorPicker("Carry color", 0x640000)
    private val _unused by config.information("Want to hide a color? You can set it's opacity to 0!")

    private val noteRegex = Regex("^Note: (?<note>.+)")
    private val floorRegex = Regex("^Floor: Floor (?<floor>[IV]+)$")
    private val selectedRegex = Regex("^Currently Selected: (?<class>\\w+)$")
    private val nameRegex = Regex("^ (?<name>\\w+): (?<class>\\w+) \\((?<level>\\d+)\\)$")

    private val cache: Object2ObjectOpenHashMap<String, Pair<Long, PlayerProfileStats>> = Object2ObjectOpenHashMap()
    private val data: ConcurrentHashMap<Int, PartyFinderSlotData> = ConcurrentHashMap()

    private var menu0: Boolean = false
    private var menu1: Boolean = false

    private var klass: PartyFinderClassType? = null

    init {
        Scheduler.repeat(1.hours) {
            val i0 = System.currentTimeMillis() - 1.hours.inWholeMilliseconds
            val it = cache.object2ObjectEntrySet().fastIterator()

            while (it.hasNext()) if (it.next().value.first < i0) it.remove()
        }

        on<GuiEvent.Open.Container> {
            menu0 = stripped == "Party Finder"
            menu1 = stripped == "Catacombs Gate"
        }

        on<GuiEvent.Close.Container> {
            when {
                menu0 -> {
                    menu0 = false
                    data.clear()
                }

                menu1 -> {
                    menu1 = false
                }
            }
        }

        on<GuiEvent.Slots.Render.Any.Pre> {
            if (!menu0) return@on
            val color = data[slot.index]?.status?.color?.takeIf { it.alpha > 10 } ?: return@on

            graphics.rectangle(slot.x, slot.y, 16, 16, color)
        }.runWhen(highlight.state)

        on<GuiEvent.Slots.Render.Any.Post> {
            if (!menu0) return@on
            val s = data[slot.index]?.members?.size?.toString() ?: return@on

            graphics.extractText(s, slot.x + 17 - client.font.width(s), slot.y + 18 - client.font.lineHeight)
        }.runWhen(stack.state)

        on<PacketEvent.Receive, ClientboundContainerSetContentPacket> {
            if (!menu0 && !menu1) return@on

            val it0 = items.getOrNull(45)?.takeIf { !it.isEmpty } ?: return@on
            val lore0 = it0.get(DataComponents.LORE)?.lines() ?: return@on

            if (menu1) {
                klass = PartyFinderClassType.get(selectedRegex.findGroup(lore0.getOrNull(2)?.stripped() ?: return@on, "class") ?: return@on)
                return@on
            }

            if (!menu0) {
                return@on
            }

            if (lore0.getOrNull(1)?.stripped() != "defeat a Dungeon.") {
                menu0 = false
                return@on
            }

            data.clear()
            val all = mutableSetOf<String>()

            for ((i, it) in items.withIndex()) {
                if (i >= 54) break
                if (it.item != Items.PLAYER_HEAD) continue

                val lore = it.lore()?.takeIf { it.size >= 4 } ?: continue
                val lore0 = lore.map { it.stripped() }

                val members = mutableSetOf<Pair<String, PartyFinderClassType>>()
                val classes = mutableSetOf<PartyFinderClassType>()

                for (l in lore0) {
                    if (members.size == 5) break
                    if (l.isEmpty()) continue

                    val pair = nameRegex.findGroups(l, "name", "class") ?: continue
                    val klass = PartyFinderClassType.get(pair["class"] ?: continue) ?: continue
                    val name = pair["name"] ?: continue

                    all += name
                    classes += klass
                    members += Pair(name, klass)
                }

                if (members.isEmpty()) {
                    continue
                }

                val master = "Master Mode" in lore0.first()
                val blocked = lore0.last().contains("Requires Catacombs Level", "Requires a Class at Level", "Complete previous floor first!", "You must complete")
                val floor = floorRegex.findGroup(lore0[1], "floor")?.parseRomanNumeral() ?: 0
                val note = noteRegex.findGroup(lore0[2], "note")

                val status = when {
                    blocked -> PartyFinderSlotStatus.BLOCKED
                    klass in classes -> PartyFinderSlotStatus.MAYBE
                    note?.contains("vc", true) == true -> PartyFinderSlotStatus.VC
                    note?.contains("perm", true) == true -> PartyFinderSlotStatus.PERM
                    note?.contains("carry", true) == true -> PartyFinderSlotStatus.CARRY
                    else -> PartyFinderSlotStatus.ALLOWED
                }

                data[i] = PartyFinderSlotData(floor, master, members, status)
                it.set(DataComponents.LORE, ItemLore(lore.build(floor, master, classes, true)))
            }

            lore()
            if (!stats.value) return@on
            ProfileAPI.get(all.takeIf { it.isNotEmpty() }?.filter { cache[it] == null }?.takeIf { it.isNotEmpty() } ?: return@on) { kv ->
                mainThread {
                    val now = System.currentTimeMillis()
                    for ((k, v) in kv) cache[k] = Pair(now, v)
                    lore()
                }
            }
        }
    }

    private fun lore() {
        //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
        val slots = (client.screen as? AbstractContainerScreen<*>)?.menu?.slots ?: return

        for ((k, v) in data) {
            val item = slots.getOrNull(k)?.item ?: continue
            if (item.item != Items.PLAYER_HEAD) continue

            val lore = item.get(DataComponents.LORE)?.lines()?.takeIf { it.isNotEmpty() } ?: continue
            item.set(DataComponents.LORE, ItemLore(lore.build(v.floor, v.master, v.members.map { it.second }, false)))
        }
    }

    private fun List<Component>.build(floor: Int, master: Boolean, classes: Collection<PartyFinderClassType>, set: Boolean): List<Component> {
        val lore = mutableListOf<Component>()

        if (!stats.value) {
            lore += this
            if (set) lore += classes.build()
            return lore
        }

        for (l in this) {
            val l0 = l.stripped()
            var l1 = l

            nameRegex.findThenNull(l0, "name", "class", "level") { (name, klass, level) ->
                val klass = PartyFinderClassType.get(klass) ?: return@findThenNull
                l1 = cache[name]?.second?.build(floor, master, l.color(), klass, level.toInt()) ?: return@findThenNull
            }

            lore += l1
        }

        if (set) lore += classes.build()
        return lore
    }

    private fun PlayerProfileStats.build(floor: Int, master: Boolean, color: Int?, klass: PartyFinderClassType, level: Int): Component {
        val pb = (if (master) dungeons?.`pbs$master` else dungeons?.`pbs$normal`)?.get(floor)?.let { (it / 1000).toMS() } ?: "?"
        val level1 = dungeons?.catacombs ?: "?"
        val secrets = dungeons?.secrets ?: "?"
        val average = dungeons?.`secrets$average`?.formatted()?.dropLast(1) ?: "?"

        return "<${color ?: "aqua"}> $name <dark_gray>[${klass.fancy}$level <dark_gray>| <yellow>C$level1<dark_gray>] [<green>$secrets <dark_gray>| <green>$average<dark_gray>] [<aqua>$pb<dark_gray>]".parse().apply { style = style.withItalic(false) }
    }

    private fun Collection<PartyFinderClassType>.build(): Component {
        var root = "<red>Missing: "
        var first = true

        for (a in PartyFinderClassType.entries) {
            if (a in this) continue

            if (!first) root += " <gray>| "
            first = false

            val color = if (a == klass) Catppuccin.Mocha.Teal.argb else Catppuccin.Mocha.Red.argb
            root += "<$color>${a.full}"
        }

        return root.parse().apply { style = style.withItalic(false) }
    }

    private fun Component.color(): Int? {
        val s = siblings
        var prev: Component? = null

        for (c in s) {
            if (c.string == ": ") return prev?.style?.color?.value
            prev = c
        }

        return null
    }
}
