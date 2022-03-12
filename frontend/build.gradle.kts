plugins {
    kotlin("js")
    kotlin("plugin.serialization") version "1.5.0"
}

group = "nl.hiddewieringa.game"
version = "0.0.1-SNAPSHOT"

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    mavenCentral()
    jcenter()
}

kotlin {
    js(IR) {
        browser {
            webpackTask {
                args.plusAssign(listOf(
                    "--env", "host=''"
                ))
                webpackConfigApplier {
                    export = false
                }
            }
            runTask {
                args.plusAssign(listOf(
                    "--env", "host='http://localhost:8081'"
                ))
                webpackConfigApplier {
                    export = false
                }
            }
        }
        binaries.executable()
    }
}

dependencies {

    implementation(kotlin("stdlib-js"))

    //React, React DOM + Wrappers (chapter 3)
    implementation("org.jetbrains:kotlin-react:17.0.1-pre.148-kotlin-1.4.21")
    implementation("org.jetbrains:kotlin-react-dom:17.0.1-pre.148-kotlin-1.4.21")
    implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-pre.148-kotlin-1.4.21")
    implementation(npm("react", "17.0.1"))
    implementation(npm("react-dom", "17.0.1"))
    implementation(npm("react-router", "5.2.0"))
    implementation(npm("react-router-dom", "5.2.0"))
    implementation(npm("react-scripts", "4.0.1"))

    implementation("org.jetbrains:kotlin-styled:5.2.1-pre.148-kotlin-1.4.21")
    implementation(npm("styled-components", "5.2.1"))
    implementation(npm("inline-style-prefixer", "~6.0.0"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    // Games
    implementation(project(":taipan"))
    implementation(project(":tictactoe"))
}
