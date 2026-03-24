package xyz.aerii.athen.ui

data class UIZone(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val type: IZoneType,
    val data: Int = 0,
    val data2: Int = -1,
    val category: String = ""
)