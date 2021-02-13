plugins {
    id("kotlin-multiplatform-library")
    kotlin("plugin.serialization") version "1.4.21"
}

kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xuse-experimental=kotlin.time.ExperimentalTime",
                    "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
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