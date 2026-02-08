@file:Suppress("UNUSED")

package xyz.aerii.athen.ui.themes

import xyz.aerii.athen.utils.rgba
import xyz.aerii.athen.utils.withAlpha

// Everything is from https://catppuccin.com/palette/, love them.
object Catppuccin {
    enum class Latte(override val rgba: Int) : Color {
        Rosewater(rgba(220, 138, 120)),
        Flamingo(rgba(221, 120, 120)),
        Pink(rgba(234, 118, 203)),
        Mauve(rgba(136, 57, 239)),
        Red(rgba(210, 15, 57)),
        Maroon(rgba(230, 69, 83)),
        Peach(rgba(254, 100, 11)),
        Yellow(rgba(223, 142, 29)),
        Green(rgba(64, 160, 43)),
        Teal(rgba(23, 146, 153)),
        Sky(rgba(4, 165, 229)),
        Sapphire(rgba(32, 159, 181)),
        Blue(rgba(30, 102, 245)),
        Lavender(rgba(114, 135, 253)),
        Text(rgba(76, 79, 105)),
        Subtext1(rgba(92, 95, 119)),
        Subtext0(rgba(108, 111, 133)),
        Overlay2(rgba(124, 127, 147)),
        Overlay1(rgba(140, 143, 161)),
        Overlay0(rgba(156, 160, 176)),
        Surface2(rgba(172, 176, 190)),
        Surface1(rgba(188, 192, 204)),
        Surface0(rgba(204, 208, 218)),
        Base(rgba(239, 241, 245)),
        Mantle(rgba(230, 233, 239)),
        Crust(rgba(220, 224, 232));

        override val argb: Int =
            (rgba ushr 8) or (rgba shl 24)
    }

    enum class Frappe(override val rgba: Int) : Color {
        Rosewater(rgba(242, 213, 207)),
        Flamingo(rgba(238, 190, 190)),
        Pink(rgba(244, 184, 228)),
        Mauve(rgba(202, 158, 230)),
        Red(rgba(231, 130, 132)),
        Maroon(rgba(234, 153, 156)),
        Peach(rgba(239, 159, 118)),
        Yellow(rgba(229, 200, 144)),
        Green(rgba(166, 209, 137)),
        Teal(rgba(129, 200, 190)),
        Sky(rgba(153, 209, 219)),
        Sapphire(rgba(133, 193, 220)),
        Blue(rgba(140, 170, 238)),
        Lavender(rgba(186, 187, 241)),
        Text(rgba(198, 208, 245)),
        Subtext1(rgba(181, 191, 226)),
        Subtext0(rgba(165, 173, 206)),
        Overlay2(rgba(148, 156, 187)),
        Overlay1(rgba(131, 139, 167)),
        Overlay0(rgba(115, 121, 148)),
        Surface2(rgba(98, 104, 128)),
        Surface1(rgba(81, 87, 109)),
        Surface0(rgba(65, 69, 89)),
        Base(rgba(48, 52, 70)),
        Mantle(rgba(41, 44, 60)),
        Crust(rgba(35, 38, 52));

        override val argb: Int =
            (rgba ushr 8) or (rgba shl 24)
    }

    enum class Macchiato(override val rgba: Int) : Color {
        Rosewater(rgba(244, 219, 214)),
        Flamingo(rgba(240, 198, 198)),
        Pink(rgba(245, 189, 230)),
        Mauve(rgba(198, 160, 246)),
        Red(rgba(237, 135, 150)),
        Maroon(rgba(238, 153, 160)),
        Peach(rgba(245, 169, 127)),
        Yellow(rgba(238, 212, 159)),
        Green(rgba(166, 218, 149)),
        Teal(rgba(139, 213, 202)),
        Sky(rgba(145, 215, 227)),
        Sapphire(rgba(125, 196, 228)),
        Blue(rgba(138, 173, 244)),
        Lavender(rgba(183, 189, 248)),
        Text(rgba(202, 211, 245)),
        Subtext1(rgba(184, 192, 224)),
        Subtext0(rgba(165, 173, 203)),
        Overlay2(rgba(147, 154, 183)),
        Overlay1(rgba(128, 135, 162)),
        Overlay0(rgba(110, 115, 141)),
        Surface2(rgba(91, 96, 120)),
        Surface1(rgba(73, 77, 100)),
        Surface0(rgba(54, 58, 79)),
        Base(rgba(36, 39, 58)),
        Mantle(rgba(30, 32, 48)),
        Crust(rgba(24, 25, 38));

        override val argb: Int =
            (rgba ushr 8) or (rgba shl 24)
    }

    enum class Mocha(override val rgba: Int) : Color {
        Rosewater(rgba(245, 224, 220)),
        Flamingo(rgba(242, 205, 205)),
        Pink(rgba(245, 194, 231)),
        Mauve(rgba(203, 166, 247)),
        Red(rgba(243, 139, 168)),
        Maroon(rgba(235, 160, 172)),
        Peach(rgba(250, 179, 135)),
        Yellow(rgba(249, 226, 175)),
        Green(rgba(166, 227, 161)),
        Teal(rgba(148, 226, 213)),
        Sky(rgba(137, 220, 235)),
        Sapphire(rgba(116, 199, 236)),
        Blue(rgba(137, 180, 250)),
        Lavender(rgba(180, 190, 254)),
        Text(rgba(205, 214, 244)),
        Subtext1(rgba(186, 194, 222)),
        Subtext0(rgba(166, 173, 200)),
        Overlay2(rgba(147, 153, 178)),
        Overlay1(rgba(127, 132, 156)),
        Overlay0(rgba(108, 112, 134)),
        Surface2(rgba(88, 91, 112)),
        Surface1(rgba(69, 71, 90)),
        Surface0(rgba(49, 50, 68)),
        Base(rgba(30, 30, 46)),
        Mantle(rgba(24, 24, 37)),
        Crust(rgba(17, 17, 27));

        override val argb: Int =
            (rgba ushr 8) or (rgba shl 24)
    }

    interface Color {
        val rgba: Int
        val argb: Int

        fun withAlpha(alpha: Float, rgba: Boolean = false): Int =
            if (rgba) this.rgba.withAlpha(alpha, rgba = true)
            else this.argb.withAlpha(alpha, rgba = false)
    }
}
