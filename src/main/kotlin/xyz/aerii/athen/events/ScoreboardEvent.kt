package xyz.aerii.athen.events

import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.aerii.athen.events.core.Event

sealed class ScoreboardEvent {
    data class UpdateTitle(
        val old: String?,
        val new: String
    ) : Event()

    data class Update(
        val old: List<String>,
        val new: List<String>,
        val components: List<Component>,
    ) : Event() {
        val added: List<String> = new - old.toSet()
        val removed: List<String> = old - new.toSet()

        private val addedSet: Set<String> = added.toSet()
        private val removedSet: Set<String> = removed.toSet()

        val addedComponents: List<Component> = components.filter { it.stripped in addedSet }
        val removedComponents: List<Component> = components.filter { it.stripped in removedSet }
    }
}
