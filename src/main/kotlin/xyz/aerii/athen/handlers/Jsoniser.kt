package xyz.aerii.athen.handlers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import xyz.aerii.athen.annotations.Load
import java.awt.image.BufferedImage
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

@Load
object Jsoniser {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    inline fun <reified T> json(
        name: String,
        fallback: T? = null,
        builder: GsonBuilder? = null
    ): T? {
        return try {
            val parser = builder?.create() ?: gson
            val type = object : TypeToken<T>() {}.type

            InputStreamReader(stream(name), StandardCharsets.UTF_8).use {
                parser.fromJson<T>(it, type) ?: fallback
            }
        } catch (_: Throwable) {
            fallback
        }
    }

    fun image(name: String): BufferedImage? =
        stream(name).use {
            runCatching { ImageIO.read(it) }.getOrNull()
        }

    fun text(name: String): String =
        stream(name).use {
            it.readBytes().toString(StandardCharsets.UTF_8)
        }

    fun bytes(name: String): ByteArray =
        stream(name).use { it.readBytes() }

    fun stream(name: String): InputStream =
        Jsoniser::class.java.getResourceAsStream("/assets/athen/$name") ?: throw IllegalStateException("File not found!")
}