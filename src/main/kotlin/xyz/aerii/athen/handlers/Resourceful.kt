package xyz.aerii.athen.handlers

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Priority
import xyz.aerii.athen.handlers.Smoothie.client

@Priority
object Resourceful {
    fun resource(path: String): Resource {
        return client.resourceManager.getResource(identify(path)).get()
    }

    fun identify(path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(Athen.modId, path)
}