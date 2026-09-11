@file:Suppress("ObjectPrivatePropertyName", "Unchecked_cast")

package foo.starred.athen.modules.impl.general

import com.google.gson.Gson
import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.messaging.enums.MessageColors
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.config.Category
import foo.starred.athen.config.ConfigManager.update
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.ui.themes.Catppuccin
import foo.starred.athen.utils.command
import foo.starred.athen.utils.data
import foo.starred.athen.utils.enchants
import foo.starred.snowbird.api.EMPTY_COMPONENT
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.compress
import foo.starred.snowbird.utils.decompress
import foo.starred.snowbird.utils.stripped
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanNumeral
import tech.thatgravyboat.skyblockapi.utils.text.TextUtils.substring

@Load
@OnlyIn(skyblock = true)
object ColoredEnchants : Module(
    "Colored enchants",
    "Custom colors for enchants!",
    Category.GENERAL
) {
    private val l = listOf("Bold", "Italic", "Underline", "Strike-through")

    private val replaceRoman by config.switch("Replace roman", true)

    private val ultimate by config.group("Ultimate enchants")
    private val `ultimate$color` by ultimate.colorPicker("Ultimate color", Catppuccin.Mocha.Mauve.argb)
    private val `ultimate$style` by ultimate.multiSelector("Ultimate style", l, listOf(0))

    private val max by config.group("Maxed enchants")
    private val `max$color` by max.colorPicker("Max color", MessageColors.RED.color)
    private val `max$style` by max.multiSelector("Max style", l)

    private val high by config.group("High-level enchants")
    private val `high$color` by high.colorPicker("High color", MessageColors.RED.color)
    private val `high$style` by high.multiSelector("High style", l)

    private val normal by config.group("Normal-level enchants")
    private val `normal$color` by normal.colorPicker("Normal color", MessageColors.BLUE.color)
    private val `normal$style` by normal.multiSelector("Normal style", l)

    private val bad by config.group("Bad-level enchants")
    private val `bad$color` by bad.colorPicker("Bad color", 0xAAAAAAFF)
    private val `bad$style` by bad.multiSelector("Bad style", l)

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
        "enchants.json".data.request {
            success<Map<String, Any>> { str ->
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

        command {
            "export" / "enchants" {
                val data = mapOf(
                    $$"ultimate$color" to `ultimate$color`,
                    $$"ultimate$style" to `ultimate$style`,
                    $$"max$color" to `max$color`,
                    $$"max$style" to `max$style`,
                    $$"high$color" to `high$color`,
                    $$"high$style" to `high$style`,
                    $$"normal$color" to `normal$color`,
                    $$"normal$style" to `normal$style`,
                    $$"bad$color" to `bad$color`,
                    $$"bad$style" to `bad$style`,
                    "replaceRoman" to replaceRoman
                )

                client.keyboardHandler.clipboard = Gson().toJson(data).compress()
                "Enchant config exported to clipboard!".mod()
            }

            "import" / "enchants" {
                val clipboard = client.keyboardHandler.clipboard
                if (clipboard.isEmpty()) return@invoke "No data found in clipboard!".mod(MessagePrefixType.ERROR)

                val map = Gson().fromJson<Map<String, Any>>(clipboard.decompress(), Map::class.java)

                for ((k, v) in map) when (k) {
                    $$"ultimate$color", $$"max$color", $$"high$color", $$"normal$color", $$"bad$color" -> update("$configKey.$k", (v as Double).toInt())
                    $$"ultimate$style", $$"max$style", $$"high$style", $$"normal$style", $$"bad$style" -> update("$configKey.$k", (v as List<Double>).map { it.toInt() })
                    "replaceRoman" -> update(k, v as Boolean)
                }

                "Enchant config imported successfully!".mod()
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
                if (l.siblings.firstOrNull()?.style?.color?.value == 0) continue

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
                append("<$c>")
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
