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
package xyz.aerii.athen.api.location

import tech.thatgravyboat.skyblockapi.utils.extentions.toFormattedName
import xyz.aerii.athen.handlers.React

enum class SkyBlockIsland(val id: String, displayName: String? = null) {
    PRIVATE_ISLAND("dynamic"),
    HUB("hub"),
    DUNGEON_HUB("dungeon_hub"),
    THE_BARN("farming_1", "The Farming Islands"),
    THE_PARK("foraging_1"),
    GOLD_MINES("mining_1"),
    DEEP_CAVERNS("mining_2"),
    DWARVEN_MINES("mining_3"),
    CRYSTAL_HOLLOWS("crystal_hollows"),
    MINESHAFT("mineshaft"),
    SPIDERS_DEN("combat_1", "Spider's Den"),
    THE_END("combat_3"),
    CRIMSON_ISLE("crimson_isle"),
    GARDEN("garden"),
    BACKWATER_BAYOU("fishing_1"),
    GALATEA("foraging_2"),

    THE_RIFT("rift"),
    DARK_AUCTION("dark_auction"),
    THE_CATACOMBS("dungeon"),
    KUUDRA("kuudra"),
    JERRYS_WORKSHOP("winter", "Jerry's Workshop"),
    ;

    val inIsland: React<Boolean>
        get() = LocationAPI.island.map { it == this }

    val displayName = displayName ?: toFormattedName()

    override fun toString() = displayName

    companion object {
        fun getByKey(key: String) = entries.firstOrNull { it.id == key }

        fun inAnyIsland(vararg islands: SkyBlockIsland) = LocationAPI.island.map { it in islands }
    }
}
