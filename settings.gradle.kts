rootProject.name = "iris"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    ":core",
    ":parser",
    ":compiler",
    ":vm",
    ":jit",
    ":cli",
)
