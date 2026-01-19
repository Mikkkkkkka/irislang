plugins { kotlin("jvm") }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core"))
    implementation(project(":parser"))
    testImplementation(project(":vm"))
    testImplementation(kotlin("test"))
}
