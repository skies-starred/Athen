/*
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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.GamePacketTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import xyz.aerii.athen.events.PacketEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(value = BundlerInfo.class, priority = Integer.MIN_VALUE)
public interface BundlerInfoMixin {

    @WrapMethod(method = "createForPacket")
    private static <T extends PacketListener, P extends BundlePacket<? super T>> BundlerInfo athen$createForPacket(
        final PacketType<@NotNull P> type,
        final Function<Iterable<Packet<? super T>>, P> bundler,
        final BundleDelimiterPacket<? super T> delimiter,
        final Operation<BundlerInfo> original
    ) {
        if (type != GamePacketTypes.CLIENTBOUND_BUNDLE) return original.call(type, bundler, delimiter);

        return original.call(type, (Function<Iterable<Packet<? super T>>, P>) (iterable) -> {
            List<Packet<? super T>> packets = new ArrayList<>();

            for (var packet : iterable) {
                if (!new PacketEvent.Receive(packet).post()) packets.add(packet);
            }

            return bundler.apply(packets);
        }, delimiter);
    }
}