package xyz.aerii.athen.handlers

import net.minecraft.resources.Identifier
import xyz.aerii.athen.Athen
import xyz.aerii.library.handlers.Resourceful

object Resourceful : Resourceful(Athen.modId) {
    fun minecraft(path: String): Identifier {
        return Identifier.withDefaultNamespace(path)
    }
}