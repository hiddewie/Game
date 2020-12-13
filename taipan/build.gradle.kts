import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("common-kotlin")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xuse-experimental=kotlin.time.ExperimentalTime",
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
        )
    }
}