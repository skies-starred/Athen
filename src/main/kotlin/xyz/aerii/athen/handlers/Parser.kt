package xyz.aerii.athen.handlers

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.command
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.hover
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.suggest
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.url
import xyz.aerii.athen.handlers.Texter.COLORS
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import java.util.ArrayDeque

private data class Frame(val component: MutableComponent, val action: (MutableComponent) -> MutableComponent)

private class ParseState(val whiteBase: Boolean) {
    var current: MutableComponent = EMPTY_COMPONENT.copy()
    var currentColor = 0xFFFFFF
    var baseColor = 0xFFFFFF
    var bold = false
    var italic = false
    var underline = false
    var strikethrough = false
    var obfuscated = false
    val stack = ArrayDeque<Frame>()
}

private fun String.findPop(start: Int): Int {
    var depth = 1
    var i = start

    while (i < length) {
        when (this[i]) {
            '<' -> depth++
            '>' -> if (--depth == 0) return i
        }

        i++
    }

    return -1
}

private fun ParseState.flush(text: String) {
    if (text.isEmpty()) return
    current.append(Component.literal(text).apply {
        color = currentColor
        style = style
            .withBold(if (bold) true else null)
            .withItalic(if (italic) true else null)
            .withUnderlined(if (underline) true else null)
            .withStrikethrough(if (strikethrough) true else null)
            .withObfuscated(if (obfuscated) true else null)
    })
}

private fun ParseState.push(action: (MutableComponent) -> MutableComponent) {
    stack.addLast(Frame(current, action))
    current = EMPTY_COMPONENT.copy()
}

private fun ParseState.pop() {
    val frame = stack.removeLast() ?: return
    val built = frame.action(current)
    current = frame.component
    current.append(built)
}

fun String.parse(whiteBase: Boolean = false): MutableComponent {
    val state = ParseState(whiteBase)
    var textStart = 0
    var i = 0

    while (i < length) {
        if (this[i] != '<') {
            i++
            continue
        }

        val end = findPop(i + 1)
        if (end == -1) {
            i++
            continue
        }

        val raw = substring(i + 1, end).trim()
        val lower = raw.lowercase()

        if (lower.startsWith('/')) {
            state.flush(substring(textStart, i))

            when (lower.drop(1).trim()) {
                "hover", "click" -> state.pop()
                "bold" -> state.bold = false
                "italic" -> state.italic = false
                "underline" -> state.underline = false
                "strikethrough" -> state.strikethrough = false
                "obfuscated" -> state.obfuscated = false
            }

            textStart = end + 1
            i = end + 1
            continue
        }

        val numericColor = lower.toIntOrNull()
        val namedColor = COLORS[lower]

        if (lower == "r" || namedColor != null || numericColor != null) {
            state.flush(substring(textStart, i))

            val colorVal = if (lower == "r") state.baseColor else namedColor ?: numericColor!!
            state.currentColor = colorVal

            if (lower != "r" && !state.whiteBase && i == 0) state.baseColor = colorVal

            textStart = end + 1
            i = end + 1
            continue
        }

        when (lower) {
            "bold" -> {
                state.flush(substring(textStart, i))
                state.bold = true
                textStart = end + 1
                i = end + 1
                continue
            }

            "italic" -> {
                state.flush(substring(textStart, i))
                state.italic = true
                textStart = end + 1
                i = end + 1
                continue
            }

            "underline" -> {
                state.flush(substring(textStart, i))
                state.underline = true
                textStart = end + 1
                i = end + 1
                continue
            }

            "strikethrough" -> {
                state.flush(substring(textStart, i))
                state.strikethrough = true
                textStart = end + 1
                i = end + 1
                continue
            }

            "obfuscated" -> {
                state.flush(substring(textStart, i))
                state.obfuscated = true
                textStart = end + 1
                i = end + 1
                continue
            }
        }

        if (lower.startsWith("hover:")) {
            state.flush(substring(textStart, i))
            val hoverComponent = raw.substringAfter(':').trim().parse()
            state.push { it.apply { hover = hoverComponent } }

            textStart = end + 1
            i = end + 1
            continue
        }

        if (lower.startsWith("click:")) {
            state.flush(substring(textStart, i))

            val rest = raw.substringAfter(':').trim()
            val colonIdx = rest.indexOf(':')

            if (colonIdx != -1) {
                val type = rest.substring(0, colonIdx).trim().lowercase()
                val value = rest.substring(colonIdx + 1).trim()

                state.push { c ->
                    when (type) {
                        "url" -> c.apply { url = value }
                        "command" -> c.apply { command = value }
                        "suggest" -> c.apply { suggest = value }
                        else -> c
                    }
                }
            }

            textStart = end + 1
            i = end + 1
            continue
        }

        i++
    }

    state.flush(substring(textStart))

    while (state.stack.isNotEmpty()) state.pop()
    return state.current
}