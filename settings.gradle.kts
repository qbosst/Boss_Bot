pluginManagement {
    repositories {
        google()  // Google's KSP plugin is still beta
        gradlePluginPortal()
    }

    plugins {
        id("com.google.devtools.ksp") version "1.5.10-1.0.0-beta02"
    }
}

include("discord")