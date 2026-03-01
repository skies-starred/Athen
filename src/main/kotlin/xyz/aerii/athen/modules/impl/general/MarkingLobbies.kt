package xyz.aerii.athen.modules.impl.general

import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.config.Category
import xyz.aerii.athen.events.LocationEvent
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.modules.Module

@Load
object MarkingLobbies: Module(
    "Mark lobbies",
    "Marks lobbies and alerts you if you have already been inside that lobby.",
    Category.GENERAL
) {
    private val onlyCrystalHollows by config.switch("Only in Crystal Hollows")
    private val lobbies = mutableSetOf<String>()

    init {
        on<LocationEvent.ServerChange> {
            if (mode != "crystal_hollows" && onlyCrystalHollows) return@on
            if (!lobbies.add(name)) "You've been in this lobby!".modMessage()
        }
    }
}