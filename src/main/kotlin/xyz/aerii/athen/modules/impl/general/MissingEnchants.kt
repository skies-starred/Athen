@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.general

import dev.deftu.omnicore.api.client.input.OmniKeys
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.italic
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Itemizer.`watch$tooltip`
import xyz.aerii.athen.handlers.Texter.colorCoded
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin.Mocha
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import xyz.aerii.athen.utils.enchants
import xyz.aerii.athen.utils.isBound
import xyz.aerii.athen.utils.isPressed

@Load
@OnlyIn(skyblock = true)
object MissingEnchants : Module(
    "Missing enchants",
    "Shows missing enchants on the item you hover over.",
    Category.GENERAL
) {
    private val keybind: Int by config.keybind("Keybind", OmniKeys.KEY_LEFT_SHIFT.code).`watch$tooltip`()
    private val _unused by config.textParagraph("You can unbind the keybind to always show.")

    private val typeRegex = Regex("""\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|VERY SPECIAL)\b\s+(?:DUNGEON\s+)?([A-Z]+(?: [A-Z]+)*)""") // https://regex101.com/r/MOQHMf/1
    private val romans = setOf("I","II","III","IV","V","VI","VII","VIII","IX","X")

    private var allEnchants: Map<String, List<String>>? = null
    private var enchantPools: List<List<String>>? = null

    init {
        Beacon.get("https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/refs/heads/master/constants/enchants.json") {
            onJsonSuccess { json ->
                allEnchants = json["enchants"]?.asJsonObject?.entrySet()?.associate { (key, value) ->
                    key to value.asJsonArray.map { it.asString.lowercase() }
                }

                enchantPools = json["enchant_pools"]?.asJsonArray?.map { pool ->
                    pool.asJsonArray.map { it.asString.lowercase() }
                }
            }
        }

        on<GuiEvent.Tooltip.Update> {
            if (keybind.isBound() && !keybind.isPressed()) return@on

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
                    if (!s.r()) continue
                    if (l.colorCoded().contains("§7")) continue
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