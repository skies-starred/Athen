@file:Suppress("Unused", "ObjectPrivatePropertyName")

package xyz.aerii.athen.modules.impl.render

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.serialization.Codec
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.GameEvent
import xyz.aerii.athen.handlers.Scribble
import xyz.aerii.athen.handlers.Typo
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module
import xyz.aerii.athen.ui.themes.Catppuccin
import xyz.aerii.library.api.*
import xyz.aerii.library.handlers.minecraft.AbstractWords
import xyz.aerii.library.handlers.parser.parse
import xyz.aerii.library.utils.literal

@Load
object VisualWords : Module(
    "Visual words",
    "Visually modify words!",
    Category.RENDER,
    true
) {
    private const val SKIP = "\u0000vw_bypass"

    private val unused by config.textParagraph("Use the command \"/athen visuals help\" to learn more about the available commands!")
    private val nameChanger = config.switch("Name changer").custom("nameChanger")
    private val nickname = config.textInput("Nickname", "cooluser4").dependsOn { nameChanger.value }.custom("nickname")

    private val scribble = Scribble("features/visualWords")
    private var stored by scribble.map("words", Codec.STRING, ComponentSerialization.CODEC.xmap({ it.visualOrderText }, { seq -> seq.toComponent() }))

    @JvmField
    val words = object : AbstractWords() {}.also { it.skips = SKIP }

    init {
        if (nameChanger.value) {
            nickname.value.fn()
        }

        nickname.state.onChange {
            it.fn()
        }

        nameChanger.state.onChange {
            if (it) return@onChange nickname.value.fn()
            words.remove(name)
            words.build()
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
                                val cmp = StringArgumentType.getString(this, "replacement").parse()
                                val seq = cmp.visualOrderText

                                words.put(word, cmp.string, cmp, seq)
                                words.build()
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
                                val cmp = StringArgumentType.getString(this, "replacement").parse()
                                val seq = cmp.visualOrderText

                                words.put(word, cmp.string, cmp, seq)
                                words.build()
                                save()

                                "Set the word <red>\"$word\" <gray>-> ".parse().skip().append(seq.toComponent()).modMessage()
                                if (!enabled) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
                            }
                        }
                    }

                    then("remove") {
                        thenCallback("word", StringArgumentType.string()) {
                            val word = StringArgumentType.getString(this, "word")

                            words.remove(word)
                            words.build()
                            save()

                            "Removed the word <red>\"$word\"".parse().skip().modMessage()
                            if (!enabled) "Feature not enabled!".modMessage(Typo.PrefixType.ERROR)
                        }
                    }

                    thenCallback("list") {
                        "Replacement words list:".modMessage()
                        for ((a, b) in words.map2) " <dark_gray>• <r>$a <gray>-> ".parse().skip().append(b.toComponent()).lie()
                    }
                }
            }
        }
    }

    private fun String.fn() {
        if (nameChanger.value && isNotEmpty()) {
            val cmp = parse(true)
            words.put(name, this, cmp, cmp.visualOrderText)
            words.build()
            return
        }

        words.remove(name)
        words.build()
    }

    private fun help() {
        val divider = ("§8§m" + "-".repeat()).literal()
        divider.lie()
        "§bVisual Words §7[Athen]".center().lie()
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
        for ((k, v) in stored) {
            val c = v.toComponent()
            words.put(k, c.string, c, v)
        }

        words.build()
    }

    private fun save() {
        stored = words.map2
        words.build()
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