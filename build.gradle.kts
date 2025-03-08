import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.kyori.blossom") version "1.3.1"
    id("signing")
}



//Constants:

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    launchConfigs.named("client") {
        // Loads OneConfig in dev env. Replace other tweak classes with this, but keep any other attributes!
        arg("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")
    }
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            arg("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")

        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
                programArgs("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")
                property("mixin.debug.export", "true")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.$modid.json")
    }
    // If you don't want mixins, remove these lines
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })

}

sourceSets {
    val dummy by creating
    main {
        dummy.compileClasspath += compileClasspath
        compileClasspath += dummy.output
        output.setResourcesDir(java.classesDirectory)
    }
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}
val modShade: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

// Dependencies:

repositories {
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.nea.moe/releases")
    maven("https://repo.hypixel.net/repository/Hypixel/")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val hypixelModApiVersion = "1.0.1.1" // mod api version

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // If you don't want mixins, remove these lines
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.2")
    implementation("com.google.code.gson:gson:2.9.1")
    shadowImpl("com.esotericsoftware:kryonet:2.22.0-RC1")
    // Basic OneConfig dependencies for legacy versions. See OneConfig example mod for more info
    modCompileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+") // Should not be included in jar
    // include should be replaced with a configuration that includes this in the jar
    shadowImpl("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+") // Should be included in jar
    shadowImpl("moe.nea:libautoupdate:1.3.1") // Should be included in jar
    modImplementation("net.hypixel:mod-api-forge:$hypixelModApiVersion")
    shadowImpl("net.hypixel:mod-api-forge-tweaker:$hypixelModApiVersion")

}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakOrder"] = 0

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "com.github.indigopolecat.bingobrewers.tweaker.BingoBrewersTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("mixinGroup", mixinGroup)

    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
        expand(inputs.properties)
    }

    rename("(.+_at.cfg)", "META-INF/$1")
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl)
    relocate("net.hypixel.modapi.tweaker", "com.github.indigopolecat.bingobrewers.modapi.tweaker")
    doLast {
        configurations.forEach {
            println("Copying jars into mod: ${it.files}")
        }
    }

    // If you want to include other dependencies and shadow them, you can relocate them in here
    //fun relocate(name: String) = relocate(name, "com.github.indigopolecat.$name")
}
tasks.assemble.get().dependsOn(tasks.shadowJar)
tasks.assemble.get().dependsOn(tasks.remapJar)

