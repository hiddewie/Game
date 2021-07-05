import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-jvm-application")

    id("org.springframework.boot") version "2.5.2"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("plugin.spring") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
    id("com.google.cloud.tools.jib") version "3.1.1"
}

group = "nl.hiddewieringa.game"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":core"))
    implementation(project(":taipan"))
    implementation(project(":tictactoe"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.12.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjsr305=strict"
        )
    }
}

tasks.create<Copy>("copyCompiledFrontend") {
    dependsOn(":frontend:build")

    from(project(":frontend").file("build/distributions"))
    into(buildDir.resolve("resources/main/public"))
}
tasks.processResources {
    dependsOn("copyCompiledFrontend")
}

application {
    mainClass.set("nl.hiddewieringa.game.server.GameServerApplicationKt")
}
