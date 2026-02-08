package xyz.aerii.athen.handlers

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.text.CommonText
import xyz.aerii.athen.handlers.Texter.literal

/**
 * Smooth operator
 */
object Smoothie {
    @JvmStatic
    val client: Minecraft = Minecraft.getInstance()

    @JvmStatic
    val player: LocalPlayer?
        get() = client.player

    @JvmStatic
    val level: ClientLevel?
        get() = client.level

    @JvmStatic
    val playerName: String?
        get() = player?.name?.string

    @JvmStatic
    val heldItem: ItemStack?
        get() = player?.mainHandItem

    @JvmStatic
    @JvmOverloads
    fun SoundEvent.play(volume: Float = 1f, pitch: Float = 1f) {
        client.player?.playSound(this, volume, pitch)
    }

    @JvmStatic
    @JvmOverloads
    fun String.alert(showTitle: Boolean = true, playSound: Boolean = true, subTitle: String? = null, soundType: SoundEvent = SoundEvents.NOTE_BLOCK_CHIME.value()) {
        if (showTitle) showTitle(subTitle)
        if (playSound) soundType.play()
    }

    @JvmStatic
    @JvmOverloads
    fun Component.alert(showTitle: Boolean = true, playSound: Boolean = true, subTitle: Component? = null, soundType: SoundEvent = SoundEvents.NOTE_BLOCK_CHIME.value()) {
        if (showTitle) showTitle(subTitle)
        if (playSound) soundType.play()
    }

    @JvmStatic
    @JvmOverloads
    fun String.showTitle(subTitle: String? = null, fadeIn: Int = 5, stay: Int = 20, fadeOut: Int = 5) {
        literal().showTitle(subTitle?.literal(), fadeIn, stay, fadeOut)
    }

    @JvmStatic
    @JvmOverloads
    fun Component.showTitle(subTitle: Component? = null, fadeIn: Int = 5, stay: Int = 20, fadeOut: Int = 5) {
        client.gui.setTimes(fadeIn, stay, fadeOut)
        client.gui.setTitle(this)
        client.gui.setSubtitle(subTitle ?: CommonText.EMPTY)
    }
}
