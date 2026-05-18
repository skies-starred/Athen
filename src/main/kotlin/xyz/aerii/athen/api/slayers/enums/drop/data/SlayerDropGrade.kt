package xyz.aerii.athen.api.slayers.enums.drop.data

import tech.thatgravyboat.skyblockapi.utils.text.TextColor

enum class SlayerDropGrade(val str: String? = null, val color: Int? = null) {
    GUARANTEED,
    OCCASIONAL("RARE DROP!", TextColor.AQUA),
    RARE("VERY RARE DROP!", TextColor.DARK_BLUE),
    EXTRAORDINARY("VERY RARE DROP!", TextColor.DARK_PURPLE),
    PRAY_RNGESUS("CRAZY RARE DROP!", TextColor.PINK),
    RNGESUS_INCARNATE("INSANE DROP!", TextColor.RED);
}