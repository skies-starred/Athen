package xyz.aerii.athen.events.core

import net.minecraft.network.protocol.Packet
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.handlers.React

inline fun <reified P : Packet<*>> Any.onProcess(
    priority: Int = 0,
    noinline handler: P.(PacketEvent.Process) -> Unit
): Node<*> {
    return EventBus.on<PacketEvent.Process> (priority) {
        (packet as? P)?.handler(this)
    }
}

inline fun <reified P : Packet<*>> Any.onReceive(
    priority: Int = 0,
    noinline handler: P.(PacketEvent.Receive) -> Unit
): Node<*> {
    return EventBus.on<PacketEvent.Receive> (priority) {
        (packet as? P)?.handler(this)
    }
}

inline fun <reified P : Packet<*>> Any.onSend(
    priority: Int = 0,
    noinline handler: P.(PacketEvent.Send) -> Unit
): Node<*> {
    return EventBus.on<PacketEvent.Send> (priority) {
        (packet as? P)?.handler(this)
    }
}

fun Node<*>.runWhen(state: React<Boolean>) = apply {
    if (!overridden && eventClass != CommandRegistration::class.java) add(state)
}

fun Node<*>.override(state: React<Boolean>) = apply {
    overridden = true
    conditions.clear()
    add(state)
}

fun Node<*>.override() = apply {
    overridden = true
    conditions.clear()
    register()
}

private fun Node<*>.add(state: React<Boolean>) = apply {
    conditions.add(state)
    state.onChange { evaluate() }
    evaluate()
}