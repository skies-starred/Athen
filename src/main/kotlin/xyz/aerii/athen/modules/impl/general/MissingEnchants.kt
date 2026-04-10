@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import com.google.gson.JsonObject
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.bold
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.italic
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.Beacon.request
import xyz.aerii.athen.handlers.Itemizer.`watch$tooltip`
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.data
import xyz.aerii.athen.utils.enchants
import xyz.aerii.library.api.EMPTY_COMPONENT
import xyz.aerii.library.api.bound
import xyz.aerii.library.api.pressed
import xyz.aerii.library.utils.literal
import xyz.aerii.library.utils.stripped

@Load
@OnlyIn(skyblock = true)
object MissingEnchants : Module(
    "Missing enchants",
    "Shows missing enchants on the item you hover over.",
    Category.GENERAL
) {
    private val keybind: Int by config.keybind("Keybind", GLFW.GLFW_KEY_LEFT_SHIFT).`watch$tooltip`()
    private val _unused by config.textParagraph("You can unbind the keybind to always show.")

    private val typeRegex = Regex("""\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)\b\s+(?:DUNGEON\s+)?([A-Z]+(?: [A-Z]+)*)""") // https://regex101.com/r/MOQHMf/1
    private val romans = setOf("I","II","III","IV","V","VI","VII","VIII","IX","X")

    private var allEnchants: Map<String, List<String>>? = null
    private var enchantPools: List<List<String>>? = null

    init {
        "enchants/neu.json".data.request {
            onSuccess<JsonObject> { json ->
                allEnchants = json["enchants"]?.asJsonObject?.entrySet()?.associate { (key, value) ->
                    key to value.asJsonArray.map { it.asString.lowercase() }
                }

                enchantPools = json["enchant_pools"]?.asJsonArray?.map { pool ->
                    pool.asJsonArray.map { it.asString.lowercase() }
                }
            }
        }

        on<GuiEvent.Tooltip.Update> {
            if (keybind.bound && !keybind.pressed) return@on

            val a = allEnchants ?: return@on
            val b = enchantPools ?: return@on

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

                    val ls = l.siblings?.lastOrNull() ?: continue
                    if ((ls.color == 11184810  || ls.color == 0) && !ls.bold) continue

                    se = true
                    continue
                }

                if (!s.isBlank()) continue
                ii = i
                break
            }

            if (ii == -1) ii = tooltip.indexOfFirst { it.string.isEmpty() }.takeIf { it != -1 } ?: minOf(tooltip.size, 1)

            val nl = ArrayList<Component>(2 + missing.size)
            nl.add(EMPTY_COMPONENT)
            nl.add("✦ Missing:".literal().withColor(Mocha.Mauve.argb).apply { italic = false })

            for (i in missing.indices step 3) {
                val chunk = missing.subList(i, minOf(i + 3, missing.size))
                nl.add(" • ${chunk.joinToString(", ")}".literal().withColor(Mocha.Text.argb).apply { italic = false })
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