package xyz.aerii.athen.modules.impl.general.keybinds

enum class KeybindWorkIn(val displayName: String) {
    OUTSIDE_GUI("Outside GUI"),
    GUI("GUI"),
    EVERYWHERE("Everywhere");

    companion object {
        fun from(name: String): KeybindWorkIn =
            entries.find { it.name == name } ?: OUTSIDE_GUI
    }
}