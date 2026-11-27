plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    `maven-publish`
}

val catalogs = extensions.getByType<VersionCatalogsExtension>()
val minecraft = stonecutter.current.version
val lib = catalogs.named("libs${minecraft.replace(".", "")}")
val mod = catalogs.named("mod")

version = "${mod("version")}+$minecraft"
group = mod("group")
base.archivesName = mod("id")

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.hypixel.net/repository/Hypixel")
    maven("https://api.modrinth.com/maven")

    maven("https://maven.starred.foo/releases")
    maven("https://maven.starred.foo/snapshots")
}

dependencies {
    minecraft(lib["minecraft"])

    localRuntime(libs.devauth)

    compileOnly(lib["caxton"])
    compileOnly(lib["gnetum"])
    compileOnly(lib["entityculling"])
    compileOnly(lib["exordium"])
    compileOnly(lib["iris"])

    implementation(lib["modmenu"])
    implementation(lib["fabric-api"])
    implementation(libs.fabric.loader)
    implementation(libs.fabric.language.kotlin)
    implementation(libs.hypixel.modapi)

    shadow(libs.kbus)
    shadow(libs.kommand)
    shadow(libs.klassgraph)
    shadow(libs.kontext.find())
    shadow(libs.cascade.find())
    shadow(libs.parallax.find())
    shadow(libs.updater.find())
    shadow(libs.hypixel.modapi.fabric)
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = rootProject.file("src/main/resources/${mod("id")}.accesswidener")

    runConfigs.named("client") {
        generateRunConfig = true
        jvmArguments.addAll("-Ddevauth.enabled=true", "-Ddevauth.account=main", "-XX:+AllowEnhancedClassRedefinition", "-XX:+IgnoreUnrecognizedVMOptions")
    }

    runConfigs.named("server") {
        generateRunConfig = false
    }
}

java {
    withSourcesJar()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution", "-Xcollection-literals", "-Xskip-prerelease-check")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

publishing {
    repositories {
        val a = if (Regex("-b[0-9]*$") in mod("version")) "snapshots" else "releases"
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
            groupId = mod("group")
            artifactId = mod("id")
            version = "${mod("version")}+$minecraft"
            from(components["java"])
        }
    }
}

tasks {
    processResources {
        val r = mapOf("id" to mod("id"), "name" to mod("name"), "version" to mod("version"), "minecraft" to lib("compatibility"), "tweaker" to mod("tweaker"))

        inputs.properties(r)
        filesMatching("fabric.mod.json") { expand(r) }
    }

    register<Copy>("buildAndCollect") {
        description = "Builds and collects mod jars."
        group = "build"
        from(jar, kotlinSourcesJar)
        into(rootProject.layout.buildDirectory.file("libs/${mod("version")}"))
        dependsOn("build")
    }
}

fun DependencyHandlerScope.shadow(dep: Any, config: ExternalModuleDependency.() -> Unit = {}) {
    val d = create((dep as? Provider<*>)?.get() ?: dep) as ExternalModuleDependency
    d.config()
    include(d)
    implementation(d)
}

fun Provider<MinimalExternalModuleDependency>.find(): String {
    return "${get()}+$minecraft"
}

operator fun VersionCatalog.get(name: String): Provider<MinimalExternalModuleDependency> {
    return findLibrary(name).get()
}

operator fun VersionCatalog.invoke(name: String): String {
    return findVersion(name).get().requiredVersion
}
