plugins {
    kotlin("jvm") version "2.2.21" apply false
}

allprojects {
    group = "dev.iris"
    version = "0.1.0"
}

subprojects {
    repositories {
        mavenCentral()
    }
}
