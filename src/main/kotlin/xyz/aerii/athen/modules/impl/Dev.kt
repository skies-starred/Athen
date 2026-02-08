package xyz.aerii.athen.modules.impl

import com.google.gson.JsonParser
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Roulette
import xyz.aerii.athen.handlers.Scribble

@Load
object Dev {
    @Suppress("ClassName") // people > People :p
    object people {
        @JvmStatic
        val cool: MutableSet<String> = mutableSetOf()

        @JvmStatic
        val bad: MutableMap<String, String> = mutableMapOf()
    }

    @JvmStatic
    val file = Scribble("main/Dev")

    @JvmStatic
    var lastVersion: String by file.string("lastVersion")

    @JvmStatic
    var debug: Boolean by file.boolean("enabled")

    @JvmStatic
    var clickUiHelperCollapsed: Boolean by file.boolean("clickUiHelperCollapsed")

    @JvmStatic
    var clickUiHelperHidden: Boolean by file.boolean("clickUiHelperHidden")

    @JvmStatic
    var lastBroadcast: String by file.string("lastBroadcast")

    @JvmStatic
    var remoteAssetsVersion: String by file.string("remoteAssetsVersion")

    init {
        val peopleFile = Roulette.file("people.json")

        if (peopleFile.exists()) {
            runCatching {
                val jsonObject = JsonParser.parseString(peopleFile.readText()).asJsonObject

                jsonObject.getAsJsonArray("cool")?.forEach {
                    people.cool.add(it.asString)
                }

                jsonObject.getAsJsonObject("bad")?.entrySet()?.forEach { (name, reason) ->
                    people.bad[name] = reason.asString
                }
            }.onFailure {
                Athen.LOGGER.error("Failed to load people.json: ${it.message}")
            }
        }
    }
}