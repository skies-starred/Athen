package foo.starred.athen.api.slayers.enums.drop.data

import foo.starred.athen.api.messaging.enums.MessageColors

enum class SlayerDropGrade(val str: String? = null, val color: Int? = null) {
    GUARANTEED,
    OCCASIONAL("RARE DROP!", MessageColors.AQUA.color),
    RARE("VERY RARE DROP!", MessageColors.DARK_BLUE.color),
    EXTRAORDINARY("VERY RARE DROP!", MessageColors.DARK_PURPLE.color),
    PRAY_RNGESUS("CRAZY RARE DROP!", MessageColors.PINK.color),
    RNGESUS_INCARNATE("INSANE DROP!", MessageColors.RED.color);
}
