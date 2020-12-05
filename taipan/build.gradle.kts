import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
}

repositories {
    jcenter()
}

dependencies {
    implementation(project(":core"))

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "14"
        freeCompilerArgs += listOf(
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlin.time.ExperimentalTime",
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
        )
    }
}
