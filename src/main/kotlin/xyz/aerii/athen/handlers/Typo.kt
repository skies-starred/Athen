package xyz.aerii.athen.handlers

import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.helpers.McClient
import xyz.aerii.athen.handlers.Smoothie.client
import xyz.aerii.athen.handlers.Texter.literal
import xyz.aerii.athen.modules.impl.Dev
import kotlin.math.roundToInt

object Typo {
    private val STRIP_COLOR_REGEX = Regex("(?i)§.")

    @JvmStatic
    val chatWidth: Int
        get() = ChatComponent.getWidth(client.options.chatWidth().get())

    @JvmStatic
    val chatHeight: Int
        get() = ChatComponent.getHeight(if (client.gui.chat.isChatFocused) client.options.chatHeightFocused().get() else client.options.chatHeightUnfocused().get())

    @JvmStatic
    fun String.stripped(): String {
        return STRIP_COLOR_REGEX.replace(this, "")
    }

    @JvmStatic
    fun Component.stripped(): String {
        return STRIP_COLOR_REGEX.replace(this.string, "")
    }

    @JvmStatic
    fun String.message() {
        McClient.connection?.sendChat(this)
    }

    @JvmStatic
    @JvmOverloads
    fun String.modMessage(prefixType: PrefixType = PrefixType.DEFAULT) {
        prefixType.component.copy().append("§f $this".literal()).lie()
    }

    @JvmStatic
    @JvmOverloads
    fun Component.modMessage(prefixType: PrefixType = PrefixType.DEFAULT) {
        val colored = if (this.style.color == null) this.copy().withColor(0xFFFFFF) else this
        prefixType.component.copy().append(" ".literal()).append(colored).lie()
    }

    @JvmStatic
    fun String.devMessage() {
        if (Dev.debug) modMessage(PrefixType.DEV)
    }

    @JvmStatic
    fun Component.devMessage() {
        if (Dev.debug) modMessage(PrefixType.DEV)
    }

    @JvmStatic
    fun String.command() {
        McClient.sendCommand(this)
    }

    @JvmStatic
    fun String.clientCommand() {
        McClient.sendClientCommand(this)
    }

    @JvmStatic
    fun String.lie() {
        @Suppress("UNNECESSARY_SAFE_CALL") // gui can be null, why the fuck does it say that it can't be null
        client.execute { client.gui?.chat?.addMessage(this.literal()) }
    }

    @JvmStatic
    fun Component.lie() {
        @Suppress("UNNECESSARY_SAFE_CALL")
        client.execute { client.gui?.chat?.addMessage(this) }
    }

    @JvmStatic
    fun String.repeatBreak(): String {
        return repeat(chatWidth / client.font.width(this))
    }

    @JvmStatic
    fun String.centeredText(): String {
        val width = chatWidth
        val width1 = client.font.width(this)
        if (width1 >= width) return this

        return " ".repeat(((width - width1) / 2f / client.font.width(" ")).roundToInt()) + this
    }

    enum class PrefixType(val component: Component) {
        DEFAULT(
            "[".literal().withColor(0xFFBCE2)
                .append("A".literal().withColor(0xFDCCDA))
                .append("t".literal().withColor(0xFCDDD3))
                .append("h".literal().withColor(0xFAEDCB))
                .append("e".literal().withColor(0xF0E2D7))
                .append("n".literal().withColor(0xE5D8E4))
                .append("]".literal().withColor(0xDBCDF0))
        ),
        SUCCESS(
            "[".literal().withColor(0xC5F3FF)
                .append("A".literal().withColor(0xACF0FF))
                .append("t".literal().withColor(0x92EEFF))
                .append("h".literal().withColor(0x79EBFF))
                .append("e".literal().withColor(0x92EEFF))
                .append("n".literal().withColor(0xACF0FF))
                .append("]".literal().withColor(0xC5F3FF))
        ),
        ERROR(
            "[".literal().withColor(0xE48296)
                .append("A".literal().withColor(0xED5775))
                .append("t".literal().withColor(0xF62B55))
                .append("h".literal().withColor(0xFF0034))
                .append("e".literal().withColor(0xF62B55))
                .append("n".literal().withColor(0xED5775))
                .append("]".literal().withColor(0xE48296))
        ),
        DEV(
            "[".literal().withColor(0x317F94)
                .append("A".literal().withColor(0x316C7C))
                .append("t".literal().withColor(0x315A64))
                .append("h".literal().withColor(0x31474C))
                .append("e".literal().withColor(0x2D5359))
                .append("n".literal().withColor(0x2A5E67))
                .append("]".literal().withColor(0x266A74))
        )
    }
}
