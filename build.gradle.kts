@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    alias(libs.plugins.ksp)
    alias(libs.plugins.fletchingTable)
    `maven-publish`
}

val ver = stonecutter.current.version
val modId = project.property("mod.id").toString()
val modName = project.property("mod.name").toString()
val modVer = project.property("mod.version").toString()

version = "$modVer+$ver"
base.archivesName = modId

repositories {
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://repo.hypixel.net/repository/Hypixel", "net.hypixel")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
    strictMaven("https://maven.parchmentmc.org/", "org.parchmentmc")
    strictMaven("https://maven.teamresourceful.com/repository/maven-public/", "tech.thatgravyboat", "com.terraformersmc", "earth.terrarium", "com.teamresourceful", "me.owdding")
    strictMaven("https://repo.nea.moe/releases", "moe.nea")

    maven("https://maven.starred.foo/releases")
    maven("https://maven.starred.foo/snapshots")
}

fletchingTable {
    mixins.create("main", Action {
        mixin("default", "$modId.mixins.json") {
            env("CLIENT")
        }
    })
}

dependencies {
    minecraft("com.mojang:minecraft:$ver")

    localRuntime("devauth".global)

    compileOnly("caxton".versioned)
    compileOnly("entityculling".versioned)
    compileOnly("exordium".versioned)
    compileOnly("iris".versioned)

    implementation("modmenu".versioned)
    implementation("fabric-api".versioned)
    implementation("fabric-loader".global)
    implementation("fabric-language-kotlin".global)
    implementation("hypixel-modapi".global)
    implementation("hypixel-modapi-fabric".global)

    shadow("classgraph".global)
    shadow("autoupdate".global)
    shadow("kommand".global)
    shadow("snowbird".versioned)
    shadow("cascade".versioned)

    shadow("skyblock-api".global) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$ver") }
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = rootProject.file("src/main/resources/$modId.accesswidener")

    runConfigs.named("client") {
        generateRunConfig = true
        jvmArguments.addAll(
            "-Ddevauth.enabled=true",
            "-Ddevauth.account=main",
            "-XX:+AllowEnhancedClassRedefinition",
            "-XX:+IgnoreUnrecognizedVMOptions",
        )
    }

    runConfigs.named("server") {
        generateRunConfig = false
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.valueOf("JVM_25"))

        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution", "-Xcollection-literals", "-Xskip-prerelease-check")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

publishing {
    repositories {
        val a = if (Regex("-b[0-9]*$") in modVer) "snapshots" else "releases"
        maven("https://maven.starred.foo/$a") {
            name = "starred"
            credentials {
                username = (project.findProperty("MAVEN_USER") as? String) ?: System.getenv("MAVEN_USER") ?: ""
                password = (project.findProperty("MAVEN_PASS") as? String) ?: System.getenv("MAVEN_PASS") ?: ""
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "foo.starred"
            artifactId = modId
            version = "$modVer+$ver"
            from(components["java"])
        }
    }
}

tasks {
    processResources {
        val r = mapOf("id" to modId, "name" to modName, "version" to modVer, "minecraft" to project.property("mod.mc_dep"), "accessWidener" to "$modId.accesswidener")

        inputs.properties(r)
        filesMatching("fabric.mod.json") { expand(r) }
    }

    register<Copy>("buildAndCollect") {
        description = "Builds and collects mod jars."
        group = "build"
        from(jar, kotlinSourcesJar)
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
    include(d)
    implementation(d)
}
