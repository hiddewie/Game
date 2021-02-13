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
                    "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
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
            browser {
                testTask {
                    useKarma {
                        useFirefox()
                    }
                }
            }
        }
        binaries.executable()
    }
}