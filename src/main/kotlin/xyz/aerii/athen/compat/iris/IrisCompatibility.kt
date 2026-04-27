package xyz.aerii.athen.compat.iris

import net.fabricmc.loader.api.FabricLoader
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram
import xyz.aerii.athen.Athen
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.utils.render.pipelines.StarredPipelines

@Load
object IrisCompatibility {
    init {
        if (FabricLoader.getInstance().isModLoaded("iris")) fn()
    }

    fun fn() {
        Athen.LOGGER.info("Attempting to ensure Iris compatibility for rendering...")

        try {
            IrisApi.getInstance().assignPipeline(StarredPipelines.LINES, IrisProgram.LINES)
            IrisApi.getInstance().assignPipeline(StarredPipelines.LINES_DEPTHLESS, IrisProgram.LINES)
            IrisApi.getInstance().assignPipeline(StarredPipelines.DEBUG_FILLED, IrisProgram.BASIC)
            IrisApi.getInstance().assignPipeline(StarredPipelines.DEBUG_FILLED_DEPTHLESS, IrisProgram.BASIC)
            IrisApi.getInstance().assignPipeline(StarredPipelines.TRIANGLE_FAN, IrisProgram.BASIC)
            IrisApi.getInstance().assignPipeline(StarredPipelines.TRIANGLE_FAN_DEPTHLESS, IrisProgram.BASIC)

            Athen.LOGGER.info("Registered pipelines to Iris API!")
        } catch (e: Exception) {
            Athen.LOGGER.error("Failed to try to ensure Iris compatibility, issues may occur!", e)
        }
    }
}