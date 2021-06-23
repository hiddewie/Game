plugins {
    id("kotlin-multiplatform-library")
    kotlin("plugin.serialization") version "1.5.0"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
            }
        }
    }
}
