@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    alias(libs.plugins.ksp)
    alias(libs.plugins.fletchingTable)
    `maven-publish`
}

val platforms = listOf("windows", "linux", "macos", "macos-arm64")
val mc = stonecutter.current.version
version = "${property("mod.version")}+$mc"
base.archivesName = property("mod.id").toString()

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
}

fletchingTable {
    mixins.create("main", Action {
        mixin("default", "${project.property("mod.id")}.mixins.json") {
            env("CLIENT")
        }
    })
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("parchment".mc(mc))
    })

    modRuntimeOnly(libs.devauth)
    modCompileOnly("entityculling".mc(mc))

    modImplementation("modmenu".mc(mc))
    modImplementation("fabric-api".mc(mc))
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.language.kotlin)
    modImplementation(libs.hypixel.modapi)
    modImplementation(libs.hypixel.modapi.fabric)

    shadow(libs.classgraph)
    shadow(libs.autoupdate)
    shadow("omnicore".mc(mc))
    shadow(libs.lwjgl.nanovg)
    for (p in platforms) shadow("${libs.lwjgl.nanovg.get()}:natives-$p")

    shadow(libs.skyblock.api) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$mc-remapped") }
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = rootProject.file("src/main/resources/${project.property("mod.id")}.accesswidener")

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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "xyz.aerii"
            artifactId = "Athen-$mc"
            version = project.property("mod.version").toString()
            from(components["java"])
        }
    }
}

tasks {
    processResources {
        val r = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "kotlin" to libs.versions.fabric.language.kotlin.get(),
            "hm_api" to libs.versions.hypixel.modapi.fabric.get(),
            "sb_api" to libs.versions.skyblock.api.get()
        )

        inputs.properties(r)
        filesMatching("fabric.mod.json") { expand(r) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()

fun DependencyHandler.shadow(dep: Any, config: ExternalModuleDependency.() -> Unit = {}) {
    val d = create((dep as? Provider<*>)?.get() ?: dep) as ExternalModuleDependency
    d.config()
    include(d)
    modImplementation(d)
}