@file:Suppress("ObjectPrivatePropertyName", "Unchecked_cast")

package xyz.aerii.athen.modules.impl.general

import com.google.gson.Gson
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanNumeral
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import tech.thatgravyboat.skyblockapi.utils.text.TextUtils.substring
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.annotations.OnlyIn
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.config.ConfigManager.updateConfig
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GuiEvent
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.stripped
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import xyz.aerii.athen.utils.compress
import xyz.aerii.athen.utils.decompress
import xyz.aerii.athen.utils.enchants
import java.awt.Color

@Load
@OnlyIn(skyblock = true)
object ColoredEnchants : Module(
    "Colored enchants",
    "Custom colors for enchants!",
    Category.GENERAL
) {
    private val l = listOf("Bold", "Italic", "Underline", "Strike-through")

    private val replaceRoman by config.switch("Replace roman", true)

    private val `ultimate$expandable` by config.expandable("Ultimate enchants")
    private val `ultimate$color` by config.colorPicker("Ultimate color", Color(Catppuccin.Mocha.Mauve.argb, true)).childOf { `ultimate$expandable` }
    private val `ultimate$style` by config.multiCheckbox("Ultimate style", l, listOf(0)).childOf { `ultimate$expandable` }

    private val `max$expandable` by config.expandable("Maxed enchants")
    private val `max$color` by config.colorPicker("Max color", Color(TextColor.RED)).childOf { `max$expandable` }
    private val `max$style` by config.multiCheckbox("Max style", l).childOf { `max$expandable` }

    private val `high$expandable` by config.expandable("High-level enchants")
    private val `high$color` by config.colorPicker("High color", Color(TextColor.RED)).childOf { `high$expandable` }
    private val `high$style` by config.multiCheckbox("High style", l).childOf { `high$expandable` }

    private val `normal$expandable` by config.expandable("Normal-level enchants")
    private val `normal$color` by config.colorPicker("Normal color", Color(TextColor.BLUE)).childOf { `normal$expandable` }
    private val `normal$style` by config.multiCheckbox("Normal style", l).childOf { `normal$expandable` }

    private val `bad$expandable` by config.expandable("Bad-level enchants")
    private val `bad$color` by config.colorPicker("Bad color", Color(0xAA, 0xAA, 0xAA, 0xFF)).childOf { `bad$expandable` }
    private val `bad$style` by config.multiCheckbox("Bad style", l).childOf { `bad$expandable` }

    private val List<Int>.bold: Boolean
        get() = 0 in this

    private val List<Int>.italic: Boolean
        get() = 1 in this

    private val List<Int>.underline: Boolean
        get() = 2 in this

    private val List<Int>.strike: Boolean
        get() = 3 in this

    private val enchantRegex = Regex("(?<enchant>[A-Za-z][A-Za-z' -]*) (?<level>[IVXLCDM]+)")
    private val enchants = mutableMapOf<String, Enchant>()

    init {
        Beacon.get("https://raw.githubusercontent.com/Fix3dll/SkyblockAddons-Data/main/skyblock/enchants.json") {
            onSuccess<Map<String, Any>> { str ->
                for ((a, b) in str) {
                    val c = b as? Map<String, Map<String, Any?>> ?: continue
                    for ((d, e) in c) {
                        enchants[d] = Enchant(
                            e["loreName"] as String,
                            Enchant.category(a),
                            (e["maxLevel"] as Double).toInt(),
                            (e["goodLevel"] as Double).toInt()
                        )
                    }
                }
            }
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("export") {
                    thenCallback("enchants") {
                        val data = mapOf(
                            $$"ultimate$color" to `ultimate$color`.rgb,
                            $$"ultimate$style" to `ultimate$style`,
                            $$"max$color" to `max$color`.rgb,
                            $$"max$style" to `max$style`,
                            $$"high$color" to `high$color`.rgb,
                            $$"high$style" to `high$style`,
                            $$"normal$color" to `normal$color`.rgb,
                            $$"normal$style" to `normal$style`,
                            $$"bad$color" to `bad$color`.rgb,
                            $$"bad$style" to `bad$style`,
                            "replaceRoman" to replaceRoman
                        )

                        McClient.clipboard = Gson().toJson(data).compress()
                        "Enchant config exported to clipboard!".modMessage()
                    }
                }

                then("import") {
                    thenCallback("enchants") {
                        val clipboard = McClient.clipboard
                        if (clipboard.isEmpty()) return@thenCallback "No data found in clipboard!".modMessage(Typo.PrefixType.ERROR)

                        val map = Gson().fromJson<Map<String, Any>>(clipboard.decompress(), Map::class.java)

                        for ((k, v) in map) when (k) {
                            $$"ultimate$color", $$"max$color", $$"high$color", $$"normal$color", $$"bad$color" -> updateConfig("$configKey.$k", Color((v as Double).toInt(), true))
                            $$"ultimate$style", $$"max$style", $$"high$style", $$"normal$style", $$"bad$style" -> updateConfig("$configKey.$k", (v as List<Double>).map { it.toInt() })
                            "replaceRoman" -> updateConfig(k, v as Boolean)
                        }

                        "Enchant config imported successfully!".modMessage()
                    }
                }
            }
        }

        on<GuiEvent.Tooltip.Update> (100) {
            if (enchants.isEmpty()) return@on
            if (item.enchants().isEmpty()) return@on

            var found = false
            for (idx in tooltip.indices) {
                val l = tooltip[idx]
                val str = l.stripped()

                if (found && str.isEmpty()) break
                if ("◆" in str) continue
                if (idx < 4 && l.siblings?.firstOrNull()?.color == 0) continue

                val final = EMPTY_COMPONENT.copy()
                var i = 0

                for (match in enchantRegex.findAll(str)) {
                    val s = match.range.first
                    val f = match.range.last + 1

                    if (s > i) final.append(l.substring(i, s))

                    val name = match.groups["enchant"]?.value?.lowercase() ?: continue
                    val lv0 = match.groups["level"]?.value ?: continue
                    val lv1 = lv0.parseRomanNumeral()

                    val ec = enchants[name] ?: run {
                        final.append(l.substring(s, f))
                        i = f
                        continue
                    }

                    var str0 = ""
                    if (f < str.length && str[f] == ',') str0 = str[f].toString()

                    final.append("${ec.style(lv1)}${ec.name} ${if (replaceRoman) lv1 else lv0}$str0".parse())
                    i = f + str0.length
                    found = true
                }

                if (i < str.length) final.append(l.substring(i, str.length))
                tooltip[idx] = final
            }
        }
    }

    private data class Enchant(
        val name: String,
        val category: Enchant.Category,
        val max: Int,
        val good: Int
    ) {
        enum class Category {
            Stacking,
            Ultimate,
            Normal
        }

        fun style(int: Int): String {
            val (s, c) = when {
                category == Enchant.Category.Ultimate -> `ultimate$style` to `ultimate$color`
                int >= max -> `max$style` to `max$color`
                int > good -> `high$style` to `high$color`
                int == good -> `normal$style` to `normal$color`
                else -> `bad$style` to `bad$color`
            }

            return buildString {
                if (s.bold) append("<bold>")
                if (s.italic) append("<italic>")
                if (s.underline) append("<underline>")
                if (s.strike) append("<strikethrough>")
                append("<${c.rgb}>")
            }
        }

        companion object {
            fun category(str: String): Enchant.Category = when (str.uppercase()) {
                "STACKING" -> Enchant.Category.Stacking
                "ULTIMATE" -> Enchant.Category.Ultimate
                else -> Enchant.Category.Normal
            }
        }
    }
}
