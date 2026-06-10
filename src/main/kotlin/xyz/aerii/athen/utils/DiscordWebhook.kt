package xyz.aerii.athen.utils

import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import xyz.aerii.athen.Athen
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object DiscordWebhook {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    fun send(url: String, content: String) {
        if (url.isBlank()) return
        Athen.SCOPE.launch {
            runCatching {
                val body = JsonObject().apply { addProperty("content", content) }.toString()
                val req = HttpRequest.newBuilder(URI(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()
                http.send(req, HttpResponse.BodyHandlers.discarding())
            }
        }
    }
}
