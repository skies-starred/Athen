@file:Suppress("Unused", "ObjectPrivatePropertyName")

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
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.centeredText
import xyz.aerii.athen.handlers.Typo.lie
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.handlers.Typo.repeatBreak
import xyz.aerii.athen.handlers.parse
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.athen.utils.EMPTY_COMPONENT
import xyz.aerii.athen.utils.kotlin.CopyOnWriteMap

@Load
object VisualWords : Module(
    "Visual words",
    "Visually modify words!",
    Category.RENDER,
    true
) {
    private class Entry(val cps: IntArray, val len: Int, val seq: FormattedCharSequence)

    private val unused by config.textParagraph("Use the command \"/athen visuals help\" to learn more about the available commands!")
    private val nameChanger by config.switch("Name changer")
    private val nickname = config.textInput("Nickname", "cooluser4").dependsOn { nameChanger }.custom("nickname")

    private const val SKIP = "\u0000vw_bypass"

    private val replace = CopyOnWriteMap<String, FormattedCharSequence>()
    private val prefix = HashMap<Int, MutableList<Entry>>()

    private val scribble = Scribble("features/visualWords")
    private var words by scribble.map("words", Codec.STRING, ComponentSerialization.CODEC.xmap({ it.visualOrderText }, { seq -> seq.toComponent() }))

    private val remote = mutableSetOf<String>()
    private val user = client.user.name
    private val `user$cps` = user.codePoints().toArray()
    private val `user$len` = `user$cps`.size

    private var nick: FormattedCharSequence? = null

    init {
        nick = nickname.value.parse(true).visualOrderText
        nickname.state.onChange { nick = it.parse(true).visualOrderText }

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
                    callback {
                        help()
                    }

                    thenCallback("help") {
                        help()
                    }

                    then("add") {
                        then("word", StringArgumentType.string()) {
                            thenCallback("replacement", StringArgumentType.greedyString()) {
                                val word = StringArgumentType.getString(this, "word")
                                val seq = StringArgumentType.getString(this, "replacement").parse().visualOrderText

                                replace[word] = seq
                                save()

                                "Added the word <red>\"$word\" <gray>-> ".parse().skip().append(seq.toComponent()).modMessage()
                                if (!enabled) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
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
                                if (!enabled) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
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
                            if (!enabled) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
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
        if (replace.isEmpty() && !nameChanger) return seq

        val a = nameChanger
        val chars = IntArray(256)
        val styles = ArrayList<Style>(256)
        var size = 0

        seq.accept { _, style, cp ->
            if (size >= chars.size) return@accept false
            chars[size] = cp
            styles.add(style)
            size++
            true
        }

        return FormattedCharSequence { sink ->
            var i = 0

            while (i < size) {
                val cp = chars[i]
                val style = styles[i]

                if (a && i + `user$len` <= size) {
                    var j = 0
                    while (j < `user$len`) {
                        if (chars[i + j] != `user$cps`[j]) break
                        j++
                    }

                    if (j == `user$len`) {
                        val seq2 = nick ?: run {
                            sink.accept(0, style, cp)
                            i++
                            continue
                        }

                        seq2.accept { _, repStyle, repCp ->
                            sink.accept(0, repStyle.applyTo(style), repCp)
                            true
                        }

                        i += `user$len`
                        continue
                    }
                }

                if (style.insertion == SKIP) {
                    sink.accept(0, style, cp)
                    i++
                    continue
                }

                val bucket = prefix[cp]
                if (bucket == null) {
                    sink.accept(0, style, cp)
                    i++
                    continue
                }

                var matched: Entry? = null
                for (entry in bucket) {
                    val len = entry.len
                    if (i + len > size) continue

                    val cps = entry.cps
                    var j = 0
                    while (j < len && chars[i + j] == cps[j]) j++
                    if (j == len) {
                        matched = entry
                        break
                    }
                }

                if (matched == null) {
                    sink.accept(0, style, cp)
                    i++
                    continue
                }

                val seq2 = matched.seq
                seq2.accept { _, repStyle, repCp ->
                    sink.accept(0, repStyle.applyTo(style), repCp)
                    true
                }

                i += matched.len
            }

            true
        }
    }

    private fun c() {
        prefix.clear()

        for ((k, v) in replace) {
            val cps = k.codePoints().toArray()
            val entry = Entry(cps, cps.size, v)
            prefix.computeIfAbsent(cps[0]) { mutableListOf() }.add(entry)
        }
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
        val builder = EMPTY_COMPONENT.copy()

        accept { _, style, cp ->
            builder.append(Character.toString(cp).literal().withStyle(style))
            true
        }

        return builder
    }
}