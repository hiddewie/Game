pluginManagement {
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "game"

include(
    // Core infrastructure
    "core",

    // Server
    "server",

    // Frontend
    "frontend",

    // Game implementations
    "tictactoe",
    "taipan"
)