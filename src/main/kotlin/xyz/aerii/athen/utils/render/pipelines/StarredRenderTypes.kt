@file:Suppress("unused")

package xyz.aerii.athen.utils.render.pipelines

//? if >= 1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
*///? } else {
import net.minecraft.client.renderer.RenderType
//? }

object StarredRenderTypes {
    val LINES: RenderType =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_lines",
            RenderSetup.builder(StarredPipelines.LINES)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_lines",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.LINES,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val LINES_DEPTHLESS: RenderType =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_lines_depthless",
            RenderSetup.builder(StarredPipelines.LINES_DEPTHLESS)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_lines_depthless",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.LINES_DEPTHLESS,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val DEBUG_FILLED: RenderType =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_debug_filled",
            RenderSetup.builder(StarredPipelines.DEBUG_FILLED)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_debug_filled",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.DEBUG_FILLED,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val DEBUG_FILLED_DEPTHLESS: RenderType =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_debug_filled_depthless",
            RenderSetup.builder(StarredPipelines.DEBUG_FILLED_DEPTHLESS)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_debug_filled_depthless",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.DEBUG_FILLED_DEPTHLESS,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val TRIANGLE_FAN: RenderType =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_triangle_fan",
            RenderSetup.builder(StarredPipelines.TRIANGLE_FAN)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_triangle_fan",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.TRIANGLE_FAN,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val TRIANGLE_FAN_DEPTHLESS: RenderType =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_triangle_fan_depthless",
            RenderSetup.builder(StarredPipelines.TRIANGLE_FAN_DEPTHLESS)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_triangle_fan_depthless",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.TRIANGLE_FAN_DEPTHLESS,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }
}
