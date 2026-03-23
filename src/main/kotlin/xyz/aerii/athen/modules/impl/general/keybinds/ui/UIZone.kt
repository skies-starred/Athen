package xyz.aerii.athen.modules.impl.general.keybinds.ui

data class UIZone(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val type: UIZoneType,
    val data: Int = 0,
    val category: String = ""
)