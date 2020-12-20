import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
}

group = "me.boss"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("net.dv8tion:JDA:4.2.0_223")

    implementation("com.sedmelluq:lavaplayer:1.3.65")
    implementation("com.sedmelluq:jda-nas:1.1.0")
    implementation("se.michaelthelin.spotify:spotify-web-api-java:6.5.1")


    implementation("org.jetbrains.exposed:exposed-core:0.28.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.28.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.28.1")
    implementation("mysql:mysql-connector-java:8.0.22")

    implementation("com.jagrosh:jda-utilities-commons:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.reflections:reflections:0.9.12")
    implementation("org.json:json:20201115")
}



tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}