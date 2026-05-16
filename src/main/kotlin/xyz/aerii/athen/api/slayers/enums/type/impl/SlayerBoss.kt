package xyz.aerii.athen.api.slayers.enums.type.impl

import xyz.aerii.athen.api.slayers.enums.type.base.ISlayerType

enum class SlayerBoss(override val display: String, val short: String, override val names: Set<String> = setOf(display)) : ISlayerType {
    Revenant("Revenant Horror", "Rev", setOf("Revenant Horror", "Atoned Horror")),
    Tarantula("Tarantula Broodfather", "Tara", setOf("Tarantula Broodfather", "Conjoined Brood")),
    Sven("Sven Packmaster", "Sven"),
    Voidgloom("Voidgloom Seraph", "Void"),
    Inferno("Inferno Demonlord", "Blaze"),
    Vampire("Bloodfiend", "Vamp");

    companion object {
        val NAMES: Set<String> = entries.map { it.display }.toSet()
    }
}