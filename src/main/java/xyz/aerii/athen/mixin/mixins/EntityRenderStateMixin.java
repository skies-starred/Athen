package xyz.aerii.athen.mixin.mixins;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.aerii.athen.accessors.EntityRenderStateAccessor;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateAccessor {
    @Unique
    private Entity athen$entity;

    @Override
    public Entity athen$getEntity() {
        return this.athen$entity;
    }

    @Override
    public void athen$setEntity(Entity entity) {
        this.athen$entity = entity;
    }
}