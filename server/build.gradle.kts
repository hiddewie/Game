import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-jvm-application")

    id("org.springframework.boot") version "2.5.2"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
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

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Postgres events
    implementation("io.vertx:vertx-pg-client:4.2.5")
    runtimeOnly("org.postgresql:postgresql")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.12.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjsr305=strict",
            "-Xjvm-default=all"
        )
    }
}

tasks.withType<JavaExec> {
    environment("SPRING_PROFILES_ACTIVE", "local")
}

tasks.create<Copy>("copyCompiledFrontend") {
    dependsOn(":frontend:build")

    from(project(":frontend").file("build/distributions"))
    into(buildDir.resolve("resources/main/public"))
}
tasks.processResources {
//    dependsOn("copyCompiledFrontend")
}

application {
    mainClass.set("nl.hiddewieringa.game.server.GameServerApplicationKt")
}

jib {
    from.image = "gcr.io/distroless/java11-debian11"

    container {
        jvmFlags = listOf(
            "-Xss512k",
            "-Xmx400m",
            "-Xms400m",
            "-XX:+UseContainerSupport"
        )
    }
}