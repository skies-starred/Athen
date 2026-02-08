package xyz.aerii.athen.utils

import java.awt.Color

val Int.red get() = this shr 16 and 0xFF
val Int.green get() = this shr 8 and 0xFF
val Int.blue get() = this and 0xFF
val Int.alpha get() = this shr 24 and 0xFF

fun Color.brighten(factor: Float): Color =
    Color(
        (red * factor).toInt().coerceAtMost(255),
        (green * factor).toInt().coerceAtMost(255),
        (blue * factor).toInt().coerceAtMost(255),
        alpha
    )

fun Int.brighten(factor: Float): Int {
    val r = (red * factor).toInt().coerceAtMost(255)
    val g = (green * factor).toInt().coerceAtMost(255)
    val b = (blue * factor).toInt().coerceAtMost(255)

    return argb(r, g, b, alpha)
}

@JvmOverloads
fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int =
    (a shl 24) or (r shl 16) or (g shl 8) or b

@JvmOverloads
fun rgba(r: Int, g: Int, b: Int, a: Int = 255): Int =
    (r shl 24) or (g shl 16) or (b shl 8) or a

@JvmOverloads
fun Int.withAlpha(alpha: Float, rgba: Boolean = false): Int {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    return if (rgba) (this and 0xFFFFFF00.toInt()) or a else (this and 0x00FFFFFF) or (a shl 24)
}