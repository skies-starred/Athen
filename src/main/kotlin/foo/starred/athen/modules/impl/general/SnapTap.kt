@file:Suppress("PropertyName")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.InputEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.kbus.extensions.override
import foo.starred.kontext.abstraction.client.extension.boundKey
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import net.minecraft.client.KeyMapping

@LoadableAthenModule
object SnapTap : Module(
    "Snap-Tap",
    "Snap Tap allows you to strafe while continuing to hold the initial key and quickly tapping the opposing key.",
    ConfigCategory.GENERAL
) {
    private val active = HashSet<Int>(4)
    private val pairs = ArrayList<Pair>(4)

    private data class Pair(
        val _current: KeyMapping,
        val _opposite: KeyMapping
    ) {
        val current: Int
            get() = _current.boundKey().value

        val value: Int
            get() = _opposite.boundKey().value
    }

    init {
        on<InputEvent.Keyboard.Press> {
            if (MinecraftGui.screen != null) return@on

            val key = keyEvent.key()
            if (active.add(key)) key.pair(false)
        }

        on<InputEvent.Keyboard.Release> {
            if (MinecraftGui.screen != null) return@on active.clear()

            val key = keyEvent.key()
            if (active.remove(key)) key.pair(true)
        }

        on<TickEvent.Client.End> {
            val options = client.options
            with(pairs) {
                add(Pair(options.keyLeft, options.keyRight))
                add(Pair(options.keyRight, options.keyLeft))
                add(Pair(options.keyUp, options.keyDown))
                add(Pair(options.keyDown, options.keyUp))
            }
        }.override().once()
    }

    private fun Int.pair(bool: Boolean) {
        for (pair in pairs) {
            if (pair.current != this) continue
            if (pair.value !in active) return

            pair._opposite.isDown = bool
            return
        }
    }
}
