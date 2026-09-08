pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mi-salud-y-fuerza"

// Solo el modulo de dominio puro (Kotlin/JVM). La app Android vive en ./android
// como build independiente (ver android/settings.gradle.kts) porque requiere el
// Android SDK y los artefactos de dl.google.com.
include(":core")
