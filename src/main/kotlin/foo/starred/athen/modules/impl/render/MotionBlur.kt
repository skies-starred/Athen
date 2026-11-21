@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render

import com.mojang.blaze3d.pipeline.*
import com.mojang.blaze3d.systems.RenderSystem
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.modules.Module
import foo.starred.kontext.abstraction.client.extension.client
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.abs

//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.buffers.GpuBuffer
//~ if >= 26.3 'blaze3d.shaders' -> 'renderpearl.api.pipeline'
import com.mojang.blaze3d.shaders.UniformType
//~ if >= 26.3 'blaze3d.systems' -> 'renderpearl.api.commands'
import com.mojang.blaze3d.systems.CommandEncoder
//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.textures.FilterMode
//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.textures.GpuTextureView
//? if >= 26.2 {
/*//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.GpuFormat
//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.pipeline.BindGroupLayout
import java.util.Optional
*///? }
//? if >= 26.3 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.ColorTargetState
*///? }

@LoadableAthenModule
object MotionBlur : Module(
    "Motion blur",
    "Motion blur, I think that's self-explanatory",
    ConfigCategory.RENDER
) {
    private val view by config.group("Head movement motion blur")
    private val `view$enabled` by view.switch("Enabled", true)
    private val `view$f3` by view.switch("Blur in third person")
    private val `view$blur` by view.slider("Blur strength", 1f, 0.1f, 3f, double = true)
    private val `view$max` by view.slider("Max blur", 0.06f, 0.01f, 0.20f, double = true)

    private val movement by config.group("Body movement motion blur")
    private val `movement$enabled` by movement.switch("Enabled", true)
    private val `movement$f3` by movement.switch("Blur in third person")
    private val `movement$blur` by movement.slider("Blur strength", 1f, 0.1f, 3f, double = true)
    private val `movement$max` by movement.slider("Max blur", 0.06f, 0.01f, 0.20f, double = true)

    private val PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("athen", "motion_blur"))
            .withVertexShader(ResourceAPI.minecraft("core/screenquad"))
            .withFragmentShader(ResourceAPI.identify("core/level/blur/motion_blur"))
            //? if >= 26.3 {
            /*.withBindGroupLayout(BindGroupLayout.builder()
                .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                .withUniform("DepthSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                .withUniform("MotionBlurConfig", UniformType.UNIFORM_BUFFER)
                .build())
            *///?} elif 26.2 {
            /*.withBindGroupLayout(BindGroupLayout.builder()
                .withSampler("InSampler")
                .withSampler("DepthSampler")
                .withUniform("MotionBlurConfig", UniformType.UNIFORM_BUFFER)
                .build())
            *///? } else {
            .withSampler("InSampler")
            .withSampler("DepthSampler")
            .withUniform("MotionBlurConfig", UniformType.UNIFORM_BUFFER)
            //?}
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .build()
    )

    private var scratch: RenderTarget? = null
    private var rotation: Matrix4f? = null
    private var position: Vec3? = null
    private var ubo: GpuBuffer? = null

    init {
        observable.observe {
            destroy()
        }
    }

    @JvmStatic
    fun fn() {
        if (!client.options.cameraType.isFirstPerson && !`view$f3` && !`movement$f3`) {
            rotation = null
            position = null
            return
        }

        //~ if >= 26.2 'mainRenderTarget' -> 'gameRenderer.mainRenderTarget()'
        val render = client.mainRenderTarget
        val width = render.width
        val height = render.height

        if (width <= 0) return
        if (height <= 0) return

        //~ if >= 26.2 'gameRenderState' -> 'gameRenderState()'
        val camera = client.gameRenderer.gameRenderState.levelRenderState.cameraRenderState
        val rotation1 = Matrix4f(camera.viewRotationMatrix)
        val position1 = camera.pos
        val rotation0 = rotation?.also { rotation = rotation1 }
        val position0 = position?.also { position = position1 }

        if (rotation0 == null || position0 == null) {
            rotation = rotation1
            position = position1
            return
        }

        val ticks = client.deltaTracker.realtimeDeltaTicks.takeIf { it > 0.001f } ?: 0.33333334f
        val multiplier = (0.33333334f / ticks).coerceIn(1.0f, 5.0f)

        val surface = surface(width, height)
        val buffer = buffer()
        val data = pack(camera, rotation1, position1, rotation0, position0, multiplier)

        val color = render.colorTextureView ?: return
        val depth = render.depthTextureView ?: return
        val target = surface.colorTextureView ?: return

        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.writeToBuffer(buffer.slice(), data)
        encoder.extract(buffer, target, color, depth)

        val source = surface.colorTexture ?: return
        val destination = render.colorTexture ?: return
        encoder.copyTextureToTexture(source, destination, 0, 0, 0, 0, 0, width, height)
    }

    private fun surface(width: Int, height: Int): RenderTarget {
        val current = scratch
        if (current != null && current.width == width && current.height == height) return current

        current?.destroyBuffers()
        //? if >= 26.3 {
        /*val created = TextureTarget("Motion Blur Scratch", width, height, GpuFormat.RGBA8_UNORM, null)
        *///?} elif 26.2 {
        /*val created = TextureTarget("Motion Blur Scratch", width, height, false, GpuFormat.RGBA8_UNORM)
        *///?} else {
        val created = TextureTarget("Motion Blur Scratch", width, height, false)
        //?}
        scratch = created
        return created
    }

    private fun buffer(): GpuBuffer {
        val current = ubo
        if (current != null && !current.isClosed) return current

        val created = RenderSystem.getDevice().createBuffer({ "MotionBlur UBO" }, GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 112L)
        ubo = created
        return created
    }

    private fun pack(camera: CameraRenderState, rotation1: Matrix4f, position1: Vec3, rotation0: Matrix4f, position0: Vec3, multiplier: Float): ByteBuffer {
        val first = client.options.cameraType.isFirstPerson
        val view = `view$enabled` && (first || `view$f3`)
        val move = `movement$enabled` && (first || `movement$f3`)

        val last = Matrix4f(rotation0).mul(Matrix4f(rotation1).transpose())
        val delta = if (move && position1.distanceTo(position0) < 20.0) position0.subtract(position1) else Vec3.ZERO
        val movement = Vector3f((delta.x * multiplier).toFloat(), (delta.y * multiplier).toFloat(), (delta.z * multiplier).toFloat())
        rotation1.transformDirection(movement)

        val inverted = Matrix4f(camera.projectionMatrix).invert()
        val data = ByteBuffer.allocateDirect(112).order(ByteOrder.nativeOrder())

        last.get(data)
        data.position(64)
        data.putFloat(if (view) `view$blur` * multiplier else 0f)
        data.putFloat(if (view) `view$max` else 0f)
        data.putFloat(1f / abs(camera.projectionMatrix.m00()))
        data.putFloat(1f / abs(camera.projectionMatrix.m11()))

        data.putFloat(movement.x)
        data.putFloat(movement.y)
        data.putFloat(movement.z)
        data.putFloat(if (move) `movement$blur` else 0f)

        data.putFloat(1.0f)
        //~ if >= 26.2 'data.putFloat(2.0f * inverted.m23())' -> 'data.putFloat(inverted.m23())'
        data.putFloat(2.0f * inverted.m23())
        //~ if >= 26.2 'data.putFloat(inverted.m33() - inverted.m23())' -> 'data.putFloat(inverted.m33())'
        data.putFloat(inverted.m33() - inverted.m23())
        data.putFloat(if (move) `movement$max` else 0f)
        data.flip()

        return data
    }

    private fun CommandEncoder.extract(buffer: GpuBuffer, target: GpuTextureView, color: GpuTextureView, depth: GpuTextureView) {
        //~ if >= 26.2 'OptionalInt' -> 'Optional'
        val pass = createRenderPass({ "Motion Blur Render Pass" }, target, OptionalInt.empty())
        //~ if >= 26.3 'pass.setPipeline(PIPELINE)' -> 'pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE))'
        pass.setPipeline(PIPELINE)
        RenderSystem.bindDefaultUniforms(pass)
        //~ if >= 26.3 'bindTexture' -> 'setUniform'
        pass.bindTexture("InSampler", color, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
        //~ if >= 26.3 'bindTexture' -> 'setUniform'
        pass.bindTexture("DepthSampler", depth, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
        pass.setUniform("MotionBlurConfig", buffer.slice())
        //~ if >= 26.2 'pass.draw(0, 3)' -> 'pass.draw(3, 1, 0, 0)'
        pass.draw(0, 3)
        pass.close()
    }

    private fun destroy() {
        scratch?.destroyBuffers()
        scratch = null
        ubo?.close()
        ubo = null
        rotation = null
        position = null
    }
}
