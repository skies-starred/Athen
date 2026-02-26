@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.BowItem
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.SoundPlayEvent
import xyz.aerii.athen.handlers.Smoothie.heldItem
import xyz.aerii.athen.modules.Module

@Load
@OnlyIn(skyblock = true)
object TerminatorSounds : Module(
    "Terminator sounds",
    "Custom sounds for terminator!",
    Category.GENERAL
) {
    private val wall by config.switch("Wall hit sound", true)
    private val customSound by config.sound("Sound", "block.note_block.bit")

    init {
        on<SoundPlayEvent> {
            val r = customSound.soundEvent ?: return@on

            if (sound == SoundEvents.ARROW_SHOOT) return@on cancel()
            if (sound == SoundEvents.ARROW_HIT && !wall) return@on cancel()
            if (sound != SoundEvents.ARROW_HIT && sound != SoundEvents.ARROW_HIT_PLAYER) return@on

            val i = heldItem ?: return@on
            if (i.item !is BowItem) return@on
            if (i.getData(DataTypes.SKYBLOCK_ID)?.skyblockId != "TERMINATOR") return@on

            cancel()
            customSound.play()
        }
    }
}