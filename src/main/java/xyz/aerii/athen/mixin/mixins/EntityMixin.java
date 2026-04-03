/*
 * Very much inspired by how SkyblockAPI handles the entity attachments. This version contains a few changes,
 * and fixes that aim to improve over the idea of attaching nametags to entities. However, this version
 * contains a few changes that may not be stable/tested enough to be in the main library.
 *
 * Original work by [SkyblockAPI](https://github.com/SkyblockAPI/SkyblockAPI) and contributors (MIT License).
 * The MIT License (MIT)
 *
 * Copyright (c) 2025
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Modifications:
 *   Copyright (c) 2025 skies-starred
 *   Licensed under the BSD 3-Clause License.
 *
 * The original MIT license applies to the portions derived from SkyblockAPI.
 */

package xyz.aerii.athen.mixin.mixins;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.aerii.athen.accessors.EntityAccessor;
import xyz.aerii.athen.api.skyblock.EntityAPI;
import xyz.aerii.athen.events.EntityEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAccessor {
    @Shadow
    @Final
    private static EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME;

    @Shadow
    @Nullable
    public abstract Component getCustomName();

    @Unique
    private List<WeakReference<Entity>> athen$attachments;

    @Unique
    private Entity athen$attachedTo;

    @Unique
    private int athen$ticks = 20;

    @Unique
    private Entity entity() {
        return (Entity) (Object) this;
    }

    @Override
    public @NotNull List<WeakReference<Entity>> athen$attachments() {
        if (athen$attachments == null) athen$attachments = new ArrayList<>();
        return athen$attachments;
    }

    @Override
    public @Nullable Entity athen$attach() {
        return athen$attachedTo;
    }

    @Override
    public void athen$attach(@NotNull Entity entity) {
        this.athen$attachedTo = entity;
    }

    @Inject(method = "setCustomName", at = @At("RETURN"))
    private void athen$setCustomName(Component name, CallbackInfo ci) {
        Entity entity = entity();

        if (name == null) return;
        if (!(entity instanceof ArmorStand)) return;

        new EntityEvent.Update.Named(name, entity).post();
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void athen$onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor, CallbackInfo ci) {
        Entity entity = entity();

        if (!(entity instanceof ArmorStand)) return;
        if (dataAccessor != DATA_CUSTOM_NAME) return;

        Component component = getCustomName();
        if (component == null) return;

        new EntityEvent.Update.Named(component, entity).post();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void athen$tick(CallbackInfo ci) {
        Entity entity = entity();

        if (athen$ticks-- > 0) return;
        athen$ticks = 20;

        if (athen$attachments != null) {
            athen$attachments.removeIf(ref -> {
                final Entity a = ref.get();
                return a == null || !a.isAlive();
            });
        }

        if (!(entity instanceof ArmorStand stand)) return;
        if (!stand.hasCustomName()) return;

        EntityAPI.attach(entity);
    }

    @Inject(method = "onRemoval", at = @At("RETURN"))
    private void athen$remove(CallbackInfo ci) {
        if (!(athen$attachedTo instanceof EntityAccessor acc)) return;

        acc.athen$attachments().removeIf(ref -> ref.get() == entity());
    }
}