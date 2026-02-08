package xyz.aerii.athen.events

import net.minecraft.sounds.SoundEvent
import net.minecraft.world.phys.Vec3
import xyz.aerii.athen.events.core.CancellableEvent

data class SoundPlayEvent(val sound: SoundEvent, val pos: Vec3, val volume: Float, val pitch: Float) : CancellableEvent()