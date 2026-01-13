plugins {
    kotlin("jvm")
    application
}

kotlin { jvmToolchain(21) }

application {
    mainClass = "dev.iris.cli.MainKt"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":parser"))
    implementation(project(":compiler"))
    implementation(project(":vm"))
    implementation(project(":jit"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
