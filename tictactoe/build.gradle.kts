plugins {
    id("kotlin-jvm-library")
    kotlin("plugin.serialization") version "1.4.21"
}

dependencies {
    implementation(project(":core"))
}