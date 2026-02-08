package xyz.aerii.athen.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject

val JsonElement.asJsonObjectOrNull: JsonObject?
    get() = try {
        asJsonObject
    } catch (_: IllegalStateException) {
        null
    }