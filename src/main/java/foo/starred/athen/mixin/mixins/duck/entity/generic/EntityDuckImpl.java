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

package foo.starred.athen.mixin.mixins.duck.entity.generic;

import foo.starred.athen.api.skyblock.EntityAPI;
import foo.starred.athen.ducks.entity.generic.EntityDuck;
import foo.starred.athen.modules.impl.slayer.carry.data.SlayerCarryPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityDuckImpl implements EntityDuck {
    @Unique
    private List<WeakReference<Entity>> athen$attachments;

    @Unique
    private Entity athen$attachedTo;

    @Unique
    private SlayerCarryPlayer athen$feat$carry;

    @Unique
    private int athen$ticks = 20;

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

    @Override
    public SlayerCarryPlayer athen$feat$carry$boss() {
        return athen$feat$carry;
    }

    @Override
    public void athen$feat$carry$boss(SlayerCarryPlayer a) {
        athen$feat$carry = a;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void athen$tick(CallbackInfo ci) {
        Entity entity = athen$self();

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

        EntityAPI.INSTANCE.attach(entity);
    }

    @Inject(method = "onRemoval", at = @At("RETURN"))
    private void athen$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!(athen$attachedTo instanceof EntityDuck acc)) return;

        acc.athen$attachments().removeIf(ref -> ref.get() == athen$self());
    }

    @Unique
    private Entity athen$self() {
        return (Entity) (Object) this;
    }
}
