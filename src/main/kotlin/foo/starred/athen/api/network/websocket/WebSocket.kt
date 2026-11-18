package foo.starred.athen.api.network.websocket

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import foo.starred.athen.Athen
import foo.starred.athen.Athen.SCOPE
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.AthenAPI
import foo.starred.athen.api.network.websocket.data.SocketPacket
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.events.GameEvent
import foo.starred.athen.events.InternalEvent
import foo.starred.athen.events.core.on
import foo.starred.athen.utils.command
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.player.MinecraftPlayer
import foo.starred.kontext.api.scheduling.scheduler.data.tasks.base.SchedulerTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.net.URI
import java.net.http.HttpClient
import java.util.concurrent.CompletionStage
import kotlin.time.Duration.Companion.seconds
import java.net.http.WebSocket as JavaWebSocket

@LoadableAthenModule
object WebSocket {
    private val http = HttpClient.newHttpClient()

    private var socket: JavaWebSocket? = null
    private var scheduler: SchedulerTask? = null

    private val out: Channel<JsonObject> = Channel(Channel.UNLIMITED)
    private val inn: Channel<String> = Channel(Channel.UNLIMITED)

    private val buffer = StringBuilder()

    @Volatile
    private var manual = false

    @Volatile
    var auth = false
        private set

    init {
        command {
            "ws" / "connect" {
                "<gray>Connecting to WebSocket...".mod()
                SCOPE.launch { connect() }
            }

            "ws" / "disconnect" {
                if (!auth) return@invoke "Not connected to WebSocket!".mod(MessagePrefixType.ERROR)
                close()
                "<gray>Disconnected from WebSocket.".mod()
            }
        }

        SCOPE.launch {
            for (c in out) {
                c.addProperty("n", client.user.name)
                socket?.sendText(c.toString(), true)
            }
        }

        SCOPE.launch {
            inn.consumeEach {
                fn(it)
            }
        }

        on<GameEvent.Stop> {
            close()
        }

        connect()
    }

    fun connect() {
        close()
        manual = false
        auth = false

        val auth = MinecraftPlayer.authenticate()
        if (!auth.first) return

        http.newWebSocketBuilder()
            .buildAsync(URI.create(AthenAPI.websocket()), object : JavaWebSocket.Listener {
                override fun onOpen(webSocket: JavaWebSocket) {
                    webSocket.sendText(JsonObject().apply {
                        addProperty("t", SocketPacket.WebSocket.ServerBound.Auth.id)
                        addProperty("n", client.user.name)
                        addProperty("s", auth.second)
                    }.toString(), true)
                    webSocket.request(1)
                }

                override fun onText(webSocket: JavaWebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                    buffer.append(data)

                    if (last) {
                        inn.trySend(buffer.toString())
                        buffer.clear()
                    }

                    webSocket.request(1)
                    return null
                }

                override fun onClose(webSocket: JavaWebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                    fail(reason)
                    return null
                }

                override fun onError(webSocket: JavaWebSocket, error: Throwable) {
                    fail(error.message)
                }
            })
            .whenComplete { socket, err ->
                if (err != null) {
                    fail(err.message)
                    return@whenComplete
                }
                WebSocket.socket = socket
            }
    }

    fun close() {
        manual = true
        scheduler?.cancel()
        auth = false
        socket?.sendClose(JavaWebSocket.NORMAL_CLOSURE, "Closed by client")
        socket = null
    }

    fun send(json: JsonObject) {
        out.trySend(json)
    }

    private fun fn(text: String) {
        val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return
        val t = json.get("t")?.asInt ?: return
        val c = json.get("c")?.asString
        val n = json.get("n")?.asString
        val b = json.get("b")?.asString

        when (t) {
            SocketPacket.WebSocket.ClientBound.AuthSuccess.id -> {
                auth = true
                InternalEvent.WebSocket.Auth.post()
                Athen.LOGGER.info("Websocket authenticated as $n")
                if (Athen.Data.debug) "<green>Connected to Websocket as <white>$n".mod()
            }

            SocketPacket.WebSocket.ClientBound.AuthError.id -> {
                Athen.LOGGER.error("Websocket authentication failed: $b")
                "<red>Failed to authenticate! <gray>Error: $b".mod(MessagePrefixType.ERROR)
            }

            SocketPacket.WebSocket.ClientBound.Error.id -> {
                Athen.LOGGER.error("Websocket error: $b")
                "<red>WS error: <gray>$b".mod(MessagePrefixType.ERROR)
            }

            SocketPacket.WebSocket.ClientBound.Warn.id -> {
                if (b != null) "<yellow>$b".mod(MessagePrefixType.ERROR)
            }

            else -> {
                InternalEvent.WebSocket.Message(t, b, c, n).post()
            }
        }
    }

    private fun fail(reason: String?) {
        auth = false
        if (manual) return

        Athen.LOGGER.error("Websocket connection failed/closed: $reason")
        scheduler?.cancel()
        scheduler = Scheduler.repeat(15.seconds) { connect() }
    }
}
