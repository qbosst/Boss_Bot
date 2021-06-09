plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.10"
    id("com.github.johnrengelman.shadow") version("6.0.0")
}

group = "me.boss"
version = "1.0.0"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    val exposedVer = "0.31.1"

    // kord
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.4.1-20210607.165810-11")

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
    implementation(files("libs/SpaceSpeakAPI-jvm-1.0.1.jar"))

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "9"
        freeCompilerArgs = listOf(
            "-Xinline-classes",
            "-Xopt-in=dev.kord.common.annotation.KordPreview",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}
val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    manifest {
        attributes["Main-Class"] = "me.qbosst.bossbot.Launcher"
    }
}
