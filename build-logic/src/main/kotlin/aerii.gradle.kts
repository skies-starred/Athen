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

@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.fletching-table.fabric")
    `maven-publish`
}

val stonecutter = project.extensions.getByName("stonecutter") as dev.kikugie.stonecutter.build.StonecutterBuildExtension
val new = stonecutter.current.parsed >= "26.1"
val ver = stonecutter.current.version
val java = if (new) 25 else 21

val loom = extensions.getByName<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom")
val impl = if (new) "implementation" else "modImplementation"
val comp = if (new) "compileOnly" else "modCompileOnly"
val rune = if (new) "runtimeOnly" else "modRuntimeOnly"

val modId = project.property("mod.id").toString()
val modName = project.property("mod.name").toString()
val modVer = project.property("mod.version").toString()

val modAW = "$modId${if (new) "" else ".obf"}.accesswidener"

version = "$modVer+$ver"
base.archivesName = modId

repositories {
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://repo.hypixel.net/repository/Hypixel", "net.hypixel")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
    strictMaven("https://maven.parchmentmc.org/", "org.parchmentmc")
    strictMaven("https://maven.teamresourceful.com/repository/maven-public/", "tech.thatgravyboat", "com.terraformersmc", "earth.terrarium", "com.teamresourceful", "me.owdding")
    strictMaven("https://maven.deftu.dev/snapshots", "dev.deftu")
    strictMaven("https://maven.deftu.dev/releases", "dev.deftu")
    strictMaven("https://repo.nea.moe/releases", "moe.nea")
    strictMaven("https://jitpack.io", "com.github.skies-starred")
}

fletchingTable {
    mixins.create("main", Action {
        mixin("default", "$modId.mixins.json") {
            env("CLIENT")
        }
    })
}

dependencies {
    "minecraft"("com.mojang:minecraft:$ver")

    rune("devauth".global)
    comp("entityculling".versioned)
    comp("exordium".versioned)
    comp("iris".versioned)

    impl("modmenu".versioned)
    impl("fabric-api".versioned)
    impl("fabric-loader".global)
    impl("fabric-language-kotlin".global)
    impl("hypixel-modapi".global)
    impl("hypixel-modapi-fabric".global)

    shadow("classgraph".global)
    shadow("autoupdate".global)
    shadow("library".versioned)
    shadow("lwjgl-nanovg".versioned)
    for (p in listOf("windows", "linux", "macos", "macos-arm64")) shadow("lwjgl-nanovg".versioned.get().toString() + ":natives-$p")

    shadow("skyblock-api".global) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$ver" + if (new) "" else "-remapped") }
    }

    if (new) return@dependencies
    "mappings"(loom.layered {
        officialMojangMappings()
        parchment("parchment".versioned)
    })
}

loom.apply {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = rootProject.file("src/main/resources/$modAW")

    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main",
                "-XX:+AllowEnhancedClassRedefinition"
            )
        )
    }

    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(java)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java)
    withSourcesJar()
}

kotlin {
    jvmToolchain(java)

    compilerOptions {
        jvmTarget.set(JvmTarget.valueOf("JVM_$java"))

        freeCompilerArgs.addAll("-XXLanguage:+ExplicitBackingFields", "-Xcontext-parameters", "-Xcontext-sensitive-resolution")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "xyz.aerii"
            artifactId = "Athen-$ver"
            version = modVer
            from(components["java"])
        }
    }
}

tasks {
    processResources {
        val r = mapOf(
            "id" to modId,
            "name" to modName,
            "version" to modVer,
            "minecraft" to project.property("mod.mc_dep"),
            "accessWidener" to modAW,
            "kotlin" to "fabric-language-kotlin".global.get().version,
            "hm_api" to "hypixel-modapi-fabric".global.get().version,
            "sb_api" to "skyblock-api".global.get().version
        )

        inputs.properties(r)
        filesMatching("fabric.mod.json") { expand(r) }
    }

    register<Copy>("buildAndCollect") {
        description = "Builds and collects mod jars."
        group = "build"
        from(tasks.named(if (new) "jar" else "remapJar").map { (it as AbstractArchiveTask).archiveFile }, tasks.named(if (new) "sourcesJar" else "remapSourcesJar").map { (it as AbstractArchiveTask).archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

val String.global: Provider<MinimalExternalModuleDependency>
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary(this).get()

val String.versioned: Provider<MinimalExternalModuleDependency>
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${ver.replace(".", "_")}").get()

fun DependencyHandlerScope.shadow(dep: Any, config: ExternalModuleDependency.() -> Unit = {}) {
    val d = create((dep as? Provider<*>)?.get() ?: dep) as ExternalModuleDependency
    d.config()
    "include"(d)
    impl(d)
}