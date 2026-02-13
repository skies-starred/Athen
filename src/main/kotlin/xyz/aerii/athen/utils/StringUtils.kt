package xyz.aerii.athen.utils

//? if >= 1.21.11 {
/*import net.minecraft.util.Util
*///? } else {
import net.minecraft.Util
//? }

private val durationRegex = Regex("""(\d+(?:\.\d+)?)([dhms])""")
private val longDurationRegex = Regex("""(\d+(?:\.\d+)?)\s+(day|days|hour|hours|minute|minutes|second|seconds)""")

fun String.toCamelCase(): String {
    return this
        .split(" ", "_", "-")
        .filter { it.isNotBlank() }
        .mapIndexed { index, word ->
            val lower = word.lowercase()
            if (index == 0) lower else lower.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
}

fun String.unabbreviate(): Double {
    val s = trim().uppercase()

    val multiplier = when {
        s.endsWith("B") -> 1_000_000_000.0
        s.endsWith("M") -> 1_000_000.0
        s.endsWith("K") -> 1_000.0
        else -> 1.0
    }

    val number = s.dropLastWhile { it in "KMB" }
    return number.toDouble() * multiplier
}

fun String.fromDuration(): Double {
    var total = 0.0

    for ((value, unit) in durationRegex.findAll(lowercase()).map { it.destructured }) {
        val v = value.toDouble()
        total += when (unit) {
            "d" -> v * 86400
            "h" -> v * 3600
            "m" -> v * 60
            "s" -> v
            else -> 0.0
        }
    }

    return total
}

fun String.fromLongDuration(): Double {
    var total = 0.0

    for ((value, unit) in longDurationRegex.findAll(lowercase()).map { it.destructured }) {
        val v = value.toDouble()
        total += when (unit) {
            "day", "days" -> v * 86400
            "hour", "hours" -> v * 3600
            "minute", "minutes" -> v * 60
            "second", "seconds" -> v
            else -> 0.0
        }
    }

    return total
}

fun String.fromHMS(): Double {
    val p = split(':').map { it.toDouble() }
    return when (p.size) {
        3 -> p[0] * 3600 + p[1] * 60 + p[2]
        2 -> p[0] * 60 + p[1]
        1 -> p[0]
        else -> 0.0
    }
}

fun String.url() = Util.getPlatform().openUri(this)