package xyz.aerii.athen.modules.impl.dungeon

import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import tech.thatgravyboat.skyblockapi.api.profile.party.PartyFinderAPI
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.extentions.getRawLore
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanNumeral
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.italic
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.events.MessageEvent
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Notifier.notify
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Smoothie.mainThread
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Texter.onHover
import xyz.aerii.athen.handlers.Texter.parse
import xyz.aerii.athen.handlers.Typo.command
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.*
import xyz.aerii.athen.utils.render.Render2D.drawRectangle
import java.awt.Color
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@Load
@OnlyIn(islands = [SkyBlockIsland.DUNGEON_HUB])
object PartyFinder : Module(
    "Party finder",
    "Shows stats in party finder, on player join, and highlights.",
    Category.DUNGEONS
) {
    private val floorRegex = Regex("^Floor: Floor (?<floor>[IV]+)$")
    private val nameRegex = Regex("^ (?<username>\\w+): (?<className>\\w+) \\((?<classLevel>\\d+)\\)$")
    private val classRegex = Regex("^Currently Selected: (?<className>\\w+)$")
    private val pfJoinRegex = Regex("^Party Finder > (?:\\[.{1,7}])? ?(?<name>.{1,16}) joined the dungeon group! \\(.*\\)$")
    private val noteRegex = Regex("^Note: (?<note>.+)")

    private var inPartyFinder = false
    private var inMainGate = false
    private var currentClass: String? = null

    private val slotData = mutableMapOf<Int, PartyData>()
    private val pfCache = mutableMapOf<String, CachedStats>()
    private val statsCache = mutableMapOf<String, CachedStats>()

    private val showStats = config.switch("Show stats").custom("showStats")
    private val statsToShow by config.multiCheckbox("Stats to show", listOf("Class level", "Cata level", "Secrets", "Secret average", "Personal best", "Missing classes"), listOf(0, 1, 2, 3, 4, 5)).dependsOn { showStats.value }

    private val highlight = config.switch("Highlight parties").custom("highlight")
    private val joinableColor by config.colorPicker("Joinable", Color(0,255, 0)).dependsOn { highlight.value }
    private val dupeColor by config.colorPicker("Dupe", Color(255, 255, 0)).dependsOn { highlight.value }
    private val blockedColor by config.colorPicker("Blocked", Color(255, 0, 0)).dependsOn { highlight.value }
    private val highlightSpecial by config.switch("Highlight special", true).dependsOn { highlight.value }
    private val vcColor by config.colorPicker("VC", Color(115, 0, 255)).dependsOn { highlight.value && highlightSpecial }
    private val permColor by config.colorPicker("Perm", Color(0, 255, 255)).dependsOn { highlight.value && highlightSpecial }
    private val carryColor by config.colorPicker("Carry", Color(100, 0, 0)).dependsOn { highlight.value && highlightSpecial }

    private val _joinStats by config.expandable("Stats on join")
    private val joinStats by config.switch("Stats on join").childOf { _joinStats }
    private val detectFloor by config.switch("Detect floor", true).childOf { _joinStats }
    private val selectedFloor by config.dropdown("Floor", listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7"), 6).childOf { _joinStats }.dependsOn { !detectFloor }
    private val masterMode by config.switch("Master mode").childOf { _joinStats }.dependsOn { !detectFloor }

    private val _autoKick by config.expandable("Auto kick")
    private val autoKick by config.switch("Auto kick").childOf { _autoKick }
    private val detectKickFloor by config.switch("Detect floor", true).childOf { _autoKick }
    private val kickFloor by config.dropdown("Floor", listOf("F7", "M4", "M5", "M6", "M7"), 0).childOf { _autoKick }.dependsOn { !detectKickFloor }
    private val requiredPB by config.textInput("Required PB", placeholder = "5:30").childOf { _autoKick }
    private val requiredSecrets by config.textInput("Required secrets", placeholder = "50k").childOf { _autoKick }
    private val requiredSecretAvg by config.textInput("Required secret average", placeholder = "8.4").childOf { _autoKick }
    private val requiredMP by config.textInput("Required MP", placeholder = "800").childOf { _autoKick }
    private val sendKickMessage by config.switch("Send kick message").childOf { _autoKick }
    private val sendKickToParty by config.switch("Send to party", false).childOf { _autoKick }.dependsOn { sendKickMessage }

    private val canKick: Boolean
        get() = autoKick && PartyAPI.leader?.name == Smoothie.playerName

    private data class CachedStats(
        val stats: PlayerStats,
        val storedAt: Long = System.currentTimeMillis()
    )

    private data class PartyMember(
        val username: String,
        val className: String
    )

    private data class PartyData(
        val floor: Int,
        val isMaster: Boolean,
        val members: Set<PartyMember>,
        val status: SlotStatus,
        val special: Special
    )

    private enum class SlotStatus {
        BLOCKED,
        DUPE,
        ALLOWED
    }

    private enum class Special {
        NONE,
        CARRY,
        VC,
        PERM
    }

    init {
        Chronos.Time every 1.hours repeat {
            val now = System.currentTimeMillis()
            pfCache.entries.removeIf { (_, cached) -> now - cached.storedAt > 1.hours.inWholeMicroseconds }
            statsCache.entries.removeIf { (_, cached) -> now - cached.storedAt > 1.hours.inWholeMicroseconds }
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("stats") {
                    thenCallback("username", StringArgumentType.word()) {
                        val username = StringArgumentType.getString(this, "username")

                        val cached = statsCache[username]
                        if (cached != null && !cached.stats.loading) return@thenCallback cached.stats.stats(username)

                        fetchPlayerStats(username, setOf("armor_data", "talisman_bag_data"), onSuccess = { stats ->
                            statsCache[username] = CachedStats(stats)
                            stats.stats(username)
                        })
                    }
                }
            }
        }

        on<MessageEvent.Chat.Receive> {
            if (!joinStats && !canKick) return@on

            val username = pfJoinRegex.findGroup(stripped, "name") ?: return@on
            if (username == client.player?.name?.string) return@on

            val cached = statsCache[username]
            if (cached != null && !cached.stats.loading) {
                if (joinStats) cached.stats.stats(username)
                if (autoKick) cached.stats.kick(username)
                return@on
            }

            fetchPlayerStats(username, setOf("armor_data", "talisman_bag_data"), onSuccess = { stats ->
                statsCache[username] = CachedStats(stats)

                if (joinStats) stats.stats(username)
                if (canKick) stats.kick(username)
            })
        }

        on<GuiEvent.Slots.Render.Pre> {
            if (!inPartyFinder) return@on

            val status = slotData[slot.index]?.status ?: return@on
            val special = slotData[slot.index]?.special ?: return@on

            if (special != Special.NONE) {
                when (special) {
                    Special.VC -> graphics.drawRectangle(slot.x, slot.y, 16, 16, vcColor)
                    Special.PERM -> graphics.drawRectangle(slot.x, slot.y, 16, 16, permColor)
                    Special.CARRY -> graphics.drawRectangle(slot.x, slot.y, 16, 16, carryColor)
                }

                return@on
            }

            when (status) {
                SlotStatus.BLOCKED -> graphics.drawRectangle(slot.x, slot.y, 16, 16, blockedColor)
                SlotStatus.DUPE -> graphics.drawRectangle(slot.x, slot.y, 16, 16, dupeColor)
                SlotStatus.ALLOWED -> graphics.drawRectangle(slot.x, slot.y, 16, 16, joinableColor)
            }
        }.runWhen(highlight.state)

        on<PacketEvent.Receive, ClientboundOpenScreenPacket> {
            val stripped = title.stripped()
            inPartyFinder = stripped == "Party Finder"
            inMainGate = stripped == "Catacombs Gate"
        }

        on<PacketEvent.Receive, ClientboundContainerClosePacket> {
            reset()
        }

        on<PacketEvent.Send, ServerboundContainerClosePacket> {
            reset()
        }

        on<PacketEvent.Receive, ClientboundContainerSetContentPacket> {
            if (inMainGate) {
                val rawLore = items.getOrNull(45)?.getRawLore() ?: return@on

                for (l in rawLore) {
                    classRegex.findThenNull(l, "className") { (cls) ->
                        currentClass = cls
                    } ?: break
                }

                return@on
            }

            if (!inPartyFinder) return@on
            with (items.getOrNull(45)?.getRawLore() ?: return@on) {
                val l = this.getOrNull(1) ?: return@with
                if (l == "defeat a Dungeon.") return@with

                inPartyFinder = false
                return@on
            }

            slotData.clear()
            val all = mutableSetOf<String>()

            for ((i, it) in items.withIndex()) {
                if (i >= 54) break
                if (it == null) continue
                if (it.item != Items.PLAYER_HEAD) continue

                val lore = it.getLore().takeIf { it.size >= 4 } ?: continue
                val strippedLore = lore.map { it.stripped() }

                val master = "Master Mode" in strippedLore.first()
                val blocked = strippedLore.last().let {
                    "Requires Catacombs Level" in it ||
                    "Requires a Class at Level" in it ||
                    "Complete previous floor first!" in it
                }

                var dupe = false

                val floor = floorRegex.findGroup(strippedLore[1], "floor")?.parseRomanNumeral() ?: -1
                val members = mutableSetOf<PartyMember>()
                val foundClasses = mutableSetOf<String>()

                var note: String? = null
                val l = strippedLore[2]
                if ("Note: " in l) {
                    noteRegex.findThenNull(l, "note") { (n) ->
                        note = n
                    }
                }

                for (l in strippedLore) {
                    if (members.size == 5) break
                    if (l.isEmpty()) continue

                    nameRegex.findThenNull(l, "username", "className") { (user, cls) ->
                        foundClasses += cls
                        members += PartyMember(user, cls)
                        all += user
                    }
                }

                if (floor == -1 || members.isEmpty()) continue
                if (currentClass != null && currentClass in foundClasses) dupe = true

                val status = when {
                    blocked -> SlotStatus.BLOCKED
                    dupe -> SlotStatus.DUPE
                    else -> SlotStatus.ALLOWED
                }

                val special = when {
                    note == null -> Special.NONE
                    note.contains("vc", true) -> Special.VC
                    note.contains("perm", true) -> Special.PERM
                    note.contains("carry", true) -> Special.CARRY
                    else -> Special.NONE
                }

                slotData[i] = PartyData(floor, master, members, status, special)
                it.set(DataComponents.LORE, ItemLore(lore.buildLore(floor, master, foundClasses)))
            }

            if (!showStats.value) return@on
            if (all.isEmpty()) return@on

            val names = all.filter { pfCache[it] == null }.takeIf { it.isNotEmpty() } ?: return@on lore()
            fetchPlayerStats(names, onSuccess = { results ->
                for ((u, s) in results) pfCache[u] = CachedStats(s)
                mainThread { lore() }
            })
        }
    }

    private fun lore() {
        val screen = client.screen as? AbstractContainerScreen<*> ?: return

        for ((i, d) in slotData) {
            val slot = screen.menu.slots.getOrNull(i) ?: continue
            val item = slot.item
            if (item.item != Items.PLAYER_HEAD) continue

            val lore = item.getLore()
            val foundClasses = d.members.map { it.className }.toSet()
            val finalLore = lore.buildLore(d.floor, d.isMaster, foundClasses, false)

            item.set(DataComponents.LORE, ItemLore(finalLore))
        }
    }

    private fun PlayerStats.kick(username: String) {
        val floor = PartyFinderAPI.queuedDungeonFloor

        val pb =
            if (detectKickFloor && floor != null) {
                val master = "Master Mode" in floor.longName
                (if (master) masterPB else normalPB)?.get(floor.floorNumber)
            } else {
                val master = kickFloor > 0
                (if (master) masterPB else normalPB)?.get(if (master) kickFloor + 3 else 7)
            }

        val secrets = secrets ?: 0
        val avg = secretAvg ?: 0.0
        val mp = bagData?.calculateMP(abiphoneContacts, consumedPrism) ?: 0

        val pbReqStr = requiredPB.takeIf(String::isNotBlank)
        val pbReq = pbReqStr?.fromHMS()?.toLong()
        val secretsReq = requiredSecrets.takeIf(String::isNotBlank)?.unabbreviate()
        val avgReq = requiredSecretAvg.takeIf(String::isNotBlank)?.toDoubleOrNull()
        val mpReq = requiredMP.takeIf(String::isNotBlank)?.unabbreviate()

        val reasons = mutableListOf<String>()

        pbReq?.let {
            val pb0 = pb?.let { t -> (t / 1000).toInt() }
            if (pb0 == null || pb0 > it) reasons += "PB: ${pb?.time()} > $pbReqStr"
        }

        secretsReq?.let {
            if (secrets < it) reasons += "Secrets: $secrets < $it"
        }

        avgReq?.let {
            if (avg < it) reasons += "Secret average: $avg < $it"
        }

        mpReq?.let {
            if (mp < it) reasons += "MP: $mp < $it"
        }

        if (reasons.isEmpty()) return
        "p kick $username".command()

        if (!sendKickMessage) return
        val reason = reasons.joinToString(", ")
        "Kicked <aqua>$username<r>: $reason".notify()

        if (!sendKickToParty) return
        Chronos.Time after 100.milliseconds then {
            "pc [Athen] Kicked $username: $reason".command()
        }
    }

    private fun PlayerStats.stats(username: String) {
        val floor = PartyFinderAPI.queuedDungeonFloor
        var floorDisplay: String
        var pb: Long?

        if (detectFloor && floor != null) {
            val isMaster = floor.longName.contains("Master Mode")
            floorDisplay = if (isMaster) "M${floor.floorNumber}" else "F${floor.floorNumber}"
            pb = (if (isMaster) masterPB else normalPB)?.get(floor.floorNumber)
        } else {
            val floor = selectedFloor + 1
            floorDisplay = if (masterMode) "M$floor" else "F$floor"
            pb = (if (masterMode) masterPB else normalPB)?.get(floor)
        }

        val cat = "§eC${catLevel ?: "?"}"
        val secretsStr = "§a${secrets ?: "?"}"
        val avgStr = secretAvg?.let { "§b%.1f".format(it) } ?: "§7?"
        val mpStr = bagData?.calculateMP(abiphoneContacts, consumedPrism)?.let { "§d$it" } ?: "§7?"

        "§8§m${"-".repeatBreak()}".lie()
        "§6Stats for §b$username §8[$cat§8] §8[§5MP: $mpStr§8]".lie()
        "  §7Secrets: $secretsStr §8| §7Avg: $avgStr".lie()

        val pbHover = buildString {
            for (f in 1..7) {
                append("§7F$f§8: §9${normalPB?.get(f).time()} §8| " + "§7M$f§8: §9${masterPB?.get(f).time()}")
                if (f < 7) append('\n')
            }
        }

        "  §7$floorDisplay PB§8: §9${pb.time()}".literal().onHover(pbHover).lie()

        armor
            ?.parseItem()
            ?.reversed()
            ?.takeIf { it.isNotEmpty() }
            ?.let { pieces ->
                "  §7Armor:".lie()

                for (p in pieces) {
                    val lore = p.lore ?: continue
                    val hover = p.name + "\n" + lore.joinToString("\n")
                    val str = when (p.index) {
                        3 -> "⛑"
                        2 -> "👕"
                        1 -> "👖"
                        else -> "👢"
                    }

                    "    §8[$str§8] ${p.name}".literal().onHover(hover).lie()
                }
            }

        "§8§m${"-".repeatBreak()}".lie()
    }

    private fun PlayerStats.buildLine(floor: Int, master: Boolean, username: String, usernameColor: Int?, className: String, classLevel: Int): Component {
        val pb = (if (master) masterPB else normalPB)?.get(floor)
        val pbStr = pb?.let {
            val m = it / 60000
            val s = (it / 1000) % 60
            "$m:${s.toString().padStart(2, '0')}"
        } ?: "No S+"

        val level = catLevel ?: "?"
        val secretsFound = secrets ?: "?"
        val secretAvg = secretAvg?.let { "%.1f".format(it) } ?: "?"
        val klass = ClassType.fromName(className) ?: ClassType.entries.random()

        val showClass = 0 in statsToShow
        val showCata = 1 in statsToShow
        val showSecrets = 2 in statsToShow
        val showAvg = 3 in statsToShow
        val showPB = 4 in statsToShow

        val extra = buildString {
            if (showClass || showCata) {
                append(" <dark_gray>[")
                if (showClass) append("<aqua>${klass.fancy}$classLevel")
                if (showClass && showCata) append("<dark_gray> | ")
                if (showCata) append("<yellow>C$level")
                append("<dark_gray>]")
            }

            if (showSecrets || showAvg) {
                append(" <dark_gray>[")
                if (showSecrets) append("<green>$secretsFound")
                if (showSecrets && showAvg) append("<dark_gray>/")
                if (showAvg) append("<aqua>$secretAvg")
                append("<dark_gray>]")
            }

            if (showPB) append(" <dark_gray>[<blue>$pbStr<dark_gray>]")
        }

        return "<${usernameColor ?: "aqua"}> $username$extra".parse().apply { italic = false }
    }

    private fun List<Component>.buildLore(floor: Int, master: Boolean, foundClasses: Set<String>, setClasses: Boolean = true): List<Component> {
        val lore = map { line ->
            if (!showStats.value) return@map line

            val stripped = line.stripped()
            var updatedLine = line

            nameRegex.findThenNull(stripped, "username", "className", "classLevel") { (username, clsN, clsL) ->
                pfCache[username]?.takeIf { !it.stats.loading }?.let { stats ->
                    updatedLine = stats.stats.buildLine(floor, master, username, line.get(), clsN, clsL.toInt())
                }
            }

            updatedLine
        }.toMutableList()

        if (setClasses && 5 in statsToShow) lore.add(foundClasses.buildLine())
        return lore
    }

    private fun Set<String>.buildLine(): Component {
        var root = "<${TextColor.RED}>Missing: "

        var first = true
        for (cls in ClassType.fullNames) {
            if (cls in this) continue

            if (!first) root += " <gray>| "
            first = false

            val color = if (cls == currentClass) Catppuccin.Mocha.Teal.argb else Catppuccin.Mocha.Red.argb
            root += "<$color>$cls"
        }

        return root.parse().apply { italic = false }
    }

    private fun Component.get(): Int? {
        val s = siblings
        var prev: Component? = null

        for (c in s) {
            if (c.string == ": ") return prev?.style?.color?.value
            prev = c
        }

        return null
    }

    private fun Long?.time(): String =
        this?.let { (it / 1000.0).toMS() } ?: "No S+"

    private fun reset() {
        when {
            inPartyFinder -> {
                inPartyFinder = false
                slotData.clear()
            }

            inMainGate -> inMainGate = false
        }
    }

    private enum class ClassType(short: String, val full: String, color: String) {
        ARCHER("A", "Archer", "<orange>"),
        BERSERK("B", "Berserk", "<red>"),
        HEALER("H", "Healer", "<pink>"),
        MAGE("M", "Mage", "<aqua>"),
        TANK("T", "Tank", "<dark_green>")
        ;

        val fancy = "$color$short"

        companion object {
            val fullNames = entries.map { it.full }

            fun fromName(name: String): ClassType? = entries.firstOrNull { it.full == name }
        }
    }
}