package foo.starred.athen.api.messaging.enums

enum class MessageColors(color: Number) {
    BLACK(0xFF000000),
    DARK_BLUE(0xFF0000AA),
    DARK_GREEN(0xFF00AA00),
    DARK_AQUA(0xFF00AAAA),
    DARK_RED(0xFFAA0000),
    DARK_PURPLE(0xFFAA00AA),
    GOLD(0xFFFFAA00),
    ORANGE(0xFFFFAA00),
    GRAY(0xFFAAAAAA),
    DARK_GRAY(0xFF555555),
    BLUE(0xFF5555FF),
    GREEN(0xFF55FF55),
    AQUA(0xFF55FFFF),
    RED(0xFFFF5555),
    PINK(0xFFFF55FF),
    YELLOW(0xFFFFFF55),
    WHITE(0xFFFFFFFF);

    val color: Int = color.toInt()
}
