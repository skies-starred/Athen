package xyz.aerii.athen.api.websocket

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import xyz.aerii.athen.Athen
import xyz.aerii.athen.Athen.SCOPE
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.api.websocket.base.IWebSocket
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Chronos
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.utils.wsUrl
import xyz.aerii.library.api.client
import xyz.aerii.library.api.name
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.handlers.time.Task
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletionStage
import kotlin.time.Duration.Companion.seconds

@Load
object WebSocket {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val set: MutableSet<IWebSocket> = mutableSetOf()
    private val url = URI(wsUrl)
    private var ws: WebSocket? = null
    private var rc: Task? = null
    private var ii: Int = 0

    @JvmStatic
    val all: MutableSet<IWebSocket> = mutableSetOf()

    @Volatile
    var auth = false
        private set

    init {
        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("ws") {
                    thenCallback("connect") {
                        "<gray>Connecting to WebSocket...".parse().modMessage()
                        SCOPE.launch { connect() }
                    }

                    thenCallback("disconnect") {
                        if (!auth) return@thenCallback "Not connected to WebSocket!".modMessage(Typo.PrefixType.ERROR)
                        SCOPE.launch { close() }
                        "<gray>Disconnected from WebSocket.".parse().modMessage()
                    }
                }
            }
        }

        connect()
    }

    fun connect() {
        close()
        auth = false

        val s = UUID.randomUUID().toString()
        client.services().sessionService().joinServer(client.user.profileId, client.user.accessToken, s)

        http.newWebSocketBuilder().buildAsync(url, object : WebSocket.Listener {
            private val buffer = StringBuilder()

            override fun onOpen(webSocket: WebSocket) {
                ws = webSocket

                val auth = JsonObject().apply {
                    addProperty("t", SocketPacket.WebSocket.ServerBound.Auth.id)
                    addProperty("n", name)
                    addProperty("s", s)
                }

                webSocket.sendText(auth.toString(), true)
                webSocket.request(1)
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                buffer.append(data)
                if (!last) {
                    webSocket.request(1)
                    return null
                }

                if (ii == 0) {
                    fn1()
                }

                val json = buffer.toString()
                buffer.clear()

                val ss = set.toList()
                runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()?.let { j ->
                    val t = j.get("t")?.asInt ?: return@let
                    val c = j.get("c")?.asString
                    val n = j.get("n")?.asString
                    val b = j.get("b")?.asString

                    when (t) {
                        SocketPacket.WebSocket.ClientBound.AuthSuccess.id -> {
                            auth = true
                            Athen.LOGGER.info("Websocket authenticated as $n")
                            "<green>Connected to Websocket as <white>$n".parse().modMessage()
                        }

                        SocketPacket.WebSocket.ClientBound.AuthError.id -> {
                            Athen.LOGGER.error("Websocket authentication failed: $b")
                            "<red>Failed to authenticate! <gray>Error: $b".parse().modMessage(Typo.PrefixType.ERROR)
                        }

                        SocketPacket.WebSocket.ClientBound.Error.id -> {
                            Athen.LOGGER.error("Websocket error: $b")
                            "<red>WS error: <gray>$b".parse().modMessage(Typo.PrefixType.ERROR)
                        }

                        SocketPacket.WebSocket.ClientBound.Warn.id -> {
                            if (b != null) "<yellow>$b".parse().modMessage(Typo.PrefixType.ERROR)
                        }

                        else -> {
                            for (s in ss) s.fn0(t, c, n, b)
                        }
                    }
                }

                webSocket.request(1)
                return null
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                auth = false
                Athen.LOGGER.error("Websocket connection failed: ${error.message}")
                if (ws != null) fn0()
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                auth = false
                Athen.LOGGER.info("Websocket closed: $reason")
                if (ws != null) fn0()
                return null
            }
        })
    }

    fun close() {
        val ows = ws

        ws = null
        rc?.cancel()

        ows?.sendClose(WebSocket.NORMAL_CLOSURE, "")
        auth = false
    }

    fun send(json: JsonObject) {
        SCOPE.launch {
            json.addProperty("n", name)
            ws?.sendText(json.toString(), true)
        }
    }

    private fun fn0() {
        rc?.cancel()
        rc = Chronos.repeat(15.seconds) { connect() }
    }

    private fun fn1() {
        set.clear()
        ii = 1

        for (a in all) {
            val b = a.fn1()
            if (b.value) set.add(a)
            b.onChange { if (it) set.add(a) else set.remove(a) }
        }
    }
}