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

package xyz.aerii.athen.api.slayers.enums.type.impl

import xyz.aerii.athen.api.slayers.enums.type.base.ISlayerType

enum class SlayerMini(override val display: String, val special: Boolean = false, override val names: Set<String> = setOf(display)) : ISlayerType {
    REVENANT_SYCOPHANT("Revenant Sycophant"),
    REVENANT_CHAMPION("Revenant Champion"),
    DEFORMED_REVENANT("Deformed Revenant", true),
    ATONED_CHAMPION("Atoned Champion"),
    ATONED_REVENANT("Atoned Revenant", true),

    TARANTULA_VERMIN("Tarantula Vermin"),
    TARANTULA_BEAST("Tarantula Beast"),
    MUTANT_TARANTULA("Mutant Tarantula", true),
    PRIMORDIAL_JOCKEY("Primordial Jockey"),
    PRIMORDIAL_VISCOUNT("Primordial Viscount", true),

    PACK_ENFORCER("Pack Enforcer"),
    SVEN_FOLLOWER("Sven Follower"),
    SVEN_ALPHA("Sven Alpha", true),

    VOIDLING_DEVOTEE("Voidling Devotee"),
    VOIDLING_RADICAL("Voidling Radical"),
    VOIDCRAZED_MANIAC("Voidcrazed Maniac", true),

    FLARE_DEMON("Flare Demon"),
    KINDLEHEART_DEMON("Kindleheart Demon"),
    BURNINGSOUL_DEMON("Burningsoul Demon", true),
}