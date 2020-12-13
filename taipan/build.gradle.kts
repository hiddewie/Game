import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-library")
}

dependencies {
    implementation(project(":core"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xuse-experimental=kotlin.time.ExperimentalTime",
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
        )
    }
}