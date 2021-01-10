import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
    kotlin("plugin.serialization") version "1.4.20"
    id("com.github.johnrengelman.shadow") version("6.0.0")
}

group = "me.boss"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    val exposedVer = "0.24.1"
    val ktorVersion = "1.4.1"

    // jda
    implementation("net.dv8tion:JDA:4.2.0_227")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.github.qbosst:JDA-Extensions:4a2cce5080")

    // music
    implementation("com.sedmelluq:lavaplayer:1.3.66") {
        // remove this as the same library but different version is already included in JDA
        exclude("com.fasterxml.jackson.core")
    }
    implementation("com.sedmelluq:jda-nas:1.1.0")
    implementation("com.adamratzman:spotify-api-kotlin-core:3.4.03")

    // database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVer")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVer")
    implementation("mysql:mysql-connector-java:8.0.22")

    // misc
    implementation("org.reflections:reflections:0.9.12")
    implementation("org.codehaus.groovy:groovy-jsr223:3.0.7")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // kotlin
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

    // local dependency of SpaceSpeak API. Cannot be published open source (yet).
    implementation(files("libs/SpaceSpeakAPI-jvm-1.0.0.jar"))
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    manifest {
        attributes["Main-Class"] = "me.qbosst.bossbot.Launcher"
    }
}