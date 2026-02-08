package xyz.aerii.athen.utils.render.pipelines

import dev.deftu.omnicore.api.client.render.DrawMode
import dev.deftu.omnicore.api.client.render.pipeline.IrisShaderType
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipeline
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipelineSnippets
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipelines
import net.minecraft.resources.ResourceLocation
import xyz.aerii.athen.Athen

object StarredPipelines {
    val lines = OmniRenderPipelines.builderWithDefaultShader(
        location = ResourceLocation.fromNamespaceAndPath(Athen.modId, "pipeline/lines"),
        snippets = arrayOf(OmniRenderPipelineSnippets.LINES)
    )
        .setCulling(false)
        .setIrisType(IrisShaderType.LINES)
        .build()

    val linesNoDepth = OmniRenderPipelines.builderWithDefaultShader(
        location = ResourceLocation.fromNamespaceAndPath(Athen.modId, "pipeline/lines_nodepth"),
        snippets = arrayOf(OmniRenderPipelineSnippets.LINES)
    )
        .setCulling(false)
        .setDepthTest(OmniRenderPipeline.DepthTest.DISABLED)
        .setIrisType(IrisShaderType.LINES)
        .build()

    val triangleStrip = OmniRenderPipelines.builderWithDefaultShader(
        location = ResourceLocation.fromNamespaceAndPath(Athen.modId, "pipeline/filled"),
        snippets = arrayOf(
            OmniRenderPipelineSnippets.builder(OmniRenderPipelineSnippets.POSITION_COLOR)
                .setDrawMode(DrawMode.TRIANGLE_STRIP)
                .build()
        )
    )
        .setIrisType(IrisShaderType.BASIC)
        .build()

    val triangleStripNoDepth = OmniRenderPipelines.builderWithDefaultShader(
        location = ResourceLocation.fromNamespaceAndPath(Athen.modId, "pipeline/filled_nodepth"),
        snippets = arrayOf(
            OmniRenderPipelineSnippets.builder(OmniRenderPipelineSnippets.POSITION_COLOR)
                .setDrawMode(DrawMode.TRIANGLE_STRIP)
                .build()
        )
    )
        .setDepthTest(OmniRenderPipeline.DepthTest.DISABLED)
        .setIrisType(IrisShaderType.BASIC)
        .build()

    val positionColorTriangles = OmniRenderPipelines.builderWithDefaultShader(
        location = ResourceLocation.fromNamespaceAndPath(Athen.modId, "pipeline/triangles"),
        snippets = arrayOf(OmniRenderPipelineSnippets.POSITION_COLOR_TRIANGLES)
    )
        .setIrisType(IrisShaderType.BASIC)
        .build()

    val positionColorTrianglesNoDepth = OmniRenderPipelines.builderWithDefaultShader(
        location = ResourceLocation.fromNamespaceAndPath(Athen.modId, "pipeline/triangles_nodepth"),
        snippets = arrayOf(OmniRenderPipelineSnippets.POSITION_COLOR_TRIANGLES)
    )
        .setDepthTest(OmniRenderPipeline.DepthTest.DISABLED)
        .setIrisType(IrisShaderType.BASIC)
        .build()
}