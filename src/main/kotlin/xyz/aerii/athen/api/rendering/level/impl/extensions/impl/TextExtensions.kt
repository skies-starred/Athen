@file:Suppress("Unused")

package xyz.aerii.athen.api.rendering.level.impl.extensions.impl

import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import xyz.aerii.athen.api.rendering.level.impl.data.impl.ExtractedText
import xyz.aerii.athen.api.rendering.level.impl.queue.impl.LevelQueueImpl
import xyz.aerii.library.api.client
import xyz.aerii.library.handlers.parser.parse

@JvmOverloads
fun extractText(
    text: String,
    pos: Vec3,
    color0: Int = -1,
    color1: Int = 0,
    scale: Float = 1f,
    depth: Boolean = true,
    shadow: Boolean = true,
    increase: Boolean = false
) {
    if (!increase) {
        LevelQueueImpl.texts.add(ExtractedText(text.parse(), pos, color0, color1, scale, shadow, depth))
        return
    }

    val scale = scale * client.gameRenderer.mainCamera.position().distanceTo(pos).toFloat() / 3f
    LevelQueueImpl.texts.add(ExtractedText(text.parse(), pos, color0, color1, scale, shadow, depth))
}

@JvmOverloads
fun extractText(
    text: Component,
    pos: Vec3,
    color0: Int = -1,
    color1: Int = 0,
    scale: Float = 1f,
    depth: Boolean = true,
    shadow: Boolean = true,
    increase: Boolean = false
) {
    if (!increase) {
        LevelQueueImpl.texts.add(ExtractedText(text, pos, color0, color1, scale, shadow, depth))
        return
    }

    val scale = scale * client.gameRenderer.mainCamera.position().distanceTo(pos).toFloat() / 3f
    LevelQueueImpl.texts.add(ExtractedText(text, pos, color0, color1, scale, shadow, depth))
}