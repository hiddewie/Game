plugins {
    id("kotlin-common")
    kotlin("multiplatform")
}

repositories {
    jcenter()
}

kotlin {
    jvm()
    js(IR) {
        browser {
            binaries.executable()
        }
    }
}