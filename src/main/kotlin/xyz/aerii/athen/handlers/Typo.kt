package xyz.aerii.athen.handlers

import net.minecraft.network.chat.Component
import xyz.aerii.athen.modules.impl.Dev
import xyz.aerii.library.api.lie
import xyz.aerii.library.utils.literal

object Typo {
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
