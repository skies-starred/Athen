package xyz.aerii.athen.utils

import net.minecraft.world.inventory.ClickType
import xyz.aerii.library.api.client

fun guiClick(id: Int, index: Int, button: Int = 0, clickType: ClickType = ClickType.PICKUP) {
    val player = client.player ?: return
    client.gameMode?.handleInventoryMouseClick(id, index, button, clickType, player)
}