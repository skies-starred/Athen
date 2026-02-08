package xyz.aerii.athen.modules.impl.general

import net.minecraft.client.KeyMapping
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.InputEvent
import xyz.aerii.athen.events.TickEvent
import xyz.aerii.athen.events.core.override
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.mixin.accessors.KeyMappingAccessor
import xyz.aerii.athen.modules.Module

@Load
object SnapTap : Module(
    "Snap-Tap",
    "Snap Tap allows you to strafe while continuing to hold the initial key and quickly tapping the opposing key.",
    Category.GENERAL
) {
    private val active = HashSet<Int>(4)
    private val pairs = ArrayList<Pair>(4)

    private data class Pair(
        val curr: KeyMapping,
        val oppo: KeyMapping
    ) {
        val key: Int
            get() = (curr as KeyMappingAccessor).boundKey.value

        val opp: Int
            get() = (oppo as KeyMappingAccessor).boundKey.value
    }

    init {
        on<InputEvent.Keyboard.Press> {
            if (client.screen != null) return@on

            val key = keyEvent.key()
            if (active.add(key)) key.pair(false)
        }

        on<InputEvent.Keyboard.Release> {
            if (client.screen != null) return@on active.clear()

            val key = keyEvent.key()
            if (active.remove(key)) key.pair(true)
        }

        on<TickEvent.Client> {
            val options = client.options ?: return@on
            with (pairs) {
                add(Pair(options.keyLeft, options.keyRight))
                add(Pair(options.keyRight, options.keyLeft))
                add(Pair(options.keyUp, options.keyDown))
                add(Pair(options.keyDown, options.keyUp))
            }
        }.override().once()
    }

    private fun Int.pair(bool: Boolean) {
        for (pair in pairs) {
            if (pair.key != this) continue
            if (pair.opp !in active) return

            pair.oppo.isDown = bool
            return
        }
    }
}