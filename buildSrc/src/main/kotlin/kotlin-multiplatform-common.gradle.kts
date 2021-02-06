plugins {
    id("kotlin-common")
    kotlin("multiplatform")
}

repositories {
    jcenter()
}

dependencies {
//    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

kotlin {
    jvm()
    js {
        browser()
    }
}