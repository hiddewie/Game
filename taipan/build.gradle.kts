plugins {
    id("kotlin-multiplatform-library")
    kotlin("plugin.serialization") version "1.6.10"
}

kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xopt-in=kotlin.time.ExperimentalTime",
                    "-Xopt-in=kotlin.ExperimentalStdlibApi"
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
            }
        }
    }
}