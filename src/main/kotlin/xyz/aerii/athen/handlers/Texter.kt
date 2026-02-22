@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.command
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.hover
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.suggest
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.url
import java.util.*

object Texter {
    val COLORS = mapOf(
        "black" to TextColor.BLACK,
        "dark_blue" to TextColor.DARK_BLUE,
        "dark_green" to TextColor.DARK_GREEN,
        "dark_aqua" to TextColor.DARK_AQUA,
        "dark_red" to TextColor.DARK_RED,
        "dark_purple" to TextColor.DARK_PURPLE,
        "gold" to TextColor.GOLD,
        "orange" to TextColor.GOLD,
        "gray" to TextColor.GRAY,
        "dark_gray" to TextColor.DARK_GRAY,
        "blue" to TextColor.BLUE,
        "green" to TextColor.GREEN,
        "aqua" to TextColor.AQUA,
        "red" to TextColor.RED,
        "pink" to TextColor.LIGHT_PURPLE,
        "yellow" to TextColor.YELLOW,
        "white" to TextColor.WHITE
    )

    @JvmStatic
    val colorToFormat: Map<net.minecraft.network.chat.TextColor, ChatFormatting> =
        ChatFormatting.entries.mapNotNull { f -> net.minecraft.network.chat.TextColor.fromLegacyFormat(f)?.let { it to f } }.toMap()

    @JvmStatic
    @Deprecated("Use ParserKt.parse()")
    fun String.parse(): MutableComponent = parse(false)

    @JvmStatic
    fun Component.colorCoded(): String {
        val sb = StringBuilder()
        parse(sb)
        for (s in siblings) s.parse(sb)
        return sb.toString()
    }

    @JvmStatic
    fun String.literal(init: MutableComponent.() -> Unit = {}): MutableComponent =
        Component.literal(this).apply(init)

    @JvmStatic
    fun MutableComponent.append(text: String, init: MutableComponent.() -> Unit = {}): MutableComponent =
        this.append(Component.literal(text).apply(init))

    @JvmStatic
    fun MutableComponent.onHover(text: String): MutableComponent = apply {
        hover = Component.literal(text)
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

    private fun Component.parse(sb: StringBuilder) {
        contents.visit({ style, text ->
            sb.appender(style)
            sb.append(text)
            Optional.empty<Any>()
        }, style)
    }

    private fun StringBuilder.appender(style: Style) {
        append("§r")
        style.color?.let { colorToFormat[it] }?.let { append("§${it.char}") }
        if (style.isBold) append("§l")
        if (style.isItalic) append("§o")
        if (style.isUnderlined) append("§n")
        if (style.isStrikethrough) append("§m")
        if (style.isObfuscated) append("§k")
    }
}