@file:Suppress("Unused")

package xyz.aerii.athen.modules.impl.render

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.serialization.Codec
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.handlers.Beacon
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.kotlin.CopyOnWriteMap

@Load
object VisualWords : Module(
    "Visual words",
    "Visually modify words!",
    Category.RENDER
) {
    private val unused by config.textParagraph("Use the command \"/athen visuals help\" to learn more about the available commands!")

    private const val SKIP = "\u0000vw_bypass"
    private val replace = CopyOnWriteMap<String, FormattedCharSequence>()
    private val scribble = Scribble("features/visualWords")
    private var words by scribble.map("words", Codec.STRING, ComponentSerialization.CODEC.xmap({ it.visualOrderText }, { seq -> seq.toComponent() }))

    private val remote = mutableSetOf<String>()
    private var f = HashSet<Int>()

    init {
        Beacon.get("https://raw.githubusercontent.com/skies-starred/athen-assets/refs/heads/main/cool.json") {
            onJsonSuccess { json ->
                val map = json.entrySet().associate { it.key to it.value.asString.parse().visualOrderText }
                replace += map
                remote += map.keys
                c()
            }
        }

        on<GameEvent.Start> {
            load()
        }

        on<GameEvent.Stop> {
            save()
        }

        on<CommandRegistration> {
            event.register(Athen.modId) {
                then("visuals") {
                    callback { help() }
                    thenCallback("help") { help() }

                    then("add") {
                        then("word", StringArgumentType.string()) {
                            thenCallback("replacement", StringArgumentType.greedyString()) {
                                val word = StringArgumentType.getString(this, "word")
                                val seq = StringArgumentType.getString(this, "replacement").parse().visualOrderText

                                replace[word] = seq
                                save()

                                "Added the word <red>\"$word\" <gray>-> ".parse().skip().append(seq.toComponent()).modMessage()
                                if (!react.value) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
                            }
                        }
                    }

                    then("set") {
                        then("word", StringArgumentType.string()) {
                            thenCallback("replacement", StringArgumentType.greedyString()) {
                                val word = StringArgumentType.getString(this, "word")
                                if (word in remote) return@thenCallback "Cannot override word '$word'".modMessage(Typo.PrefixType.ERROR)

                                val seq = StringArgumentType.getString(this, "replacement").parse().visualOrderText
                                replace[word] = seq
                                save()

                                "Set the word <red>\"$word\" <gray>-> ".parse().skip().append(seq.toComponent()).modMessage()
                                if (!react.value) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
                            }
                        }
                    }

                    then("remove") {
                        thenCallback("word", StringArgumentType.string()) {
                            val word = StringArgumentType.getString(this, "word")
                            if (word in remote) return@thenCallback "Cannot remove word '$word'".modMessage(Typo.PrefixType.ERROR)
                            replace.remove(word)
                            save()
                            "Removed the word <red>\"$word\"".parse().skip().modMessage()
                            if (!react.value) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
                        }
                    }

                    thenCallback("list") {
                        "Replacement words list:".modMessage()
                        for ((a, b) in replace) " <dark_gray>• <r>$a <gray>-> ".parse().skip().append(b.toComponent()).lie()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun fn(seq: FormattedCharSequence): FormattedCharSequence {
        if (replace.isEmpty()) return seq

        val input = seq.str()
        val styles = seq.style()
        val len = input.length

        return FormattedCharSequence { sink ->
            var i = 0
            while (i < len) {
                val cp = input.codePointAt(i)
                val cpLen = Character.charCount(cp)
                val style = styles[i]

                val skip = style.insertion == SKIP || (!react.value && replace.keys.any { it !in remote })
                if (skip || cp !in f) {
                    sink.accept(0, style, cp)
                    i += cpLen
                    continue
                }

                val match = replace.entries.firstOrNull { input.regionMatches(i, it.key, 0, it.key.length) }
                if (match == null) {
                    sink.accept(0, style, cp)
                    i += cpLen
                    continue
                }

                match.value.accept { _, repStyle, repCp -> sink.accept(0, style.applyTo(repStyle), repCp) }
                i += match.key.length
            }
            true
        }
    }

    private fun c() {
        f = replace.keys.mapNotNullTo(HashSet(replace.size)) { str -> str.codePointAt(0).takeIf { it >= 0 } }
    }

    private fun help() {
        val divider = ("§8§m" + "-".repeatBreak()).literal()
        divider.lie()
        "§bVisual Words §7[Athen]".centeredText().lie()
        divider.lie()
        " <dark_gray>• <${Catppuccin.Mocha.Green.argb}>/${Athen.modId} visuals add [word] [word, supports space]".parse().lie()
        " <dark_gray>• <${Catppuccin.Mocha.Green.argb}>/${Athen.modId} visuals set [word] [word, supports space]".parse().lie()
        " <dark_gray>• <${Catppuccin.Mocha.Green.argb}>/${Athen.modId} visuals remove [word]".parse().lie()
        " <dark_gray>• <${Catppuccin.Mocha.Green.argb}>/${Athen.modId} visuals list".parse().lie()
        divider.lie()
        " <dark_gray>• <r>The text supports the format: ".parse().append("<hex><bold>te</bold>xt").lie()
        " <hover:<${Catppuccin.Mocha.Mauve.argb}>Click to join!><click:url:${Athen.discordUrl}><dark_gray>• <r>Want to know more about formats? Ask in the <${Catppuccin.Mocha.Mauve.argb}>discord<r>!".parse().lie()
        divider.lie()
    }

    private fun load() {
        words.forEach { (k, comp) -> replace[k] = comp }
        c()
    }

    private fun save() {
        words = replace.filterKeys { it !in remote }
        c()
    }

    private fun Component.skip(): MutableComponent =
        copy().withStyle(style.withInsertion(SKIP))

    private fun FormattedCharSequence.toComponent(): Component {
        val builder = Component.literal("")

        accept { _, style, cp ->
            builder.append(Component.literal(Character.toString(cp)).withStyle(style))
            true
        }

        return builder
    }

    private fun FormattedCharSequence.str(): String {
        val builder = StringBuilder()

        accept { _, _, cp ->
            builder.appendCodePoint(cp)
            true
        }

        return builder.toString()
    }

    private fun FormattedCharSequence.style(): List<Style> {
        val list = mutableListOf<Style>()

        accept { _, style, _ ->
            list.add(style)
            true
        }

        return list
    }
}