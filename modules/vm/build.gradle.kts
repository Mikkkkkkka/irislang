plugins { kotlin("jvm") }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
    testImplementation(project(":jit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.test {
    testLogging {
        // Показывать STDOUT и STDERR из тестов
        showStandardStreams = true

        // Показывать события тестов
        events("passed", "skipped", "failed")
    }
}
