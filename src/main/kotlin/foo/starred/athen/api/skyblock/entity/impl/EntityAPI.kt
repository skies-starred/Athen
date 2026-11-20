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
@file:Suppress("ConstPropertyName", "LocalVariableName")

package foo.starred.athen.api.skyblock.entity.impl

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.skyblock.entity.duck.EntityDuck
import foo.starred.athen.events.EntityEvent
import foo.starred.athen.events.core.on
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.scheduling.scheduler.extensions.clientTicks
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.spider.CaveSpider
import net.minecraft.world.entity.monster.spider.Spider
import java.lang.ref.WeakReference
import kotlin.math.abs

@LoadableAthenModule
object EntityAPI {
    private const val dungeon = "Boomer|Flaming|Fortified|Golden|Healing|Healthy|Speedy|Stealth|Stormy"
    private val nameRegex = Regex("""^(?:\[Lv\d+] )?(?:[^\w \-]+ )?(?:\[\w+] )?(?:(?:$dungeon) )?(?<bool>.\s*Corrupted )?(?<name>.+?)(?: ᛤ)? \S*❤""")

    init {
        on<EntityEvent.Load> {
            load(entity as? ArmorStand ?: return@on)
        }

        on<EntityEvent.Update.Named> {
            attach(entity)
        }
    }

    fun name(component: Component): String? {
        val name = component.stripped().takeIf { '❤' in it } ?: return null
        val match = nameRegex.find(name) ?: return null

        return match.groups["name"]?.value
            ?.let { if (match.groups["bool"] != null) it.dropLast(1) else it }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun attach(entity: Entity) {
        val literal = entity.customName ?: return
        val level = client.level ?: return
        val duck = entity as? EntityDuck ?: return
        if (literal.stripped().damage()) return

        var candidate0: Entity? = null
        var candidate1: Entity? = null

        var distance0 = Double.MAX_VALUE
        var distance1 = Double.MAX_VALUE

        var ticks = Int.MAX_VALUE

        val all = level.getEntities(entity, entity.boundingBox.inflate(0.0, 1.0, 0.0))
        for (_entity in all) {
            var living = _entity

            if (living is ArmorStand) continue
            if (!living.isAlive) continue
            if (living.isInvisible && living !is Creeper && !living.armor()) continue

            if (living is CaveSpider) {
                level.getEntities(living, living.boundingBox.inflate(0.0, 0.5, 0.0)).firstOrNull { it is Spider && !it.isInvisible && it.isAlive }?.let { living = it }
            }

            val _distance = living.distanceToSqr(entity)
            val _ticks = abs(living.tickCount - entity.tickCount)
            if (_distance == 0.0 && _ticks == 0) {
                candidate0 = living
                break
            }

            if (candidate0 != null && _distance >= distance0 && (_distance != distance0 || _ticks >= ticks)) {
                if (candidate1 == null || _distance < distance1) {
                    candidate1 = living
                    distance1 = _distance
                }

                continue
            }

            candidate1 = candidate0
            distance1 = distance0

            candidate0 = living
            distance0 = _distance
            ticks = _ticks
        }

        val best = candidate0?.takeIf { it !is ArmorStand && it.isAlive } ?: return
        val current = duck.`athen$attach`()
        if (current != null && (current == candidate0 || current == candidate1)) return

        val list = (best as? EntityDuck)?.`athen$attachments`() ?: return
        if (current is EntityDuck) current.`athen$attachments`().removeIf { it.get() == entity || it.get()?.isAlive != true }

        list.removeIf { it.get()?.isAlive != true }
        if (!list.any { it.get() == entity }) list.add(WeakReference(entity))

        duck.`athen$attach`(best)
        EntityEvent.Update.Attach(literal, entity).post()
    }

    private fun String.damage(): Boolean {
        return all { it.isDigit() || it == ',' || it in "✧✯❤⚔✷ﬗ♞☄" }
    }

    private fun Entity.armor(): Boolean {
        val living = this as? LivingEntity ?: return false
        return !living.getItemBySlot(HEAD).isEmpty || !living.getItemBySlot(CHEST).isEmpty || !living.getItemBySlot(LEGS).isEmpty || !living.getItemBySlot(FEET).isEmpty
    }

    private fun load(entity: ArmorStand) {
        var tries = 0

        fun tick() {
            if (entity.customName != null) return attach(entity)
            if (tries++ < 3) Scheduler.schedule(1.clientTicks, ::tick)
        }

        Scheduler.schedule(2.clientTicks, ::tick)
    }
}
