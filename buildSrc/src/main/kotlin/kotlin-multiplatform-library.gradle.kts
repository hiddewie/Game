import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("kotlin-multiplatform-common")
}

repositories {
    jcenter()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
            }
        }

        val commonTest by getting {
            dependencies {
                // Testing
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":test-support"))
            }
        }

        val jvmTest by getting {
            dependencies {
                // Testing
                implementation(kotlin("test-junit"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
            }
        }

        val jsTest by getting {
            dependencies {
                // Testing
                implementation(kotlin("test-js"))
            }
        }
    }
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