package xyz.aerii.athen.api.slayers.enums.type.impl

import xyz.aerii.athen.api.slayers.enums.type.base.ISlayerType

enum class SlayerDemon(override val display: String, override val names: Set<String> = setOf(display)) : ISlayerType {
    Quazii("ⓆⓊⒶⓏⒾⒾ"),
    Typhoeus("ⓉⓎⓅⒽⓄⒺⓊⓈ")
}