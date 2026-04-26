package xyz.aerii.athen.handlers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.impl.ModSettings
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
object Websocket {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val url = URI(wsUrl)
    private var ws: WebSocket? = null
    private var rc: Task? = null
    private var cc: String = "general"

    @Volatile
    var authenticated = false
        private set

    init {
        if (ModSettings.irc) connect()
    }

    fun connect() {
        close()
        authenticated = false

        val s = UUID.randomUUID().toString()
        client.services().sessionService().joinServer(client.user.profileId, client.user.accessToken, s)

        http.newWebSocketBuilder()
            .buildAsync(url, object : WebSocket.Listener {
                private val buffer = StringBuilder()

                override fun onOpen(webSocket: WebSocket) {
                    ws = webSocket
                    val auth = JsonObject().apply {
                        addProperty("t", "auth")
                        addProperty("n", name)
                        addProperty("s", s)
                    }
                    webSocket.sendText(auth.toString(), true)
                    webSocket.request(1)
                }

                override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                    buffer.append(data)
                    if (last) {
                        val text = buffer.toString()
                        buffer.clear()

                        runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()?.let { json ->
                            val type = json.get("t")?.asString ?: return@let
                            val c = json.get("c")?.asString
                            val b = json.get("b")?.asString
                            val n = json.get("n")?.asString

                            when (type) {
                                "auth_ok" -> {
                                    authenticated = true
                                    Athen.LOGGER.info("Websocket authenticated as $n")
                                    "<green>Connected to Websocket as <white>$n".parse().modMessage()
                                }

                                "err" -> {
                                    Athen.LOGGER.error("IRC message rejected: $b")
                                    "<red>Message rejected: <gray>$b".parse().modMessage(Typo.PrefixType.ERROR)
                                }

                                "joined" -> {
                                    c?.let { 
                                        cc = it
                                        "<gray>Joined channel <aqua>#$it".parse().modMessage() 
                                    }
                                }

                                "left" -> {
                                    c?.let { 
                                        if (cc == it) cc = "general"
                                        "<gray>Left channel <aqua>#$it".parse().modMessage() 
                                    }
                                }

                                "msg" -> {
                                    if (c != null && n != null && b != null) {
                                        if (n == name) return@let
                                        "<dark_gray>[<aqua>#$c<dark_gray>] <white>$n<dark_gray>: <gray>$b".parse().modMessage()
                                    }
                                }

                                "list" -> {
                                    b?.let { body ->
                                        val channels = runCatching {
                                            JsonParser.parseString(body).asJsonArray.map {
                                                val arr = it.asJsonArray
                                                "${arr[0].asString} (${arr[1].asInt})"
                                            }.sortedWith(compareBy({ if (it.startsWith("general ")) 0 else 1 }, { it }))
                                        }.getOrNull()

                                        channels?.let {
                                            if (it.isEmpty()) "<gray>No active channels.".parse().modMessage()
                                            else "<gray>Active channels: <aqua>${it.joinToString("<dark_gray>, <aqua>") { ch -> "#$ch" }}".parse().modMessage()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    webSocket.request(1)
                    return null
                }

                override fun onError(webSocket: WebSocket, error: Throwable) {
                    authenticated = false
                    Athen.LOGGER.error("Websocket connection failed: ${error.message}")
                    "<red>Websocket connection failed: <gray>${error.message}".parse().modMessage(Typo.PrefixType.ERROR)
                    if (ws != null) rc()
                }

                override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                    authenticated = false
                    Athen.LOGGER.info("Websocket closed: $reason")
                    if (ws != null) rc()
                    return null
                }
            })
    }

    private fun rc() {
        rc?.cancel()
        rc = Chronos.repeat(15.seconds) { connect() }
    }

    private fun msg(json: JsonObject) {
        json.addProperty("n", name)
        ws?.sendText(json.toString(), true)
    }

    fun create(channel: String, pin: String? = null) {
        msg(JsonObject().apply {
            addProperty("t", "create")
            addProperty("c", channel)
            pin?.let { addProperty("p", it) }
        })
    }

    fun setPin(pin: String) {
        msg(JsonObject().apply {
            addProperty("t", "set_pin")
            addProperty("p", pin)
        })
    }

    fun join(channel: String, pin: String? = null) {
        msg(JsonObject().apply {
            addProperty("t", "join")
            addProperty("c", channel)
            pin?.let { addProperty("p", it) }
        })
    }

    fun leave() {
        msg(JsonObject().apply { addProperty("t", "leave") })
    }

    fun send(body: String) {
        msg(JsonObject().apply {
            addProperty("t", "msg")
            addProperty("b", body)
        })
        "<dark_gray>[<aqua>#$cc<dark_gray>] <white>$name<dark_gray>: <gray>$body".parse().modMessage()
    }

    fun list() {
        msg(JsonObject().apply { addProperty("t", "list") })
    }

    fun close() {
        val ows = ws

        ws = null
        rc?.cancel()

        ows?.sendClose(WebSocket.NORMAL_CLOSURE, "")
        authenticated = false
    }
}