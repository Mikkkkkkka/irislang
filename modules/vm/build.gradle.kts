plugins { kotlin("jvm") }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    testLogging {
        // Показывать STDOUT и STDERR из тестов
        showStandardStreams = true

        // Показывать события тестов
        events("passed", "skipped", "failed")
    }
}
