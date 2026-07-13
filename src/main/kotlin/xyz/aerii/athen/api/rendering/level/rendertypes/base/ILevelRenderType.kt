package xyz.aerii.athen.api.rendering.level.rendertypes.base

import net.minecraft.client.renderer.rendertype.RenderType

interface ILevelRenderType {
    val depth: RenderType
    val depthless: RenderType
}