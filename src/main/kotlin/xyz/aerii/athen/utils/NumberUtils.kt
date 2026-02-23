package xyz.aerii.athen.utils

import dev.deftu.omnicore.api.client.input.OmniKeyboard
import dev.deftu.omnicore.api.client.input.OmniMouse

@JvmName("timesIntPair")
operator fun Pair<Int, Int>.times(k: Number): Pair<Int, Int> =
    (first * k.toDouble()).toInt() to (second * k.toDouble()).toInt()

@JvmName("timesFloatPair")
operator fun Pair<Float, Float>.times(k: Number): Pair<Float, Float> =
    first * k.toFloat() to second * k.toFloat()

@JvmName("timesDoublePair")
operator fun Pair<Double, Double>.times(k: Number): Pair<Double, Double> =
    first * k.toDouble() to second * k.toDouble()

fun Int.isPressed(): Boolean = when {
    this == -1 -> false
    this > 0 -> OmniKeyboard.isPressed(this)
    else -> OmniMouse.isPressed(this)
}

fun Int.isBound(): Boolean = this != -1

/**
 * Abbreviates large numbers with K, M, B suffixes.
 *
 * @param decimals Number of decimal places (default 1)
 * @return Abbreviated string (e.g., "1.2M", "120K", "999")
 */
fun Number.abbreviate(decimals: Int = 1): String {
    val value = this.toDouble()
    return when {
        value >= 1_000_000_000 -> (value / 1_000_000_000).formatWithSuffix("B", decimals)
        value >= 1_000_000 -> (value / 1_000_000).formatWithSuffix("M", decimals)
        value >= 1_000 -> (value / 1_000).formatWithSuffix("K", decimals)
        else -> if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.${decimals}f", value)
    }
}

/**
 * Formats seconds into human-readable duration.
 *
 * @param short If true, returns only the largest unit (e.g., "5d" instead of "5d 3h 20m")
 * @param secondsDecimals Number of decimal places for seconds (default 0)
 * @param secondsOnly If true, always returns seconds (e.g., "3670s" or "3670.5s")
 * @return Formatted duration string (e.g., "1h 30m 45s", "2.5s")
 */
fun Number.toDuration(short: Boolean = false, secondsDecimals: Int = 0, secondsOnly: Boolean = false): String {
    val totalSeconds = this.toDouble()

    if (secondsOnly) {
        val secs = if (secondsDecimals > 0) totalSeconds else totalSeconds.toLong().toDouble()
        return secs.formatSeconds(secondsDecimals)
    }

    val seconds = totalSeconds.toLong()
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    val fractionalSeconds = if (secondsDecimals > 0) totalSeconds % 60 else remainingSeconds.toDouble()

    if (short) {
        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> fractionalSeconds.formatSeconds(secondsDecimals)
        }
    }

    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (days > 0 || hours > 0 || minutes > 0) {
            if (remainingSeconds > 0) append("${remainingSeconds}s")
        } else {
            append(fractionalSeconds.formatSeconds(secondsDecimals))
        }
    }.trimEnd()
}

/**
 * Formats milliseconds into human-readable duration.
 */
fun Number.toDurationFromMillis(short: Boolean = false, secondsDecimals: Int = 0, secondsOnly: Boolean = false): String =
    (this.toDouble() / 1000).toDuration(short, secondsDecimals, secondsOnly)

/**
 * Formats a number with thousands separators.
 *
 * @return Formatted string with commas (e.g., "1,200,000", "1,234.56")
 */
fun Number.formatted(): String {
    val value = this.toDouble()
    return if (value % 1.0 == 0.0) "%,d".format(value.toLong()) else "%,.2f".format(value)
}

fun Number.toHMS(): String {
    val t = toInt()
    return "%02d:%02d:%02d".format(t / 3600, (t % 3600) / 60, t % 60)
}

fun Number.toMS(): String {
    val t = toInt()
    return "%02d:%02d".format((t % 3600) / 60, t % 60)
}

fun Int.plural(w1: String, w2: String) =
    if (equals(1)) w1 else w2

private fun Double.formatWithSuffix(suffix: String, decimals: Int): String {
    return if (this % 1.0 == 0.0 && this < 100) "${toInt()}$suffix"
    else String.format("%.${decimals}f", this).trimEnd('0').trimEnd('.') + suffix
}

private fun Double.formatSeconds(decimals: Int): String =
    if (decimals > 0) "${String.format("%.${decimals}f", this)}s" else "${toInt()}s"
