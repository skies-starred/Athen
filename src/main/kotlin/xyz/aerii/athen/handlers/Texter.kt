@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.command
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.hover
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.suggest
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.url
import xyz.aerii.library.handlers.parser.parse

object Texter {
    @JvmStatic
    fun MutableComponent.onHover(text: String): MutableComponent = apply {
        hover = text.parse()
    }

    @JvmStatic
    fun MutableComponent.onHover(component: Component): MutableComponent = apply {
        hover = component
    }

    @JvmStatic
    fun MutableComponent.onCommand(text: String): MutableComponent = apply {
        command = text
    }

    @JvmStatic
    fun MutableComponent.onSuggest(suggestion: String): MutableComponent = apply {
        suggest = suggestion
    }

    @JvmStatic
    fun MutableComponent.onUrl(string: String): MutableComponent = apply {
        url = string
    }
}