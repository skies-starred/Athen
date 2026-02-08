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

package xyz.aerii.athen.api.skyblock

import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.api.area.slayer.SLAYER_MOBS
import tech.thatgravyboat.skyblockapi.api.area.slayer.SlayerMob
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.utils.DiscoverableValue
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanNumeral
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import xyz.aerii.athen.handlers.Smoothie.level
import xyz.aerii.athen.handlers.Typo.stripped

private val tierRegex = Regex("""(?:^|\s)(?<level>[MDCLXVI]{1,7})\s""")

data class SlayerInfo(val entity: Entity) {
    private fun discoverType(): SlayerMob? {
        val nametag = level?.getEntity(entity.id + 1) ?: return null

        return SLAYER_MOBS.find { mob ->
            val inGameNames = mob.inGameNames
            inGameNames.any { nametag.name.stripped().contains(it) }
        }
    }

    private fun discoverOwner(): String? =
        level?.getEntity(entity.id + 3)?.customName?.stripped()?.substringAfterLast(":")?.trim()

    private fun discoverTier(): Int? =
        level?.getEntity(entity.id + 1)?.customName?.stripped()?.let {
            if ("Atoned Horror" in it) 5
            else tierRegex.findGroup(it, "level")?.parseRomanNumeral()
        }

    val isOwnedByPlayer: Boolean get() = owner == McPlayer.name
    val owner by DiscoverableValue(::discoverOwner)
    val type by DiscoverableValue(::discoverType)
    val tier by DiscoverableValue(::discoverTier)

    val str: String
        get() = "${type}_T${tier}"

    // attempts to check once when the class is created
    init { tier;type;owner; }

    override fun toString(): String {
        return "SlayerInfo(owner=$owner, isOwnedByPlayer=$isOwnedByPlayer, type=$type, tier=$tier, age=${entity.tickCount / 20}s)"
    }
}