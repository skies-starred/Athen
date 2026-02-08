@file:Suppress("unused")

package xyz.aerii.athen.utils.render.pipelines

//? if >= 1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
*///? } else {
import net.minecraft.client.renderer.RenderType
//? }

object StarredRenderTypes {
    val LINES_DEPTH =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_lines_depth",
            RenderSetup.builder(StarredPipelines.lines.vanilla)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_lines_depth",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.lines.vanilla,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val LINES_NO_DEPTH =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_lines_no_depth",
            RenderSetup.builder(StarredPipelines.linesNoDepth.vanilla)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_lines_no_depth",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.linesNoDepth.vanilla,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val TRIANGLE_STRIP =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_triangle_depth",
            RenderSetup.builder(StarredPipelines.triangleStrip.vanilla)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_triangle_depth",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.triangleStrip.vanilla,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }

    val TRIANGLE_STRIP_NO_DEPTH =
        //? if >= 1.21.11 {
        /*RenderType.create(
            "starred_triangle_no_depth",
            RenderSetup.builder(StarredPipelines.triangleStripNoDepth.vanilla)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup()
        )
        *///? } else {
        RenderType.create(
            "starred_triangle_no_depth",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            StarredPipelines.triangleStripNoDepth.vanilla,
            RenderType.CompositeState.builder().createCompositeState(false)
        )
        //? }
}
