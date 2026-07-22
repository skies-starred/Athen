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
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.dungeon.enums.DungeonClass
import xyz.aerii.athen.api.dungeon.enums.DungeonPlayer
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.events.*
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.library.api.name
import xyz.aerii.library.api.player
import xyz.aerii.library.handlers.Observable
import xyz.aerii.library.utils.stripped

@Priority
object DungeonAPI {
    // <editor-fold desc="Regex & Variables">
    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")

    private val endRegex = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")
    private val bossStartRegex = Regex("^\\[BOSS] (?<boss>.+?):")

    private val uniqueClassRegex = Regex("^Your .+ stats are doubled because you are the only player using this class!$")
    private val sectionCompleteRegex = Regex("""^\w{1,16} (?:activated|completed) a \w+! \((?:7/7|8/8)\)$""")

    private val playerTabRegex = Regex("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    private val playerGhostRegex = Regex("^ ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.$")

    private val cataRegex = Regex("^ Catacombs (?<level>\\d+):")
    private val locationRegex = Regex(" *[⏣ф\uE067\uE020] *(?<location>(?:\\s?[^[ൠ\uE018]\\s]+)*)(?: [ൠ\uE018] x\\d)?")

    val bloodOpened: Observable<Boolean> = Observable(false)
    val bloodKilledAll: Observable<Boolean> = Observable(false)
    val bloodSpawnedAll: Observable<Boolean> = Observable(false)

    val F7Phase: Observable<Int> = Observable(0)
    val P3Phase: Observable<Int> = Observable(0)
    val floor: Observable<DungeonFloor?> = Observable(null)
    val inBoss: Observable<Boolean> = Observable(false)

    var started = false
        private set

    var complete = false
        private set

    var unique = false
        private set

    var teammates: List<DungeonPlayer> = emptyList()
        private set

    var self: DungeonPlayer? = null
        private set

    val dungeonClass: DungeonClass?
        get() = self?.dungeonClass
    // </editor-fold>

    init {
        on<LocationEvent.Hypixel.Island> {
            "DungeonAPI: Cleaning up.".devMessage()

            bloodKilledAll.value = false
            bloodSpawnedAll.value = false
            bloodOpened.value = false

            complete = false
            started = false

            unique = false
            inBoss.value = false
            F7Phase.value = 0
            P3Phase.value = 0
            floor.value = null

            teammates = emptyList()
            self = null
        }

        on<TabListEvent.Change> {
            val firstColumn = new.firstOrNull() ?: return@on
            val str = name
            val next = arrayOfNulls<DungeonPlayer>(5)

            for (i in 0 until 5) {
                val idx = 1 + i * 4
                if (idx >= firstColumn.size) continue

                val match = playerTabRegex.find(firstColumn[idx].stripped()) ?: continue
                val name = match.groups["name"]?.value ?: continue
                val classStr = match.groups["class"]?.value ?: "EMPTY"

                val player = teammates.getOrNull(i)?.takeIf { it.name == name } ?: DungeonPlayer(name)
                player.dungeonClass = DungeonClass.get(classStr)

                if (name == str) self = player
                next[i] = player
            }

            teammates = next.filterNotNull()
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)

        on<LocationEvent.Hypixel.Area> {
            dungeonFloorRegex.find(new.string, "floor") { (f) ->
                val old = floor.value
                val new = DungeonFloor.getByName(f)
                if (old == new) return@find

                floor.value = new
                floor.value?.let { DungeonEvent.Enter(it).post() }
            }
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)

        on<MessageEvent.Chat.Receive> {
            playerGhostRegex.findThenNull(stripped, "name") { (s) ->
                var n0 = s
                if (n0 == "You") n0 = name

                for (t in teammates) {
                    if (t.name != n0) continue
                    t.deaths++
                    DungeonEvent.Player.Death(t).post()
                    "DungeonAPI: Player died! Player deaths: ${t.deaths}".devMessage()
                }
            } ?: return@on

            if (!inBoss.value && floor.value != DungeonFloor.E) {
                bossStartRegex.findThenNull(stripped, "boss") { (boss) ->
                    if (boss == "The Watcher") return@findThenNull
                    inBoss.value = floor.value?.chatBossName == boss
                }
            }

            when {
                stripped == "[BOSS] The Watcher: That will be enough for now." -> {
                    bloodSpawnedAll.value = true
                }

                stripped == "[BOSS] The Watcher: You have proven yourself. You may pass." -> {
                    bloodKilledAll.value = true
                }

                uniqueClassRegex.matches(stripped) -> {
                    unique = true
                }

                endRegex.matches(stripped) -> {
                    complete = true
                    floor.value?.let { (DungeonEvent.End(it)).post() }
                    "DungeonAPI: Floor ended.".devMessage()
                }

                !started && stripped == "[NPC] Mort: Here, I found this map when I first entered the dungeon." -> {
                    started = true
                    floor.value?.let { (DungeonEvent.Start(it)).post() }
                    "DungeonAPI: Floor started.".devMessage()
                }

                sectionCompleteRegex.matches(stripped) -> {
                    P3Phase.value++
                    "DungeonAPI: P3 Phase set to $P3Phase.".devMessage()
                }

                stripped == "[BOSS] Storm: I should have known that I stood no chance." -> {
                    P3Phase.value = 1
                    "DungeonAPI: P3 Phase set to 1.".devMessage()
                }

                stripped == "The Core entrance is opening!" -> {
                    P3Phase.value = 0
                    "DungeonAPI: P3 Phase set to 0.".devMessage()
                }
            }
        }.runWhen(SkyBlockIsland.THE_CATACOMBS.inIsland)

        on<TickEvent.Client.End> {
            if (ticks % 5 != 0) return@on

            if (floor.value?.floorNumber == 7 && inBoss.value) {
                val y = player?.y ?: return@on

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
}
