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

package xyz.aerii.athen.api.location

import net.hypixel.data.type.GameType
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.events.ScoreboardEvent
import xyz.aerii.athen.events.TabListEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.React
import kotlin.time.Clock
import kotlin.time.Instant

@Priority
object LocationAPI {
    private val locationRegex = Regex(" *[⏣ф] *(?<location>(?:\\s?[^ൠ\\s]+)*)(?: ൠ x\\d)?")
    private val guestRegex = Regex("^ *\u270C *\\((?<guests>\\d+)/(?<max>\\d+)\\) *$")
    private val playerCountRegex = Regex(" *(?:players|party) \\((?<count>\\d+)\\) *")

    var forceOnSkyBlock: Boolean = false

    val isOnSkyBlock = React(false).onChange { (if (it) LocationEvent.SkyBlockJoin else LocationEvent.SkyBlockLeave).post() }

    val island = React<SkyBlockIsland?>(null)

    val area = React(SkyBlockArea.NONE)

    var serverId: String? = null
        private set

    var isGuest: Boolean = false
        private set

    var onHypixel: Boolean = false
        private set

    var onAlpha: Boolean = false
        private set

    var playerCount: Int = 0
        get() = field.coerceAtLeast(McClient.players.size)
        private set

    val maxPlayerCount: Int?
        get() = when {
            serverId?.startsWith("mega") == true -> 60
            else -> when (island.value) {
                SkyBlockIsland.PRIVATE_ISLAND, SkyBlockIsland.GARDEN -> null
                SkyBlockIsland.KUUDRA -> 4
                SkyBlockIsland.MINESHAFT -> 4
                SkyBlockIsland.THE_CATACOMBS -> 5
                SkyBlockIsland.BACKWATER_BAYOU -> 16
                SkyBlockIsland.HUB -> 26
                SkyBlockIsland.JERRYS_WORKSHOP -> 27
                SkyBlockIsland.DARK_AUCTION -> 30
                else -> 24
            }
        }

    var lastServerChange: Instant = Instant.DISTANT_PAST
        private set

    init {
        on<LocationEvent.ServerChange> {
            lastServerChange = Clock.System.now()
            isOnSkyBlock.value = type == GameType.SKYBLOCK

            val newIsland = if (isOnSkyBlock.value && mode != null) SkyBlockIsland.getByKey(mode) else null
            val oldIsland = island.value

            island.value = newIsland
            LocationEvent.IslandChange(oldIsland, newIsland).post()
            serverId = name
        }

        on<TabListEvent.Change> {
            val component = new.firstOrNull()?.firstOrNull() ?: return@on
            playerCount = playerCountRegex.findGroup(component.stripped.lowercase(), "count")?.toIntOrNull() ?: 0
        }.runWhen(isOnSkyBlock)

        on<ScoreboardEvent.UpdateTitle> {
            isGuest = new.contains("guest", ignoreCase = true)
        }.runWhen(isOnSkyBlock)

        on<ScoreboardEvent.Update> {
            locationRegex.anyMatch(added, "location") { (location) ->
                val old = area.value
                area.value = SkyBlockArea.getByKey(location) ?: SkyBlockArea.NONE
                LocationEvent.AreaChange(old, area.value).post()
            }

            guestRegex.anyMatch(added, "guests") { (current) ->
                playerCount = current.toIntOrNull() ?: 0
            }
        }.runWhen(isOnSkyBlock)

        on<LocationEvent.ServerDisconnect> {
            reset()
        }
    }

    private fun reset() {
        val oldArea = area.value
        val oldIsland = island.value

        isOnSkyBlock.value = false
        area.value = SkyBlockArea.NONE
        island.value = null

        isGuest = false
        onHypixel = false
        onAlpha = false
        serverId = null

        if (oldArea != SkyBlockArea.NONE) LocationEvent.AreaChange(oldArea, area.value).post()
        if (oldIsland != null) LocationEvent.IslandChange(oldIsland, island.value).post()
    }
}
