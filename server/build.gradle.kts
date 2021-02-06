import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-jvm-application")

    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("plugin.spring") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
}

group = "nl.hiddewieringa.game"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":common"))
    implementation(project(":core"))
    implementation(project(":taipan"))
    implementation(project(":tictactoe"))

//    implementation("org.springframework.boot:spring-boot-starter-rsocket")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
//    implementation("org.springframework.boot:spring-boot-starter-websocket")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

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
