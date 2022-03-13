import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("js")
    kotlin("plugin.serialization") version "1.6.10"
}

group = "nl.hiddewieringa.game"
version = "0.0.1-SNAPSHOT"

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    mavenCentral()
    jcenter()
}

// Workaround for broken Webpack-serve CLI, see https://youtrack.jetbrains.com/issue/KT-49124
rootProject.extensions.configure<NodeJsRootExtension> {
    versions.webpackCli.version = "4.9.0"
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
    implementation("org.jetbrains:kotlin-react:17.0.2-pre.155-kotlin-1.5.0")
    implementation("org.jetbrains:kotlin-react-dom:17.0.2-pre.154-kotlin-1.5.0")
    implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-pre.154-kotlin-1.5.0")
    implementation(npm("react", "17.0.2"))
    implementation(npm("react-dom", "17.0.2"))
    implementation(npm("react-router", "5.2.0"))
    implementation(npm("react-router-dom", "5.2.0"))
    implementation(npm("react-scripts", "4.0.1"))

    implementation("org.jetbrains:kotlin-styled:5.2.3-pre.154-kotlin-1.5.0")
    implementation(npm("styled-components", "5.2.3"))
    implementation(npm("inline-style-prefixer", "~6.0.0"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // Games
    implementation(project(":taipan"))
    implementation(project(":tictactoe"))
}
