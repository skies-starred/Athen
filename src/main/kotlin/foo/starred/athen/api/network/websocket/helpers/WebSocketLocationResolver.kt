package foo.starred.athen.api.network.websocket.helpers

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.location.LocationAPI
import foo.starred.athen.api.network.websocket.base.IWebSocket
import foo.starred.athen.api.network.websocket.data.SocketPacket
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.events.InternalEvent
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.core.on
import foo.starred.athen.modules.impl.ModSettings
import foo.starred.kontext.api.scheduling.scheduler.data.tasks.base.SchedulerTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@LoadableAthenModule
object WebSocketLocationResolver : IWebSocket {
    private var pending: String? = null
    private var task: SchedulerTask? = null
    private var last: Long = 0L

    val users: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        ModSettings.location.state.observe {
            if (it) {
                fn(LocationAPI.id ?: return@observe)
                return@observe
            }

            users.clear()
            task?.cancel()
            task = null
            pending = null
            `socket$send`(SocketPacket.WebSocket.ServerBound.Location.id, "b" to "")
        }

        on<InternalEvent.WebSocket.Auth> {
            fn(LocationAPI.id ?: return@on)
        }

        on<LocationEvent.Hypixel.Server> {
            fn(name)
            users.clear()
        }

        on<LocationEvent.Server.Disconnect> {
            task?.cancel()
            task = null
            pending = null
            users.clear()
        }

        on<InternalEvent.WebSocket.Message> {
            if (id != SocketPacket.WebSocket.ClientBound.Lobby.id) return@on

            users.clear()
            body?.split(",")?.filter { it.isNotEmpty() }?.let(users::addAll)
        }
    }

    private fun fn(id: String) {
        if (!auth) return
        if (!ModSettings.location.value) return

        val now = System.currentTimeMillis()
        if (now - last >= 10_000) {
            task?.cancel()
            task = null
            last = now
            pending = null
            `socket$send`(SocketPacket.WebSocket.ServerBound.Location.id, "b" to id)
            return
        }

        pending = id
        if (task != null) return
        task = Scheduler.schedule((10_000 - (now - last)).milliseconds) {
            task = null
            val pending0 = pending ?: return@schedule
            pending = null
            last = System.currentTimeMillis()
            `socket$send`(SocketPacket.WebSocket.ServerBound.Location.id, "b" to pending0)
        }
    }
}
