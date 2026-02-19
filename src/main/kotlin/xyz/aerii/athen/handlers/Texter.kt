@file:Suppress("UNUSED")

package xyz.aerii.athen.handlers

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.command
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.hover
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.suggest
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.url
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import java.util.*

object Texter {
    private val COLOR_PATTERN = Regex("<(#?[0-9a-fA-F]{6}|0x[0-9a-fA-F]{6})>")
    private val RESET_PATTERN = Regex("<r>")
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

    const val RESET = "§r"
    const val BOLD = "§l"
    const val ITALIC = "§o"
    const val UNDERLINE = "§n"
    const val STRIKETHROUGH = "§m"
    const val OBFUSCATED = "§k"

    @JvmStatic
    val colorToFormat: Map<net.minecraft.network.chat.TextColor, ChatFormatting> =
        ChatFormatting.entries.mapNotNull { f -> net.minecraft.network.chat.TextColor.fromLegacyFormat(f)?.let { it to f } }.toMap()

    @JvmStatic
    fun String.parse(whiteBase: Boolean = false): MutableComponent {
        val result = EMPTY_COMPONENT.copy()
        var currentColor = 0xFFFFFF
        var baseColor = 0xFFFFFF

        val len = length
        var textStart = 0
        var i = 0

        while (i < len) {
            if (this[i] != '<') {
                i++
                continue
            }

            val end = indexOf('>', i + 1).takeIf { it != -1 } ?: break
            val tag = substring(i + 1, end).trim().lowercase()
            val newColor = if (tag == "r") baseColor else COLORS[tag] ?: tag.toIntOrNull()

            if (newColor != null) {
                if (i > textStart) result.append(substring(textStart, i).literal { color = currentColor })
                currentColor = newColor
                if (tag != "r" && i == textStart && !whiteBase) baseColor = newColor
                textStart = end + 1
            }

            i = end + 1
        }

        if (textStart < len) result.append(substring(textStart).literal { color = currentColor })
        return result
    }

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

    private fun String.toColor(): Int? = COLORS[trim().lowercase()]

    private fun Component.parse(sb: StringBuilder) {
        contents.visit({ style, text ->
            sb.appender(style)
            sb.append(text)
            Optional.empty<Any>()
        }, style)
    }

    private fun StringBuilder.appender(style: Style) {
        append(RESET)
        style.color?.let { colorToFormat[it] }?.let { append("§${it.char}") }

        if (style.isBold) append(BOLD)
        if (style.isItalic) append(ITALIC)
        if (style.isUnderlined) append(UNDERLINE)
        if (style.isStrikethrough) append(STRIKETHROUGH)
        if (style.isObfuscated) append(OBFUSCATED)
    }
}