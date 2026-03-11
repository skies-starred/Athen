package xyz.aerii.athen.modules.impl

import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.handlers.Roulette
import xyz.aerii.athen.handlers.Roulette.scope
import xyz.aerii.athen.handlers.Scribble

@Load
object Dev {
    @JvmField
    var cool: Set<String> = setOf()

    @JvmField
    var bad: Map<String, String> = mapOf()

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
        scope.launch {
            Roulette.download.await()
            Roulette.file("people.json").takeIf { it.exists() }?.readText()?.let(JsonParser::parseString)?.asJsonObject?.let {
                val c = HashSet<String>()
                val b = HashMap<String, String>()

                it.getAsJsonArray("cool")?.forEach { name ->
                    c.add(name.asString)
                }

                it.getAsJsonObject("bad")?.entrySet()?.forEach { (name, reason) ->
                    b[name] = reason.asString
                }

                cool = c
                bad = b
            } ?: return@launch
        }.invokeOnCompletion {
            it?.let { Athen.LOGGER.error("Failed to load people.json: ${it.message}") }
        }
    }
}