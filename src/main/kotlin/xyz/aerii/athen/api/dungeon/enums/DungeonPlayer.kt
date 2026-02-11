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

@file:Suppress("PropertyName")

package xyz.aerii.athen.api.dungeon.enums

import net.minecraft.world.entity.Entity
import xyz.aerii.athen.Athen
import xyz.aerii.athen.utils.Discoverable
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.stripped

class DungeonPlayer(
    val name: String,
    dungeonClass: DungeonClass? = null,
    classLevel: Int? = null
) {
    var deaths = 0

    var dungeonClass: DungeonClass? = dungeonClass
        internal set

    var classLevel: Int? = classLevel
        internal set

    var cataLevel: Int? = null
        internal set

    var dead = false
        internal set

    val entity by Discoverable(::d) { !it.isAlive }

    init {
        Athen.LOGGER.debug("Created KuudraPlayer with entity: {}", entity)
    }

    private fun d(): Entity? =
        client.level?.players()?.find { it.uuid.version() == 4 && it.name.stripped() == name }

    override fun toString() =
        "DungeonPlayer(name='$name', dead=$dead, class=$dungeonClass, classLevel=$classLevel, cataLevel=$cataLevel)"
}
