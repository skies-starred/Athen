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
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.EventBus.on
import xyz.aerii.athen.events.core.onProcess
import xyz.aerii.athen.events.core.onSend
import xyz.aerii.athen.events.core.runWhen
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.React
import xyz.aerii.athen.handlers.React.Companion.or
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Typo.devMessage
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.mixin.accessors.ServerboundInteractPacketAccessor
import xyz.aerii.athen.modules.impl.dungeon.terminals.simulator.TerminalSimulator
import xyz.aerii.athen.modules.impl.dungeon.terminals.solver.TerminalSolver
import java.util.concurrent.ConcurrentHashMap

@Priority
object TerminalAPI {
    private var cd = 0

    val currentItems = ConcurrentHashMap<Int, ItemStack>()
    val slotCounts = mutableMapOf<Int, Int>()

    var terminalOpen: React<Boolean> = React(false)
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

        onProcess<ClientboundOpenScreenPacket> {
            val title = title.stripped()
            val type = TerminalType.get(title) ?: return@onProcess reset()

            if (!terminalOpen.value) openTime = System.currentTimeMillis()
            terminalOpen.value = true
            currentTerminal = type
            currentTitle = title
            lastId = containerId
            currentItems.clear()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        onProcess<ClientboundContainerSetSlotPacket> {
            if (!terminalOpen.value) return@onProcess
            if (containerId != lastId) return@onProcess
            if (slot > (currentTerminal?.slots ?: 0)) return@onProcess
            if (slot < 0) return@onProcess

            currentItems[slot] = item
            if (currentItems.size == currentTerminal?.slots || currentTerminal == TerminalType.MELODY) DungeonEvent.Terminal.Update(slot, item).post()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        onProcess<ClientboundContainerSetContentPacket> {
            if (!terminalOpen.value) return@onProcess
            if (containerId != lastId) return@onProcess

            currentItems.clear()
            for ((i, it) in items.withIndex()) if (i < (currentTerminal?.slots ?: 0)) currentItems[i] = it
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        onProcess<ClientboundContainerClosePacket> {
            if (terminalOpen.value) Chronos.Tick.run { reset() }
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        onSend<ServerboundContainerClickPacket> {
            if (!terminalOpen.value) return@onSend
            if (currentTerminal == TerminalType.MELODY) return@onSend
            if (containerId == lastId) return@onSend
            if (System.currentTimeMillis() - openTime >= TerminalSolver.fcDelay) return@onSend

            it.cancel()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        onSend<ServerboundContainerClosePacket> {
            if (!terminalOpen.value) return@onSend

            reset()
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        onSend<ServerboundInteractPacket> {
            val entity = client.level?.getEntity((this as ServerboundInteractPacketAccessor).entityId()) as? ArmorStand ?: return@onSend
            if (entity.displayName?.stripped() != "Inactive Terminal") return@onSend

            if (cd > 0 || lastId != -1) it.cancel() else cd = 15
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)

        on<TickEvent.Server> {
            if (cd <= 0) return@on

            cd--
        }.runWhen((DungeonAPI.F7Phase.map { it == 3 } or TerminalSimulator.s) or TerminalSimulator.s0)
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