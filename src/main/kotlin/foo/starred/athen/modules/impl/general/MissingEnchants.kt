@file:Suppress("Unused")

package foo.starred.athen.modules.impl.general

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.items.ItemAPI.`watch$tooltip`
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.config.Category
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin.Mocha
import foo.starred.athen.utils.data
import foo.starred.athen.utils.enchants
import foo.starred.snowbird.api.EMPTY_COMPONENT
import foo.starred.snowbird.api.bound
import foo.starred.snowbird.api.pressed
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.stripped
import net.minecraft.network.chat.Component

@Load
@OnlyIn(skyblock = true)
object MissingEnchants : Module(
    "Missing enchants",
    "Shows missing enchants on the item you hover over.",
    Category.GENERAL
) {
    private val keybind: Int by config.keybind("Keybind", InputConstants.KEY_LSHIFT).`watch$tooltip`()
    private val _unused by config.information("You can unbind the keybind to always show.")

    private val typeRegex = Regex("""\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)\b\s+(?:DUNGEON\s+)?([A-Z]+(?: [A-Z]+)*)""") // https://regex101.com/r/MOQHMf/1
    private val romans = setOf("I","II","III","IV","V","VI","VII","VIII","IX","X")

    private var all: Map<String, List<String>>? = null
    private var pools: List<List<String>>? = null

    init {
        "enchants.json".data.request {
            success<JsonObject> { json ->
                val map = HashMap<String, MutableList<String>>()

                for (category in listOf("NORMAL", "STACKING", "ULTIMATE")) {
                    json[category]?.asJsonObject?.entrySet()?.forEach { (_, v) ->
                        val obj = v.asJsonObject
                        val nbt = obj["nbtName"].asString.lowercase()
                        obj["appliedTo"]?.asJsonArray?.forEach { type ->
                            map.getOrPut(type.asString) { mutableListOf() }.add(nbt)
                        }
                    }
                }

                all = map
                pools = json["ENCHANT_POOLS"]?.asJsonArray?.map { pool -> pool.asJsonArray.map { it.asString.lowercase() } }
            }
        }

        on<GuiEvent.Tooltip.Update> {
            if (keybind.bound && !keybind.pressed) return@on

            val a = all ?: return@on
            val b = pools ?: return@on

            val rawLore = tooltip.asReversed()
            var i: String? = null

            for (line in rawLore) {
                val m = typeRegex.find(line.stripped()) ?: continue
                i = m.groupValues[1]
                break
            }

            if (i == null) return@on
            val enchants = item.enchants()
            if (enchants.contains("one_for_all")) return@on

            val all = a[i] ?: return@on
            val missing = ArrayList<String>(all.size)

            a@ for (enc in all) {
                if (enc in enchants) continue

                for (pool in b) if (enc in pool && pool.any(enchants::contains)) continue@a
                if (!enc.startsWith("ultimate_")) missing.add(enc.prettify())
            }

            val ult = enchants.any { it.startsWith("ultimate_") }
            if (ult && missing.isEmpty()) return@on
            if (!ult) missing.add(0, "Ultimate enchant")

            var ii = -1
            var se = false

            for (i in tooltip.indices) {
                val l = tooltip[i]
                val s = l.string

                if (!se) {
                    if (':' in s) continue
                    if ("◆" in s) continue
                    if (!s.r()) continue

                    val ls = l.siblings.lastOrNull() ?: continue
                    if ((ls.style.color?.value == 11184810  || ls.style.color?.value == 0) && !ls.style.isBold) continue

                    se = true
                    continue
                }

                if (s.isNotBlank()) continue
                ii = i
                break
            }

            if (ii == -1) ii = tooltip.indexOfFirst { it.string.isEmpty() }.takeIf { it != -1 } ?: minOf(tooltip.size, 1)

            val nl = ArrayList<Component>(2 + missing.size)
            nl.add(EMPTY_COMPONENT)
            nl.add("<${Mocha.Mauve.argb}>✦ Missing:".parse())

            for (i in missing.indices step 3) {
                val chunk = missing.subList(i, minOf(i + 3, missing.size))
                nl.add("<${Mocha.Text.argb}> • ${chunk.joinToString(", ")}".parse())
            }

            tooltip.addAll(ii, nl)
        }
    }

    private fun String.prettify(): String =
        buildString(length) {
            var cap = true
            for (c in this@prettify) {
                when {
                    c == '_' -> {
                        append(' ')
                        cap = true
                    }

                    cap -> {
                        append(c.uppercaseChar())
                        cap = false
                    }

                    else -> append(c)
                }
            }
        }

    fun String.r(): Boolean =
        split(',').any { romans.contains(it.substringAfterLast(' ').trim()) }
}
