@file:Suppress("UNUSED")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.config.Category
import foo.starred.athen.events.SoundPlayEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.id
import foo.starred.snowbird.api.held
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.BowItem

@Load
@OnlyIn(skyblock = true)
object TerminatorSounds : Module(
    "Terminator sounds",
    "Custom sounds for terminator!",
    Category.GENERAL
) {
    private val wall by config.switch("Wall hit sound", true)
    private val sound0 by config.sound("Sound")

    init {
        on<SoundPlayEvent> {
            if (sound == SoundEvents.ARROW_SHOOT) return@on cancel()
            if (sound == SoundEvents.ARROW_HIT && !wall) return@on cancel()
            if (sound != SoundEvents.ARROW_HIT && sound != SoundEvents.ARROW_HIT_PLAYER) return@on

            val i = held ?: return@on
            if (i.item !is BowItem) return@on
            if (i.id() != "TERMINATOR") return@on

            cancel()
            sound0.play()
        }
    }
}
