import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.20"
    id("com.github.johnrengelman.shadow") version("6.0.0")
}

group = "me.boss"
version = "1.0"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    val exposedVer = "0.24.1"

    // kord
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.4.0-20210519.172029-219")

    // logging
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.3")

    // database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVer")
    implementation("mysql:mysql-connector-java:8.0.22")

    // kotlin
    implementation(kotlin("reflect"))
    implementation(kotlin("scripting-jsr223"))

    // local dependency of SpaceSpeak API. Cannot be published open source (yet).
    implementation(files("libs/SpaceSpeakAPI-jvm-1.0.0.jar"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-Xinline-classes",
            "-Xopt-in=dev.kord.common.annotation.KordPreview"
        )
    }
}

val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    manifest {
        attributes["Main-Class"] = "me.qbosst.bossbot.Launcher"
    }
}