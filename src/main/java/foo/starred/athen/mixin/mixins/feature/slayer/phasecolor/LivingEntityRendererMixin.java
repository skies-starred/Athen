package foo.starred.athen.mixin.mixins.feature.slayer.phasecolor;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import foo.starred.athen.api.storage.ResourceAPI;
import foo.starred.athen.ducks.entity.render.EntityRenderStateDuck;
import foo.starred.athen.modules.impl.slayer.EndermanPhaseColor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.pipeline.ColorTargetState;
//~ if >= 26.3 'blaze3d' -> 'renderpearl.api'
import com.mojang.blaze3d.pipeline.RenderPipeline;
//? if <= 26.1
import com.mojang.blaze3d.vertex.VertexFormat;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private static final RenderType athen$renderType = RenderType.create(
            "starred_enderman",
            RenderSetup.builder(
                            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                                    .withLocation("athen/pipeline/enderman")
                                    .withVertexShader(ResourceAPI.INSTANCE.identify("core/level/entity/enderman"))
                                    .withFragmentShader(ResourceAPI.INSTANCE.identify("core/level/entity/enderman"))
                                    //? if >= 26.2 {
                                    /*.withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
                                    .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                                    //~ if >= 26.3 'blaze3d' -> 'renderpearl.api.pipeline'
                                    .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
                                    .withColorTargetState(ColorTargetState.DEFAULT)
                                    *///?} else {
                                    .withSampler("Sampler0")
                                    .withSampler("Sampler1")
                                    .withSampler("Sampler2")
                                    .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
                                    //?}
                                    .withShaderDefine("ALPHA_CUTOUT", 0.1f)
                                    .build()
                    )
                    .withTexture("Sampler0", Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png"))
                    .sortOnUpload()
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()
    );

    @Inject(method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I", at = @At("HEAD"), cancellable = true)
    private void athen$getModelTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (!(state instanceof EndermanRenderState)) return;
        if (!EndermanPhaseColor.INSTANCE.getEnabled()) return;

        Entity entity = ((EntityRenderStateDuck) state).athen$entity();
        if (entity == null) return;

        Integer color = EndermanPhaseColor.get(entity);
        if (color == null) return;

        cir.setReturnValue(color);
    }

    @Inject(method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true)
    private void athen$getRenderType(LivingEntityRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing, CallbackInfoReturnable<RenderType> cir) {
        if (!(state instanceof EndermanRenderState)) return;
        if (!EndermanPhaseColor.INSTANCE.getEnabled()) return;

        Entity entity = ((EntityRenderStateDuck) state).athen$entity();
        if (entity == null) return;

        Integer color = EndermanPhaseColor.get(entity);
        if (color == null) return;

        if (!isBodyVisible) return;
        if (forceTransparent) return;

        cir.setReturnValue(athen$renderType);
    }
}
