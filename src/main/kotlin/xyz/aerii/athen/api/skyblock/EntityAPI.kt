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

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster./*? >= 1.21.11 { *//*spider.*//*? }*/CaveSpider
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster./*? >= 1.21.11 {*//*spider.*//*? }*/Spider
import xyz.aerii.athen.accessors.EntityAccessor
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.events.EntityEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.stripped
import java.lang.ref.WeakReference
import kotlin.math.abs

@Priority
object EntityAPI {
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
        val level = client.level ?: return
        if (lit.stripped().damage()) return

        var a: Entity? = null
        var a2: Entity? = null
        var d = Double.MAX_VALUE
        var d2 = Double.MAX_VALUE
        var t = Int.MAX_VALUE

        val es = level.getEntities(ent, ent.boundingBox.inflate(0.0, 1.0, 0.0)) ?: return
        for (e0 in es) {
            var e = e0

            if (e is ArmorStand) continue
            if (!e.isAlive) continue
            if (e.isInvisible && e !is Creeper && !e.armor()) continue

            if (e is CaveSpider) level.getEntities(e, e.boundingBox.inflate(0.0, 0.5, 0.0)).firstOrNull { it is Spider && !it.isInvisible && it.isAlive }?.let { e = it }

            val d0 = e.distanceToSqr(ent)
            if (a != null && d0 > d) continue

            val t0 = abs(e.tickCount - ent.tickCount)
            if (d0 == 0.0 && t0 == 0) {
                a = e
                break
            }

            if (a == null || d0 < d || (d0 == d && t0 < t)) {
                a2 = a
                d2 = d

                a = e
                d = d0
                t = t0
            } else if (a2 == null || d0 < d2) {
                a2 = e
                d2 = d0
            }
        }

        val b = a ?: return

        val acc = (ent as? EntityAccessor) ?: return
        val c = acc.`athen$attach`()
        if (c != null && (c == a || c == a2)) return

        val list = (b as? EntityAccessor)?.`athen$attachments`() ?: return

        list.removeIf { it.get()?.isAlive != true }
        if (!list.any { it.get() == ent }) list.add(WeakReference(ent))
        acc.`athen$attach`(b)

        EntityEvent.Update.Attach(lit, ent).post()
    }

    private fun String.damage(): Boolean =
        all { it.isDigit() || it == ',' || it in "✧✯❤⚔✷ﬗ♞☄" }

    private fun Entity.armor(): Boolean {
        val l = this as? LivingEntity ?: return false
        return !l.getItemBySlot(EquipmentSlot.HEAD).isEmpty || !l.getItemBySlot(EquipmentSlot.CHEST).isEmpty || !l.getItemBySlot(EquipmentSlot.LEGS).isEmpty || !l.getItemBySlot(EquipmentSlot.FEET).isEmpty
    }

    private fun load(ent: ArmorStand) {
        var tries = 0

        fun tick() {
            if (ent.customName != null) return attach(ent)
            if (tries++ < 3) Chronos.Tick after 1 then ::tick
        }

        Chronos.Tick after 2 then ::tick
    }
}