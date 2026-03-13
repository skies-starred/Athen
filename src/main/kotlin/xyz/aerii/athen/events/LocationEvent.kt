package xyz.aerii.athen.events

import net.hypixel.data.type.ServerType
import xyz.aerii.athen.api.location.SkyBlockArea
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.events.core.Event

sealed class LocationEvent {
    data class ServerChange(
        val name: String,
        val type: ServerType?,
        val lobby: String?,
        val mode: String?,
        val map: String?,
    ) : Event()

    data class IslandChange(
        val old: SkyBlockIsland?,
        val new: SkyBlockIsland?
    ) : Event()

    data class AreaChange(
        val old: SkyBlockArea,
        val new: SkyBlockArea
    ) : Event()

    data object SkyBlockJoin : Event()

    data object SkyBlockLeave : Event()

    @Deprecated("Use Server.Connect")
    data object ServerConnect : Event()

    @Deprecated("Use Server.Disconnect")
    data object ServerDisconnect : Event()

    sealed class Server : Event() {
        data object Connect : Server()

        data object Disconnect : Server()
    }
}
