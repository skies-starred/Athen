/*
 * Original work by [CyanQT](https://github.com/cyanqt) and contributors (Unknown License).
 *
 * Modifications:
 *   Copyright (c) 2025 skies-starred
 *   Licensed under the BSD 3-Clause License.
 *
 * The original (unknown) license applies to the portions derived from CyanQT.
 * Please reach out to @skies.starred on discord if you have any information about the license.
 */

@file:Suppress("ObjectPropertyName")

package xyz.aerii.athen.api.dungeon.terminals

import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.events.DungeonEvent
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.mixin.accessors.ServerboundInteractPacketAccessor
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.TerminalSimulator
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import xyz.aerii.library.api.level
import xyz.aerii.library.handlers.Observable
import xyz.aerii.library.handlers.Observable.Companion.and
import xyz.aerii.library.handlers.Observable.Companion.or
import xyz.aerii.library.handlers.time.client
import xyz.aerii.library.utils.stripped
import java.util.concurrent.ConcurrentHashMap

@Priority
object TerminalAPI {
    private var cd = 0

    val currentItems = ConcurrentHashMap<Int, ItemStack>()
    val slotCounts = mutableMapOf<Int, Int>()

    var terminalOpen: Observable<Boolean> = Observable(false)
        private set

    var currentTerminal: TerminalType? = null
        private set

    var currentTitle: String = ""
        private set

    var openTime: Long = 0
        private set

    var lastId: Int = -1
        private set

    var `rubix$lastTarget`: Int? = null
    var `melody$button`: Int? = null
    var `melody$current`: Int? = null
    var `melody$correct`: Int? = null

    init {
        terminalOpen.onChange {
            (if (it) DungeonEvent.Terminal.Open else DungeonEvent.Terminal.Close).post()
        }

        on<PacketEvent.Process.Pre, ClientboundOpenScreenPacket> {
            val title = title.stripped()
            val type = TerminalType.get(title)?.takeIf { it.solver } ?: return@on reset()

            if (!terminalOpen.value) openTime = System.currentTimeMillis()
            terminalOpen.value = true
            currentTerminal = type
            currentTitle = title
            lastId = containerId
            currentItems.clear()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        on<PacketEvent.Process.Pre, ClientboundContainerSetSlotPacket> {
            if (!terminalOpen.value) return@on
            if (containerId != lastId) return@on
            if (slot > (currentTerminal?.slots ?: 0)) return@on
            if (slot < 0) return@on

            currentItems[slot] = item
            if (currentItems.size == currentTerminal?.slots || currentTerminal == TerminalType.MELODY) DungeonEvent.Terminal.Update(slot, item).post()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        on<PacketEvent.Process.Pre, ClientboundContainerSetContentPacket> {
            if (!terminalOpen.value) return@on
            if (containerId != lastId) return@on

            currentItems.clear()
            for ((i, it) in items.withIndex()) if (i < (currentTerminal?.slots ?: 0)) currentItems[i] = it
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        on<PacketEvent.Process.Pre, ClientboundContainerClosePacket> {
            if (terminalOpen.value) Chronos.schedule(1.client) { reset() }
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        on<PacketEvent.Send, ServerboundContainerClickPacket> {
            if (!terminalOpen.value) return@on
            if (currentTerminal == TerminalType.MELODY) return@on
            if (containerId != lastId) return@on it.cancel()
            if (System.currentTimeMillis() - openTime >= TerminalSolver.fcDelay) return@on

            it.cancel()
        }.runWhen(((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0) and TerminalSolver.observable)

        on<PacketEvent.Send, ServerboundContainerClosePacket> {
            if (!terminalOpen.value) return@on

            reset()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        on<PacketEvent.Send, ServerboundInteractPacket> {
            val entity = level?.getEntity((this as ServerboundInteractPacketAccessor).entityId()) as? ArmorStand ?: return@on
            if (entity.displayName?.stripped() != "Inactive Terminal") return@on

            if (cd > 0 || lastId != -1) it.cancel() else cd = 15
        }.runWhen(((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0) and TerminalSolver.observable)

        on<TickEvent.Server> {
            if (cd > 0) cd--
        }.runWhen(((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0) and TerminalSolver.observable)
    }

    private fun reset() {
        "TerminalAPI: reset".devMessage()

        `rubix$lastTarget` = null
        `melody$button` = null
        `melody$current` = null
        `melody$correct` = null

        terminalOpen.value = false
        currentTerminal = null
        currentTitle = ""
        currentItems.clear()
        slotCounts.clear()
        lastId = -1
    }
}