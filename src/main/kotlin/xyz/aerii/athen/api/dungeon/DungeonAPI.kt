/*
 * Original work by [SkyblockAPI](https://github.com/SkyblockAPI/SkyblockAPI) and contributors (MIT License).
 * The MIT License (MIT)
 *
 * Copyright (c) 2025
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Modifications:
 *   Copyright (c) 2025 skies-starred
 *   Licensed under the BSD 3-Clause License.
 *
 * The original MIT license applies to the portions derived from SkyblockAPI.
 */
@file:Suppress("UNUSED")

package xyz.aerii.athen.api.dungeon

import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonKey
import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanOrArabic
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.dungeon.enums.DungeonClass
import xyz.aerii.athen.api.dungeon.enums.DungeonPlayer
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.events.*
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.Smoothie
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.handlers.Typo.stripped

@Priority
object DungeonAPI {
    // <editor-fold desc="Regex & Variables">
    private val watcherSpawnedAllRegex = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val watcherKilledAllRegex = Regex("\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.")

    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")

    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")

    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.")
    private val endRegex = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")
    private val bossStartRegex = Regex("^\\[BOSS] (?<boss>.+?):")

    private val uniqueClassRegex = Regex("Your .+ stats are doubled because you are the only player using this class!")
    private val sectionCompleteRegex = Regex("""^\w{1,16} (?:activated|completed) a \w+! \((?:7/7|8/8)\)$""")

