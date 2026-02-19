package xyz.aerii.athen.api.kuudra.enums

enum class KuudraTier(val int: Int) {
    BASIC(1),
    HOT(2),
    BURNING(3),
    FIERY(4),
    INFERNAL(5);

    val str0: String =
        name.lowercase()

    val str: String =
        str0.replaceFirstChar { it.uppercase() }

    companion object {
        fun get(int: Int): KuudraTier? =
            KuudraTier.entries.firstOrNull { it.int == int }
    }
}