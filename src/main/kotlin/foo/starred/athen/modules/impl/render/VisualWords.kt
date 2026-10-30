@file:Suppress("Unused", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render

import com.mojang.serialization.Codec
import foo.starred.athen.Athen
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GameEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.kontext.abstraction.chat.generic.MinecraftChat
import foo.starred.kontext.abstraction.chat.message.MinecraftMessages
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.api.EMPTY_COMPONENT
import foo.starred.kontext.api.format.extension.component.literal
import foo.starred.kontext.api.storage.impl.generic.extension.map
import foo.starred.kontext.api.text.parser.impl.parse
import foo.starred.kontext.api.text.replacer.AbstractTextReplacer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence

@LoadableAthenModule
object VisualWords : Module(
    "Visual words",
    "Visually modify words!",
    ConfigCategory.RENDER
) {
    private const val SKIP = "\u0000vw_bypass"

    private val unused by config.information("Command: <red>/athen visuals help")
    private val nameChanger = config.switch("Name changer").unique("nameChanger")
    private val nickname = config.input("Nickname", "cooluser4").unique("nickname")

    private val json = JsonStore("features/visualWords")
    private var stored by json.map("words", Codec.STRING, ComponentSerialization.CODEC.xmap({ it.visualOrderText }, { seq -> seq.toComponent() }))

    @JvmField
    val words = object : AbstractTextReplacer() {}.also { it.skips = SKIP }

    init {
        observable.observe {
            words.version++
        }

        if (nameChanger.value) {
            nickname.value.fn()
        }

        nickname.state.observe {
            it.fn()
        }

        nameChanger.state.observe {
            if (it) return@observe nickname.value.fn()
            words.remove(client.user.name)
            words.build()
        }

        on<GameEvent.Start> {
            load()
        }

        on<GameEvent.Stop> {
            save()
        }

        command {
            "visuals" {
                help()
            }

            "visuals" / "help" {
                help()
            }

            "visuals" / "add" / string("word") / greedyString("replacement") {
                val a = string("word")
                val b = string("replacement").parse()
                val c = b.visualOrderText

                words.put(a, b.string, b, c)
                words.build()
                save()

                "Added the word <red>\"$a\" <gray>-> ".parse().skip().append(b).mod()
                if (!enabled) "Feature not enabled!".mod(MessagePrefixType.ERROR)
            }

            "visuals" / "set" / string("word") / greedyString("replacement") {
                val a = string("word")
                val b = string("replacement").parse()
                val c = b.visualOrderText

                words.put(a, b.string, b, c)
                words.build()
                save()

                "Set the word <red>\"$a\" <gray>-> ".parse().skip().append(b).mod()
                if (!enabled) "Feature not enabled!".mod(MessagePrefixType.ERROR)
            }

            "visuals" / "remove" / string("word") {
                val a = string("word")

                words.remove(a)
                words.build()
                save()

                "Removed the word <red>\"$a\"".parse().skip().mod()
                if (!enabled) "Feature not enabled!".mod(MessagePrefixType.ERROR)
            }

            "visuals" / "list" {
                "Replacement words list:".mod()
                for ((a, b) in words.map2) MinecraftMessages.display(" <dark_gray>• <r>$a <gray>-> ".parse().skip().append(b.toComponent()))
            }
        }
    }

    private fun String.fn() {
        if (nameChanger.value && isNotEmpty()) {
            val cmp = parse(true)
            words.put(client.user.name, cmp.string, cmp, cmp.visualOrderText)
            words.build()
            return
        }

        words.remove(client.user.name)
        words.build()
    }

    private fun help() {
        val divider = ("§8§m" + MinecraftChat.repeat("-")).literal()
        MinecraftMessages.display(divider)
        MinecraftMessages.display(MinecraftChat.center("§bVisual Words §7[Athen]"))
        MinecraftMessages.display(divider)
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen visuals add [word] [word, supports space]")
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen visuals set [word] [word, supports space]")
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen visuals remove [word]")
        MinecraftMessages.display(" <dark_gray>• <${MochaColorScheme.Green.argb}>/athen visuals list")
        MinecraftMessages.display(divider)
        MinecraftMessages.display(" <dark_gray>• <r>The text supports the format: ".parse().append("<hex><bold>te</bold>xt"))
        MinecraftMessages.display(" <hover:<${MochaColorScheme.Mauve.argb}>Click to join!><click:url:${Athen.Data.discord}><dark_gray>• <r>Want to know more about formats? Ask in the <${MochaColorScheme.Mauve.argb}>discord<r>!")
        MinecraftMessages.display(divider)
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
