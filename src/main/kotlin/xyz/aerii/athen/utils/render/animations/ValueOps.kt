package xyz.aerii.athen.utils.render.animations

import java.awt.Color
import kotlin.math.pow

data class ValueOps<T>(
    val fn: (T, T, Float) -> T,
    val eq: (T, T) -> Boolean
)

val FloatOps = ValueOps<Float>(
    { a, b, t -> a + (b - a) * t },
    { a, b -> a == b }
)

val IntOps = ValueOps<Int>(
    { a, b, t ->
        val ar = (a ushr 16) and 0xFF
        val ag = (a ushr 8) and 0xFF
        val ab = a and 0xFF
        val aa = (a ushr 24) and 0xFF

        val br = (b ushr 16) and 0xFF
        val bg = (b ushr 8) and 0xFF
        val bb = b and 0xFF
        val ba = (b ushr 24) and 0xFF

        val r = (ar + (br - ar) * t).toInt()
        val g = (ag + (bg - ag) * t).toInt()
        val bl = (ab + (bb - ab) * t).toInt()
        val al = (aa + (ba - aa) * t).toInt()

        (al shl 24) or (r shl 16) or (g shl 8) or bl
    },
    { a, b -> a == b }
)

val ColorOps = ValueOps<Color>(
    { a, b, t ->
        Color(
            (a.red + (b.red - a.red) * t).toInt().coerceIn(0, 255),
            (a.green + (b.green - a.green) * t).toInt().coerceIn(0, 255),
            (a.blue + (b.blue - a.blue) * t).toInt().coerceIn(0, 255),
            (a.alpha + (b.alpha - a.alpha) * t).toInt().coerceIn(0, 255)
        )
    },
    { a, b ->
        a.red == b.red && a.green == b.green && a.blue == b.blue && a.alpha == b.alpha
    }
)

inline fun <reified T> selectOps(): ValueOps<T> =
    when (T::class) {
        Float::class -> FloatOps as ValueOps<T>
        Int::class -> IntOps as ValueOps<T>
        Color::class -> ColorOps as ValueOps<T>
        else -> error("Unsupported type ${T::class}")
    }

fun linear(t: Float) = t
fun easeOutQuad(t: Float) = 1f - (1f - t) * (1f - t)
fun easeInOutCubic(t: Float) =
    if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