    private val playerTabRegex = Regex("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    private val playerGhostRegex = Regex(" ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.")

    private val cataRegex = Regex("^ Catacombs (?<level>\\d+):")
    private val locationRegex = Regex(" *[⏣ф] *(?<location>(?:\\s?[^ൠ\\s]+)*)(?: ൠ x\\d)?")

    val bloodOpened: React<Boolean> = React(false)
    val bloodKilledAll: React<Boolean> = React(false)
    val bloodSpawnedAll: React<Boolean> = React(false)

    var floorStarted = false
        private set
    var floorCompleted = false
        private set

    var witherKeys = 0
        private set
    var bloodKeys = 0
        private set

    val F7Phase: React<Int> = React(0)
    val P3Phase: React<Int> = React(0)
    val floor: React<DungeonFloor?> = React(null)
    val inBoss: React<Boolean> = React(false)

    var uniqueClass = false
        private set

    var teammates: List<DungeonPlayer> = emptyList()
        private set

    var ownPlayer: DungeonPlayer? = null
        private set

    val dungeonClass: DungeonClass?
        get() = ownPlayer?.dungeonClass

    val classLevel: Int
        get() = ownPlayer?.classLevel ?: 0

    val cataLevel: Int
        get() = ownPlayer?.cataLevel ?: 0

    val isPaul: Boolean
        get() = Perk.EZPZ.active
    // </editor-fold>

    init {
        on<LocationEvent.IslandChange> { reset() }

        on<TabListEvent.Change> {
            val firstColumn = new.firstOrNull() ?: return@on
            val ownName = Smoothie.playerName
            val next = arrayOfNulls<DungeonPlayer>(5)

            for (i in 0 until 5) {
                val idx = 1 + i * 4
                if (idx >= firstColumn.size) continue

                val match = playerTabRegex.find(firstColumn[idx].stripped()) ?: continue
                val name = match.groups["name"]?.value ?: continue
                val classStr = match.groups["class"]?.value ?: "EMPTY"
                val level = match.groups["level"]?.value?.parseRomanOrArabic()

                val player = teammates.getOrNull(i)?.takeIf { it.name == name } ?: DungeonPlayer(name)

                player.dungeonClass = DungeonClass.get(classStr)
                player.classLevel = level
                player.dead = classStr == "DEAD"

                if (name == ownName) ownPlayer = player
                next[i] = player
            }

            teammates = next.filterNotNull()
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)

        on<TabListEvent.Change> {
            val lines = new.getOrNull(3) ?: return@on

            for (line in lines) {
                cataRegex.findThenNull(line.stripped(), "level") { (level) ->
                    if (level.toIntOrNull() == null || level.toIntOrNull() == cataLevel) return@findThenNull

                    ownPlayer?.cataLevel = level.toInt()
                } ?: return@on
            }
        }.runWhen(SkyBlockIsland.DUNGEON_HUB.inIsland)

        on<ScoreboardEvent.Update> {
            locationRegex.anyMatch(new, "location") { (location) ->
                dungeonFloorRegex.find(location, "floor") { (f) ->
                    val old = floor.value
                    val new = DungeonFloor.getByName(f)

                    if (old == new) return@find

                    floor.value = new
                    floor.value?.let { DungeonEvent.Enter(it).post() }
                }
            }
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)

        on<ChatEvent> {
            if (actionBar) return@on
            val message = message.stripped()

            playerGhostRegex.findThenNull(message, "name") { (name) ->
                var name = name
                if (name == "You") Smoothie.player?.let { name = it.name.stripped() }

                for (t in teammates) {
                    if (t.name != name) continue
                    t.dead = true
                    t.deaths++
                    DungeonEvent.Player.Death(t).post()
                    "DungeonAPI: Player died! Player deaths: ${t.deaths}".devMessage()
                }
            } ?: return@on

            if (!inBoss.value && floor.value != DungeonFloor.E) {
                bossStartRegex.findThenNull(message, "boss") { (boss) ->
                    if (boss == "The Watcher") return@findThenNull
                    inBoss.value = floor.value?.chatBossName == boss
                }
            }

            when {
                watcherSpawnedAllRegex.matches(message) -> {
                    bloodSpawnedAll.value = true
                    return@on
                }

                watcherKilledAllRegex.matches(message) -> {
                    bloodKilledAll.value = true
                    return@on
                }

                uniqueClassRegex.matches(message) -> {
                    uniqueClass = true
                    return@on
                }

                endRegex.matches(message) -> {
                    floorCompleted = true
                    floor.value?.let { (DungeonEvent.End(it)).post() }
                    "DungeonAPI: Floor ended.".devMessage()
                    return@on
                }

                !floorStarted && startRegex.matches(message) -> {
                    floorStarted = true
                    floor.value?.let { (DungeonEvent.Start(it)).post() }
                    "DungeonAPI: Floor started.".devMessage()
                    return@on
                }

                sectionCompleteRegex.matches(message) -> {
                    P3Phase.value++
                    "DungeonAPI: P3 Phase set to $P3Phase.".devMessage()
                    return@on
                }

                message == "[BOSS] Storm: I should have known that I stood no chance." -> {
                    P3Phase.value = 1
                    "DungeonAPI: P3 Phase set to 1.".devMessage()
                    return@on
                }

                message == "The Core entrance is opening!" -> {
                    P3Phase.value = 0
                    "DungeonAPI: P3 Phase set to 0.".devMessage()
                    return@on
                }
            }

            matchWhen(message) {
                case(keyObtainedRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(keyPickedUpRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(witherDoorOpenRegex) {
                    if (witherKeys > 0) --witherKeys
                }

                case(bloodDoorOpenRegex) {
                    if (bloodKeys > 0) --bloodKeys
                    bloodOpened.value = true
                }
            }
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)

        on<TickEvent.Client> {
            if (ticks % 5 != 0) return@on

            if (floor.value?.floorNumber == 7 && inBoss.value) {
                val y = Smoothie.player?.y ?: return@on

                F7Phase.value = when {
                    y > 210 -> 1
                    y > 155 -> 2
                    y > 100 -> 3
                    y > 45 -> 4
                    else -> 5
                }
            }
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)
    }

    fun reset() {
        "DungeonAPI: Cleaning up.".devMessage()

        bloodKilledAll.value = false
        bloodSpawnedAll.value = false
        bloodOpened.value = false

        floorCompleted = false
        floorStarted = false

        witherKeys = 0
        bloodKeys = 0

        uniqueClass = false
        inBoss.value = false
        F7Phase.value = 0
        P3Phase.value = 0
        floor.value = null

        teammates = emptyList()
        ownPlayer = null
    }

    private fun handleGetKey(type: String) {
        val key = DungeonKey.getById(type) ?: return
        when (key) {
            DungeonKey.WITHER -> ++witherKeys
            DungeonKey.BLOOD -> ++bloodKeys
        }

        DungeonEvent.KeyPickUp(key).post()
    }
}
