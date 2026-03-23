package xyz.aerii.athen.modules.impl.general.keybinds

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import xyz.aerii.athen.api.dungeon.DungeonAPI
import xyz.aerii.athen.api.dungeon.enums.DungeonClass
import xyz.aerii.athen.api.location.LocationAPI
import xyz.aerii.athen.api.location.SkyBlockIsland
import xyz.aerii.athen.handlers.Smoothie.client

data class KeybindCondition(
    var workIn: KeybindWorkIn = KeybindWorkIn.OUTSIDE_GUI,
    val islands: MutableSet<SkyBlockIsland> = mutableSetOf(),
    val floors: MutableSet<DungeonFloor> = mutableSetOf(),
    val classes: MutableSet<DungeonClass> = mutableSetOf(),
    val phases: MutableSet<Int> = mutableSetOf()
) {
    fun eval(): Boolean {
        val screen = client.screen
        val ig = screen is AbstractContainerScreen<*>
        val og = screen == null

        if (workIn == KeybindWorkIn.GUI && !ig) return false
        if (workIn == KeybindWorkIn.OUTSIDE_GUI && !og) return false

        if (classes.isNotEmpty() && DungeonAPI.dungeonClass !in classes) return false
        if (phases.isNotEmpty() && (DungeonAPI.floor.value?.floorNumber != 7 || DungeonAPI.F7Phase.value !in phases)) return false
        if (floors.isNotEmpty() && DungeonAPI.floor.value !in floors) return false
        if (islands.isNotEmpty() && LocationAPI.island.value !in islands) return false

        return true
    }

    fun copy(): KeybindCondition =
        KeybindCondition(workIn, islands.toMutableSet(), floors.toMutableSet(), classes.toMutableSet(), phases.toMutableSet())

    companion object {
        val CODEC: Codec<KeybindCondition> = RecordCodecBuilder.create { inst ->
            inst.group(
                Codec.STRING.optionalFieldOf("workIn", "OUTSIDE_GUI").forGetter { it.workIn.name },
                Codec.STRING.listOf().optionalFieldOf("islands", emptyList()).forGetter { it.islands.map { i -> i.id } },
                Codec.STRING.listOf().optionalFieldOf("floors", emptyList()).forGetter { it.floors.map { f -> f.name } },
                Codec.STRING.listOf().optionalFieldOf("classes", emptyList()).forGetter { it.classes.map { c -> c.name } },
                Codec.INT.listOf().optionalFieldOf("phases", emptyList()).forGetter { it.phases.toList() }
            ).apply(inst) { workIn, islands, floors, classes, phases ->
                KeybindCondition(
                    KeybindWorkIn.from(workIn),
                    islands.mapNotNull { SkyBlockIsland.getByKey(it) }.toMutableSet(),
                    floors.mapNotNull { DungeonFloor.getByName(it) }.toMutableSet(),
                    classes.mapNotNull { DungeonClass.get(it) }.toMutableSet(),
                    phases.toMutableSet()
                )
            }
        }
    }
}
