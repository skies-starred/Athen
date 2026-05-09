package xyz.aerii.athen.utils

fun String.regex(): Regex? {
    return try {
        Regex(this)
    } catch (_: Exception) {
        null
    }
}