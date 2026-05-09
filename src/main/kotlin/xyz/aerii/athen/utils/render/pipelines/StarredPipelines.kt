package xyz.aerii.athen.utils.render.pipelines

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import xyz.aerii.athen.Athen

//? if >= 26.1 {
/*import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.platform.CompareOp
import java.util.Optional
*///? } else {
import com.mojang.blaze3d.platform.DepthTestFunction
//? }

object StarredPipelines {
    private fun string(s: String) = "${Athen.modId}/$s"
    //~ if >= 26.1 'DepthTestFunction.LEQUAL_DEPTH_TEST' -> 'DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)'
    private val depth = DepthTestFunction.LEQUAL_DEPTH_TEST

    val LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            //~ if >= 26.1 'withDepthTestFunction' -> 'withDepthStencilState'
            .withDepthTestFunction(depth)
            .withLocation(string("pipeline/depth/line"))
            .build()
    )

    val LINES_DEPTHLESS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            //~ if >= 26.1 'withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)' -> 'withDepthStencilState(Optional.empty())'
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withLocation(string("pipeline/depthless/line"))
            .build()
    )

    val DEBUG_FILLED: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            //~ if >= 26.1 'withDepthTestFunction' -> 'withDepthStencilState'
            .withDepthTestFunction(depth)
            .withLocation(string("pipeline/depth/debug_filled"))
            .build()
    )

    val DEBUG_FILLED_DEPTHLESS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            //~ if >= 26.1 'withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)' -> 'withDepthStencilState(Optional.empty())'
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withLocation(string("pipeline/depthless/debug_filled"))
            .build()
    )

    val TRIANGLE_FAN: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
            //~ if >= 26.1 'withDepthTestFunction' -> 'withDepthStencilState'
            .withDepthTestFunction(depth)
            .withLocation(string("pipeline/depth/triangle_fan"))
            .withCull(false)
            .build()
    )

    val TRIANGLE_FAN_DEPTHLESS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
            //~ if >= 26.1 'withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)' -> 'withDepthStencilState(Optional.empty())'
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withLocation(string("pipeline/depthless/triangle_fan"))
            .withCull(false)
            .build()
    )
}