/*
 * Contains code and is inspired from SkyOcean's build scripts which are licensed under the MIT license.
 * You may find their full license at: https://github.com/meowdding/SkyOcean/blob/main/LICENSE.md
 *
 * Modifications to this file are licensed under Starred's BSD-3 clause license.
 * You may read my license at: https://github.com/skies-starred/Athen/blob/master/LICENSE
 *
 * Their MIT license:
 * Copyright (c) Meowdding and SkyOcean contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.kikugie.dev/snapshots")
}

dependencies {
    implementation(libs.plugins.kotlin.jvm.provider)
    implementation(libs.plugins.ksp.provider)
    implementation(libs.plugins.fletchingTable.provider)
    implementation(libs.plugins.loom.asProvider().provider)
    implementation(libs.plugins.loom.obf.provider)
    implementation("dev.kikugie.stonecutter:dev.kikugie.stonecutter.gradle.plugin:0.9")
}

val Provider<PluginDependency>.provider: Provider<String>
    get() = map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
    }