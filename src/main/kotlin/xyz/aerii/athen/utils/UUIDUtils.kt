package xyz.aerii.athen.utils

import com.mojang.serialization.Codec
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Scribble

object UUIDUtils {
    private val uuidHistory = Scribble("features/uuidHistory")
    private var nameToUuid = uuidHistory.mutableMap("nameToUuid", Codec.STRING, Codec.STRING)

    private val apis = listOf(
        "https://playerdb.co/api/player/minecraft/",
        "https://api.mojang.com/users/profiles/minecraft/",
        "https://api.ashcon.app/mojang/v2/user/"
    )

    fun getUUID(name: String, log: Boolean = false, callback: (String?) -> Unit) {
        val cached = nameToUuid.value[name]
        if (cached != null) return callback(cached)

        name.api(0, callback, log)
    }

    private fun String.api(apiIndex: Int, callback: (String?) -> Unit, log: Boolean = false) {
        if (apiIndex >= apis.size) return callback(null)

        Beacon.get("${apis[apiIndex]}$this", log) {
            timeout(connect = 10_000, read = 10_000)
            retries(max = 1)

            onJsonSuccess { json ->
                val uuid = when (apiIndex) {
                    0 -> json.getAsJsonObject("data")?.getAsJsonObject("player")?.get("raw_id")?.asString
                    1 -> json.get("id")?.asString
                    else -> json.get("uuid")?.asString?.replace("-", "")
                }

                if (uuid != null) {
                    nameToUuid.update { set(this@api, uuid) }
                    callback(uuid)
                } else {
                    api(apiIndex + 1, callback, log)
                }
            }

            onError {
                api(apiIndex + 1, callback, log)
            }
        }
    }
}