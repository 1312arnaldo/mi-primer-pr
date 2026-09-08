pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mi-salud-y-fuerza-android"

// Build compuesto: reutiliza el modulo :core del repositorio raiz sin publicarlo.
includeBuild("..") {
    dependencySubstitution {
        substitute(module("com.misaludyfuerza:core")).using(project(":core"))
    }
}

include(":app")
