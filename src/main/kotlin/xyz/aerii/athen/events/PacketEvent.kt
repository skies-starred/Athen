package xyz.aerii.athen.events

import net.minecraft.network.protocol.Packet
import xyz.aerii.athen.events.core.CancellableEvent

sealed class PacketEvent {
    data class Process(val packet: Packet<*>) : CancellableEvent()

    data class Receive(val packet: Packet<*>) : CancellableEvent()

    data class Send(val packet: Packet<*>) : CancellableEvent()
}
