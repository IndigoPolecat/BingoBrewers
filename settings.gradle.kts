pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // Multi-version orchestration. https://stonecutter.kikugie.dev/
    id("dev.kikugie.stonecutter") version "0.9.7"
    // Lets a single build script target both obfuscated (1.21.11) and unobfuscated (26.1+) Minecraft.
    id("dev.kikugie.loom-back-compat") version "0.4.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        version("1.21.11", "1.21.11")
        version("26.1.2", "26.1.2")
        version("26.2", "26.2")
        version("26.3", "26.3")
        vcsVersion = "26.3"
    }
}

rootProject.name = "BingoBrewers"
