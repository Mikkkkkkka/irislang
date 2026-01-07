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

val projects = arrayOf("core", "parser", "compiler", "vm", "jit", "cli")

include(*projects.map {proj -> ":$proj"}.toTypedArray())

for (proj in projects) {
    project(":$proj").projectDir = file("modules/$proj")
}
