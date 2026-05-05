@file:Suppress("FunctionName", "Unused")

package xyz.aerii.athen.api.websocket.base

import com.google.gson.JsonObject
import xyz.aerii.athen.api.websocket.WebSocket

interface IWebSocket {
    val auth: Boolean
        get() = WebSocket.auth

    fun JsonObject.`socket$send`() {
        WebSocket.send(this)
    }

    fun `socket$send`(id: Int, vararg kv: Pair<String, Any?>) {
        JsonObject().apply {
            addProperty("t", id)

            for ((k, v) in kv) {
                when (v) {
                    null -> {}
                    is String -> addProperty(k, v)
                    is Number -> addProperty(k, v)
                    is Boolean -> addProperty(k, v)
                    else -> error("Unsupported type: ${v::class}")
                }
            }
        }.`socket$send`()
    }
}