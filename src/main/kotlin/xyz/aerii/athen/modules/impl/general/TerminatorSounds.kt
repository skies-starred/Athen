@file:Suppress("UNUSED")

package xyz.aerii.athen.modules.impl.general

import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.BowItem
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.SoundPlayEvent
import xyz.aerii.athen.handlers.Smoothie.heldItem
import xyz.aerii.athen.handlers.Smoothie.play
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.utils.url

@Load
@OnlyIn(skyblock = true)
object TerminatorSounds : Module(
    "Terminator sounds",
    "Custom sounds for terminator!",
    Category.GENERAL
) {
    private var real: SoundEvent? = null
    private val s = config.textInput("Sound", "block.note_block.bit").custom("sound")
    private val p by config.slider("Pitch", 1f, 0f, 1f, true)
    private val v by config.slider("Volume", 1f, 0f, 1f, true)

    private val _unused by config.button("Play") {
        real?.play(v, p)
    }

    private val _unused0 by config.button("Open sounds list") {
        "https://www.digminecraft.com/lists/sound_list_pc.php".url()
    }

    init {
        real = s.value.prs()

        s.state.onChange {
            real = it.prs()
        }

        on<SoundPlayEvent> {
            val r = real ?: return@on
            if (sound != SoundEvents.ARROW_HIT && sound != SoundEvents.ARROW_HIT_PLAYER) return@on
            val i = heldItem ?: return@on
            if (i.item !is BowItem) return@on
            if (i.getData(DataTypes.SKYBLOCK_ID)?.skyblockId != "TERMINATOR") return@on

            cancel()
            r.play(v, p)
        }
    }

    private fun String.prs(): SoundEvent? {
        val p = ResourceLocation.tryParse(this) ?: return null
        return SoundEvent.createVariableRangeEvent(p)
    }
}