/*
 * Very much inspired by how SkyblockAPI handles the entity attachments. This version contains some changes,
 * and fixes that aim to improve over the idea of attaching nametags to entities. However, this version
 * also contains a few changes that may not be stable/tested enough to be in the SkyblockAPI library.
 *
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

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.player.Player
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import xyz.aerii.athen.accessors.EntityAccessor
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.api.skyblock.SlayerAPI.slayerNames
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.stripped
import java.lang.ref.WeakReference

@Priority
object EntityAPI {
    private val bool: Boolean
        get() = SkyBlockIsland.THE_CATACOMBS.inIsland.value

    init {
        on<EntityEvent.Load> {
            load(entity as? ArmorStand ?: return@on)
        }

        on<EntityEvent.Update.Named> {
            attach(entity as? ArmorStand ?: return@on)
        }
    }

    @JvmStatic
    fun attach(ent: Entity) {
        val lit = ent.customName ?: return
        val str = lit.stripped().takeIf { !it.damage() } ?: return
        val b = ent.find(lit, str) ?: return

        val acc = (ent as? EntityAccessor)?.takeIf { it.`athen$attach`() != b } ?: return
        val list = (b as? EntityAccessor)?.`athen$attachments`() ?: return

        list.removeIf { it.get() == null }
        list.add(WeakReference(ent))
        acc.`athen$attach`(b)

        EntityEvent.Update.Attach(lit, ent).post()
    }

    private fun Entity.find(lit: Component, str: String): Entity? {
        val level = client.level ?: return null
        if ("Withermancer" in str) return level.getEntity(id - 3)
        if (str.endsWith("❤") && ("[Lv" in str || "☠" in str || bool)) return level.getEntity(id - 1)

        val s = slayer(lit, str) ?: return null
        return level.getEntity(id - s)?.takeIf { it.v(this) }
    }

    private fun slayer(lit: Component, str: String): Int? {
        return when {
            str.startsWith("Spawned by: ") -> 3
            str.time() && lit.color == TextColor.RED -> 2
            slayerNames.any { str.contains(it) } -> 1
            else -> null
        }
    }

    private fun Entity.v(a: Entity): Boolean {
        if (this !is Blaze && this !is EnderMan && this !is Spider && this !is Wolf && this !is Zombie && this !is Player) return false
        if (distanceToSqr(a) > 4) return false
        return true
    }

    private fun String.time(): Boolean =
        indexOf(':').let { it > 0 && it < length - 1 && all { c -> c.isDigit() || c == ':' } }

    private fun String.damage(): Boolean =
        all { it.isDigit() || it == ',' || it in "✧✯❤⚔✷ﬗ♞☄" }

    private fun load(ent: ArmorStand) {
        var tries = 0

        fun tick() {
            if (ent.customName != null) return attach(ent)
            if (tries++ < 3) Chronos.Tick after 1 then ::tick
        }

        Chronos.Tick after 2 then ::tick
    }
}