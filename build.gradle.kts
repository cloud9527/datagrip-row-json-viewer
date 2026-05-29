import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "com.example"
version = "2.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    intellijPlatform {
        local("D:/program/JetBrains/DataGrip 2025.1")
        bundledPlugin("com.intellij.database")
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("241")
            untilBuild.set("263.*")
        }
    }

    buildSearchableOptions.set(false)

    publishing {
        token.set(providers.environmentVariable("marketplaceToken")
            .orElse(providers.gradleProperty("marketplaceToken")))
        channels.add("default")
    }
}
