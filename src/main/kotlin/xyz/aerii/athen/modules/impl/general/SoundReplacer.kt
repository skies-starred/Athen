@file:Suppress("unused")

package xyz.aerii.athen.modules.impl.general

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.SoundPlayEvent
import xyz.aerii.athen.modules.Module

@Load
object SoundReplacer : Module(
    "Sound replacer",
    "Replaces all sounds, with a sound that you select.",
    Category.GENERAL
) {
    private val customSound by config.sound("Sound", "entity.cat.purreow")

    init {
        on<SoundPlayEvent> {
            val r = customSound.soundEvent?.takeIf { it != sound } ?: return@on

            cancel()
            customSound.play()
        }
    }
}