package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import xyz.aerii.athen.events.core.Event

data class ChatEvent(val message: Component, val actionBar: Boolean) : Event()
