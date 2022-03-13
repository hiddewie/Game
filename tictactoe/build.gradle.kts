plugins {
    id("kotlin-multiplatform-library")
    kotlin("plugin.serialization") version "1.6.10"
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
