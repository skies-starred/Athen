package xyz.aerii.athen.api.slayers.enums.tier

import tech.thatgravyboat.skyblockapi.api.data.MayorPerks
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanNumeral
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup

enum class SlayerTier(val int: Int, private val _xp: Int) {
    One(1, 5),
    Two(2, 25),
    Three(3, 100),
    Four(4, 500),
    Five(5, 1500);

    val xp: Int
        get() = if (MayorPerks.SLAYER_XP_BUFF.active) (_xp * 1.25).toInt() else _xp

    companion object {
        private val tierRegex = Regex("""(?:^|\s)(?<level>[MDCLXVI]{1,7})\s""")

        fun find(string: String): SlayerTier? {
            if ("Atoned Horror" in string) return Five
            if ("Conjoined Brood" in string) return Five

            val i = tierRegex.findGroup(string, "level")?.parseRomanNumeral() ?: return null
            return entries.find { it.int == i }
        }
    }
}