package xyz.aerii.athen.events

import net.hypixel.data.type.ServerType
import xyz.aerii.athen.api.location.SkyBlockArea
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.api.location.area.base.ISkyBlockArea
import xyz.aerii.athen.events.core.Event

sealed class LocationEvent {
    sealed class Hypixel {
        data class Server(
            val name: String,
            val type: ServerType?,
            val lobby: String?,
            val mode: String?,
            val map: String?,
        ) : Event()

        data class Island(
            val old: SkyBlockIsland?,
            val new: SkyBlockIsland?
        ) : Event()

        data class Area(
            val old: ISkyBlockArea,
            val new: ISkyBlockArea
        ) : Event()
    }

    sealed class SkyBlock {
        data object Connect : Event()

        data object Disconnect : Event()
    }

    sealed class Server : Event() {
        data object Connect : Server()

        data object Disconnect : Server()
    }
}
