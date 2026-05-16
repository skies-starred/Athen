package xyz.aerii.athen.api.slayers.enums.drop.base

import net.minecraft.world.item.Item

interface ISlayerDropParser<T> where T : Enum<T>, T : ISlayerDrop {
    val set0: Set<T>
    val set1: Set<T>
    val set2: Set<T>

    fun find(item: Item, id: String, texture: String? = null, enchant: String? = null): T? {
        if (texture != null) {
            for (s in set1) {
                if (s.str2 != texture) continue

                return s
            }

            return null
        }

        if (enchant != null) {
            for (s in set2) {
                if (s.str2 != enchant) continue

                return s
            }

            return null
        }

        for (s in set0) {
            if (s.item != item) continue
            if (s.name != id) continue

            return s
        }

        return null
    }
}