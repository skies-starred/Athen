package xyz.aerii.athen.modules.impl.dungeon

import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.world.entity.item.ItemEntity
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.PacketEvent
import xyz.aerii.athen.handlers.Smoothie.alert
import xyz.aerii.athen.handlers.Smoothie.level
import xyz.aerii.athen.handlers.Smoothie.mainThread
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module

@Load
@OnlyIn(islands = [SkyBlockIsland.THE_CATACOMBS])
object RareItemAlert : Module(
    "Rare item alert",
    "Alerts you when you get rare items!",
    Category.DUNGEONS
) {
    init {
        on<PacketEvent.Receive, ClientboundTakeItemEntityPacket> {
            val entity = level?.getEntity(itemId) as? ItemEntity ?: return@on
            if ("Skeleton Master Chestplate" !in entity.item.displayName.stripped()) return@on
            if (entity.item?.getData(DataTypes.DUNGEON_QUALITY) != 50) return@on

            mainThread {
                "<red>Rare drop! <yellow>Skeleton Master Chestplate <gray>[Quality=50]".parse().modMessage()
                "<red>Rare drop!".parse().alert()
            }
        }
    }
}