plugins {
    id("kotlin-common")
    kotlin("multiplatform")
}

repositories {
    jcenter()
}

kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-Xopt-in=kotlinx.coroutines.ObsoleteCoroutinesApi"
                )
            }
        }
    }
    jvm().compilations.all {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
    js(IR) {
        browser {
            testTask {
                useMocha()
            }
        }
        binaries.executable()
    }
}