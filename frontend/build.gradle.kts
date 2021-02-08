plugins {
    kotlin("js")
    kotlin("plugin.serialization") version "1.4.21"
}

group = "nl.hiddewieringa.game"
version = "0.0.1-SNAPSHOT"

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    mavenCentral()
    jcenter()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
}

dependencies {
    implementation(project(":common"))

    implementation(kotlin("stdlib-js"))

    //React, React DOM + Wrappers (chapter 3)
    implementation("org.jetbrains:kotlin-react:17.0.0-pre.134-kotlin-1.4.10")
    implementation("org.jetbrains:kotlin-react-dom:17.0.0-pre.134-kotlin-1.4.10")
    implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-pre.134-kotlin-1.4.10")
    implementation(npm("react", "17.0.0"))
    implementation(npm("react-dom", "17.0.0"))
    implementation(npm("react-router", "^5.2.0"))
    implementation(npm("react-router-dom", "^5.2.0"))
    implementation(npm("react-scripts", "4.0.1"))

    //Kotlin Styled (chapter 3)
    implementation("org.jetbrains:kotlin-styled:5.2.0-pre.134-kotlin-1.4.10")
    implementation(npm("styled-components", "~5.2.0"))
    implementation(npm("inline-style-prefixer", "~6.0.0"))

    //Video Player (chapter 7)
//    implementation(npm("react-player", "~2.6.0"))

    //Share Buttons (chapter 7)
//    implementation(npm("react-share", "~4.2.1"))

    //Coroutines (chapter 8)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    // Games
//    implementation(project(":taipan"))
//    implementation(project(":tictactoe"))
}
